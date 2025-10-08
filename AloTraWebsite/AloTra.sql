-- ===================================================================================
-- SCRIPT TẠO DATABASE ALOTRA - PHIÊN BẢN HOÀN CHỈNH
-- Tác giả: Gemini
-- Ngày cập nhật: 15/09/2025
-- ===================================================================================
/* Đề tài:
¨Xây dựng website chuỗi bán trà sữa AloTra bằng Spring boot + Thymeleaf + Bootstrap + JPA + SQLServer/MySQL/ postgreSQL+JWT+websocket+Cloudairy."
Chức năng chung: tìm kiếm và lọc sản phẩm, đăng ký tài khoản có gửi mã OTP kích hoạt qua Email, đăng nhập, đăng xuất, quên mật khẩu có gửi mã OTP kích hoạt qua Email. Mật khẩu phải được mã hóa. Nếu dùng Spring boot thì dùng Spring Security.

Guest: Giao diện Trang chủ (hiển thị sản phẩm bán trên 10 sản phẩm của các shop (sắp xếp theo thứ tự từ lớn đến nhỏ)
User: Giao diện trang chủ, trang sản phẩm theo danh mục, 20 (sản phẩm mới, bán chạy, đánh giá, yêu thích) nhất được phân trang (hoặc lazy loading), trang profile user (có quản lý địa chỉ nhận hàng khác nhau nếu làm đề tài về Bán hàng), trang chi tiết sản phẩm, giỏ hàng được lưu trên database, thanh toán(COD, VNPAY hoặc MOMO), quản lý lịch sử mua hàng theo trạng thái (đơn hàng mới, đã xác nhận, đang giao, đã giao, hủy, trả hàng- hoàn tiền), thích sản phẩm, sản phẩm đã xem, đánh giá sản phẩm đã mua, bình luận (text (tối thiểu 50 ký tự), hình ảnh/video) sản phẩm đã mua, chọn mã giảm giá,...
Vendor (Seller): có các quyền của User và thêm các chức năng: đăng ký shop, quản lý trang chủ shop, quản lý sản phẩm của mình, quản lý đơn hàng của shop theo trạng thái (đơn hàng mới, đã xác nhận, đang giao, đã giao, hủy, trả hàng- hoàn tiền), tạo chương trình khuyến mãi, quản lý doanh thu của shop.
Admin: tìm kiếm và quản lý user, quản lý sản phẩm của từng shop, Quản lý doanh mục, quản lý chiết khấu app cho các shop, quản lý chương trình khuyến mãi (giảm % sản phẩm, giảm phí vận chuyển), quản lý nhà vận chuyển (tên nhà vận chuyển, phí vận chuyển).
Shipper (phần thêm): Quản lý đơn hàng được phân công đi giao, thống kê đơn hàng được phân công giao*/
-- Xóa database nếu tồn tại để tạo lại từ đầu
IF DB_ID('AloTra') IS NOT NULL
BEGIN
    ALTER DATABASE AloTra SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE AloTra;
END
GO

CREATE DATABASE AloTra
GO
USE AloTra
GO
USE AloTra;
GO
ALTER TABLE Products
ALTER COLUMN Description NVARCHAR(MAX);
GO
-- ===================================================================================
-- PHẦN 1: QUẢN LÝ USER, PHÂN QUYỀN VÀ THÔNG TIN CÁ NHÂN
-- ===================================================================================

CREATE TABLE Roles (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi vai trò.
    Code VARCHAR(50) UNIQUE NOT NULL,                   -- Mã vai trò dùng trong code (không đổi), VD: 'ADMIN', 'USER', 'BRANCH_MANAGER'. Dùng để kiểm tra quyền hạn.
    Name NVARCHAR(100) NOT NULL                         -- Tên vai trò đầy đủ hiển thị cho người dùng, VD: 'Quản trị viên', 'Khách hàng'.
);
GO

