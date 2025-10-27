# 🗺️ Hướng dẫn Parse Địa chỉ từ Google Places Autocomplete

## ❌ Vấn đề trước đây

Khi người dùng chọn địa chỉ từ Google Places Autocomplete:
```
"Phó Cơ Điều, Khu phố 13, Phường Minh Phụng, Thành phố Hồ Chí Minh, 72415, Việt Nam"
```

Hệ thống **SAI** khi lưu:
- `line1`: "Phó Cơ Điều, Khu phố 13, Phường Minh Phụng, Thành phố Hồ Chí Minh, 72415, Việt Nam" ❌ (toàn bộ formatted_address)
- `ward`: "Thành phố Hồ Chí Minh" ❌ (lấy nhầm)
- `district`: "72415" ❌ (postal code)
- `city`: "Việt Nam" ❌ (country)
- `latitude`: null ❌
- `longitude`: null ❌

## ✅ Giải pháp mới

### 1. Parser thông minh trong `google-maps-loader.js`

Hàm `parseVietnameseAddress()` giờ đây:

#### **Bước 1: Phân loại chính xác address_components**

```javascript
components.forEach(component => {
    const types = component.types;
    const longName = component.long_name;

    // 📮 Postal code - LOẠI BỎ HOÀN TOÀN
    if (types.includes('postal_code')) {
        result.postalCode = longName; // Lưu riêng, KHÔNG dùng
        return; // Skip
    }

    // 🛣️ Street number + route
    if (types.includes('street_number')) {
        result.street = longName + ' ';
    } else if (types.includes('route')) {
        result.street += longName;
    }

    // 🏘️ Neighborhood (Khu phố)
    else if (types.includes('neighborhood')) {
        if (longName.includes('Khu phố') || longName.includes('KP')) {
            result.street += ', ' + longName; // Thêm vào street
        }
    }

    // 🏘️ Ward (Phường/Xã) - ƯU TIÊN: sublocality_level_1
    else if (types.includes('sublocality_level_1')) {
        result.ward = longName;
    }

    // 🏙️ District (Quận/Huyện) - administrative_area_level_2
    else if (types.includes('administrative_area_level_2')) {
        result.district = longName;
    }

    // 🌆 City (Tỉnh/TP) - administrative_area_level_1
    else if (types.includes('administrative_area_level_1')) {
        result.city = longName;
    }
});
```

#### **Bước 2: Post-processing & Validation**

```javascript
// ✅ Loại bỏ postal code nếu nhầm vào district/city
if (result.district && /^\d{5,6}$/.test(result.district)) {
    result.district = '';
}

// ✅ Nếu city là "Việt Nam" hoặc country code, tìm lại
if (result.city === 'Việt Nam' || result.city === 'VN') {
    const cityComp = components.find(c => 
        c.types.includes('administrative_area_level_1') &&
        !['Việt Nam', 'VN', 'Vietnam'].includes(c.long_name)
    );
    if (cityComp) {
        result.city = cityComp.long_name;
    }
}
```

### 2. Xử lý trong các file JavaScript

#### **profile.js & checkout.js** (Địa chỉ giao hàng)

```javascript
autocomplete.addListener('place_changed', () => {
    const place = autocomplete.getPlace();
    const parsed = window.googleMapsLoader.parseVietnameseAddress(
        place.address_components
    );

    // ✅ CHỈ lấy street cho Line1
    document.getElementById('new-line1').value = parsed.street || '';
    
    // ✅ Các trường riêng biệt
    document.getElementById('new-ward').value = parsed.ward || '';
    document.getElementById('new-district').value = parsed.district || '';
    document.getElementById('new-city').value = parsed.city || '';
});
```

#### **register-branch.js** (Địa chỉ chi nhánh)

```javascript
autocomplete.addListener('place_changed', () => {
    const parsed = window.googleMapsLoader.parseVietnameseAddress(
        place.address_components
    );

    // ✅ Kết hợp thành địa chỉ đầy đủ cho chi nhánh
    const fullAddress = [
        parsed.street,
        parsed.ward,
        parsed.district,
        parsed.city
    ].filter(Boolean).join(', ');
    
    input.value = fullAddress;
});
```

