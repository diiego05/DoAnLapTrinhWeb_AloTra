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
            const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
            if (token) xhr.setRequestHeader("Authorization", "Bearer " + token);
        },
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                alert("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                localStorage.removeItem("jwtToken");
                sessionStorage.removeItem("jwtToken");
                window.location.href = "/alotra-website/login";
            }
        }
    });


    // =================== ĐĂNG XUẤT ===================
    $(document).on("click", "#logoutBtn", function(e) {
        e.preventDefault();
        localStorage.removeItem("jwtToken");
        sessionStorage.removeItem("jwtToken");
        alert("Bạn đã đăng xuất.");
        window.location.href = "/alotra-website/";
    });

    // =================== KIỂM TRA TRẠNG THÁI LOGIN ===================
    window.checkLoginStatus = function() {
        const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
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