CREATE TABLE Users (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi người dùng.
    Email VARCHAR(255) UNIQUE NOT NULL,                 -- Email dùng để đăng nhập, là duy nhất và bắt buộc. Dùng để gửi OTP, thông báo.
    Phone VARCHAR(20) UNIQUE,                           -- Số điện thoại, là duy nhất. Có thể dùng để đăng nhập hoặc liên lạc khi giao hàng.
    PasswordHash VARCHAR(255) NOT NULL,                 -- Chuỗi mật khẩu đã được mã hóa một chiều (hashed) để bảo mật, không bao giờ lưu mật khẩu gốc.
    FullName NVARCHAR(150) NOT NULL,                    -- Họ và tên đầy đủ của người dùng, hiển thị trên trang cá nhân, bình luận.
    AvatarUrl NVARCHAR(500),                            -- Đường dẫn đến ảnh đại diện của người dùng (nếu có).
    Gender NVARCHAR(10) NULL,                           -- Giới tính ('Nam', 'Nữ', 'Khác').
    DateOfBirth DATE NULL,                              -- Ngày sinh của người dùng.
    IdCardNumber VARCHAR(20) NULL UNIQUE,               -- Số CMND/CCCD, là duy nhất (nếu có cung cấp).
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- Trạng thái tài khoản ('ACTIVE', 'BANNED', 'INACTIVE'). Dùng để khóa/mở tài khoản.
    EmailVerifiedAt DATETIME NULL,                      -- Mốc thời gian khi email được xác thực (qua OTP). Nếu là NULL nghĩa là tài khoản chưa được kích hoạt.
    RoleId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới bảng Roles để xác định nhóm quyền của người dùng.
    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),      -- Thời gian tài khoản được tạo, tự động lấy giờ hệ thống.
    UpdatedAt DATETIME NOT NULL DEFAULT GETDATE(),      -- Thời gian thông tin tài khoản được cập nhật lần cuối.
    FOREIGN KEY (RoleId) REFERENCES Roles(Id)
);
GO

CREATE TABLE OtpCodes (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi mã OTP được tạo ra.
    UserId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới người dùng sở hữu mã OTP này.
    Code VARCHAR(10) NOT NULL,                          -- Chuỗi mã OTP ngẫu nhiên (VD: '123456') được gửi tới email/SĐT của người dùng.
    Type VARCHAR(30) NOT NULL,                          -- Phân loại mục đích của OTP ('REGISTER' để kích hoạt, 'RESET_PASSWORD' để đặt lại mật khẩu).
    ExpiresAt DATETIME NOT NULL,                        -- Thời gian mã OTP sẽ hết hạn. Sau thời gian này mã sẽ không còn hợp lệ.
    UsedAt DATETIME NULL,                               -- Mốc thời gian khi mã OTP được sử dụng. Nếu là NULL nghĩa là chưa được sử dụng.
    FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE -- Nếu người dùng bị xóa, các mã OTP liên quan cũng sẽ tự động bị xóa.
);
GO

CREATE TABLE Addresses (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi địa chỉ.
    UserId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới người dùng sở hữu địa chỉ này.
    Label NVARCHAR(100),                                -- Nhãn gợi nhớ cho địa chỉ, VD: 'Nhà riêng', 'Văn phòng công ty'.
    Recipient NVARCHAR(150) NOT NULL,                   -- Họ và tên đầy đủ của người nhận hàng tại địa chỉ này.
    Phone VARCHAR(20) NOT NULL,                         -- Số điện thoại liên lạc của người nhận hàng.
    Line1 NVARCHAR(255) NOT NULL,                       -- Địa chỉ chi tiết (số nhà, tên đường).
    Ward NVARCHAR(100),                                 -- Phường/Xã.
    District NVARCHAR(100),                             -- Quận/Huyện.
    City NVARCHAR(100),                                 -- Tỉnh/Thành phố.
    IsDefault BIT NOT NULL DEFAULT 0,                   -- Cờ xác định đây có phải là địa chỉ giao hàng mặc định hay không (1: mặc định, 0: không).
    FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE -- Nếu người dùng bị xóa, các địa chỉ của họ cũng sẽ tự động bị xóa.
);
GO

