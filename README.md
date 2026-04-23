# 🏷️ Online Auction System

> Dự án Java OOP - Hệ thống đấu giá trực tuyến được xây dựng theo mô hình Multi-module, hướng đối
> tượng.

---

## 📝 1. Giới thiệu dự án

> *Mục tiêu, ý tưởng và đối tượng hướng đến của sàn đấu giá.*

---

## 🛠️ 2. Công nghệ sử dụng

| Công nghệ | Mục đích                           |
|-----------|------------------------------------|
| Java      | Ngôn ngữ lập trình chính           |
| JavaFX    | Xây dựng giao diện người dùng (UI) |
| MySQL     | Hệ quản trị cơ sở dữ liệu          |
| Maven     | Quản lý dependency & build tool    |
| BCrypt    | Mã hóa mật khẩu                    |
| JDBC      | Kết nối Java với cơ sở dữ liệu     |

---

## ✨ 3. Tính năng cốt lõi

- 🔐 **Xác thực người dùng** — Đăng ký, Đăng nhập, mã hóa mật khẩu bằng BCrypt
- 👤 **Hồ sơ người dùng** — Xem và chỉnh sửa thông tin cá nhân
- 🔨 **Đấu giá Real-time** — Cơ chế đặt giá, Anti-sniping, State Machine quản lý trạng thái phiên đấu
  giá
- 📦 **Quản lý kho đồ** — Quản lý danh sách sản phẩm (Item) theo phân cấp đối tượng
- 🛡️ **Phân quyền** — Cấu trúc phân quyền người dùng rõ ràng

---

## 👥 4. Phân công công việc

| Thành viên             | Vai trò                                      | Chi tiết công việc                                                                                                                                                                                                                                                                                                                           |
|------------------------|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Phan Gia Vinh**      | Team Leader / System Architect / Backend Dev | - Phát triển & tối ưu kiến trúc Multi-module, định hình luồng dữ liệu toàn hệ thống<br>- Thiết kế Rich Domain Model cho Auction (cơ chế Anti-sniping, State Machine)<br>- Xây dựng AuthService, mã hóa BCrypt, cấu trúc phân quyền & thực thể User<br>- Lắp ghép các module (UI–Service–DAO), quản trị mã nguồn & xử lý lỗi kỹ thuật cốt lõi |
| **Nguyễn Hà My**       | Frontend Developer                           | - Nghiên cứu & thiết kế UI/UX trên nền tảng JavaFX<br>- Lập trình các màn hình: Đăng ký, Đăng nhập, Hồ sơ người dùng<br>- Xử lý Event Handling, validate dữ liệu đầu vào & kết nối Controller với Backend Services                                                                                                                           |
| **Đỗ Tràng Toản**      | Backend Developer                            | - Thiết kế hệ thống phân cấp đối tượng cho Sản phẩm (Item Models)<br>- Xây dựng các lớp Service xử lý nghiệp vụ cho User & Item<br>- Chuẩn bị logic xử lý dữ liệu trung gian, đảm bảo tính nhất quán giữa Core và Database                                                                                                                   |
| **Nguyễn Hoàng Thông** | Database Developer / Network Dev             | - Lên ý tưởng & thiết lập cấu hình sơ khai bộ khung hệ thống Multi-module<br>- Phân tích & thiết kế lược đồ CSDL (ERD), viết SQL chuẩn hóa<br>- Xây dựng hệ thống DAO để tương tác an toàn với CSDL<br>- Thiết lập JDBC Connection, quản trị & vận hành Database trực tuyến                                                                  |

🔗 **Quản lý tiến độ chi tiết qua Trello**

---

## ⚙️ 5. Hướng dẫn cài đặt & Khởi chạy

### Yêu cầu hệ thống

- [Java version]
- [Maven version]
- [MySQL version]

### Các bước cài đặt

> *Đang cập nhật...*

> **Lưu ý:** Chạy lệnh `install` ở thư mục gốc trước khi run Client.
---

## 📸 6. Hình ảnh minh họa

> *Team chèn ảnh chụp màn hình UI đã hoàn thiện tại đây.*

---

## 📄 License

Dự án được phát triển cho mục đích học tập trong khuôn khổ môn **Lập trình nâng cao**.