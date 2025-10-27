-- =====================================================
-- 🗺️ THÊM TỌA ĐỘ CHO BẢNG BranchRegistrationRequests
-- =====================================================
-- Script này thêm 2 cột Latitude và Longitude vào bảng
-- BranchRegistrationRequests để lưu toạ độ ngay khi
-- user gửi yêu cầu đăng ký chi nhánh.
-- =====================================================

USE AloTraWeb;
GO

-- Kiểm tra và thêm cột Latitude
IF NOT EXISTS (
    SELECT * FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'BranchRegistrationRequests') 
    AND name = 'Latitude'
)
BEGIN
    ALTER TABLE BranchRegistrationRequests
    ADD Latitude FLOAT NULL;
    PRINT '✅ Đã thêm cột Latitude vào bảng BranchRegistrationRequests';
END
ELSE
BEGIN
    PRINT 'ℹ️ Cột Latitude đã tồn tại';
END
GO

-- Kiểm tra và thêm cột Longitude
IF NOT EXISTS (
    SELECT * FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'BranchRegistrationRequests') 
    AND name = 'Longitude'
)
BEGIN
    ALTER TABLE BranchRegistrationRequests
    ADD Longitude FLOAT NULL;
    PRINT '✅ Đã thêm cột Longitude vào bảng BranchRegistrationRequests';
END
ELSE
BEGIN
    PRINT 'ℹ️ Cột Longitude đã tồn tại';
END
GO

PRINT '🎉 Hoàn tất! Bảng BranchRegistrationRequests đã có cột Latitude và Longitude';
GO