-- ===================================================================================
-- PHẦN 2: QUẢN LÝ CHUỖI CỬA HÀNG VÀ SẢN PHẨM
-- ===================================================================================
CREATE TABLE Branches (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi chi nhánh.
    ManagerId BIGINT NULL,                              -- Khóa ngoại, liên kết tới User quản lý chi nhánh này (nếu có).
    Name NVARCHAR(200) NOT NULL,                        -- Tên đầy đủ của chi nhánh, VD: 'AloTra chi nhánh Thủ Đức'.
    Slug VARCHAR(220) UNIQUE NOT NULL,                  -- Tên chi nhánh trên URL, không dấu, duy nhất, VD: 'alotra-thu-duc'.
    Address NVARCHAR(500) NOT NULL,                     -- Địa chỉ cụ thể của chi nhánh để hiển thị và giao hàng.
    Phone VARCHAR(20),                                  -- Số điện thoại liên hệ của chi nhánh.
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- Trạng thái hoạt động ('ACTIVE': đang mở cửa, 'TEMPORARILY_CLOSED': tạm đóng cửa).
    FOREIGN KEY (ManagerId) REFERENCES Users(Id)
);
GO

CREATE TABLE Categories (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi danh mục.
    ParentId BIGINT NULL,                               -- Liên kết tới chính nó, để tạo danh mục đa cấp (VD: Danh mục cha 'Trà Sữa' có danh mục con 'Trà Sữa Trái Cây').
    Name NVARCHAR(150) NOT NULL,                        -- Tên hiển thị của danh mục, VD: 'Trà Sữa Truyền Thống'.
    Slug VARCHAR(180) UNIQUE NOT NULL,                  -- Tên trên URL của danh mục, VD: 'tra-sua-truyen-thong'.
    SortOrder INT DEFAULT 0,                            -- Số thứ tự ưu tiên hiển thị. Số nhỏ hơn sẽ được hiển thị trước.
    FOREIGN KEY (ParentId) REFERENCES Categories(Id)
);
GO

CREATE TABLE Sizes (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi size.
    Code VARCHAR(10) UNIQUE NOT NULL,                   -- Mã ngắn gọn của size dùng trong hệ thống, VD: 'S', 'M', 'L'.
    Name NVARCHAR(50) NOT NULL                          -- Tên đầy đủ của size hiển thị cho khách hàng, VD: 'Size Nhỏ', 'Size Vừa'.
);
GO

CREATE TABLE Toppings (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi loại topping.
    Name NVARCHAR(200) NOT NULL,                        -- Tên của topping, VD: 'Trân châu đen', 'Pudding trứng'.
    Price DECIMAL(12,2) NOT NULL,                       -- Giá tiền của topping, sẽ được cộng thêm vào giá sản phẩm chính.
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'        -- Trạng thái ('ACTIVE': đang bán, 'INACTIVE': tạm ngưng).
);
GO
USE AloTra
Go
ALTER TABLE Toppings
ADD CONSTRAINT UQ_Toppings_Name UNIQUE (Name);
CREATE TABLE Products (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi sản phẩm gốc.
    CategoryId BIGINT NOT NULL,                         -- Khóa ngoại, liên kết tới danh mục mà sản phẩm này thuộc về.
    Name NVARCHAR(255) NOT NULL,                        -- Tên chung của sản phẩm, VD: 'Trà Sữa Trân Châu Đường Đen'.
    Slug VARCHAR(280) UNIQUE NOT NULL,                  -- Tên sản phẩm trên URL, không dấu, duy nhất.
    Description NVARCHAR(MAX),                          -- Mô tả chi tiết, câu chuyện về sản phẩm.
    RatingAvg DECIMAL(3,2) NOT NULL DEFAULT 0.00,       -- Điểm đánh giá trung bình của sản phẩm, được tính toán tự động.
    RatingCount INT NOT NULL DEFAULT 0,                 -- Tổng số lượt đã đánh giá sản phẩm này.
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- Trạng thái chung của sản phẩm ('ACTIVE': đang bán, 'HIDDEN': tạm ẩn khỏi menu).
    FOREIGN KEY (CategoryId) REFERENCES Categories(Id)
);
GO

CREATE TABLE ProductVariants (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi 'biến thể' sản phẩm.
    ProductId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới sản phẩm gốc trong bảng Products.
    SizeId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới size của biến thể này (S, M, L).
    Price DECIMAL(12,2) NOT NULL,                       -- Giá bán của sản phẩm ứng với size này. Đây là giá mà khách hàng trả.
    Sku VARCHAR(100) UNIQUE NOT NULL,                   -- Mã SKU (Stock Keeping Unit), mã định danh duy nhất cho từng biến thể để quản lý tồn kho.
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- Trạng thái của riêng biến thể này (có thể chỉ bán size M và ẩn size L).
    FOREIGN KEY (ProductId) REFERENCES Products(Id) ON DELETE CASCADE,
    FOREIGN KEY (SizeId) REFERENCES Sizes(Id),
    UNIQUE (ProductId, SizeId)                          -- Ràng buộc: mỗi sản phẩm chỉ có một giá duy nhất cho mỗi size.
);
GO

