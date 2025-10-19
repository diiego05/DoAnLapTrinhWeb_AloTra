import { apiFetch, getJwtToken } from '/alotra-website/js/auth-helper.js';

document.addEventListener("DOMContentLoaded", async () => {
    const token = getJwtToken();
    const contextPath = document.body.dataset.contextPath || '';

    // ================= 🛒 GIỎ HÀNG =================
    if (token) updateCartFloatingCount();

    const priceDisplay = document.getElementById("product-price");
    const originalPriceEl = document.getElementById("product-original-price");
    const discountTagEl = document.getElementById("product-discount-percent");

    const sizeContainer = document.getElementById("size-options");
    const addToCartBtn = document.getElementById("add-to-cart-btn");
    const addToCartPrice = document.getElementById("add-to-cart-price");
    const quantityInput = document.getElementById("quantity");
    const btnMinus = document.getElementById("btn-minus");
    const btnPlus = document.getElementById("btn-plus");

    let currentPrice = 0;
    let currentOriginalPrice = 0;
    let currentDiscountPercent = 0;
    let currentVariantId = null;

    const formatCurrency = (value) =>
        new Intl.NumberFormat("vi-VN").format(value) + " ₫";

    function updateTotal() {
        const qty = parseInt(quantityInput.value);
        addToCartPrice.textContent = formatCurrency(currentPrice * qty);
    }

    function selectSize(btn) {
        document.querySelectorAll(".size-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");

        currentOriginalPrice = parseFloat(btn.dataset.originalPrice);
        currentPrice = parseFloat(btn.dataset.discountedPrice);
        currentDiscountPercent = parseInt(btn.dataset.discountPercent);
        currentVariantId = btn.dataset.variantId;

        priceDisplay.textContent = formatCurrency(currentPrice);

        // ✅ Nếu originalPrice lớn hơn discountedPrice thì mới hiển thị giá gốc và % giảm
        if (currentOriginalPrice > currentPrice) {
            originalPriceEl.textContent = formatCurrency(currentOriginalPrice);
            originalPriceEl.classList.remove("d-none");
            discountTagEl.textContent = `-${currentDiscountPercent}%`;
            discountTagEl.classList.remove("d-none");
        } else {
            originalPriceEl.classList.add("d-none");
            discountTagEl.classList.add("d-none");
        }

        updateTotal();
    }

    btnMinus.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        if (qty > 1) quantityInput.value = qty - 1;
        updateTotal();
    });

    btnPlus.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        quantityInput.value = qty + 1;
        updateTotal();
    });

    addToCartBtn.addEventListener("click", async () => {
        if (!token) {
            showCartToast("⚠️ Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng", true);
            return;
        }
        if (!currentVariantId) {
            showCartToast("⚠️ Vui lòng chọn kích cỡ sản phẩm", true);
            return;
        }

        try {
            await apiFetch(`/api/cart/items`, {
                method: "POST",
                body: JSON.stringify({
                    variantId: currentVariantId,
                    quantity: parseInt(quantityInput.value),
                }),
            });

            showCartToast("✅ Đã thêm sản phẩm vào giỏ hàng!");
            bounceCartIcon();
            updateCartFloatingCount();
        } catch (error) {
            showCartToast(`❌ Thêm giỏ hàng thất bại: ${error.message}`, true);
        }
    });

    // 🚀 GỌI API lấy biến thể sản phẩm
    async function loadProductVariants() {
        const productId = addToCartBtn.dataset.productId;
        try {
            const res = await apiFetch(`/api/products/${productId}/variants`);
            const variants = await res.json();

            if (!Array.isArray(variants) || variants.length === 0) return;
            sizeContainer.innerHTML = "";

            variants.forEach((v, index) => {
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "btn btn-outline-primary size-btn p-2";
                if (index === 0) btn.classList.add("active");

                btn.dataset.variantId = v.id;
                btn.dataset.originalPrice = v.originalPrice ?? v.price;
                btn.dataset.discountedPrice = v.discountedPrice ?? v.originalPrice ?? v.price;
                btn.dataset.discountPercent = v.discountPercent ?? 0;

                // ✅ Đổi từ sizeName sang sizeCode
                btn.innerHTML = `
                    <div class="d-flex flex-column text-center" style="min-width: 60px;">
                        <span class="fw-bold fs-5">${v.sizeCode}</span>
                    </div>
                `;

                btn.addEventListener("click", () => selectSize(btn));
                sizeContainer.appendChild(btn);

                if (index === 0) selectSize(btn);
            });

        } catch (err) {
            console.error("❌ Lỗi tải biến thể sản phẩm:", err);
        }
    }

    loadProductVariants();

    window.showCartToast = function (message, isError = false) {
        let toast = document.getElementById("cart-toast");
        if (!toast) {
            toast = document.createElement("div");
            toast.id = "cart-toast";
            toast.className = "cart-toast";
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.style.background = isError ? "#dc3545" : "#198754";
        toast.classList.add("show");
        clearTimeout(toast.hideTimeout);
        toast.hideTimeout = setTimeout(() => toast.classList.remove("show"), 2500);
    };

    function bounceCartIcon() {
        const cartBtn = document.querySelector("#floating-cart .cart-btn");
        if (!cartBtn) return;
        cartBtn.classList.add("bounce");
        setTimeout(() => cartBtn.classList.remove("bounce"), 500);
    }

    function updateCartFloatingCount() {
        const t = getJwtToken();
        if (!t) return;
        apiFetch(`/api/cart`)
            .then(res => res.json())
            .then(cart => {
                const badge = document.getElementById("cart-count");
                if (badge) badge.textContent = cart.itemsCount || 0;
            })
            .catch(() => console.warn("Không thể cập nhật số lượng giỏ hàng"));
    }

    // ================= ❤️ YÊU THÍCH =================
    const wishlistBtn = document.getElementById("wishlist-btn");
    const wishlistIcon = document.getElementById("wishlist-icon");
    const productId = Number(addToCartBtn.dataset.productId);
    let isFavorite = false;

    async function checkWishlist() {
        if (!token) return;
        try {
            const res = await apiFetch(`/api/wishlist`);
            if (!res?.ok) return;
            const data = await res.json();
            isFavorite = data.some(p => p.id === productId);
            updateWishlistIcon();
        } catch {
            console.error("Không thể tải danh sách yêu thích");
        }
    }

    function updateWishlistIcon() {
        if (isFavorite) {
            wishlistIcon.classList.remove("fa-regular");
            wishlistIcon.classList.add("fa-solid", "text-danger");
        } else {
            wishlistIcon.classList.remove("fa-solid", "text-danger");
            wishlistIcon.classList.add("fa-regular");
        }
    }

    wishlistBtn.addEventListener("click", async () => {
        if (!token) {
			showLoginModal();
			        return;
        }

        try {
            wishlistIcon.classList.add("fa-beat");
            setTimeout(() => wishlistIcon.classList.remove("fa-beat"), 400);

            if (isFavorite) {
                await apiFetch(`/api/wishlist/${productId}`, { method: "DELETE" });
                isFavorite = false;
            } else {
                await apiFetch(`/api/wishlist/${productId}`, { method: "POST" });
                isFavorite = true;
            }

            updateWishlistIcon();
            if (window.loadWishlist) window.loadWishlist();
        } catch {
            alert("Lỗi khi cập nhật danh mục yêu thích!");
        }
    });
	function showLoginModal() {
	    // Tạo overlay
		const contextPath = document.body.dataset.contextPath || '/alotra-website';
	    const overlay = document.createElement('div');
	    overlay.id = 'login-modal-overlay';
	    overlay.style.cssText = `
	        position: fixed;
	        top: 0;
	        left: 0;
	        width: 100%;
	        height: 100%;
	        background: rgba(0, 0, 0, 0.6);
	        backdrop-filter: blur(4px);
	        z-index: 9998;
	        display: flex;
	        align-items: center;
	        justify-content: center;
	        animation: fadeIn 0.2s ease;
	    `;

	    // Tạo modal
	    const modal = document.createElement('div');
	    modal.style.cssText = `
	        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	        border-radius: 20px;
	        padding: 40px 50px;
	        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
	        max-width: 450px;
	        width: 90%;
	        text-align: center;
	        animation: slideUp 0.3s ease;
	        position: relative;
	    `;

	    modal.innerHTML = `
	        <div style="color: white;">
	            <div style="font-size: 48px; margin-bottom: 20px;">
	                <i class="fas fa-heart-circle-exclamation"></i>
	            </div>
	            <h3 style="font-size: 24px; font-weight: 700; margin-bottom: 15px; letter-spacing: 0.5px;">
	                Yêu cầu đăng nhập
	            </h3>
	            <p style="font-size: 16px; opacity: 0.95; margin-bottom: 30px; line-height: 1.6;">
	                Vui lòng đăng nhập để sử dụng tính năng yêu thích!
	            </p>
	            <button id="login-modal-ok-btn" style="
	                background: white;
	                color: #667eea;
	                border: none;
	                padding: 14px 50px;
	                border-radius: 30px;
	                font-size: 16px;
	                font-weight: 700;
	                cursor: pointer;
	                transition: all 0.3s ease;
	                box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
	                text-transform: uppercase;
	                letter-spacing: 1px;
	            ">
	                Đăng nhập
	            </button>
	        </div>
	    `;

	    overlay.appendChild(modal);
	    document.body.appendChild(overlay);

	    // Thêm animation CSS
	    const style = document.createElement('style');
	    style.textContent = `
	        @keyframes fadeIn {
	            from { opacity: 0; }
	            to { opacity: 1; }
	        }
	        @keyframes slideUp {
	            from {
	                opacity: 0;
	                transform: translateY(30px) scale(0.95);
	            }
	            to {
	                opacity: 1;
	                transform: translateY(0) scale(1);
	            }
	        }
	        #login-modal-ok-btn:hover {
	            transform: translateY(-2px) scale(1.05);
	            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
	        }
	        #login-modal-ok-btn:active {
	            transform: translateY(0) scale(0.98);
	        }
	    `;
	    document.head.appendChild(style);

	    // Xử lý nút OK
	    const okBtn = document.getElementById('login-modal-ok-btn');
	    okBtn.addEventListener('click', () => {
	        window.location.href = `${contextPath}/login`;
	    });

	    // Đóng khi click overlay
	    overlay.addEventListener('click', (e) => {
	        if (e.target === overlay) {
	            overlay.style.animation = 'fadeOut 0.2s ease';
	            setTimeout(() => overlay.remove(), 200);
	        }
	    });

	    // Thêm animation fadeOut
	    style.textContent += `
	        @keyframes fadeOut {
	            from { opacity: 1; }
	            to { opacity: 0; }
	        }
	    `;
	}
    checkWishlist();
});
