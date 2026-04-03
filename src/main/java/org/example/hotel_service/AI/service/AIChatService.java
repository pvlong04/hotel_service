package org.example.hotel_service.AI.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.hotel_service.AI.dtos.response.AIResponse;
import org.example.hotel_service.entities.*;
import org.example.hotel_service.enums.RoomStatus;
import org.example.hotel_service.repositories.AmenityRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.example.hotel_service.repositories.RoomTypeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIChatService implements AIChatServiceImp {
    static final Pattern MODEL_NOT_FOUND_PATTERN = Pattern.compile("model '([^']+)' not found", Pattern.CASE_INSENSITIVE);
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static final String STATUS_OK = "OK";
    static final String STATUS_FALLBACK = "FALLBACK";
    static final String STATUS_ERROR = "ERROR";

    // Keywords that indicate a room-related question
    static final List<String> ROOM_KEYWORDS = List.of(
            "phòng", "phong", "room", "giá", "gia", "price", "đặt", "dat", "book",
            "suite", "deluxe", "standard", "superior", "loại", "loai", "type",
            "giường", "giuong", "bed", "tầng", "tang", "floor",
            "sức chứa", "suc chua", "capacity", "người", "nguoi",
            "còn trống", "con trong", "available", "sẵn", "san",
            "rẻ", "re", "tiết kiệm", "tiet kiem", "cheap",
            "đắt", "dat", "cao cấp", "cao cap", "luxury",
            "view", "biển", "bien", "thành phố", "thanh pho"
    );

    ChatClient chatClient;
    RoomRepository roomRepository;
    RoomTypeRepository roomTypeRepository;
    AmenityRepository amenityRepository;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    @NonFinal
    String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:gemma3:1b}")
    @NonFinal
    String configuredModel;

    // Constructor injection (replaces @RequiredArgsConstructor because we have @NonFinal + @Value fields)
    public AIChatService(ChatClient chatClient,
                         RoomRepository roomRepository,
                         RoomTypeRepository roomTypeRepository,
                         AmenityRepository amenityRepository) {
        this.chatClient = chatClient;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.amenityRepository = amenityRepository;
    }

    // ── In-memory conversation history ──
    static final Map<String, List<ConversationMessage>> conversationHistory = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<ConversationMessage>> eldest) {
                    return size() > MAX_SESSIONS;
                }
            }
    );
    static final int MAX_HISTORY_SIZE = 20;
    static final int MAX_SESSIONS = 500;

    // ════════════════════════════════════════════════════════════════════════
    //  Main chat entry-point
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public AIResponse chat(String message, String sessionId) {
        String normalizedMessage = message == null ? "" : message.trim();
        String normalizedSessionId = (sessionId == null || sessionId.isBlank()) ? null : sessionId.trim();

        // 1) Build a dynamic system prompt that includes real hotel data
        String systemPrompt = buildDynamicSystemPrompt();

        // 2) Build the user prompt including conversation history
        String contextualPrompt = buildContextualPrompt(normalizedMessage, normalizedSessionId);

        // 3) Detect whether this is a room-related query
        boolean isRoomQuery = isRoomRelatedQuery(normalizedMessage);

        // 4) If room-related, gather relevant rooms
        List<AIResponse.RoomSuggestion> roomSuggestions = isRoomQuery
                ? findRelevantRooms(normalizedMessage)
                : null;

        // 5) If we have rooms, add them to the user prompt so AI knows about them
        if (roomSuggestions != null && !roomSuggestions.isEmpty()) {
            contextualPrompt = appendRoomContext(contextualPrompt, roomSuggestions);
        }

        // Quick connectivity check
        if (!isOllamaReachable()) {
            log.warn("Ollama is not reachable at {}", ollamaBaseUrl);
            return AIResponse.builder()
                    .answer("Xin lỗi, dịch vụ AI đang tạm thời không khả dụng. Ollama chưa được khởi động. " +
                            "Vui lòng liên hệ lễ tân để được hỗ trợ trực tiếp. 🙏")
                    .sessionId(normalizedSessionId)
                    .status(STATUS_ERROR)
                    .modelUsed(configuredModel)
                    .warning("Ollama server không hoạt động")
                    .suggestedRooms(roomSuggestions)
                    .build();
        }

        try {
            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(contextualPrompt)
                    .call()
                    .content();

            // Save to history
            if (normalizedSessionId != null) {
                saveToHistory(normalizedSessionId, normalizedMessage, answer);
            }

            return AIResponse.builder()
                    .answer(answer)
                    .sessionId(normalizedSessionId)
                    .status(STATUS_OK)
                    .modelUsed(configuredModel)
                    .suggestedRooms(roomSuggestions)
                    .build();
        } catch (NonTransientAiException e) {
            return handleNonTransientError(e, contextualPrompt, systemPrompt,
                    normalizedMessage, normalizedSessionId, roomSuggestions);
        } catch (Exception e) {
            return handleGenericError(e, normalizedSessionId, roomSuggestions);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Dynamic system prompt — built from REAL database data
    // ════════════════════════════════════════════════════════════════════════
    private String buildDynamicSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Bạn là trợ lý ảo thông minh của khách sạn Luxury Hotel & Resort.

                Nhiệm vụ của bạn:
                - Hỗ trợ khách hàng về thông tin phòng, giá cả, tiện ích và dịch vụ của khách sạn.
                - Hướng dẫn quy trình đặt phòng, thanh toán, check-in/check-out.
                - Giải đáp các câu hỏi về chính sách hủy phòng, đổi phòng.
                - Gợi ý các phòng phù hợp dựa trên nhu cầu khách hàng.
                - Khi khách hỏi về phòng, hãy sử dụng THÔNG TIN PHÒNG THỰC TẾ bên dưới để trả lời chính xác.

                Thông tin chung khách sạn:
                - Tên: Luxury Hotel & Resort
                - Địa chỉ: TP. Hồ Chí Minh, Việt Nam
                - Tiêu chuẩn: 5 sao quốc tế
                - Giờ check-in: 14:00 | Giờ check-out: 12:00
                - Thanh toán: VNPay, Chuyển khoản, Tiền mặt
                - Chính sách hủy: Miễn phí trước 24h, phí 50% trong vòng 24h

                """);

        // ── Inject real room type data ──
        try {
            List<RoomType> roomTypes = roomTypeRepository.findAll();
            if (!roomTypes.isEmpty()) {
                sb.append("=== LOẠI PHÒNG HIỆN CÓ (DỮ LIỆU THỰC) ===\n");
                NumberFormat vnFormat = NumberFormat.getInstance(Locale.of("vi", "VN"));
                for (RoomType rt : roomTypes) {
                    sb.append(String.format("• %s (%s): %s VND/đêm",
                            rt.getName(), rt.getCode(), vnFormat.format(rt.getPricePerNight())));
                    if (rt.getWeekendPrice() != null) {
                        sb.append(String.format(", cuối tuần %s VND", vnFormat.format(rt.getWeekendPrice())));
                    }
                    sb.append(String.format(" | Sức chứa: %d NL + %d TE | Giường: %s × %d",
                            rt.getMaxAdults(), rt.getMaxChildren(),
                            rt.getBedType() != null ? rt.getBedType().name() : "N/A",
                            rt.getBedCount()));
                    if (rt.getRoomSize() != null) {
                        sb.append(String.format(" | %sm²", rt.getRoomSize()));
                    }
                    sb.append(String.format(" | Tổng: %d phòng, Trống: %d phòng",
                            rt.getTotalRooms(), rt.getAvailableRooms()));
                    if (rt.getDescription() != null && !rt.getDescription().isBlank()) {
                        String desc = rt.getDescription().length() > 100 ? rt.getDescription().substring(0, 100) + "..." : rt.getDescription();
                        sb.append(" | Mô tả: ").append(desc);
                    }
                    sb.append("\n");

                    // Amenities for this room type (lazy load is OK inside @Transactional)
                    List<Amenity> amenities = rt.getAmenities();
                    if (amenities != null && !amenities.isEmpty()) {
                        String amenityNames = amenities.stream()
                                .map(Amenity::getName)
                                .collect(Collectors.joining(", "));
                        sb.append("  Tiện nghi: ").append(amenityNames).append("\n");
                    }
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("Could not load room types for AI context: {}", e.getMessage());
        }

        // ── Inject general amenities ──
        try {
            List<Amenity> allAmenities = amenityRepository.findAll();
            if (!allAmenities.isEmpty()) {
                Map<String, List<String>> grouped = allAmenities.stream()
                        .collect(Collectors.groupingBy(
                                a -> a.getCategory() != null ? a.getCategory().name() : "OTHER",
                                Collectors.mapping(Amenity::getName, Collectors.toList())));
                sb.append("=== TIỆN NGHI KHÁCH SẠN ===\n");
                grouped.forEach((category, names) ->
                        sb.append("• ").append(category).append(": ").append(String.join(", ", names)).append("\n"));
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("Could not load amenities for AI context: {}", e.getMessage());
        }

        sb.append("""
                Quy tắc trả lời:
                - LUÔN trả lời bằng tiếng Việt, ngắn gọn, lịch sự và chuyên nghiệp.
                - SỬ DỤNG DỮ LIỆU THỰC ở trên để trả lời chính xác về giá, loại phòng, tiện nghi.
                - Khi khách hỏi gợi ý phòng, hãy TRẢ LỜI DỰA TRÊN THÔNG TIN PHÒNG ĐƯỢC CUNG CẤP trong câu hỏi.
                - Nếu không biết thông tin cụ thể, gợi ý liên hệ lễ tân: 1900-xxxx.
                - Sử dụng emoji phù hợp.
                - Giữ câu trả lời dưới 300 từ.
                """);

        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Detect & retrieve relevant rooms
    // ════════════════════════════════════════════════════════════════════════
    private boolean isRoomRelatedQuery(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return ROOM_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Find up to 5 rooms that are most relevant to the user's message.
     * Strategy: keyword matching on room type name, bed type, price intent, status.
     */
    private List<AIResponse.RoomSuggestion> findRelevantRooms(String message) {
        try {
            String lower = message.toLowerCase();

            // Fetch all available rooms (with details) — capped to reasonable size
            List<Room> allRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE, PageRequest.of(0, 100)).getContent();

            if (allRooms.isEmpty()) {
                return Collections.emptyList();
            }

            // Score each room by keyword relevance
            List<ScoredRoom> scored = new ArrayList<>();
            for (Room room : allRooms) {
                int score = 0;
                RoomType rt = room.getRoomType();
                if (rt == null) continue;

                String typeName = rt.getName() != null ? rt.getName().toLowerCase() : "";
                String typeCode = rt.getCode() != null ? rt.getCode().toLowerCase() : "";

                // Match room type name/code
                if (lower.contains(typeName) || lower.contains(typeCode)) score += 10;
                if (lower.contains("standard") && typeName.contains("standard")) score += 5;
                if (lower.contains("deluxe") && typeName.contains("deluxe")) score += 5;
                if (lower.contains("suite") && typeName.contains("suite")) score += 5;
                if (lower.contains("superior") && typeName.contains("superior")) score += 5;

                // Price intent
                if ((lower.contains("rẻ") || lower.contains("re") || lower.contains("tiết kiệm") || lower.contains("cheap"))
                        && rt.getPricePerNight() != null) {
                    // Lower price → higher score
                    score += Math.max(0, 10 - (int) (rt.getPricePerNight() / 500000));
                }
                if ((lower.contains("đắt") || lower.contains("cao cấp") || lower.contains("luxury") || lower.contains("sang"))
                        && rt.getPricePerNight() != null) {
                    // Higher price → higher score
                    score += (int) (rt.getPricePerNight() / 500000);
                }

                // Bed type matching
                if (rt.getBedType() != null) {
                    String bedLower = rt.getBedType().name().toLowerCase();
                    if (lower.contains(bedLower)) score += 5;
                    if (lower.contains("đôi") && (bedLower.equals("double") || bedLower.equals("king") || bedLower.equals("queen")))
                        score += 3;
                    if (lower.contains("đơn") && (bedLower.equals("single") || bedLower.equals("twin")))
                        score += 3;
                }

                // Capacity matching
                if (lower.contains("gia đình") || lower.contains("gia dinh") || lower.contains("family")) {
                    if (rt.getMaxAdults() >= 3 || rt.getMaxChildren() > 0) score += 5;
                }
                if (lower.contains("cặp đôi") || lower.contains("couple") || lower.contains("2 người")) {
                    if (rt.getMaxAdults() == 2) score += 3;
                }

                // Note matching (e.g. "view biển")
                if (room.getNote() != null && !room.getNote().isBlank()) {
                    String noteLower = room.getNote().toLowerCase();
                    if (lower.contains("view") && noteLower.contains("view")) score += 5;
                    if (lower.contains("biển") && noteLower.contains("biển")) score += 5;
                }

                // Available rooms always get a baseline score
                score += 1;

                scored.add(new ScoredRoom(room, score));
            }

            // Sort by score descending, take top 5
            return scored.stream()
                    .sorted(Comparator.comparingInt(ScoredRoom::score).reversed())
                    .limit(5)
                    .map(sr -> mapToRoomSuggestion(sr.room()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Could not fetch relevant rooms: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private record ScoredRoom(Room room, int score) {}

    private AIResponse.RoomSuggestion mapToRoomSuggestion(Room room) {
        RoomType rt = room.getRoomType();
        Floor floor = room.getFloor();

        // Resolve primary image URL
        String imageUrl = null;
        List<RoomImage> roomImages = room.getImages();
        if (roomImages != null && !roomImages.isEmpty()) {
            imageUrl = roomImages.stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(RoomImage::getUrl)
                    .orElse(roomImages.get(0).getUrl());
        } else if (rt != null) {
            // Fall back to room type images
            List<RoomTypeImage> rtImages = rt.getImages();
            if (rtImages != null && !rtImages.isEmpty()) {
                imageUrl = rtImages.stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .findFirst()
                        .map(RoomTypeImage::getUrl)
                        .orElse(rtImages.get(0).getUrl());
            }
        }

        return AIResponse.RoomSuggestion.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getRoomNumber())
                .roomTypeName(rt != null ? rt.getName() : null)
                .floorName(floor != null ? (floor.getName() != null ? floor.getName() : floor.getCode()) : null)
                .status(room.getStatus() != null ? room.getStatus().name() : null)
                .pricePerNight(rt != null ? rt.getPricePerNight() : null)
                .weekendPrice(rt != null ? rt.getWeekendPrice() : null)
                .bedType(rt != null && rt.getBedType() != null ? rt.getBedType().name() : null)
                .bedCount(rt != null ? rt.getBedCount() : null)
                .maxAdults(rt != null ? rt.getMaxAdults() : null)
                .maxChildren(rt != null ? rt.getMaxChildren() : null)
                .roomSize(rt != null && rt.getRoomSize() != null ? rt.getRoomSize().toPlainString() : null)
                .primaryImageUrl(imageUrl)
                .note(room.getNote())
                .build();
    }

    /**
     * Append a concise room-data section to the user prompt so the LLM can reference specific rooms.
     */
    private String appendRoomContext(String prompt, List<AIResponse.RoomSuggestion> rooms) {
        StringBuilder sb = new StringBuilder(prompt);
        sb.append("\n\n--- PHÒNG LIÊN QUAN (hệ thống đã tìm được, hãy đề cập đến chúng trong câu trả lời) ---\n");
        NumberFormat vnFormat = NumberFormat.getInstance(Locale.of("vi", "VN"));
        for (AIResponse.RoomSuggestion r : rooms) {
            sb.append(String.format("• Phòng %s — %s | %s VND/đêm | Giường: %s×%d | %d NL + %d TE",
                    r.getRoomNumber(),
                    r.getRoomTypeName() != null ? r.getRoomTypeName() : "N/A",
                    r.getPricePerNight() != null ? vnFormat.format(r.getPricePerNight()) : "N/A",
                    r.getBedType() != null ? r.getBedType() : "N/A",
                    r.getBedCount() != null ? r.getBedCount() : 1,
                    r.getMaxAdults() != null ? r.getMaxAdults() : 0,
                    r.getMaxChildren() != null ? r.getMaxChildren() : 0));
            if (r.getFloorName() != null) sb.append(" | Tầng ").append(r.getFloorName());
            if (r.getNote() != null) sb.append(" | ").append(r.getNote());
            sb.append("\n");
        }
        sb.append("---\n");
        sb.append("Hãy giới thiệu các phòng phù hợp ở trên cho khách trong câu trả lời. " +
                "Nếu khách muốn đặt phòng, hướng dẫn họ nhấn vào phòng ở danh sách bên dưới.\n");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Conversation history
    // ════════════════════════════════════════════════════════════════════════
    private String buildContextualPrompt(String currentMessage, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return currentMessage;
        }
        List<ConversationMessage> history = conversationHistory.get(sessionId);
        if (history == null || history.isEmpty()) {
            return currentMessage;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Lịch sử trò chuyện trước đó:\n");
        for (ConversationMessage msg : history) {
            sb.append("Khách: ").append(msg.userMessage()).append("\n");
            sb.append("Trợ lý: ").append(msg.assistantMessage()).append("\n\n");
        }
        sb.append("Câu hỏi hiện tại của khách: ").append(currentMessage);
        return sb.toString();
    }

    private void saveToHistory(String sessionId, String userMessage, String assistantMessage) {
        conversationHistory.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        List<ConversationMessage> history = conversationHistory.get(sessionId);
        history.add(new ConversationMessage(userMessage, assistantMessage));
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Error handling
    // ════════════════════════════════════════════════════════════════════════
    private AIResponse handleNonTransientError(NonTransientAiException e, String contextualPrompt,
                                                String systemPrompt, String normalizedMessage,
                                                String normalizedSessionId,
                                                List<AIResponse.RoomSuggestion> roomSuggestions) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : "";
        String missingModel = extractMissingModel(errorMessage);
        if (missingModel != null) {
            log.warn("Ollama model not found: {}", missingModel);
            String fallbackModel = findFallbackModel(missingModel);
            if (fallbackModel != null) {
                try {
                    String fallbackAnswer = callOllamaDirect(fallbackModel, contextualPrompt, systemPrompt);
                    if (normalizedSessionId != null) {
                        saveToHistory(normalizedSessionId, normalizedMessage, fallbackAnswer);
                    }
                    return AIResponse.builder()
                            .answer(fallbackAnswer)
                            .sessionId(normalizedSessionId)
                            .status(STATUS_FALLBACK)
                            .modelUsed(fallbackModel)
                            .warning("Model mặc định không tồn tại, đã chuyển sang model " + fallbackModel)
                            .suggestedRooms(roomSuggestions)
                            .build();
                } catch (Exception fallbackEx) {
                    log.warn("Fallback model {} failed: {}", fallbackModel, fallbackEx.getMessage());
                }
            }

            return AIResponse.builder()
                    .answer("Model AI '" + missingModel + "' chưa được cài đặt. " +
                            "Vui lòng chạy: ollama pull " + missingModel + " 🙏")
                    .sessionId(normalizedSessionId)
                    .status(STATUS_ERROR)
                    .modelUsed(missingModel)
                    .warning("Model chưa được cài đặt")
                    .suggestedRooms(roomSuggestions)
                    .build();
        }

        log.warn("AI provider returned non-retryable error: {}", errorMessage);
        return AIResponse.builder()
                .answer("Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau. 🙏")
                .sessionId(normalizedSessionId)
                .status(STATUS_ERROR)
                .modelUsed(configuredModel)
                .warning("Lỗi từ nhà cung cấp AI")
                .suggestedRooms(roomSuggestions)
                .build();
    }

    private AIResponse handleGenericError(Exception e, String normalizedSessionId,
                                           List<AIResponse.RoomSuggestion> roomSuggestions) {
        log.error("AI Chat error: {}", e.getMessage(), e);

        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConnectException) {
                return AIResponse.builder()
                        .answer("Không thể kết nối đến dịch vụ AI. Vui lòng đảm bảo Ollama đang chạy. 🙏")
                        .sessionId(normalizedSessionId)
                        .status(STATUS_ERROR)
                        .modelUsed(configuredModel)
                        .warning("Không thể kết nối Ollama")
                        .suggestedRooms(roomSuggestions)
                        .build();
            }
            cause = cause.getCause();
        }

        return AIResponse.builder()
                .answer("Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ lễ tân. 🙏")
                .sessionId(normalizedSessionId)
                .status(STATUS_ERROR)
                .modelUsed(configuredModel)
                .warning("Lỗi hệ thống AI")
                .suggestedRooms(roomSuggestions)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Ollama helpers (connectivity, fallback, direct call)
    // ════════════════════════════════════════════════════════════════════════
    private boolean isOllamaReachable() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractMissingModel(String message) {
        if (message == null || message.isBlank()) return null;
        Matcher matcher = MODEL_NOT_FOUND_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findFallbackModel(String missingModel) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;

            JsonNode modelsNode = OBJECT_MAPPER.readTree(response.body()).path("models");
            if (!modelsNode.isArray() || modelsNode.isEmpty()) return null;

            List<String> candidates = new ArrayList<>();
            for (JsonNode node : modelsNode) {
                String name = node.path("name").asText(null);
                if (name != null && !name.isBlank()) candidates.add(name);
            }
            if (candidates.isEmpty()) return null;

            String familyPrefix = missingModel.contains(":") ? missingModel.substring(0, missingModel.indexOf(':')) : missingModel;
            for (String c : candidates) {
                if (c.equalsIgnoreCase(missingModel) || c.startsWith(familyPrefix + ":")) return c;
            }
            return candidates.get(0);
        } catch (Exception ex) {
            log.warn("Unable to query Ollama model list: {}", ex.getMessage());
            return null;
        }
    }

    private String callOllamaDirect(String model, String userPrompt, String systemPrompt) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("stream", false);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl() + "/api/chat"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama chat failed with status " + response.statusCode());
        }

        String answer = OBJECT_MAPPER.readTree(response.body()).path("message").path("content").asText(null);
        if (answer == null || answer.isBlank()) throw new IllegalStateException("Ollama returned empty answer");
        return answer;
    }

    private String normalizeBaseUrl() {
        String value = ollamaBaseUrl == null ? "http://localhost:11434" : ollamaBaseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    record ConversationMessage(String userMessage, String assistantMessage) {}
}