CREATE TABLE BranchInventory (
    BranchId BIGINT NOT NULL,                           -- Khóa ngoại, liên kết tới chi nhánh.
    VariantId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới biến thể sản phẩm.
    StockQuantity INT NOT NULL DEFAULT 0,               -- Số lượng tồn kho hiện tại của biến thể này tại chi nhánh này.
    Status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',    -- Trạng thái tồn kho ('AVAILABLE': còn hàng, 'OUT_OF_STOCK': hết hàng).
    PRIMARY KEY (BranchId, VariantId),                  -- Khóa chính kết hợp: đảm bảo mỗi biến thể chỉ có một dòng tồn kho tại mỗi chi nhánh.
    FOREIGN KEY (BranchId) REFERENCES Branches(Id) ON DELETE CASCADE,
    FOREIGN KEY (VariantId) REFERENCES ProductVariants(Id) ON DELETE CASCADE
);
GO

CREATE TABLE ProductMedia (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng.
    ProductId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới sản phẩm mà media này thuộc về.
    Url NVARCHAR(600) NOT NULL,                         -- Đường dẫn (link) tới file ảnh/video.
    MediaType VARCHAR(20) NOT NULL DEFAULT 'IMAGE',     -- Loại media ('IMAGE' hoặc 'VIDEO').
    IsPrimary BIT NOT NULL DEFAULT 0,                   -- Cờ xác định đây có phải là ảnh đại diện chính của sản phẩm hay không (1: chính, 0: phụ).
    FOREIGN KEY (ProductId) REFERENCES Products(Id) ON DELETE CASCADE
);
GO

-- ===================================================================================
-- PHẦN 3: MARKETING VÀ KHUYẾN MÃI
-- ===================================================================================
CREATE TABLE PromotionalCampaigns (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi chiến dịch.
    Name NVARCHAR(255) NOT NULL,                        -- Tên của chiến dịch, VD: 'Chào Hè Sôi Động', 'Mừng Đại Lễ 30/4'.
    Slug VARCHAR(280) UNIQUE NOT NULL,                  -- Tên trên URL của chiến dịch, không dấu, duy nhất.
    Description NVARCHAR(MAX) NULL,                     -- Nội dung, thể lệ chi tiết của chương trình khuyến mãi.
    BannerUrl NVARCHAR(500) NULL,                       -- Đường dẫn (link) tới ảnh banner quảng cáo cho chiến dịch.
    ViewCount BIGINT NOT NULL DEFAULT 0,                -- Bộ đếm, ghi nhận số lượt người dùng xem chi tiết chiến dịch này.
    StartAt DATETIME NOT NULL,                          -- Thời gian chiến dịch bắt đầu hiển thị và có hiệu lực.
    EndAt DATETIME NOT NULL,                            -- Thời gian chiến dịch kết thúc.
    Status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'     -- Trạng thái chiến dịch ('SCHEDULED': Sắp diễn ra, 'ACTIVE': Đang diễn ra, 'EXPIRED': Đã kết thúc).
);
GO

