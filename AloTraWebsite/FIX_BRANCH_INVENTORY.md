# 🏪 Hướng dẫn: Sửa lỗi Chi nhánh mới không có sản phẩm

## ❌ Vấn đề trước đây

Khi Admin duyệt yêu cầu tạo chi nhánh mới, chi nhánh được tạo **KHÔNG có sản phẩm nào**. Vendor không thể ẩn/hiện sản phẩm vì bảng `BranchInventory` không có dữ liệu.

### Nguyên nhân

Trong `RegistrationService.adminApproveBranch()`, khi Admin duyệt yêu cầu tạo chi nhánh:

```java
// ❌ CODE CŨ - CHỈ LƯU BRANCH, KHÔNG TẠO INVENTORY
Branch saved = branchRepo.save(branch);
req.setBranch(saved);
// THIẾU LOGIC TẠO INVENTORY CHO TẤT CẢ SẢN PHẨM!
```

Hệ thống **trực tiếp lưu vào repository** thay vì gọi `BranchService.save()`, dẫn đến logic tạo `BranchInventory` không được thực thi.

## ✅ Giải pháp đã áp dụng

### 1. Thêm Dependencies vào `RegistrationService`

```java
@Service
@RequiredArgsConstructor
public class RegistrationService {
    // ...existing code...
    
    // 📦 Repository để tạo inventory cho sản phẩm
    private final ProductVariantRepository productVariantRepo;
    private final BranchInventoryRepository branchInventoryRepo;
```

### 2. Sửa logic `adminApproveBranch()`

Thêm logic tạo `BranchInventory` ngay sau khi lưu chi nhánh mới:

```java
@Transactional
public BranchRegistrationRequest adminApproveBranch(Long id, String note) {
    // ...existing code...
    
    if (req.getType() == BranchRequestType.CREATE) {
        // Tạo và lưu chi nhánh
        Branch branch = new Branch();
        // ...set properties...
        Branch saved = branchRepo.save(branch);
        req.setBranch(saved);
        
        // ✅ TẠO INVENTORY CHO TẤT CẢ SẢN PHẨM (VARIANTS)
        List<ProductVariant> allVariants = productVariantRepo.findAll();
        System.out.println("📦 [RegistrationService] Creating inventory for " + 
                         allVariants.size() + " variants in new branch: " + saved.getName());
        
        for (ProductVariant variant : allVariants) {
            BranchInventory inventory = new BranchInventory();
            inventory.setBranchId(saved.getId());
            inventory.setVariantId(variant.getId());
            inventory.setStatus("AVAILABLE"); // Mặc định là AVAILABLE
            branchInventoryRepo.save(inventory);
        }
        
        System.out.println("✅ [RegistrationService] Created " + 
                         allVariants.size() + " inventory records for branch: " + saved.getName());
    }
    
    // ...existing code...
}
```

## 🔍 Cách hoạt động

### Khi Admin duyệt yêu cầu tạo chi nhánh mới:

1. **Tạo Branch** mới với thông tin từ request
2. **Lưu Branch** vào database → có `branch.id`
3. **Lấy tất cả ProductVariant** hiện có trong hệ thống
4. **Tạo BranchInventory** cho từng variant:
   - `branchId` = ID chi nhánh mới
   - `variantId` = ID của từng variant
   - `status` = "AVAILABLE" (mặc định)
5. **Lưu tất cả inventory** vào database

### Kết quả:

- ✅ Chi nhánh mới có **TẤT CẢ sản phẩm** của hệ thống
- ✅ Vendor có quyền **ẨN/HIỆN** từng sản phẩm (thay đổi status)
- ✅ Đồng bộ với logic `BranchService.save()` (khi Admin tạo chi nhánh trực tiếp)

## 📊 Ví dụ minh họa

### Trước khi sửa:

```
Branches:
├─ ID: 1, Name: "Chi nhánh A" ✅
└─ ID: 2, Name: "Chi nhánh B" ✅ (mới tạo)

BranchInventory:
├─ branchId=1, variantId=101, status=AVAILABLE
├─ branchId=1, variantId=102, status=HIDDEN
└─ (KHÔNG CÓ DỮ LIỆU CHO branchId=2) ❌
```

### Sau khi sửa:

```
Branches:
├─ ID: 1, Name: "Chi nhánh A" ✅
└─ ID: 2, Name: "Chi nhánh B" ✅ (mới tạo)

BranchInventory:
├─ branchId=1, variantId=101, status=AVAILABLE
├─ branchId=1, variantId=102, status=HIDDEN
├─ branchId=2, variantId=101, status=AVAILABLE ✅
└─ branchId=2, variantId=102, status=AVAILABLE ✅
```

## 🧪 Kiểm tra

### 1. Xem log khi Admin duyệt chi nhánh

