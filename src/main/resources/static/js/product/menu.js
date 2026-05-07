"use strict";

document.addEventListener("DOMContentLoaded", () => {
    // ============================
    // 📌 LẤY CONTEXT PATH
    // ============================
    const detectContextPath = () => {
        const parts = window.location.pathname.split('/').filter(Boolean);
        return parts.length > 0 ? '/' + parts[0] : '';
    };
    const contextPath = detectContextPath();

    // ============================
    // 🛒 GẮN SỰ KIỆN "ĐẶT MUA"
    // ============================
    const buttons = document.querySelectorAll(".menu-add-to-cart-btn");
    if (buttons.length === 0) {
        console.warn("⚠️ Không tìm thấy nút 'Đặt mua' nào trên trang.");
    }

    buttons.forEach(btn => {
        btn.addEventListener("click", async (e) => {
            e.preventDefault();
            e.stopPropagation();

            const productId = btn.dataset.productId;
            const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");

            if (!token) {
                showCartToast("⚠️ Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng", true);
                return;
            }

            try {
                const res = await fetch(`${contextPath}/api/cart/items`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`
                    },
                    body: JSON.stringify({ productId: productId, quantity: 1 })
                });

                if (!res.ok) {
                    const errData = await res.json().catch(() => null);
                    throw new Error(errData?.message || "Lỗi server");
                }

                showCartToast("✅ Đã thêm sản phẩm vào giỏ hàng!");
                bounceCartIcon();
                updateCartFloatingCount();

            } catch (err) {
                console.error(err);
                showCartToast(`❌ ${err.message}`, true);
            }
        });
    });

    // ============================
    // 🔔 TOAST THÔNG BÁO
    // ============================
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
        toast.hideTimeout = setTimeout(() => toast.classList.remove("show"), 2000);
    };

    // ============================
    // 🛒 Hiệu ứng rung icon giỏ hàng
    // ============================
    function bounceCartIcon() {
        const cartBtn = document.querySelector("#floating-cart .cart-btn");
        if (!cartBtn) return;
        cartBtn.classList.add("bounce");
        setTimeout(() => cartBtn.classList.remove("bounce"), 500);
    }

    // ============================
    // 🔄 Cập nhật số lượng giỏ hàng
    // ============================
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
        .catch(err => console.error("❌ Không thể cập nhật số lượng giỏ hàng:", err));
    }
});