CREATE TABLE Coupons (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi mã giảm giá.
    CampaignId BIGINT NULL,                             -- Khóa ngoại, liên kết tới chiến dịch mà coupon này thuộc về (nếu có). NULL nếu là coupon độc lập.
    BranchId BIGINT NULL,                               -- Khóa ngoại, nếu có giá trị thì coupon này chỉ áp dụng được tại một chi nhánh cụ thể.
    Code VARCHAR(50) UNIQUE NOT NULL,                   -- Mã giảm giá mà người dùng sẽ nhập vào, VD: 'FREESHIP', 'GIAM20K'.
    Type VARCHAR(20) NOT NULL CHECK (Type IN ('ORDER_PERCENT', 'ORDER_FIXED', 'SHIPPING_PERCENT', 'SHIPPING_FIXED')), -- Phân loại coupon.
    Value DECIMAL(12,2) NOT NULL,                       -- Giá trị giảm giá (nếu type là PERCENT thì là số %, nếu là FIXED thì là số tiền).
    MaxDiscount DECIMAL(12,2) NULL,                     -- Số tiền giảm tối đa (chỉ áp dụng cho loại PERCENT).
    MinOrderTotal DECIMAL(12,2) NULL,                   -- Giá trị đơn hàng tối thiểu để có thể áp dụng coupon.
    StartAt DATETIME NOT NULL,                          -- Thời gian coupon bắt đầu có hiệu lực.
    EndAt DATETIME NOT NULL,                            -- Thời gian coupon hết hiệu lực.
    UsageLimit INT NULL,                                -- Tổng số lượt sử dụng tối đa của coupon này. NULL nghĩa là không giới hạn.
    UsedCount INT NOT NULL DEFAULT 0,                   -- Số lượt đã được sử dụng, hệ thống tự động tăng lên.
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- Trạng thái của coupon ('ACTIVE': có thể sử dụng, 'INACTIVE': tạm khóa).
    FOREIGN KEY (CampaignId) REFERENCES PromotionalCampaigns(Id),
    FOREIGN KEY (BranchId) REFERENCES Branches(Id)
);
GO
-- ===================================================================================
-- PHẦN 4: GIỎ HÀNG VÀ TƯƠNG TÁC KHÁCH HÀNG
-- ===================================================================================

CREATE TABLE Carts (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi giỏ hàng.
    UserId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới người dùng sở hữu giỏ hàng này.
    BranchId BIGINT NOT NULL,                           -- Khóa ngoại, liên kết tới chi nhánh mà người dùng đang đặt hàng.
    CONSTRAINT UQ_Cart_User_Branch UNIQUE (UserId, BranchId), -- Ràng buộc: mỗi người dùng chỉ có MỘT giỏ hàng tại MỘT chi nhánh.
    FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE,
    FOREIGN KEY (BranchId) REFERENCES Branches(Id) ON DELETE CASCADE
);
GO

CREATE TABLE CartItems (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi dòng sản phẩm trong giỏ hàng.
    CartId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới giỏ hàng cha (bảng Carts).
    VariantId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới biến thể sản phẩm (món + size) được thêm vào giỏ.
    Quantity INT NOT NULL,                              -- Số lượng của sản phẩm này trong giỏ.
    PriceAtAddition DECIMAL(12,2) NOT NULL,             -- "Ảnh chụp nhanh" (snapshot) giá của sản phẩm tại thời điểm thêm vào giỏ, để không bị ảnh hưởng nếu giá gốc thay đổi.
    FOREIGN KEY (CartId) REFERENCES Carts(Id) ON DELETE CASCADE,
    FOREIGN KEY (VariantId) REFERENCES ProductVariants(Id),
    CONSTRAINT UQ_Cart_Variant UNIQUE (CartId, VariantId) -- Ràng buộc: mỗi biến thể sản phẩm chỉ xuất hiện 1 lần trong giỏ (nếu muốn thêm sẽ tăng Quantity).
);
GO

CREATE TABLE CartItemToppings (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng.
    CartItemId BIGINT NOT NULL,                         -- Khóa ngoại, liên kết tới món hàng cụ thể trong giỏ (bảng CartItems).
    ToppingId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới loại topping đã được chọn cho món hàng đó.
    PriceAtAddition DECIMAL(12,2) NOT NULL,             -- "Ảnh chụp nhanh" giá của topping tại thời điểm thêm vào, đảm bảo giá không đổi.
    FOREIGN KEY (CartItemId) REFERENCES CartItems(Id) ON DELETE CASCADE,
    FOREIGN KEY (ToppingId) REFERENCES Toppings(Id),
    UNIQUE (CartItemId, ToppingId)                      -- Ràng buộc: mỗi loại topping chỉ được chọn 1 lần cho mỗi món hàng.
);
GO

