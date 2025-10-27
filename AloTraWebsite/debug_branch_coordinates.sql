-- =====================================================
-- 🔍 KIỂM TRA VÀ SỬA TỌA ĐỘ CHI NHÁNH
-- =====================================================
-- Script này giúp debug vấn đề tính khoảng cách sai
-- =====================================================

USE AloTraWeb;
GO

-- 1️⃣ Kiểm tra tất cả chi nhánh và tọa độ của chúng
PRINT '🏪 DANH SÁCH CHI NHÁNH VÀ TỌA ĐỘ:';
PRINT '================================================';

SELECT 
    Id,
    Name,
    Address,
    Latitude,
    Longitude,
    Status,
    CASE 
        WHEN Latitude IS NULL OR Longitude IS NULL THEN 'Thiếu tọa độ'
        WHEN Latitude < 8.0 OR Latitude > 24.5 THEN 'Latitude ngoài VN'
        WHEN Longitude < 102.0 OR Longitude > 110.5 THEN 'Longitude ngoài VN'
        ELSE 'OK'
    END AS CoordinateStatus
FROM Branches
ORDER BY Name;

-- 2️⃣ Liệt kê chi nhánh ACTIVE nhưng thiếu tọa độ
PRINT '';
PRINT '⚠️ CHI NHÁNH ACTIVE THIẾU TỌA ĐỘ:';
PRINT '================================================';

SELECT 
    Id,
    Name,
    Address,
    Status
FROM Branches
WHERE Status = 'ACTIVE'
  AND (Latitude IS NULL OR Longitude IS NULL);

-- 3️⃣ Liệt kê chi nhánh có tọa độ ngoài phạm vi VN
PRINT '';
PRINT '⚠️ CHI NHÁNH CÓ TỌA ĐỘ NGOÀI VIỆT NAM:';
PRINT '================================================';

SELECT 
    Id,
    Name,
    Address,
    Latitude,
    Longitude
FROM Branches
WHERE (Latitude < 8.0 OR Latitude > 24.5)
   OR (Longitude < 102.0 OR Longitude > 110.5);

-- 4️⃣ Tính khoảng cách từ một địa chỉ mẫu đến tất cả chi nhánh
PRINT '';
PRINT '📏 TÍNH KHOẢNG CÁCH TỪ ĐỊA CHỈ MẪU:';
PRINT '================================================';
PRINT 'Địa chỉ: 1 Võ Văn Ngân, Thủ Đức, TP.HCM';
PRINT 'Tọa độ mẫu: Lat=10.8500, Lng=106.7700';
PRINT '';

-- Hàm tính khoảng cách Haversine (tạm thời)
DECLARE @UserLat FLOAT = 10.8500;
DECLARE @UserLng FLOAT = 106.7700;

SELECT 
    Name,
    Address,
    Latitude,
    Longitude,
    ROUND(
        6371 * 2 * ASIN(SQRT(
            POWER(SIN(RADIANS(Latitude - @UserLat) / 2), 2) +
            COS(RADIANS(@UserLat)) * COS(RADIANS(Latitude)) *
            POWER(SIN(RADIANS(Longitude - @UserLng) / 2), 2)
        )),
        2
    ) AS DistanceKm
FROM Branches
WHERE Status = 'ACTIVE'
  AND Latitude IS NOT NULL
  AND Longitude IS NOT NULL
ORDER BY DistanceKm;

-- 5️⃣ CẬP NHẬT TỌA ĐỘ CHO CÁC CHI NHÁNH CỤ THỂ (nếu cần)
-- Uncomment và chỉnh sửa theo chi nhánh thực tế

/*
-- AloTra UTE (1 Võ Văn Ngân)
UPDATE Branches
SET Latitude = 10.8500, Longitude = 106.7700
WHERE Name LIKE '%UTE%';

-- AloTra Rạp Cầu Bông (207 Đinh Tiên Hoàng)
UPDATE Branches
SET Latitude = 10.7900, Longitude = 106.6950
WHERE Name LIKE '%Rạp Cầu Bông%';

-- Kiểm tra lại sau khi update
SELECT Id, Name, Address, Latitude, Longitude
FROM Branches
WHERE Name IN ('AloTra UTE', 'AloTra Rạp Cầu Bông');
*/

PRINT '';
PRINT '✅ HOÀN TẤT KIỂM TRA!';
GO
