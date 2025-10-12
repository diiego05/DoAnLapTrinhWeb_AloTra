import { getJwtToken } from '/alotra-website/js/auth-helper.js';

document.addEventListener("DOMContentLoaded", () => {
    // ======== 🔐 LẤY TOKEN + CONTEXT PATH =========
    const token = getJwtToken();
    const detectContextPath = () => {
        let parts = window.location.pathname.split('/').filter(Boolean);
        return parts.length > 0 ? '/' + parts[0] : '';
    };
    const contextPath = detectContextPath();
    console.log("✅ contextPath:", contextPath);

    // ✅ Gọi cập nhật số lượng cart khi load trang nếu đã đăng nhập
    if (token) {
        updateCartFloatingCount();
    }

    // ======== 🛍️ CÁC BIẾN GIAO DIỆN =========
    const priceDisplay = document.getElementById("product-price");
    const addToCartBtn = document.getElementById("add-to-cart-btn");
    const addToCartPrice = document.getElementById("add-to-cart-price");
    const quantityInput = document.getElementById("quantity");
    const btnMinus = document.getElementById("btn-minus");
    const btnPlus = document.getElementById("btn-plus");
    const sizeButtons = document.querySelectorAll(".size-btn");

    let currentPrice = 0;
    let currentVariantId = null;

    // ======= 💵 ĐỊNH DẠNG GIÁ =======
    const formatCurrency = (value) =>
        new Intl.NumberFormat("vi-VN").format(value) + " ₫";

    function updateTotal() {
        const qty = parseInt(quantityInput.value);
        addToCartPrice.textContent = formatCurrency(currentPrice * qty);
    }

    function selectSize(btn) {
        sizeButtons.forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        currentPrice = parseFloat(btn.dataset.price);
        currentVariantId = btn.dataset.variantId;
        priceDisplay.textContent = formatCurrency(currentPrice);
        updateTotal();
    }

    // 🪄 Gắn sự kiện chọn size
    sizeButtons.forEach(btn => btn.addEventListener("click", () => selectSize(btn)));

    // ➖ Giảm số lượng
    btnMinus.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        if (qty > 1) quantityInput.value = qty - 1;
        updateTotal();
    });

    // ➕ Tăng số lượng
    btnPlus.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        quantityInput.value = qty + 1;
        updateTotal();
    });

    // ================== 🛒 THÊM VÀO GIỎ HÀNG ==================
    addToCartBtn.addEventListener("click", async () => {
        if (!token) {
            showCartToast("⚠️ Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng", true);
            return;
        }
        if (!currentVariantId) {
            showCartToast("⚠️ Vui lòng chọn kích cỡ sản phẩm", true);
            return;
        }

        const quantity = parseInt(quantityInput.value);

        try {
            const res = await fetch(`${contextPath}/api/cart/items`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({
                    variantId: currentVariantId,
                    quantity: quantity
                })
            });

            if (!res.ok) {
                let errMsg = "Không xác định";
                try {
                    const data = await res.json();
                    errMsg = data.message || errMsg;
                } catch {}
                throw new Error(errMsg);
            }

            showCartToast("✅ Đã thêm sản phẩm vào giỏ hàng!");
            bounceCartIcon();
            updateCartFloatingCount();

        } catch (error) {
            showCartToast(`❌ Thêm giỏ hàng thất bại: ${error.message}`, true);
        }
    });

    // ✅ Chọn size mặc định khi load trang
    if (sizeButtons.length > 0) selectSize(sizeButtons[0]);

    // ================== 🔔 TOAST THÔNG BÁO ==================
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

    // 🛍 Hiệu ứng rung icon giỏ hàng
    function bounceCartIcon() {
        const cartBtn = document.querySelector("#floating-cart .cart-btn");
        if (!cartBtn) return;
        cartBtn.classList.add("bounce");
        setTimeout(() => cartBtn.classList.remove("bounce"), 500);
    }

    // 🔄 Cập nhật số lượng giỏ hàng nổi
    function updateCartFloatingCount() {
        const t = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
        if (!t) return;
        fetch(`${contextPath}/api/cart`, {
            headers: { "Authorization": `Bearer ${t}` }
        })
        .then(res => res.json())
        .then(cart => {
            const badge = document.getElementById("cart-count");
            if (badge) badge.textContent = cart.itemsCount || 0;
        })
        .catch(() => console.warn("Không thể cập nhật số lượng giỏ hàng"));
    }
});