CREATE TABLE Wishlists (
    UserId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới người dùng.
    ProductId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới sản phẩm (gốc) mà người dùng yêu thích.
    PRIMARY KEY (UserId, ProductId),                    -- Khóa chính kết hợp: đảm bảo mỗi người dùng chỉ có thể "thích" một sản phẩm một lần.
    FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE,
    FOREIGN KEY (ProductId) REFERENCES Products(Id) ON DELETE CASCADE
);
GO
-- ===================================================================================
-- PHẦN 5: ĐƠN HÀNG, THANH TOÁN VÀ VẬN CHUYỂN
-- ===================================================================================
CREATE TABLE ShippingCarriers (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi nhà vận chuyển.
    Name NVARCHAR(150) UNIQUE NOT NULL,                -- Tên nhà vận chuyển, VD: 'GrabExpress', 'ShopeeFood', 'Tự vận chuyển'.
    LogoUrl NVARCHAR(500) NULL,                         -- Đường dẫn (link) tới logo của nhà vận chuyển.
    BaseFee DECIMAL(12,2) NOT NULL DEFAULT 0,           -- Phí vận chuyển cơ bản hoặc mặc định. Hệ thống có thể tính toán phức tạp hơn dựa trên phí này.
    IsActive BIT NOT NULL DEFAULT 1                     -- Trạng thái hoạt động (1: cho phép lựa chọn, 0: tạm ẩn).
);
GO

CREATE TABLE Orders (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi đơn hàng.
    OrderCode VARCHAR(40) UNIQUE NOT NULL,              -- Mã đơn hàng duy nhất, dùng để tra cứu và giao tiếp với khách hàng, VD: 'ALOTRA-1694745600'.
    UserId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới người dùng đã đặt hàng.
    BranchId BIGINT NOT NULL,                           -- Khóa ngoại, liên kết tới chi nhánh xử lý đơn hàng này.
    CarrierId BIGINT NULL,                              -- Khóa ngoại, liên kết tới nhà vận chuyển được chọn.
    CouponId BIGINT NULL,                               -- Khóa ngoại, liên kết tới mã giảm giá đã được áp dụng (nếu có).
    ShipName NVARCHAR(150) NOT NULL,                    -- "Snapshot" tên người nhận hàng tại thời điểm đặt.
    ShipPhone VARCHAR(20) NOT NULL,                     -- "Snapshot" SĐT người nhận.
    ShipAddressText NVARCHAR(500) NOT NULL,             -- "Snapshot" địa chỉ giao hàng đầy đủ.
    SubtotalAmount DECIMAL(12,2) NOT NULL,              -- Tổng tiền hàng (chưa bao gồm giảm giá, phí vận chuyển).
    DiscountAmount DECIMAL(12,2) NOT NULL DEFAULT 0,    -- Tổng số tiền được giảm giá từ coupon.
    ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0,       -- Phí vận chuyển cuối cùng.
    TotalPayable DECIMAL(12,2) NOT NULL,                -- Tổng số tiền cuối cùng khách phải trả (Subtotal - Discount + ShippingFee).
    PaymentMethod VARCHAR(20) NOT NULL,                 -- Phương thức thanh toán ('COD', 'VNPAY', 'MOMO').
    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'UNPAID',-- Trạng thái thanh toán ('UNPAID', 'PAID', 'REFUNDED').
    OrderStatus VARCHAR(30) NOT NULL DEFAULT 'NEW',     -- Trạng thái xử lý đơn hàng ('NEW', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'CANCELLED').
    Note NVARCHAR(500) NULL,                            -- Ghi chú của khách hàng cho đơn hàng.
    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),      -- Thời gian đơn hàng được tạo.
    UpdatedAt DATETIME NOT NULL DEFAULT GETDATE(),      -- Thời gian đơn hàng được cập nhật trạng thái lần cuối.
    FOREIGN KEY (UserId) REFERENCES Users(Id),
    FOREIGN KEY (BranchId) REFERENCES Branches(Id),
    FOREIGN KEY (CarrierId) REFERENCES ShippingCarriers(Id),
    FOREIGN KEY (CouponId) REFERENCES Coupons(Id)
);
GO

