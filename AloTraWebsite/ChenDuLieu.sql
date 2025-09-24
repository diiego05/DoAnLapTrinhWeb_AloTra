USE AloTra
GO
-- Đảm bảo bạn đang dùng đúng database
USE AloTra;
GO

-- Đảm bảo bạn đang dùng đúng database
USE AloTra;
GO

-- 1. XÓA DỮ LIỆU CŨ
-- ===================================================================
DELETE FROM Addresses;
DELETE FROM Branches;
DELETE FROM Users;
DELETE FROM Roles;
GO

-- 2. RESET BỘ ĐẾM ID VỀ 0 (QUAN TRỌNG)
-- ===================================================================
DBCC CHECKIDENT ('[Roles]', RESEED, 0);
DBCC CHECKIDENT ('[Users]', RESEED, 0);
DBCC CHECKIDENT ('[Branches]', RESEED, 0);
DBCC CHECKIDENT ('[Addresses]', RESEED, 0);
GO

-- 3. CHÈN LẠI DỮ LIỆU
-- ===================================================================

-- Bảng Roles (Bây giờ ID sẽ bắt đầu lại từ 1)
INSERT INTO Roles (Code, Name) VALUES
('ADMIN', N'Quản trị viên'),
('VENDOR', N'Chủ cửa hàng'),
('USER', N'Khách hàng');
GO

-- Bảng Users (Bây giờ RoleId 1, 2, 3 sẽ hợp lệ)
INSERT INTO Users (Email, Phone, IdCardNumber, PasswordHash, FullName, RoleId) VALUES
('admin@alotra.com', '0901000001', '001001001001', 'hashed_password', N'Admin Tổng', 1),
('manager.td@alotra.com', '0901000002', '001001001002', 'hashed_password', N'Quản lý Thủ Đức', 2),
('manager.q1@alotra.com', '0901000003', '001001001003', 'hashed_password', N'Quản lý Quận 1', 2),
('khachhang.a@email.com', '0901000004', '001001001004', 'hashed_password', N'Khách Hàng A', 3);
GO

-- Bảng Branches
INSERT INTO Branches (ManagerId, Name, Slug, Address, Phone, Status) VALUES
(2, N'AloTra Chi nhánh Thủ Đức', 'alotra-thu-duc', N'123 Võ Văn Ngân, TP. Thủ Đức', '0281112223', 'ACTIVE'),
(3, N'AloTra Chi nhánh Quận 1', 'alotra-quan-1', N'456 Lê Lợi, Quận 1, TP. HCM', '0284445556', 'ACTIVE');
GO

-- Bảng Addresses
INSERT INTO Addresses (UserId, Label, Recipient, Phone, Line1, Ward, District, City, IsDefault) VALUES
(4, N'Nhà riêng', N'Khách Hàng A', '0901000004', N'789 Đường ABC, Phường 10', N'Phường 10', N'Quận Gò Vấp', N'TP. HCM', 1);
GO
INSERT INTO Sizes (Code, Name) 
VALUES 
    ('S', N'Size Nhỏ'),
    ('M', N'Size Vừa'),
    ('L', N'Size Lớn'),
    ('XL', N'Size Khổng Lồ');
GO
INSERT INTO Toppings (Name, Price, Status) 
VALUES 
    (N'Trân châu đen', 7000, 'ACTIVE'),
    (N'Pudding trứng', 10000, 'ACTIVE'),
    (N'Thạch phô mai', 10000, 'ACTIVE'),
    (N'Kem sữa Macchiato', 12000, 'ACTIVE'),
    (N'Trân châu trắng', 8000, 'ACTIVE');
GO

INSERT INTO Categories (ParentId, Name, Slug, SortOrder)
VALUES
    (NULL, N'Trà Sữa', 'tra-sua', 1),
    (NULL, N'Trà Trái Cây', 'tra-trai-cay', 2),
    (NULL, N'Cà Phê', 'ca-phe', 3),
    (NULL, N'Bánh Ngọt', 'banh-ngot', 4);
GO

