// src/main/resources/static/js/login.js
$(document).ready(function() {
    console.log("Login page JavaScript loaded!");

    $('#loginForm').on('submit', function(e) {
        e.preventDefault(); // Ngăn form submit mặc định

        const username = $('#inputEmail').val();
        const password = $('#inputPassword').val();
        const rememberMe = $('#inputRememberPassword').is(':checked');

        // Gọi API đăng nhập (AJAX)
        $.ajax({
            url: $(this).attr('action'), // Lấy URL từ thuộc tính action của form
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ username: username, password: password }),
            success: function(response) {
                // Giả sử backend trả về { token: "...", refreshToken: "..." }
                if (response.token) {
                    if (rememberMe) {
                        localStorage.setItem('jwt_token', response.token);
                        // Optional: localStorage.setItem('refresh_token', response.refreshToken);
                    } else {
                        sessionStorage.setItem('jwt_token', response.token);
                        // Optional: sessionStorage.setItem('refresh_token', response.refreshToken);
                    }
                    alert('Đăng nhập thành công!');
                    // Cập nhật trạng thái đăng nhập trên header ngay lập tức (nếu main.js có hàm checkLoginStatus)
                    if (typeof checkLoginStatus === 'function') {
                        checkLoginStatus();
                    }
                    window.location.href = '/alotra-website/'; // Chuyển hướng về trang chủ với context path
                } else {
                    alert('Đăng nhập thất bại: ' + (response.message || 'Lỗi không xác định'));
                }
            },
            error: function(xhr, status, error) {
                let errorMessage = 'Đăng nhập thất bại. Vui lòng thử lại.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                }
                alert(errorMessage);
                console.error('Lỗi đăng nhập:', status, error, xhr.responseJSON);
            }
        });
    });
});