CREATE TABLE OrderItems (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng.
    OrderId BIGINT NOT NULL,                            -- Khóa ngoại, liên kết tới đơn hàng cha (bảng Orders).
    VariantId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới biến thể sản phẩm đã được mua.
    ProductName NVARCHAR(255) NOT NULL,                 -- "Snapshot" tên sản phẩm tại thời điểm đặt.
    SizeName NVARCHAR(50) NOT NULL,                     -- "Snapshot" tên size tại thời điểm đặt.
    UnitPrice DECIMAL(12,2) NOT NULL,                   -- "Snapshot" đơn giá của sản phẩm tại thời điểm đặt.
    Quantity INT NOT NULL,                              -- Số lượng đã mua.
    TotalAmount DECIMAL(12,2) NOT NULL,                 -- Tổng tiền của dòng này (UnitPrice * Quantity).
    FOREIGN KEY (OrderId) REFERENCES Orders(Id) ON DELETE CASCADE,
    FOREIGN KEY (VariantId) REFERENCES ProductVariants(Id)
);
GO

CREATE TABLE OrderItemToppings (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng.
    OrderItemId BIGINT NOT NULL,                        -- Khóa ngoại, liên kết tới món hàng trong đơn hàng (bảng OrderItems).
    ToppingId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới loại topping đã được chọn.
    ToppingName NVARCHAR(200) NOT NULL,                 -- "Snapshot" tên của topping tại thời điểm đặt hàng.
    AdditionalPrice DECIMAL(12,2) NOT NULL,             -- "Snapshot" giá của topping tại thời điểm đặt hàng.
    FOREIGN KEY (OrderItemId) REFERENCES OrderItems(Id) ON DELETE CASCADE,
    FOREIGN KEY (ToppingId) REFERENCES Toppings(Id),
    UNIQUE (OrderItemId, ToppingId)                     -- Ràng buộc: mỗi loại topping chỉ được chọn 1 lần cho mỗi món hàng trong đơn.
);
GO

CREATE TABLE Payments (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi giao dịch thanh toán.
    OrderId BIGINT NOT NULL,                            -- Khóa ngoại, liên kết tới đơn hàng được thanh toán.
    Gateway VARCHAR(20) NOT NULL,                       -- Cổng thanh toán đã sử dụng, VD: 'COD', 'VNPAY', 'MOMO'.
    Amount DECIMAL(12,2) NOT NULL,                      -- Số tiền đã thanh toán.
    TransactionCode VARCHAR(100) NULL,                  -- Mã giao dịch từ cổng thanh toán trả về (nếu có).
    Status VARCHAR(20) NOT NULL,                        -- Trạng thái giao dịch ('PENDING': đang chờ, 'SUCCESS': thành công, 'FAILED': thất bại).
    PaidAt DATETIME NULL,                               -- Mốc thời gian thanh toán thành công.
    RawResponse NVARCHAR(MAX) NULL,                     -- Dữ liệu gốc (dạng JSON/XML) mà cổng thanh toán trả về, dùng để gỡ lỗi.
    FOREIGN KEY (OrderId) REFERENCES Orders(Id) ON DELETE CASCADE
);
GO

CREATE TABLE Shippers (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi hồ sơ shipper.
    UserId BIGINT UNIQUE NOT NULL,                      -- Khóa ngoại, liên kết tới tài khoản trong bảng Users có vai trò là 'SHIPPER'. UNIQUE để đảm bảo một tài khoản User chỉ có thể là một Shipper.
    CarrierId BIGINT NULL,                              -- Khóa ngoại, liên kết tới Nhà vận chuyển (ShippingCarriers) mà shipper này làm việc cho
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- Trạng thái của shipper ('ACTIVE': đang hoạt động, 'INACTIVE': tạm nghỉ).
    FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE, -- Ràng buộc khóa ngoại tới bảng Users. Nếu User bị xóa, hồ sơ Shipper này cũng bị xóa theo.
    FOREIGN KEY (CarrierId) REFERENCES ShippingCarriers(Id)    -- Ràng buộc khóa ngoại tới bảng ShippingCarriers.
);

