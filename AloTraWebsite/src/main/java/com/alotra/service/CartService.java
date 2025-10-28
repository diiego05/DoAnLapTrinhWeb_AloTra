package com.alotra.service;

import com.alotra.dto.ProductVariantDTO;
import com.alotra.dto.cart.*;
import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final CartItemToppingRepository cartItemToppingRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ToppingRepository toppingRepository;
	private final ProductRepository productRepository;
	private final ProductMediaRepository productMediaRepository;

	// ✅ Thêm ProductVariantService để tính giá hiện tại
	private final ProductVariantService productVariantService;

	public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
			CartItemToppingRepository cartItemToppingRepository, ProductVariantRepository productVariantRepository,
			ToppingRepository toppingRepository, ProductRepository productRepository,
			ProductMediaRepository productMediaRepository, ProductVariantService productVariantService) { // 🟢 Thêm ở
																											// đây
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.cartItemToppingRepository = cartItemToppingRepository;
		this.productVariantRepository = productVariantRepository;
		this.toppingRepository = toppingRepository;
		this.productRepository = productRepository;
		this.productMediaRepository = productMediaRepository;
		this.productVariantService = productVariantService; // 🟢 Gán
	}

	@Transactional
	public void addItem(Long userId, AddCartItemRequest req) {
		if (req.getQuantity() == null || req.getQuantity() < 1)
			req.setQuantity(1);
		Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(new Cart(userId)));
		Long variantId = req.getVariantId();
		if (variantId == null) {
			if (req.getProductId() == null)
				throw new RuntimeException("missing productId or variantId");
			variantId = productVariantRepository.findTopByProduct_IdOrderByPriceAsc(req.getProductId())
					.map(ProductVariant::getId).orElseThrow(() -> new RuntimeException("variant not found"));
		}
		ProductVariant variant = productVariantRepository.findById(variantId).orElseThrow();
		CartItem item = cartItemRepository.findByCart_IdAndVariant_Id(cart.getId(), variantId).orElse(null);
		if (item == null) {
			item = new CartItem();
			item.setCart(cart);
			item.setVariant(variant);
			item.setQuantity(req.getQuantity());
			item.setPriceAtAddition(variant.getPrice());
			item.setNote(req.getNote());
			item = cartItemRepository.save(item);
		} else {
			item.setQuantity(item.getQuantity() + req.getQuantity());
			if (req.getNote() != null && !req.getNote().isBlank())
				item.setNote(req.getNote());
			cartItemRepository.save(item);
		}
		if (req.getToppingIds() != null) {
			cartItemToppingRepository.deleteByCartItem_Id(item.getId());
			List<Topping> tops = toppingRepository.findAllById(req.getToppingIds());
			for (Topping t : tops) {
				CartItemTopping ct = new CartItemTopping();
				ct.setCartItem(item);
				ct.setTopping(t);
				ct.setPriceAtAddition(t.getPrice());
				cartItemToppingRepository.save(ct);
			}
		}
	}

	@Transactional
	public void updateItem(Long userId, Long itemId, UpdateCartItemRequest req) {
		Cart cart = cartRepository.findByUserId(userId).orElseThrow();
		CartItem item = cartItemRepository.findById(itemId).orElseThrow();
		if (!item.getCart().getId().equals(cart.getId()))
			throw new RuntimeException("forbidden");
		if (req.getQuantity() != null) {
			if (req.getQuantity() < 1)
				throw new RuntimeException("invalid quantity");
			item.setQuantity(req.getQuantity());
		}
		if (req.getVariantId() != null) {
			ProductVariant v = productVariantRepository.findById(req.getVariantId()).orElseThrow();
			Optional<CartItem> existed = cartItemRepository.findByCart_IdAndVariant_Id(cart.getId(), v.getId());
			if (existed.isPresent() && !Objects.equals(existed.get().getId(), item.getId())) {
				CartItem target = existed.get();
				target.setQuantity(target.getQuantity() + item.getQuantity());
				cartItemRepository.delete(item);
				item = target;
			} else {
				item.setVariant(v);
				item.setPriceAtAddition(v.getPrice());
			}
		}
		if (req.getNote() != null)
			item.setNote(req.getNote());
		cartItemRepository.save(item);
		if (req.getToppingIds() != null) {
			List<CartItemTopping> currentToppings = cartItemToppingRepository.findByCartItem_Id(item.getId());

			// 2. Xóa tất cả topping cũ
			if (!currentToppings.isEmpty()) {
				cartItemToppingRepository.deleteAll(currentToppings);
				// ✅ Flush để đảm bảo xóa hoàn tất trước khi insert
				cartItemToppingRepository.flush();
			}
			List<Topping> tops = toppingRepository.findAllById(req.getToppingIds());
			for (Topping t : tops) {
				CartItemTopping ct = new CartItemTopping();
				ct.setCartItem(item);
				ct.setTopping(t);
				ct.setPriceAtAddition(t.getPrice());
				cartItemToppingRepository.save(ct);
			}
			 cartItemToppingRepository.flush();
		}
	}

	@Transactional
	public void removeItem(Long userId, Long itemId) {
		Cart cart = cartRepository.findByUserId(userId).orElseThrow();
		CartItem item = cartItemRepository.findById(itemId).orElseThrow();
		if (!item.getCart().getId().equals(cart.getId()))
			throw new RuntimeException("forbidden");
		cartItemRepository.delete(item);
	}

	@Transactional
	public void clear(Long userId) {
		Cart cart = cartRepository.findByUserId(userId).orElseThrow();
		cartItemRepository.deleteByCart_Id(cart.getId());
	}

	@Transactional(readOnly = true)
	public CartResponse getCart(Long userId) {
		Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(new Cart(userId)));

		List<CartItem> items = cartItemRepository.findAllByCart_Id(cart.getId());
		List<CartItemResponse> list = new ArrayList<>();

		for (CartItem it : items) {
			ProductVariant variant = it.getVariant();

			// ✅ Lấy giá hiện tại đã tính khuyến mãi từ ProductVariantService
			BigDecimal currentPrice = productVariantService.getVariantDTOsByProductId(variant.getProduct().getId())
					.stream().filter(v -> v.getId().equals(variant.getId())).findFirst()
					.map(ProductVariantDTO::getPrice).orElse(variant.getPrice());

			CartItemResponse r = new CartItemResponse();
			r.setId(it.getId());
			r.setVariantId(variant.getId());
			r.setUnitPrice(currentPrice);
			r.setQuantity(it.getQuantity());
			r.setNote(it.getNote());

			Product p = variant.getProduct();
			r.setProductId(p.getId());
			r.setProductName(p.getName());
			r.setImageUrl(productMediaRepository.findFirstByProduct_IdAndIsPrimaryTrueOrderByIdAsc(p.getId())
					.map(ProductMedia::getUrl).orElseGet(() -> productMediaRepository
							.findFirstByProduct_IdOrderByIdAsc(p.getId()).map(ProductMedia::getUrl).orElse(null)));

			Size s = variant.getSize();
			r.setSizeId(s != null ? s.getId() : null);
			r.setSizeName(s != null ? s.getName() : null);

			// 🧋 Topping giữ nguyên giá lúc thêm
			List<CartItemToppingDTO> ts = it.getToppings().stream().map(t -> {
				CartItemToppingDTO d = new CartItemToppingDTO();
				d.setId(t.getId());
				d.setToppingId(t.getTopping().getId());
				d.setName(t.getTopping().getName());
				d.setPrice(t.getPriceAtAddition());
				return d;
			}).collect(Collectors.toList());
			r.setToppings(ts);

			BigDecimal toppingTotal = ts.stream().map(CartItemToppingDTO::getPrice).reduce(BigDecimal.ZERO,
					BigDecimal::add);

			BigDecimal line = currentPrice.add(toppingTotal).multiply(BigDecimal.valueOf(it.getQuantity()));

			r.setLineTotal(line);
			list.add(r);
		}

		CartResponse resp = new CartResponse();
		resp.setItems(list);
		resp.setItemsCount(list.size());
		BigDecimal subtotal = list.stream().map(CartItemResponse::getLineTotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		resp.setSubtotal(subtotal);
		resp.setShippingFee(BigDecimal.ZERO);
		resp.setTotal(subtotal);
		return resp;
	}

	// 🆕 Thêm hàm removeItems
	@Transactional
	public void removeItems(Long userId, List<Long> itemIds) {
		for (Long id : itemIds) {
			removeItem(userId, id);
		}
	}

	public List<CartItemDetail> getItemDetailsByIds(Long userId, List<Long> cartItemIds) {
		List<CartItem> cartItems = cartItemRepository.findAllByCart_UserIdAndIdIn(userId, cartItemIds);

		return cartItems.stream().map(ci -> {
			var variant = ci.getVariant();
			var product = variant.getProduct();

			// ✅ Lấy giá khuyến mãi hiện tại từ ProductVariantService
			BigDecimal currentPrice = productVariantService.getVariantDTOsByProductId(variant.getProduct().getId())
					.stream().filter(v -> v.getId().equals(variant.getId())).findFirst()
					.map(ProductVariantDTO::getPrice).orElse(ci.getPriceAtAddition());

			List<CartItemDetail.ToppingEntry> toppings = ci.getToppings().stream()
					.map(t -> new CartItemDetail.ToppingEntry(t.getTopping().getId(), t.getTopping().getName(),
							t.getPriceAtAddition()))
					.toList();

			BigDecimal toppingTotal = toppings.stream().map(CartItemDetail.ToppingEntry::getPrice)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			return new CartItemDetail(ci.getId(), product.getId(), product.getName(), variant.getId(),
					variant.getSize().getName(), ci.getQuantity(), ci.getNote(), currentPrice, // ✅ giá đã có khuyến mãi
					toppingTotal, toppings);
		}).toList();
	}

	/** DTO nội bộ cho OrderService sử dụng */
	@lombok.Data
	@lombok.AllArgsConstructor
	@lombok.NoArgsConstructor
	@lombok.Builder // 👉 có thể bỏ nếu không cần builder
	public static class CartItemDetail {
		private Long cartItemId;
		private Long productId;
		private String productName;
		private Long variantId;
		private String sizeName;
		private Integer quantity;
		private String note;
		private java.math.BigDecimal unitPrice;
		private java.math.BigDecimal toppingTotalEach;
		private java.util.List<ToppingEntry> toppings;

		@lombok.Data
		@lombok.AllArgsConstructor
		@lombok.NoArgsConstructor
		public static class ToppingEntry {
			private Long toppingId;
			private String name;
			private java.math.BigDecimal price;
		}
	}

}
