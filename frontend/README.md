# Hotel Management UI (Frontend)

Frontend quản lý khách sạn dùng để thao tác trực quan với backend APIs của dự án `hotel_service`.

## Công nghệ

- ReactJS
- JavaScript
- Vite
- Tailwind CSS

## Tính năng giao diện

- Đăng nhập và lưu bearer token để gọi API cần xác thực
- Dashboard thống kê nhanh dữ liệu đã tải
- Quản lý khách sạn (tìm kiếm khách sạn)
- Quản lý phòng (lọc danh sách phòng theo hotel/status)
- Quản lý đặt phòng (kiểm tra phòng trống, xem đơn của tôi, tạo reservation)
- Quản lý người dùng (lọc danh sách users)

## Chạy local

```bash
npm install
npm run dev
```

Mặc định frontend gọi backend tại `http://localhost:9000`.

Nếu cần đổi, tạo file `.env`:

```bash
VITE_API_BASE_URL=http://localhost:9000
```

## Build

```bash
npm run build
```
