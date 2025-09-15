"use strict";

$(document).ready(function() {
    console.log("AloTra Website - Frontend Initialized!");

    // ===================================================================
    // LOGIC CHO NÚT "XEM THÊM" SẢN PHẨM
    // ===================================================================
    const productContainer = $("#product-list-container");
    const viewMoreBtn = $("#view-more-btn");

    if (productContainer.length && viewMoreBtn.length) {
        const hiddenProducts = productContainer.find(".col:gt(4)").hide();
        if (hiddenProducts.length === 0) {
            viewMoreBtn.hide();
        }
        viewMoreBtn.on('click', function() {
            hiddenProducts.show();
            $(this).hide();
        });
    }

    // ===================================================================
    // HIỆU ỨNG CUỘN MƯỢT (SMOOTH SCROLL) - ĐÃ SỬA LỖI
    // ===================================================================
    $('a[href^="#"]').on('click', function(event) {
        const href = $(this).attr('href');

        // Chỉ thực hiện cuộn mượt nếu href không phải là "#" đơn thuần
        if (href.length > 1) {
            const target = $(href);
            if (target.length) {
                event.preventDefault();
                $('html, body').stop().animate({
                    scrollTop: target.offset().top
                }, 800);
            }
        }
    });

    // ===================================================================
    // CẤU HÌNH AJAX TOÀN CỤC ĐỂ GỬI JWT
    // ===================================================================
    $.ajaxSetup({
        beforeSend: function(xhr) {
            const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
            if (token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        },
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                if (window.location.pathname.indexOf('/login') === -1) {
                    alert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    localStorage.removeItem('jwt_token');
                    sessionStorage.removeItem('jwt_token');
                    window.location.href = '/alotra-website/login';
                }
            }
        }
    });

    // ... (Các chức năng khác giữ nguyên) ...

    // ===================================================================
    // CHỨC NĂNG THÊM VÀO GIỎ HÀNG
    // ===================================================================
    $(document).on('click', '.btn-add-to-cart', function(e) {
        e.preventDefault();

        const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
        if (!token) {
            alert('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.');
            window.location.href = '/alotra-website/login?redirect=' + encodeURIComponent(window.location.pathname);
            return;
        }

        const productId = $(this).data('product-id');
        if (!productId) {
            alert("Không tìm thấy ID sản phẩm.");
            return;
        }

        $.ajax({
            url: '/api/cart/add',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ productId: productId, quantity: 1 }),
            success: function(response) {
                alert('Đã thêm sản phẩm vào giỏ hàng!');
            },
            error: function(xhr) {
                if (xhr.status !== 401 && xhr.status !== 403) {
                     alert('Không thể thêm sản phẩm: ' + (xhr.responseJSON ? xhr.responseJSON.message : 'Lỗi không xác định'));
                }
            }
        });
    });

    // ===================================================================
    // CHỨC NĂNG ĐĂNG XUẤT
    // ===================================================================
    $(document).on('click', '#logoutBtn', function(e) {
        e.preventDefault();
        localStorage.removeItem('jwt_token');
        sessionStorage.removeItem('jwt_token');
        alert('Bạn đã đăng xuất thành công.');
        window.location.href = '/alotra-website/';
    });

    // ===================================================================
    // KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP VÀ CẬP NHẬT GIAO DIỆN
    // ===================================================================
    window.checkLoginStatus = function() {
        const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
        const authLink = $('#authLink');
        const logoutNavItem = $('#logoutNavItem');

        if (token) {
            authLink.html('<i class="fas fa-user"></i> Tài khoản').attr('href', '/alotra-website/profile');
            logoutNavItem.removeClass('d-none');
        } else {
            authLink.html('<i class="fas fa-user"></i> Đăng nhập').attr('href', '/alotra-website/login');
            logoutNavItem.addClass('d-none');
        }
    };

    checkLoginStatus();
});