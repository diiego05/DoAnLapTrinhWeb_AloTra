package com.alotra.service;

import com.alotra.dto.BranchDTO;
import com.alotra.entity.Branch;
import com.alotra.entity.BranchInventory;
import com.alotra.entity.CartItem;
import com.alotra.entity.ProductVariant;
import com.alotra.repository.BranchInventoryRepository;
import com.alotra.repository.BranchRepository;
import com.alotra.repository.CartItemRepository;
import com.alotra.repository.ProductVariantRepository;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BranchService {

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private ProductVariantRepository productVariantRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private BranchInventoryRepository branchInventoryRepository;

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
	private static final String INVENTORY_STATUS = "AVAILABLE"; // ✅ Đồng bộ trạng thái

	/**
	 * Tạo slug từ tên chi nhánh
	 */
	private String generateSlug(String input) {
		if (input == null)
			return "";
		String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
		String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
		String slug = NONLATIN.matcher(normalized).replaceAll("");
		return slug.toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Lấy chi nhánh theo ID
	 */
	public Branch findById(Long id) {
		return branchRepository.findById(id).orElse(null);
	}

	/**
	 * Thêm mới chi nhánh + tự động tạo tồn kho mặc định cho tất cả biến thể sản
	 * phẩm
	 */
	public void save(Branch branch) {
		// Tạo slug
		branch.setSlug(generateSlug(branch.getName()));
		Branch savedBranch = branchRepository.save(branch);

		// Tạo tồn kho cho tất cả variant hiện có
		List<ProductVariant> variants = productVariantRepository.findAll();
		for (ProductVariant variant : variants) {
			BranchInventory inventory = new BranchInventory();
			inventory.setBranchId(savedBranch.getId());
			inventory.setVariantId(variant.getId());
			inventory.setStatus(INVENTORY_STATUS);
			branchInventoryRepository.save(inventory);
		}
	}

	/**
	 * Xóa chi nhánh theo ID
	 */
	public void deleteById(Long id) {
		branchRepository.deleteById(id);
	}

	/**
	 * Tìm kiếm và lọc chi nhánh
	 */
	public List<Branch> searchByKeywordAndStatus(String keyword, String status) {
		return branchRepository.searchAndFilter(StringUtils.hasText(keyword) ? keyword : null,
				StringUtils.hasText(status) ? status : null);
	}

	/**
	 * Lấy toàn bộ chi nhánh
	 */
	public List<Branch> getAllBranches() {
		return branchRepository.findAll();
	}

	/**
	 * Lấy toàn bộ chi nhánh có trạng thái ACTIVE
	 */

	public List<BranchDTO> getAllBranchesActiveDTO() {
		return branchRepository.findByStatus("ACTIVE").stream().map(
				b -> new BranchDTO(b.getId(), b.getName(), b.getSlug(), b.getAddress(), b.getPhone(), b.getStatus()))
				.toList();
	}

	/**
	 * Kiểm tra danh sách CartItem có khả dụng tại chi nhánh không
	 *
	 * @return danh sách cartItemId KHÔNG khả dụng
	 */
	public List<Long> checkCartItemAvailability(Long branchId, List<Long> cartItemIds) {
		List<CartItem> items = cartItemRepository.findAllById(cartItemIds);

		return items.stream().filter(item -> !branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(branchId,
				item.getVariant().getId(), INVENTORY_STATUS // ✅ dùng AVAILABLE
		)).map(CartItem::getId).collect(Collectors.toList());
	}

	/**
	 * Lấy danh sách chi nhánh có đầy đủ các variant đang đặt
	 */
	public List<Branch> findAvailableBranches(List<Long> variantIds) {
		var allBranches = branchRepository.findByStatus("ACTIVE"); // ✅ chỉ lấy chi nhánh hoạt động
		return allBranches.stream().filter(branch -> variantIds.stream().allMatch(variantId -> branchInventoryRepository
				.existsByBranchIdAndVariantIdAndStatus(branch.getId(), variantId, INVENTORY_STATUS // ✅ dùng AVAILABLE
				))).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public boolean isVendorOfBranch(Long vendorId, Long branchId) {
	    Branch branch = branchRepository.findById(branchId)
	            .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
	    if (branch.getManager() == null) {
	        throw new RuntimeException("Chi nhánh này chưa có người quản lý");
	    }
	    return branch.getManager().getId().equals(vendorId);
	}


    /**
     * 📍 Lấy branchId mà vendor hiện tại đang quản lý
     */
    @Transactional(readOnly = true)
    public Long getBranchIdByVendorId(Long vendorId) {
        Branch branch = branchRepository.findByManagerId(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor này chưa quản lý chi nhánh nào"));
        return branch.getId();
    }

    /**
     * 📍 Kiểm tra vendor có phải quản lý chi nhánh đó không
     */
    @Transactional(readOnly = true)
    public void validateVendorBranch(Long vendorId, Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        if (branch.getManager() == null || !branch.getManager().getId().equals(vendorId)) {
            throw new RuntimeException("Bạn không có quyền truy cập chi nhánh này");
        }
    }

    /**
     * 📍 Lấy thông tin chi nhánh của vendor
     */
    @Transactional(readOnly = true)
    public BranchDTO getBranchInfoByVendor(Long vendorId) {
        Branch branch = branchRepository.findByManagerId(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor này chưa quản lý chi nhánh nào"));
        return new BranchDTO(branch.getId(), branch.getName(), branch.getSlug(),
                branch.getAddress(), branch.getPhone(), branch.getStatus());
    }

}