-- ===================================================================================
-- BƯỚC 1: SETUP - LẤY ID CỦA CÁC DANH MỤC (CHỈ CẦN CHẠY 1 LẦN)
-- ===================================================================================
-- 1. Trà Sữa Phúc Long
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Phúc Long', 'tra-sua-phuc-long', N'Hương vị trà sữa đặc trưng, đậm vị trà của Phúc Long.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-phuc-long'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 2. Trà Sữa Ô Long
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Ô Long', 'tra-sua-o-long', N'Trà Ô Long thơm dịu, kết hợp hài hòa cùng sữa.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-o-long'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 3. Hồng Trà Sữa
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Hồng Trà Sữa', 'hong-tra-sua', N'Hồng trà thơm nồng, đậm đà trong từng ngụm trà sữa.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'hong-tra-sua'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 4. Trà Sữa Lài
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Lài', 'tra-sua-lai', N'Trà lài thanh mát, mang đến hương thơm nhẹ nhàng, thư giãn.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-lai'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 5. Trà Sữa Ô Long Quế Hoa
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Ô Long Quế Hoa', 'tra-sua-o-long-que-hoa', N'Hương thơm tinh tế của quế hoa kết hợp cùng trà Ô Long.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-o-long-que-hoa'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 6. Trà Sữa Matcha
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Matcha', 'tra-sua-matcha', N'Vị đắng nhẹ đặc trưng của matcha cao cấp từ Nhật Bản.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-matcha'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 7. Trà Sữa Socola
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Socola', 'tra-sua-socola', N'Hương vị socola ngọt ngào, đậm đà cho các tín đồ hảo ngọt.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-socola'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 8. Trà Sữa Bá Tước
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Bá Tước', 'tra-sua-ba-tuoc', N'Trà Bá Tước (Earl Grey) với hương cam bergamot độc đáo.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-ba-tuoc'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 9. Hồng Trà Đào Sữa
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Hồng Trà Đào Sữa', 'hong-tra-dao-sua', N'Sự kết hợp giữa hồng trà, đào và sữa, tạo nên vị ngọt dịu.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'hong-tra-dao-sua'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 10. Trà Sữa Nhãn Sen
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-sua'), N'Trà Sữa Nhãn Sen', 'tra-sua-nhan-sen', N'Trà sữa thơm hương sen thanh mát cùng nhãn ngọt ngào.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-sua-nhan-sen'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919452/Gemini_Generated_Image_evqjcgevqjcgevqj_cewtsy.png', 1);
GO

-- 11. Hồng Trà Đào
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Hồng Trà Đào', 'hong-tra-dao', N'Trà đào kinh điển với những miếng đào giòn ngọt, tươi mát.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'hong-tra-dao'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 12. Trà Vải Lài
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Vải Lài', 'tra-vai-lai', N'Trà lài thanh tao kết hợp cùng vị ngọt của quả vải.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-vai-lai'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 13. Trà Nhãn Sen
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Nhãn Sen', 'tra-nhan-sen', N'Hương sen dịu nhẹ cùng vị ngọt thanh của long nhãn.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-nhan-sen'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 14. Trà Vải Sen
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Vải Sen', 'tra-vai-sen', N'Sự kết hợp độc đáo giữa hương sen và vị ngọt của quả vải.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-vai-sen'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 15. Trà Lài Đác Thơm
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Lài Đác Thơm', 'tra-lai-dac-thom', N'Trà lài tươi mát cùng hạt đác và thơm (dứa) chua ngọt.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-lai-dac-thom'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 16. Trà Lucky Tea
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Lucky Tea', 'tra-lucky-tea', N'Hương vị trà may mắn đặc biệt của quán.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-lucky-tea'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 17. Trà Ô Long Dâu
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Ô Long Dâu', 'tra-o-long-dau', N'Trà Ô Long kết hợp với dâu tây tươi tạo vị chua ngọt sảng khoái.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-o-long-dau'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 18. Trà Lài Mãng Cầu Thạch Dừa
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Lài Mãng Cầu Thạch Dừa', 'tra-lai-mang-cau-thach-dua', N'Vị mãng cầu chua ngọt đặc trưng cùng thạch dừa giòn dai.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-lai-mang-cau-thach-dua'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 19. Bá Tước Lựu Đỏ
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Bá Tước Lựu Đỏ', 'ba-tuoc-luu-do', N'Trà Bá Tước độc đáo kết hợp cùng vị lựu đỏ tươi mát.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'ba-tuoc-luu-do'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- 20. Trà Ô Long Cam Đào Thạch Vải
INSERT INTO Products (CategoryId, Name, Slug, Description) 
VALUES ((SELECT Id FROM Categories WHERE Slug = 'tra-trai-cay'), N'Trà Ô Long Cam Đào Thạch Vải', 'tra-o-long-cam-dao-thach-vai', N'Một sự kết hợp đa dạng các loại trái cây nhiệt đới.');
INSERT INTO ProductMedia (ProductId, Url, IsPrimary) 
VALUES ((SELECT Id FROM Products WHERE Slug = 'tra-o-long-cam-dao-thach-vai'), N'https://res.cloudinary.com/dkuxcfsxa/image/upload/v1757919619/Gemini_Generated_Image_e0pbr2e0pbr2e0pb_eh4l9y.png', 1);
GO

