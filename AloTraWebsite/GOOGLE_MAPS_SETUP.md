# 🗺️ Hướng dẫn cấu hình Google Maps API

## 📋 Tổng quan

Hệ thống AloTra hỗ trợ **2 phương thức geocoding**:

1. **Google Maps API** (khuyến nghị cho production) - Chính xác cao, nhanh
2. **OpenStreetMap Nominatim** (miễn phí, mặc định) - Không cần API key

## ⚙️ Trạng thái hiện tại

- ✅ Hệ thống đang dùng **Nominatim** (miễn phí)
- ⚠️ Google Maps API **chưa được cấu hình**
- 📝 Nếu bạn muốn độ chính xác cao hơn, hãy làm theo hướng dẫn bên dưới

---

## 🚀 Cách cấu hình Google Maps API

### Bước 1: Tạo Google Cloud Project

1. Truy cập: https://console.cloud.google.com/
2. Nhấn **"Select a project"** → **"New Project"**
3. Đặt tên project (VD: `AloTra-Maps`)
4. Nhấn **"Create"**

### Bước 2: Bật các API cần thiết

1. Vào **"APIs & Services"** → **"Library"**
2. Tìm và bật các API sau:
   - ✅ **Maps JavaScript API**
   - ✅ **Places API**
   - ✅ **Geocoding API**

### Bước 3: Tạo API Key

1. Vào **"APIs & Services"** → **"Credentials"**
2. Nhấn **"+ CREATE CREDENTIALS"** → **"API key"**
3. Copy API key vừa tạo

### Bước 4: Hạn chế API Key (bảo mật)

1. Nhấn vào API key vừa tạo
2. Tại **"Application restrictions"**:
   - Chọn **"HTTP referrers (web sites)"**
   - Thêm: `http://localhost:8080/*`
   - Thêm: `https://yourdomain.com/*` (nếu có)

3. Tại **"API restrictions"**:
   - Chọn **"Restrict key"**
   - Chọn:
     - Maps JavaScript API
     - Places API
     - Geocoding API

4. Nhấn **"Save"**

### Bước 5: Cấu hình trong ứng dụng

Mở file `src/main/resources/application.properties`:

```properties
# Dán API key vào đây (thay thế YOUR_API_KEY_HERE)
google.maps.apiKey=AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Bước 6: Khởi động lại ứng dụng

```bash
# Stop server hiện tại (Ctrl + C)
# Start lại server
mvn spring-boot:run
```

---

## ✅ Kiểm tra cấu hình

### 1. Kiểm tra API key có được load không

Truy cập: http://localhost:8080/alotra-website/api/public/config/maps-key

- **Nếu trả về 204 No Content** → API key chưa được cấu hình
- **Nếu trả về API key** → Đã cấu hình thành công

### 2. Test geocoding

Mở Console trình duyệt và chạy:

```javascript
fetch('/alotra-website/api/geocoding/geocode?address=Số 1 Võ Văn Ngân, Thủ Đức, TP.HCM')
  .then(res => res.json())
  .then(data => console.log('Kết quả:', data));
```

Kết quả mong đợi:
```json
{
  "latitude": 10.850726,
  "longitude": 106.771495
}
```

### 3. Test trên trang Profile

1. Đăng nhập vào hệ thống
2. Vào trang **Profile** (http://localhost:8080/alotra-website/profile)
3. Nhấn **"Thêm địa chỉ"**
4. Nhập địa chỉ vào ô **"Địa chỉ chi tiết"**
5. Kiểm tra xem có gợi ý địa chỉ tự động không

---

## 💰 Chi phí Google Maps API

### Miễn phí mỗi tháng:
- **$200 credit** (khoảng 28,000 lượt geocoding)
- Phù hợp cho website vừa và nhỏ

### Lưu ý:
- ⚠️ **Cần thêm thẻ tín dụng** để kích hoạt (nhưng chỉ tính phí khi vượt $200/tháng)
- 📊 Theo dõi usage tại: https://console.cloud.google.com/billing

---

## 🔧 Xử lý sự cố

### Lỗi: "API key not configured"
**Nguyên nhân:** Chưa cấu hình API key hoặc file `application.properties` chưa được reload

**Giải pháp:**
1. Kiểm tra file `application.properties`
2. Đảm bảo không có khoảng trắng thừa
3. Khởi động lại server

### Lỗi: "REQUEST_DENIED"
**Nguyên nhân:** API key bị hạn chế hoặc API chưa được bật

**Giải pháp:**
1. Kiểm tra lại **API restrictions**
2. Đảm bảo đã bật đủ 3 API: Maps JavaScript, Places, Geocoding
3. Kiểm tra **Application restrictions** có đúng domain không

### Lỗi: "OVER_QUERY_LIMIT"
**Nguyên nhân:** Vượt giới hạn miễn phí

**Giải pháp:**
1. Kiểm tra usage tại Google Cloud Console
2. Tối ưu code để cache kết quả
3. Nâng cấp billing plan (nếu cần)

---

## 📚 Tài liệu tham khảo

- [Google Maps Platform Documentation](https://developers.google.com/maps/documentation)
- [Places API Overview](https://developers.google.com/maps/documentation/places/web-service/overview)
- [Geocoding API Guide](https://developers.google.com/maps/documentation/geocoding/overview)

---

## 🆘 Hỗ trợ

Nếu gặp vấn đề, vui lòng:
1. Kiểm tra Console log của trình duyệt
2. Kiểm tra Server log (terminal)
3. Liên hệ team Dev

---

**Cập nhật lần cuối:** 26/10/2025
**Version:** 1.0.0
