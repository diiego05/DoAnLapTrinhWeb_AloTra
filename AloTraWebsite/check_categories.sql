-- =====================================================
-- 🔍 KIỂM TRA DỮ LIỆU CATEGORIES
-- =====================================================

USE AloTraWeb;
GO

-- Kiểm tra xem bảng Categories có dữ liệu không
SELECT COUNT(*) AS TotalCategories FROM Categories;
GO

-- Xem 10 categories đầu tiên
SELECT TOP 10 
    Id, 
    Name, 
    Slug, 
    SortOrder, 
    ParentId
FROM Categories
ORDER BY SortOrder ASC;
GO

-- Nếu chưa có dữ liệu, chạy script này để thêm dữ liệu mẫu:
/*
-- =====================================================
-- 📝 THÊM DỮ LIỆU MẪU CHO CATEGORIES
-- =====================================================

-- Xóa dữ liệu cũ (nếu có)
DELETE FROM Categories;
GO

-- Categories cấp 1
INSERT INTO Categories (Name, Slug, SortOrder, ParentId) VALUES
(N'Trà Sữa', 'tra-sua', 1, NULL),
(N'Trà Hoa Quả', 'tra-hoa-qua', 2, NULL),
(N'Cà Phê', 'ca-phe', 3, NULL),
(N'Nước Ép', 'nuoc-ep', 4, NULL),
(N'Đồ Ăn Vặt', 'do-an-vat', 5, NULL);
GO

-- Categories cấp 2 (con của Trà Sữa)
DECLARE @TraSuaId BIGINT = (SELECT Id FROM Categories WHERE Slug = 'tra-sua');

INSERT INTO Categories (Name, Slug, SortOrder, ParentId) VALUES
(N'Trà Sữa Truyền Thống', 'tra-sua-truyen-thong', 1, @TraSuaId),
(N'Trà Sữa Trân Châu', 'tra-sua-tran-chau', 2, @TraSuaId),
(N'Trà Sữa Matcha', 'tra-sua-matcha', 3, @TraSuaId),
(N'Trà Sữa Socola', 'tra-sua-socola', 4, @TraSuaId);
GO

-- Categories cấp 2 (con của Cà Phê)
DECLARE @CaPheId BIGINT = (SELECT Id FROM Categories WHERE Slug = 'ca-phe');

INSERT INTO Categories (Name, Slug, SortOrder, ParentId) VALUES
(N'Cà Phê Sữa Đá', 'ca-phe-sua-da', 1, @CaPheId),
(N'Cà Phê Đen', 'ca-phe-den', 2, @CaPheId),
(N'Bạc Xỉu', 'bac-xiu', 3, @CaPheId);
GO

SELECT '✅ Đã thêm dữ liệu mẫu thành công!' AS Message;
GO
*/