Khi Admin duyệt yêu cầu tạo chi nhánh, console sẽ hiển thị:

```
📦 [RegistrationService] Creating inventory for 50 variants in new branch: Chi nhánh Test
✅ [RegistrationService] Created 50 inventory records for branch: Chi nhánh Test
```

### 2. Kiểm tra database

```sql
-- Lấy số lượng inventory của chi nhánh mới tạo
SELECT COUNT(*) FROM BranchInventory WHERE BranchId = <new_branch_id>;

-- Kết quả mong đợi: Bằng với tổng số ProductVariant
SELECT COUNT(*) FROM ProductVariants;
```

### 3. Kiểm tra giao diện Vendor

Sau khi Admin duyệt:
1. Đăng nhập với tài khoản Vendor vừa được duyệt
2. Vào **"Quản lý sản phẩm"**
3. Xem danh sách sản phẩm → Phải hiển thị **TẤT CẢ sản phẩm**
4. Thử ẩn/hiện sản phẩm → Phải hoạt động bình thường

## 📝 Lưu ý quan trọng

### 1. Đồng bộ với BranchService

Logic này **ĐỒNG BỘ** với `BranchService.save()`:

```java
// BranchService.save() - Khi Admin tạo chi nhánh trực tiếp
public void save(Branch branch) {
    // ...existing code...
    Branch savedBranch = branchRepository.save(branch);

    // Tạo tồn kho cho tất cả variant hiện có
    List<ProductVariant> variants = productVariantRepository.findAll();
    for (ProductVariant variant : variants) {
        BranchInventory inventory = new BranchInventory();
        inventory.setBranchId(savedBranch.getId());
        inventory.setVariantId(variant.getId());
        inventory.setStatus(INVENTORY_STATUS); // "AVAILABLE"
        branchInventoryRepository.save(inventory);
    }
}
```

### 2. Khi thêm sản phẩm mới

Khi Admin thêm **sản phẩm mới** vào hệ thống, cần tạo `BranchInventory` cho **TẤT CẢ chi nhánh hiện có**.

Kiểm tra logic trong `ProductService` hoặc `ProductVariantService`:

```java
// Khi tạo ProductVariant mới
public void saveVariant(ProductVariant variant) {
    ProductVariant saved = productVariantRepository.save(variant);
    
    // ✅ TẠO INVENTORY CHO TẤT CẢ CHI NHÁNH
    List<Branch> branches = branchRepository.findAll();
    for (Branch branch : branches) {
        BranchInventory inventory = new BranchInventory();
        inventory.setBranchId(branch.getId());
        inventory.setVariantId(saved.getId());
        inventory.setStatus("AVAILABLE");
        branchInventoryRepository.save(inventory);
    }
}
```

### 3. Xử lý chi nhánh cũ (đã tạo trước khi sửa)

Nếu có chi nhánh cũ **chưa có inventory**, chạy script SQL sau:

```sql
-- Tạo inventory cho chi nhánh thiếu dữ liệu
INSERT INTO BranchInventory (BranchId, VariantId, Status)
SELECT b.Id, v.Id, 'AVAILABLE'
FROM Branches b
CROSS JOIN ProductVariants v
WHERE NOT EXISTS (
    SELECT 1 FROM BranchInventory bi 
    WHERE bi.BranchId = b.Id AND bi.VariantId = v.Id
);
```

Hoặc tạo API endpoint để tự động fill:

```java
@PostMapping("/api/admin/branches/fix-missing-inventory")
public ResponseEntity<?> fixMissingInventory() {
    List<Branch> branches = branchRepository.findAll();
    List<ProductVariant> variants = productVariantRepository.findAll();
    
    int created = 0;
    for (Branch branch : branches) {
        for (ProductVariant variant : variants) {
            if (!branchInventoryRepository.existsByBranchIdAndVariantId(
                    branch.getId(), variant.getId())) {
                BranchInventory inventory = new BranchInventory();
                inventory.setBranchId(branch.getId());
                inventory.setVariantId(variant.getId());
                inventory.setStatus("AVAILABLE");
                branchInventoryRepository.save(inventory);
                created++;
            }
        }
    }
    
    return ResponseEntity.ok(Map.of("created", created));
}
```

## ✅ Kết luận

Sau khi sửa:
- ✅ Chi nhánh mới tự động có **TẤT CẢ sản phẩm**
- ✅ Vendor có quyền **quản lý inventory** (ẩn/hiện sản phẩm)
- ✅ Đồng bộ với logic tạo chi nhánh từ Admin panel
- ✅ Khách hàng thấy đúng sản phẩm khả dụng tại từng chi nhánh

---
**Ngày cập nhật:** 27/10/2025  
**Người thực hiện:** GitHub Copilot  
**File đã sửa:** `RegistrationService.java`  
**Version:** 2.1.0
