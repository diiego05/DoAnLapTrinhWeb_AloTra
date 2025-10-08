"use strict";

// ===============================================================
// === KHỞI TẠO TOÀN SITE (jQuery) ===
// ===============================================================
$(document).ready(function() {
    console.log("✅ AloTra Website initialized.");

    // =================== XEM THÊM SẢN PHẨM ===================
    const productContainer = $("#product-list-container");
    const viewMoreBtn = $("#view-more-btn");

    if (productContainer.length && viewMoreBtn.length) {
        const hiddenProducts = productContainer.find(".col:gt(4)").hide();
        if (hiddenProducts.length === 0) viewMoreBtn.hide();
        viewMoreBtn.on("click", function() {
            hiddenProducts.show();
            $(this).hide();
        });
    }

    // =================== CUỘN MƯỢT ===================
    $('a[href^="#"]').on("click", function(event) {
        const href = $(this).attr("href");
        if (href.length > 1) {
            const target = $(href);
            if (target.length) {
                event.preventDefault();
                $("html, body").stop().animate({
                    scrollTop: target.offset().top
                }, 800);
            }
        }
    });

    // =================== CẤU HÌNH AJAX JWT ===================
    $.ajaxSetup({
        beforeSend: function(xhr) {
            const token = localStorage.getItem("jwt_token") || sessionStorage.getItem("jwt_token");
            if (token) xhr.setRequestHeader("Authorization", "Bearer " + token);
        },
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                alert("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                localStorage.removeItem("jwt_token");
                sessionStorage.removeItem("jwt_token");
                window.location.href = "/alotra-website/login";
            }
        }
    });

    // =================== THÊM GIỎ HÀNG TRANG DANH SÁCH ===================
    $(document).on("click", ".btn-add-to-cart", function(e) {
        e.preventDefault();

        const token = localStorage.getItem("jwt_token") || sessionStorage.getItem("jwt_token");
        if (!token) {
            alert("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
            window.location.href = "/alotra-website/login?redirect=" + encodeURIComponent(window.location.pathname);
            return;
        }

        const productId = $(this).data("product-id");
        if (!productId) {
            alert("Không tìm thấy ID sản phẩm.");
            return;
        }

        $.ajax({
            url: "/api/cart/add",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({ productId, quantity: 1 }),
            success: () => alert("✅ Đã thêm sản phẩm vào giỏ hàng!"),
            error: (xhr) => alert("Lỗi thêm giỏ hàng: " + (xhr.responseJSON?.message || "Không xác định"))
        });
    });

    // =================== ĐĂNG XUẤT ===================
    $(document).on("click", "#logoutBtn", function(e) {
        e.preventDefault();
        localStorage.removeItem("jwt_token");
        sessionStorage.removeItem("jwt_token");
        alert("Bạn đã đăng xuất.");
        window.location.href = "/alotra-website/";
    });

    // =================== KIỂM TRA TRẠNG THÁI LOGIN ===================
    window.checkLoginStatus = function() {
        const token = localStorage.getItem("jwt_token") || sessionStorage.getItem("jwt_token");
        const authLink = $("#authLink");
        const logoutNavItem = $("#logoutNavItem");

        if (token) {
            authLink.html('<i class="fas fa-user"></i> Tài khoản').attr("href", "/alotra-website/profile");
            logoutNavItem.removeClass("d-none");
        } else {
            authLink.html('<i class="fas fa-user"></i> Đăng nhập').attr("href", "/alotra-website/login");
            logoutNavItem.addClass("d-none");
        }
    };

    checkLoginStatus();
});


// ===============================================================
// === LOGIC TRANG CHI TIẾT SẢN PHẨM (Vanilla JS) ===
// ===============================================================
document.addEventListener("DOMContentLoaded", function() {
    const addToCartBtn = document.getElementById("add-to-cart-btn");
    if (!addToCartBtn) return; // Chỉ chạy ở trang chi tiết sản phẩm

    const priceDisplay = document.getElementById("product-price");
    const addToCartPrice = document.getElementById("add-to-cart-price");
    const quantityInput = document.getElementById("quantity");
    const btnMinus = document.getElementById("btn-minus");
    const btnPlus = document.getElementById("btn-plus");
    const sizeButtons = document.querySelectorAll(".size-btn");

    let currentPrice = 0;
    let currentVariantId = null;

    const formatCurrency = (v) => new Intl.NumberFormat("vi-VN").format(v) + " ₫";

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

    sizeButtons.forEach(btn => btn.addEventListener("click", () => selectSize(btn)));

    btnMinus?.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        if (qty > 1) quantityInput.value = qty - 1;
        updateTotal();
    });

    btnPlus?.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        quantityInput.value = qty + 1;
        updateTotal();
    });

    // =================== THÊM GIỎ HÀNG TRANG CHI TIẾT ===================
    addToCartBtn.addEventListener("click", () => {
        if (!currentVariantId) {
            alert("Vui lòng chọn kích cỡ!");
            return;
        }

        const token = localStorage.getItem("jwt_token") || sessionStorage.getItem("jwt_token");
        if (!token) {
            alert("Bạn cần đăng nhập để thêm sản phẩm vào giỏ hàng!");
            const redirectUrl = encodeURIComponent(window.location.pathname);
            window.location.href = `/alotra-website/login?redirect=${redirectUrl}`;
            return;
        }

        const qty = parseInt(quantityInput.value);

        fetch("/api/cart/add", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({ variantId: currentVariantId, quantity: qty })
        })
        .then(res => {
            if (res.ok) {
                alert("✅ Đã thêm sản phẩm vào giỏ hàng!");
            } else if (res.status === 401) {
                alert("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                localStorage.removeItem("jwt_token");
                sessionStorage.removeItem("jwt_token");
                window.location.href = "/alotra-website/login";
            } else {
                return res.json().then(err => {
                    alert("Lỗi: " + (err.message || "Không thể thêm vào giỏ hàng."));
                });
            }
        })
        .catch(err => console.error("❌ Lỗi fetch:", err));
    });

    // ✅ Chọn size mặc định (nhỏ nhất)
    if (sizeButtons.length > 0) selectSize(sizeButtons[0]);
});
