"use strict";

// === HÀM HIỂN THỊ CONFIRM ĐĂNG XUẤT ===
function showLogoutConfirm() {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            z-index: 99998;
            opacity: 0;
            transition: opacity 0.2s ease-out;
        `;

        const dialog = document.createElement('div');
        dialog.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) scale(0.9);
            z-index: 99999;
            background: white;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
            min-width: 450px;
            max-width: 550px;
            opacity: 0;
            transition: all 0.3s ease-out;
        `;

        dialog.innerHTML = `
            <button style="position: absolute; top: 16px; right: 16px; background: transparent; border: none; font-size: 28px; color: #ccc; cursor: pointer; padding: 0; line-height: 1; width: 32px; height: 32px;" id="logoutClose">×</button>
            <div style="padding: 50px 24px 30px; text-align: center;">
                <h4 style="color: #006633; font-weight: 700; font-size: 26px; margin-bottom: 24px; letter-spacing: 1px;">THÔNG BÁO</h4>
                <p style="font-size: 16px; color: #333; margin: 0; line-height: 1.6;">Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?</p>
            </div>
            <div style="padding: 0 30px 40px; display: flex; gap: 20px; justify-content: center;">
                <button class="btn" id="logoutCancel" style="min-width: 160px; font-weight: 600; padding: 14px 28px; border-radius: 8px; font-size: 16px; background: #28a745; color: white; border: none; transition: all 0.2s;">
                    Hủy bỏ
                </button>
                <button class="btn" id="logoutConfirm" style="min-width: 160px; font-weight: 600; padding: 14px 28px; border-radius: 8px; font-size: 16px; background: #28a745; color: white; border: none; transition: all 0.2s;">
                    Đồng ý
                </button>
            </div>
        `;

        document.body.appendChild(overlay);
        document.body.appendChild(dialog);

        // Hover effect cho các button
        const buttons = dialog.querySelectorAll('.btn');
        buttons.forEach(btn => {
            btn.addEventListener('mouseenter', () => {
                btn.style.background = '#218838';
                btn.style.transform = 'translateY(-2px)';
                btn.style.boxShadow = '0 4px 12px rgba(40, 167, 69, 0.3)';
            });
            btn.addEventListener('mouseleave', () => {
                btn.style.background = '#28a745';
                btn.style.transform = 'translateY(0)';
                btn.style.boxShadow = 'none';
            });
        });

        setTimeout(() => {
            overlay.style.opacity = '1';
            dialog.style.opacity = '1';
            dialog.style.transform = 'translate(-50%, -50%) scale(1)';
        }, 10);

        const handleClose = (result) => {
            overlay.style.opacity = '0';
            dialog.style.opacity = '0';
            dialog.style.transform = 'translate(-50%, -50%) scale(0.9)';

            setTimeout(() => {
                overlay.remove();
                dialog.remove();
            }, 200);

            resolve(result);
        };

        dialog.querySelector('#logoutConfirm').onclick = () => handleClose(true);
        dialog.querySelector('#logoutCancel').onclick = () => handleClose(false);
        dialog.querySelector('#logoutClose').onclick = () => handleClose(false);
        overlay.onclick = () => handleClose(false);

        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                handleClose(false);
                document.removeEventListener('keydown', handleEsc);
            }
        };
        document.addEventListener('keydown', handleEsc);
    });
}
window.showLogoutConfirm = showLogoutConfirm;
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
                showAlert("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                localStorage.removeItem("jwtToken");
                sessionStorage.removeItem("jwtToken");
                window.location.href = "/alotra-website/login";
            }
        }
    });

    // =================== ĐĂNG XUẤT ===================
	$(document).on("click", "#logoutBtn", async function(e) {
	    e.preventDefault();

	    const confirmed = await showLogoutConfirm();
	    if (!confirmed) return;

	    try {
	        if (window.ChatWidget) {
	            window.ChatWidget.clearChatUI();
	            console.log('✅ Chat UI cleared');
	        }
	    } catch (error) {
	        console.error('❌ Error clearing chat:', error);
	    }

	    localStorage.removeItem("jwtToken");
	    sessionStorage.removeItem("jwtToken");
	    localStorage.removeItem('chatRoomId');
	    localStorage.removeItem('guestUserId');
	    localStorage.removeItem('currentChatRoomId');

	    // ✅ THÊM DÒNG NÀY
	    document.body.removeAttribute('data-user-id');

	    setTimeout(() => {
	        window.location.href = "/alotra-website/";
	    }, 100);
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