## 📊 So sánh kết quả

### Input từ Google:
```
"Phó Cơ Điều, Khu phố 13, Phường Minh Phụng, Thành phố Hồ Chí Minh, 72415, Việt Nam"
```

### Output CŨ (SAI):
```json
{
  "line1": "Phó Cơ Điều, Khu phố 13, Phường Minh Phụng, Thành phố Hồ Chí Minh, 72415, Việt Nam",
  "ward": "Thành phố Hồ Chí Minh",
  "district": "72415",
  "city": "Việt Nam"
}
```

### Output MỚI (ĐÚNG):
```json
{
  "line1": "Phó Cơ Điều, Khu phố 13",
  "ward": "Phường Minh Phụng",
  "district": "Quận 6",
  "city": "Thành phố Hồ Chí Minh"
}
```

## 🧪 Cách kiểm tra

### 1. Mở Console trình duyệt
Khi chọn địa chỉ từ autocomplete, bạn sẽ thấy:

```
🗺️ [DEBUG] Parsing address components: [...]
✅ [DEBUG] Parsed address result: {
  street: "Phó Cơ Điều, Khu phố 13",
  ward: "Phường Minh Phụng",
  district: "Quận 6",
  city: "Thành phố Hồ Chí Minh",
  postalCode: "72415"
}
```

### 2. Kiểm tra database

Sau khi lưu, query database:
```sql
SELECT Line1, Ward, District, City, Latitude, Longitude
FROM Addresses
ORDER BY Id DESC;
```

Kết quả mong đợi:
```
Line1     = "Phó Cơ Điều, Khu phố 13"
Ward      = "Phường Minh Phụng"
District  = "Quận 6"
City      = "Thành phố Hồ Chí Minh"
Latitude  = 10.7484 (có giá trị)
Longitude = 106.6347 (có giá trị)
```

## 🔍 Xử lý các trường hợp đặc biệt

### 1. Chỉ có số nhà + đường (không có neighborhood)
```
Input: "123 Nguyễn Văn Linh, Phường Tân Phú, ..."
Output: 
  street: "123 Nguyễn Văn Linh"
```

### 2. Có cả neighborhood
```
Input: "123 Nguyễn Văn Linh, Khu phố 7, Phường Tân Phú, ..."
Output:
  street: "123 Nguyễn Văn Linh, Khu phố 7"
```

### 3. Không có số nhà
```
Input: "Nguyễn Văn Linh, Phường Tân Phú, ..."
Output:
  street: "Nguyễn Văn Linh"
```

### 4. Địa chỉ Huyện/Xã (nông thôn)
```
Input: "Ấp 3, Xã Tân Lập, Huyện Châu Thành, ..."
Output:
  street: "Ấp 3"
  ward: "Xã Tân Lập"
  district: "Huyện Châu Thành"
```

## ⚠️ Lưu ý quan trọng

1. **Postal code KHÔNG bao giờ được lưu vào ward/district/city**
2. **Country name ("Việt Nam") KHÔNG bao giờ được lưu vào city**
3. **Geocoding vẫn hoạt động bình thường** - tọa độ sẽ được lưu tự động
4. **Tương thích ngược** - Nominatim fallback vẫn hoạt động bình thường

## 📝 Files đã được sửa

1. ✅ `google-maps-loader.js` - Parser chính
2. ✅ `checkout.js` - Thêm địa chỉ khi checkout
3. ✅ `profile.js` - Quản lý địa chỉ user
4. ✅ `register-branch.js` - Đăng ký chi nhánh
5. ✅ `register-shipper.js` - Đăng ký shipper (đã sửa trước đó)

## 🎯 Kết luận

Hệ thống giờ đây:
- ✅ Parse chính xác từng component của địa chỉ
- ✅ Loại bỏ postal code và country name
- ✅ Lưu đúng cấu trúc: Line1 (street only) + Ward + District + City
- ✅ Tự động geocode và lưu tọa độ
- ✅ Hỗ trợ cả Google Maps và Nominatim

---
**Cập nhật:** 26/10/2025  
**Người thực hiện:** GitHub Copilot  
**Version:** 2.0.0