CREATE TABLE ShippingAssignments (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi lượt phân công.
    OrderId BIGINT UNIQUE NOT NULL,                     -- Khóa ngoại, liên kết tới đơn hàng được giao. UNIQUE để đảm bảo 1 đơn hàng chỉ được gán cho 1 shipper tại một thời điểm.
    ShipperId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới shipper được phân công giao đơn hàng này.
    AssignedAt DATETIME NOT NULL DEFAULT GETDATE(),     -- Thời gian đơn hàng được phân công.
    DeliveredAt DATETIME NULL,                          -- Thời gian giao hàng thành công.
    Status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',     -- Trạng thái giao hàng ('ASSIGNED': đã gán, 'DELIVERED': đã giao, 'FAILED': giao thất bại).
    Note NVARCHAR(300) NULL,                            -- Ghi chú từ shipper, VD: 'Khách hẹn giao lại vào buổi chiều'.
    FOREIGN KEY (OrderId) REFERENCES Orders(Id),
    FOREIGN KEY (ShipperId) REFERENCES Shippers(Id)
);
GO
-- ===================================================================================
-- PHẦN 6: ĐÁNH GIÁ SẢN PHẨM
-- ===================================================================================
CREATE TABLE Reviews (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng, định danh duy nhất cho mỗi đánh giá.
    OrderItemId BIGINT NOT NULL,                        -- Khóa ngoại, liên kết tới MÓN HÀNG CỤ THỂ trong một đơn hàng đã mua.
    UserId BIGINT NOT NULL,                             -- Khóa ngoại, liên kết tới người dùng đã viết đánh giá.
    ProductId BIGINT NOT NULL,                          -- Khóa ngoại, liên kết tới sản phẩm gốc được đánh giá (để dễ dàng truy vấn).
    Rating TINYINT NOT NULL CHECK (Rating BETWEEN 1 AND 5), -- Điểm đánh giá từ 1 đến 5 sao.
    Content NVARCHAR(MAX) NOT NULL,                     -- Nội dung văn bản của bài đánh giá.
    Status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',      -- Trạng thái của đánh giá ('VISIBLE': hiển thị, 'HIDDEN': bị ẩn, 'PENDING': chờ duyệt).
    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),      -- Thời gian đánh giá được tạo.
    CONSTRAINT UQ_Review_Once UNIQUE (OrderItemId),     -- Ràng buộc quan trọng: đảm bảo mỗi món hàng đã mua chỉ được đánh giá MỘT LẦN DUY NHẤT.
    FOREIGN KEY (OrderItemId) REFERENCES OrderItems(Id),
    FOREIGN KEY (UserId) REFERENCES Users(Id),
    FOREIGN KEY (ProductId) REFERENCES Products(Id)
);
GO

CREATE TABLE ReviewMedia (
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- Khóa chính tự tăng.
    ReviewId BIGINT NOT NULL,                           -- Khóa ngoại, liên kết tới bài đánh giá cha (bảng Reviews).
    Url VARCHAR(600) NOT NULL,                          -- Đường dẫn (link) tới file ảnh/video mà người dùng tải lên.
    MediaType VARCHAR(20) NOT NULL DEFAULT 'IMAGE',     -- Phân loại media ('IMAGE' hoặc 'VIDEO').
    FOREIGN KEY (ReviewId) REFERENCES Reviews(Id) ON DELETE CASCADE -- Nếu bài đánh giá bị xóa, tất cả media liên quan cũng sẽ tự động bị xóa.
);
GO

-- ===================================================================================
-- KẾT THÚC SCRIPT
-- ===================================================================================

USE AloTra
GO
ALTER TABLE Users DROP CONSTRAINT UQ__Users__713A7B9145E128CB;
GO

-- 1️⃣ Gán giá trị mặc định cho các dòng hiện tại (nếu đang NULL)
UPDATE Users
SET FailedLoginAttempts = 0
WHERE FailedLoginAttempts IS NULL;

-- 2️⃣ Xóa ràng buộc DEFAULT cũ nếu có (tránh trùng)
DECLARE @constraintName NVARCHAR(255);
SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
WHERE OBJECT_NAME(dc.parent_object_id) = 'Users' AND c.name = 'FailedLoginAttempts';

IF @constraintName IS NOT NULL
    EXEC('ALTER TABLE Users DROP CONSTRAINT ' + @constraintName);

-- 3️⃣ Tạo lại cột với NOT NULL
ALTER TABLE Users
ALTER COLUMN FailedLoginAttempts INT NOT NULL;

-- 4️⃣ Thêm DEFAULT constraint mới
ALTER TABLE Users
ADD CONSTRAINT DF_Users_FailedLoginAttempts DEFAULT 0 FOR FailedLoginAttempts;