-- Giả định bạn đã có Size 'M' trong bảng Sizes với Code = 'M'
-- Thêm các biến thể sản phẩm (Size M) với Sku và Status

INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-phuc-long'), (SELECT Id FROM Sizes WHERE Code = 'M'), 35000, 'TSPL-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-o-long'), (SELECT Id FROM Sizes WHERE Code = 'M'), 32000, 'TSOL-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'hong-tra-sua'), (SELECT Id FROM Sizes WHERE Code = 'M'), 30000, 'HTS-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-lai'), (SELECT Id FROM Sizes WHERE Code = 'M'), 30000, 'TSL-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-o-long-que-hoa'), (SELECT Id FROM Sizes WHERE Code = 'M'), 38000, 'TSOLQH-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-matcha'), (SELECT Id FROM Sizes WHERE Code = 'M'), 40000, 'TSMA-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-socola'), (SELECT Id FROM Sizes WHERE Code = 'M'), 35000, 'TSSC-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-ba-tuoc'), (SELECT Id FROM Sizes WHERE Code = 'M'), 35000, 'TSBT-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'hong-tra-dao-sua'), (SELECT Id FROM Sizes WHERE Code = 'M'), 42000, 'HTDS-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-sua-nhan-sen'), (SELECT Id FROM Sizes WHERE Code = 'M'), 42000, 'TSNS-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'hong-tra-dao'), (SELECT Id FROM Sizes WHERE Code = 'M'), 45000, 'HTD-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-vai-lai'), (SELECT Id FROM Sizes WHERE Code = 'M'), 45000, 'TVL-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-nhan-sen'), (SELECT Id FROM Sizes WHERE Code = 'M'), 45000, 'TNS-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-vai-sen'), (SELECT Id FROM Sizes WHERE Code = 'M'), 45000, 'TVS-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-lai-dac-thom'), (SELECT Id FROM Sizes WHERE Code = 'M'), 48000, 'TLDT-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-lucky-tea'), (SELECT Id FROM Sizes WHERE Code = 'M'), 50000, 'TLT-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-o-long-dau'), (SELECT Id FROM Sizes WHERE Code = 'M'), 48000, 'TOLD-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-lai-mang-cau-thach-dua'), (SELECT Id FROM Sizes WHERE Code = 'M'), 50000, 'TLMCTD-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'ba-tuoc-luu-do'), (SELECT Id FROM Sizes WHERE Code = 'M'), 48000, 'BTLD-M', 'ACTIVE');
GO
INSERT INTO ProductVariants (ProductId, SizeId, Price, Sku, Status) VALUES 
((SELECT Id FROM Products WHERE Slug = 'tra-o-long-cam-dao-thach-vai'), (SELECT Id FROM Sizes WHERE Code = 'M'), 55000, 'TOLCDTV-M', 'ACTIVE');
GO