const contextPath = window.location.pathname.split("/")[1]
    ? "/" + window.location.pathname.split("/")[1]
    : "";

export function getJwtToken() {
    return localStorage.getItem("jwtToken");
}

export async function apiFetch(url, options = {}) {
    const token = getJwtToken();
    const headers = {
        ...(options.headers || {}),
        ...(token ? { "Authorization": "Bearer " + token } : {}),
        "Content-Type": "application/json"
    };

    const res = await fetch(contextPath + url, { ...options, headers });

    // ✅ Nếu token tồn tại nhưng không hợp lệ → hiển thị thông báo
    if ((res.status === 401 || res.status === 403) && token) {
        localStorage.removeItem("jwtToken");
        showLoginAlert();
        return null;
    }

    return res;
}

export function requireAuth() {
    const token = getJwtToken();
    if (!token) {
        showLoginAlert();
        return false;
    }
    return true;
}

// ✅ Hàm hiển thị message box yêu cầu đăng nhập
function showLoginAlert() {
    const existing = document.getElementById("loginAlertModal");
    if (existing) return;

    const modalHtml = `
    <div class="modal fade" id="loginAlertModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header bg-warning">
            <h5 class="modal-title">Phiên đăng nhập đã hết hạn</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <p>Vui lòng đăng nhập lại để tiếp tục sử dụng các tính năng của hệ thống.</p>
          </div>
          <div class="modal-footer">
            <button id="loginNowBtn" class="btn btn-primary">Đăng nhập ngay</button>
          </div>
        </div>
      </div>
    </div>`;

    document.body.insertAdjacentHTML("beforeend", modalHtml);
    const modal = new bootstrap.Modal(document.getElementById("loginAlertModal"));
    modal.show();

    document.getElementById("loginNowBtn").addEventListener("click", () => {
        modal.hide();
        window.location.href = contextPath + "/login";
    });
}
