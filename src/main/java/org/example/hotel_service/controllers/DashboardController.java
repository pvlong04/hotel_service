package org.example.hotel_service.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.response.DashboardOverviewResponse;
import org.example.hotel_service.services.dashboard.DashboardServiceImp;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    DashboardServiceImp dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @AuthenticationPrincipal Jwt jwt
    ) {
        DashboardOverviewResponse result = dashboardService.getOverview(jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu dashboard thành công", result));
    }
}

