# AloTra Website — Đồ án Lập trình Web

Một ứng dụng web được xây dựng cho môn Đồ án Lập trình Web, tập trung vào trải nghiệm người dùng thân thiện, dễ sử dụng và dễ mở rộng.

![Status](https://img.shields.io/badge/status-active-success)
![Made with Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=javascript&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)

---

## Tổng quan

AloTra Website là ứng dụng web Java kết hợp giao diện HTML/CSS/JS, xây dựng theo định hướng “đúng — đủ — dễ hiểu”. Dự án áp dụng kiến trúc phân lớp rõ ràng, tuân thủ tiêu chuẩn clean code, tài liệu hóa các bước cài đặt/chạy để đảm bảo có thể thẩm định nhanh.

- Mục tiêu: hoàn thiện luồng chức năng cốt lõi, đảm bảo chất lượng mã và khả năng mở rộng.
- Phạm vi: giao diện web, xử lý nghiệp vụ cơ bản, thao tác dữ liệu (CRUD), tìm kiếm/lọc, định hướng phân quyền.

- Mã nguồn: [AloTra Website](https://github.com/diiego05/DoAnLapTrinhWeb_AloTra)
- Nhánh mặc định: `main`

---

## Thành phần công nghệ

Tỷ lệ ngôn ngữ (ước tính theo số dòng mã):

- Java: ~46.8%
- HTML: ~27.9%
- JavaScript: ~22.1%
- CSS: ~3.2%


Thư mục chính:
- `AloTraWebsite/` — thư mục dự án ứng dụng web
- `README.md` — tài liệu này

---

## Tính năng

- [x] Trang giao diện thân thiện, bố cục rõ ràng
- [x] Quản lý người dùng (đăng ký/đăng nhập/đăng xuất)
- [x] Quản lý nội dung/dữ liệu (CRUD)
- [x] Tìm kiếm/lọc dữ liệu
- [x] Phân quyền cơ bản (người dùng/quản trị)


---
## Kiến trúc và chất lượng mã

- Phân lớp rõ ràng (định hướng): Controller → Service → Repository (DAO)
- Tách bạch xử lý nghiệp vụ với giao diện
- Quy ước đặt tên có ý nghĩa, format code nhất quán
- Định hướng sử dụng DTO/Response thống nhất, dễ kiểm soát lỗi
- Dễ mở rộng: tích hợp đăng nhập/phân quyền, logging, cache, JPA…


## Sơ đồ dòng chảy (khái quát):

```
Client (HTML/CSS/JS)
       │
       ▼
Controller (REST/Web) → Validation/Mapping (DTO)
       │
       ▼
Service (Business Logic)
       │
       ▼
Repository/DAO (Data Access)
       │
       ▼
Database (SqlServer)
```

---
  
## Yêu cầu hệ thống

- Java Development Kit (JDK) 17 hoặc mới hơn
- Git
- Tùy chọn:
  - IDE (IntelliJ IDEA/Eclipse/VS Code) để phát triển
  - Spring Tool Suit

---

## Hướng dẫn chạy nhanh

Bạn có thể chạy theo cách sau.

### Chạy cục bộ bằng IDE

Dự án Java có thể dùng nhiều cách đóng gói/chạy khác nhau (Tomcat/Servlet, Spring Boot, v.v.), bạn có thể:

- Mở dự án trong IDE (IntelliJ/Eclipse/STS)
- Cấu hình SDK Java (JDK 17+)
- Nếu là Spring Boot: chạy class `main` (Application)
- Nếu là ứng dụng Servlet/Tomcat:
  - Cấu hình Application Server (Tomcat)
  - Deploy module web lên server và run


## Cấu trúc thư mục

```
/
├─ AloTraWebsite/
└─ README.md
```



## Liên hệ

- Vấn đề/trao đổi: tạo Issue trong repo hoặc liên hệ qua GitHub
## Thành viên nhóm 10
- Nguyễn Thành Tin
- Lâm Văn Dỉ
- Trác Ngọc Đăng Khoa
- Kinh Văn Việt
  
Xin cảm ơn thầy/cô đã dành thời gian xem xét dự án. Chúng em rất mong nhận được góp ý để tiếp tục hoàn thiện! 💚
