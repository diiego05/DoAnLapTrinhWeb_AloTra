# 🗺️ Hệ thống Dynamic Menu từ Categories

## ✅ Đã hoàn thành

Hệ thống menu động giờ đây **KHÔNG sử dụng bảng riêng** mà **tự động build từ bảng `Categories`** có sẵn.

---

## 🔧 Các thay đổi đã thực hiện

### 1. ❌ Đã xóa
- ✅ `MenuItem.java` (entity)
- ✅ `MenuItemRepository.java` (repository)
- ✅ `MenuApiController.java` (controller cũ gây xung đột)
- ✅ `DynamicMenuController.java` (controller tạm thời)
- ✅ `DynamicMenuService.java` (service tạm thời)

### 2. ✅ Đã tạo mới
- ✅ `CategoryMenuService.java` - Service build menu từ Categories
- ✅ `CategoryMenuController.java` - API endpoint `/api/categories/menu`
- ✅ Thêm method `findAllOrderBySortOrderAsc()` vào `CategoryRepository`

### 3. 📝 Đã cập nhật
- ✅ `dynamic-menu.js` - Frontend gọi API mới `/api/categories/menu`
- ✅ `dynamic-menu.css` - Styles cho mega menu

---

## 🎯 Cách hoạt động

### Backend Flow:
```
Categories (Database)
    ↓
CategoryRepository.findAllOrderBySortOrderAsc()
    ↓
CategoryMenuService.buildMenu()
    ↓
CategoryMenuController (API: /api/categories/menu)
    ↓
Frontend (dynamic-menu.js)
```

### API Endpoint:
```
GET /alotra-website/api/categories/menu
```

**Response Example:**
```json
[
  {
    "id": 1,
    "title": "Trà Sữa",
    "url": "/alotra-website/products?category=tra-sua",
    "icon": "fas fa-mug-hot",
    "children": [
      {
        "id": 2,
        "title": "Trà Sữa Truyền Thống",
        "url": "/alotra-website/products?category=tra-sua-truyen-thong",
        "icon": "fas fa-mug-hot",
        "children": []
      }
    ]
  }
]
```

---

## 📊 Cấu trúc Categories

Để menu hiển thị đúng, bảng `Categories` cần có:

| Column | Type | Mô tả |
|--------|------|-------|
| `Id` | BIGINT | Primary key |
| `Name` | NVARCHAR(255) | Tên hiển thị trên menu |
| `Slug` | NVARCHAR(255) | URL-friendly name |
| `SortOrder` | INT | Thứ tự hiển thị (càng nhỏ càng ưu tiên) |
| `ParentId` | BIGINT (nullable) | ID của category cha (NULL = cấp 1) |

**Ví dụ dữ liệu:**
```sql
-- Category cấp 1
INSERT INTO Categories (Name, Slug, SortOrder, ParentId) 
VALUES (N'Trà Sữa', 'tra-sua', 1, NULL);

-- Category cấp 2 (con của "Trà Sữa")
INSERT INTO Categories (Name, Slug, SortOrder, ParentId) 
VALUES (N'Trà Sữa Truyền Thống', 'tra-sua-truyen-thong', 1, 1);
```

---

## 🎨 Tính năng Menu

### ✨ Tự động
- ✅ Tự động lấy categories từ database
- ✅ Tự động build cấu trúc parent-child
- ✅ Tự động sắp xếp theo `sortOrder`
- ✅ Tự động tạo URL từ `slug`

### 🎯 Mega Menu
- ✅ Menu cấp 1 có submenu → hiển thị dạng **Mega Menu**
- ✅ Chia submenu thành **4 cột** (responsive)
- ✅ **Hover effect** mượt mà
- ✅ Icon động cho mỗi category

### 🖱️ UX
- ✅ Hover vào menu cấp 1 → mở Mega Menu
- ✅ Di chuột ra ngoài → tự động đóng sau 200ms
- ✅ Responsive trên mobile

---

## 🚀 Cách thêm/sửa menu

### 1️⃣ Thêm Category mới
Chỉ cần thêm dữ liệu vào bảng `Categories`:

```sql
-- Thêm category cấp 1
INSERT INTO Categories (Name, Slug, SortOrder, ParentId)
VALUES (N'Cà Phê', 'ca-phe', 2, NULL);

-- Thêm category cấp 2
INSERT INTO Categories (Name, Slug, SortOrder, ParentId)
VALUES (N'Cà Phê Sữa Đá', 'ca-phe-sua-da', 1, 2);
```

→ Menu sẽ **TỰ ĐỘNG cập nhật** khi reload trang!

### 2️⃣ Thay đổi thứ tự menu
Chỉnh sửa `SortOrder`:

```sql
UPDATE Categories SET SortOrder = 10 WHERE Id = 1;
UPDATE Categories SET SortOrder = 5 WHERE Id = 2;
```

→ Category có `SortOrder` nhỏ hơn sẽ hiển thị trước.

### 3️⃣ Ẩn category khỏi menu
Xóa category hoặc thêm trường `IsActive`:

```sql
-- Xóa hẳn (cẩn thận)
DELETE FROM Categories WHERE Id = 1;
```

---

## 🧪 Kiểm tra hệ thống

### 1. Kiểm tra API
Mở trình duyệt và truy cập:
```
http://localhost:8080/alotra-website/api/categories/menu
```

→ Nếu trả về JSON với danh sách categories → ✅ Thành công!

### 2. Kiểm tra Frontend
Mở trang chủ và kiểm tra:
- ✅ Menu hiển thị đúng tên categories
- ✅ Hover vào menu cấp 1 → hiển thị Mega Menu
- ✅ Click vào menu item → chuyển đến trang sản phẩm theo category

### 3. Debug (nếu cần)
Mở **Console trình duyệt** (F12) và xem:
```javascript
// Kiểm tra dữ liệu menu
fetch('/alotra-website/api/categories/menu')
  .then(res => res.json())
  .then(data => console.log('Menu data:', data));
```

---

## 📝 Lưu ý quan trọng

### ⚠️ Không cần tạo bảng mới
- ❌ **KHÔNG** cần bảng `MenuItems` trong database
- ✅ Chỉ cần bảng `Categories` có sẵn

### 🔄 Tự động cập nhật
- Thêm/sửa/xóa category → Menu tự động thay đổi
- **KHÔNG** cần restart server
- Chỉ cần **reload trang** để thấy thay đổi

### 🎨 Tùy chỉnh icon
Mặc định tất cả categories dùng icon `fas fa-mug-hot`.

Nếu muốn icon khác nhau cho từng category, thêm cột `Icon` vào bảng `Categories`.

---

## 🎯 Kết luận

Hệ thống menu động giờ đây:
- ✅ **Đơn giản hơn** - không cần bảng riêng
- ✅ **Tự động hóa** - dựa trên Categories có sẵn
- ✅ **Dễ bảo trì** - chỉ cần quản lý Categories
- ✅ **Linh hoạt** - hỗ trợ đa cấp menu

**Không cần thêm bất kỳ bảng nào vào database!** 🎉

---

**Cập nhật:** 27/10/2025  
**Version:** 3.0 (Categories-only, no conflicts)