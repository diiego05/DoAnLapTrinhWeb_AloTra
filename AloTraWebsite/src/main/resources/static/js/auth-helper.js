// === Lấy context-path động ===
const contextPath = window.location.pathname.split("/")[1] ? "/" + window.location.pathname.split("/")[1] : "";

// === Lấy token JWT ===
export function getJwtToken() {
    return localStorage.getItem("jwtToken");
}

// === Gửi request có token ===
export async function apiFetch(url, options = {}) {
    const token = getJwtToken();
    const headers = {
        ...(options.headers || {}),
        "Authorization": token ? "Bearer " + token : "",
        "Content-Type": "application/json"
    };
    const res = await fetch(contextPath + url, { ...options, headers });
    if (res.status === 401 || res.status === 403) {
        localStorage.removeItem("jwtToken");
        window.location.href = contextPath + "/login";
        return null;
    }
    return res;
}

// === Kiểm tra đăng nhập khi load trang ===
export function requireAuth() {
    const token = getJwtToken();
    if (!token) {
        window.location.href = contextPath + "/login";
    }
}
