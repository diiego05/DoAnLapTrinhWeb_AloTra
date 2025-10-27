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
import java.util.Comparator;
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

    // 🗺️ Geocoding
    @Autowired
    private GeocodingService geocodingService;

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
     * ✅ Kiểm tra tọa độ có nằm trong phạm vi Việt Nam không
     */
    private boolean isValidVietnameseCoordinates(Double lat, Double lng) {
        if (lat == null || lng == null) return false;
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) return false;
        return lat >= 8.0 && lat <= 24.5 && lng >= 102.0 && lng <= 110.5;
    }

    /**
     * 🧹 Backfill toạ độ cho các chi nhánh chưa có hoặc không hợp lệ.
     * @return số chi nhánh được cập nhật
     */
    @Transactional
    public int backfillCoordinatesForAllBranches() {
        int updated = 0;
        List<Branch> branches = branchRepository.findAll();
        
        for (Branch b : branches) {
            if (!isValidVietnameseCoordinates(b.getLatitude(), b.getLongitude())) {
                boolean success = geocodeBranch(b);
                if (success) {
                    updated++;
                }
            }
        }
        
        if (updated > 0) {
            branchRepository.saveAll(branches);
            System.out.println("✅ [BranchService] Backfilled coordinates for " + updated + " branches");
        }
        
        return updated;
    }

    /**
     * 🗺️ Geocode một chi nhánh (có retry logic)
     */
    private boolean geocodeBranch(Branch branch) {
        String addr = branch.getAddress();
        if (!StringUtils.hasText(addr)) {
            System.out.println("⚠️ [BranchService] Branch " + branch.getId() + " has no address");
            return false;
        }

        // Thử 1: Địa chỉ gốc
        var llOpt = geocodingService.geocodeAddress(addr.trim());
        
        // Thử 2: Thêm ", Vietnam" nếu chưa có
        if (llOpt.isEmpty()) {
            String q = addr.trim();
            if (!q.toLowerCase(Locale.ROOT).contains("vietnam") && 
                !q.toLowerCase(Locale.ROOT).contains("việt nam")) {
                q = q + ", Vietnam";
                llOpt = geocodingService.geocodeAddress(q);
            }
        }

        if (llOpt.isPresent()) {
            var ll = llOpt.get();
            if (isValidVietnameseCoordinates(ll.latitude(), ll.longitude())) {
                branch.setLatitude(ll.latitude());
                branch.setLongitude(ll.longitude());
                System.out.println("✅ [BranchService] Geocoded branch " + branch.getId() + ": " + 
                                 ll.latitude() + ", " + ll.longitude());
                return true;
            } else {
                System.out.println("⚠️ [BranchService] Invalid coordinates for branch " + branch.getId() + 
                                 ": " + ll.latitude() + ", " + ll.longitude());
            }
        } else {
            System.out.println("❌ [BranchService] Failed to geocode branch " + branch.getId() + 
                             " with address: " + addr);
        }
        
        return false;
    }

	/**
	 * Lấy chi nhánh theo ID
	 */
	public Branch findById(Long id) {
		return branchRepository.findById(id).orElse(null);
	}

	/**
	 * Thêm mới chi nhánh + tự động tạo tồn kho mặc định cho tất cả biến thể sản phẩm
	 */
	public void save(Branch branch) {
		// Tạo slug
		branch.setSlug(generateSlug(branch.getName()));

        // 📍 Tính toạ độ cho địa chỉ chi nhánh nếu chưa có hoặc không hợp lệ
        if (!isValidVietnameseCoordinates(branch.getLatitude(), branch.getLongitude())) {
            geocodeBranch(branch);
        } else {
            System.out.println("✅ [BranchService] Branch already has valid coordinates: " + 
                             branch.getLatitude() + ", " + branch.getLongitude());
        }

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

	/**
	 * 📍 Tìm chi nhánh ACTIVE gần nhất theo toạ độ (lat/lng)
	 */
	@Transactional(readOnly = true)
	public BranchDTO findNearestActiveBranch(double lat, double lng) {
		// Validate input coordinates - CHẤP NHẬN phạm vi rộng hơn cho VN
		if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
			System.out.println("⚠️ [BranchService] Invalid coordinates (NaN/Infinity): lat=" + lat + ", lng=" + lng);
			return null;
		}
		
		// Kiểm tra phạm vi Việt Nam với tolerance
		if (lat < 7.5 || lat > 24.5 || lng < 101.5 || lng > 110.5) {
			System.out.println("⚠️ [BranchService] Coordinates outside Vietnam bounds: lat=" + lat + ", lng=" + lng);
			// Vẫn tiếp tục tìm kiếm thay vì return null
		}
		
		System.out.println("🗺️ [BranchService] Finding nearest ACTIVE branch:");
		System.out.println("   📍 User Location: lat=" + lat + ", lng=" + lng);
		
		var branches = branchRepository.findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull("ACTIVE");
		System.out.println("   🏪 Found " + branches.size() + " ACTIVE branches with coordinates");
		
		if (branches.isEmpty()) {
			System.out.println("   ⚠️ No ACTIVE branches with coordinates found!");
			return null;
		}
		
		// ✅ KHÔNG filter theo validation - chỉ log warning
		var branchesWithDistance = branches.stream()
			.filter(b -> {
				if (b.getLatitude() == null || b.getLongitude() == null) {
					System.out.println("   ⚠️ Branch " + b.getName() + " has null coordinates");
					return false;
				}
				if (!Double.isFinite(b.getLatitude()) || !Double.isFinite(b.getLongitude())) {
					System.out.println("   ⚠️ Branch " + b.getName() + " has invalid coordinates");
					return false;
				}
				return true;
			})
			.map(b -> {
				double distance = LocationService.haversineKm(lat, lng, b.getLatitude(), b.getLongitude());
				System.out.println("   🏢 Branch: " + b.getName());
				System.out.println("      📍 Address: " + b.getAddress());
				System.out.println("      📊 Coords: (" + b.getLatitude() + ", " + b.getLongitude() + ")");
				System.out.println("      📏 Distance: " + String.format("%.2f km", distance));
				
				return new Object() {
					final Branch branch = b;
					final double dist = distance;
				};
			})
			.sorted((a, b) -> Double.compare(a.dist, b.dist))
			.toList();
		
		if (branchesWithDistance.isEmpty()) {
			System.out.println("   ⚠️ No valid branches after filtering!");
			return null;
		}
		
		var nearest = branchesWithDistance.get(0);
		System.out.println("   ✅ Selected nearest: " + nearest.branch.getName() + 
						 " (" + String.format("%.2f km", nearest.dist) + ")");
		
		// Log top 3 để debug
		System.out.println("   📋 Top 3 nearest branches:");
		branchesWithDistance.stream().limit(3).forEach(item -> {
			System.out.println("      - " + item.branch.getName() + ": " + 
							 String.format("%.2f km", item.dist));
		});
		
		return new BranchDTO(
			nearest.branch.getId(), 
			nearest.branch.getName(), 
			nearest.branch.getSlug(), 
			nearest.branch.getAddress(), 
			nearest.branch.getPhone(), 
			nearest.branch.getStatus()
		);
	}

	/**
	 * 🏪 Lấy danh sách chi nhánh ACTIVE có sản phẩm (theo productId)
	 * @param productId ID của sản phẩm
	 * @return Danh sách BranchDTO có sản phẩm này
	 */
	@Transactional(readOnly = true)
	public List<BranchDTO> findBranchesWithProduct(Long productId) {
		System.out.println("🔍 [BranchService] Finding branches with product ID: " + productId);
		
		// ✅ Sửa: Sử dụng method đúng từ repository
		List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
		if (variants.isEmpty()) {
			System.out.println("⚠️ [BranchService] No variants found for product " + productId);
			return List.of();
		}
		
		List<Long> variantIds = variants.stream()
			.map(ProductVariant::getId)
			.collect(Collectors.toList());
		
		System.out.println("📦 [BranchService] Found " + variantIds.size() + " variants: " + variantIds);
		
		// Lấy tất cả chi nhánh ACTIVE
		List<Branch> activeBranches = branchRepository.findByStatus("ACTIVE");
		System.out.println("🏪 [BranchService] Found " + activeBranches.size() + " ACTIVE branches");
		
		// Lọc chi nhánh có ít nhất 1 variant của sản phẩm này
		List<BranchDTO> result = activeBranches.stream()
			.filter(branch -> {
				// Kiểm tra xem chi nhánh có ít nhất 1 variant của sản phẩm này không
				boolean hasProduct = variantIds.stream()
					.anyMatch(variantId -> 
						branchInventoryRepository.existsByBranchIdAndVariantIdAndStatus(
							branch.getId(), variantId, INVENTORY_STATUS
						)
					);
				
				if (hasProduct) {
					System.out.println("✅ [BranchService] Branch " + branch.getName() + " has this product");
				}
				
				return hasProduct;
			})
			.map(b -> new BranchDTO(
				b.getId(), 
				b.getName(), 
				b.getSlug(), 
				b.getAddress(), 
				b.getPhone(), 
				b.getStatus()
			))
			.collect(Collectors.toList());
		
		System.out.println("✅ [BranchService] Found " + result.size() + " branches with product " + productId);
		return result;
	}
}
