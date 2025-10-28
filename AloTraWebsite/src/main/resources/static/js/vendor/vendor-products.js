"use strict";

document.addEventListener("DOMContentLoaded", () => {
    // 🧭 Lấy contextPath
    const detectContextPath = () => {
        let ctx = document.body?.dataset?.contextPath;
        if (!ctx || ctx === "/") {
            const parts = window.location.pathname.split("/").filter(Boolean);
            ctx = parts.length ? `/${parts[0]}/` : "/";
        }
        if (!ctx.endsWith("/")) ctx += "/";
        return ctx;
    };

    const contextPath = detectContextPath();
    const baseUrl = new URL(contextPath, window.location.origin).toString();

    // 🌿 DOM Elements
    const tableBody = document.getElementById("vendor-product-table-body");
    const alertContainer = document.getElementById("alert-container");
    const paginationContainer = document.getElementById("pagination-container");

    const detailModal = new bootstrap.Modal(document.getElementById("vendorProductDetailModal"));
    const detailImages = document.getElementById("detailImages");
    const detailInfo = document.getElementById("detailInfo");
    const detailVariants = document.getElementById("detailVariants");

    // 🧮 Phân trang
    let allProducts = [];
    let currentPage = 1;
    const itemsPerPage = 6;

    // 🪄 Hiển thị thông báo
    const showAlert = (message, type = "success") => {
        alertContainer.innerHTML = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>`;
    };

    // 🧭 Render phân trang
    const renderPagination = (totalItems) => {
        const totalPages = Math.ceil(totalItems / itemsPerPage);
        paginationContainer.innerHTML = "";

        if (totalPages <= 1) return; // Không cần phân trang nếu ít hơn 1 trang

        let html = `<ul class="pagination pagination-sm justify-content-center mb-0">`;
        for (let i = 1; i <= totalPages; i++) {
            html += `
                <li class="page-item ${i === currentPage ? "active" : ""}">
                    <a href="#" class="page-link" data-page="${i}">${i}</a>
                </li>`;
        }
        html += `</ul>`;
        paginationContainer.innerHTML = html;

        // Gắn sự kiện click cho từng trang
        paginationContainer.querySelectorAll(".page-link").forEach(link => {
            link.addEventListener("click", e => {
                e.preventDefault();
                currentPage = parseInt(e.target.dataset.page);
                renderTable();
            });
        });
    };

    // 🧾 Render bảng sản phẩm theo trang hiện tại
    const renderTable = () => {
        tableBody.innerHTML = "";

        if (!allProducts || allProducts.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-muted py-3">Không có sản phẩm</td>
                </tr>`;
            paginationContainer.innerHTML = "";
            return;
        }

        const start = (currentPage - 1) * itemsPerPage;
        const end = start + itemsPerPage;
        const pageItems = allProducts.slice(start, end);

        tableBody.innerHTML = pageItems.map(p => {
            const img = p.imageUrl || `${baseUrl}images/default-product.png`;
            const statusValue = p.branchInventoryStatus === "AVAILABLE" ? "ACTIVE" : "INACTIVE";

            return `
                <tr>
                    <td>
                        <img src="${img}" width="50" height="50"
                             class="rounded border" style="object-fit:cover;">
                    </td>
                    <td>${p.name}</td>
                    <td>
                        <select class="form-select form-select-sm product-status" data-id="${p.id}">
                            <option value="ACTIVE" ${statusValue === "ACTIVE" ? "selected" : ""}>Đang bán</option>
                            <option value="INACTIVE" ${statusValue === "INACTIVE" ? "selected" : ""}>Tạm ẩn</option>
                        </select>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-primary view-btn" data-id="${p.id}">
                            <i class="fas fa-eye me-1"></i> Xem chi tiết
                        </button>
                    </td>
                </tr>`;
        }).join("");

        // Gắn sự kiện đổi trạng thái sau khi render
        document.querySelectorAll(".product-status").forEach(select => {
            select.addEventListener("change", async (e) => {
                const id = e.target.dataset.id;
                const status = e.target.value;
                await updateProductStatus(id, status);
            });
        });

        renderPagination(allProducts.length);
    };

    // 📦 Load danh sách sản phẩm Vendor
    const loadVendorProducts = async () => {
        tableBody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted py-3">Đang tải...</td>
            </tr>`;

        try {
            const res = await fetch(`${baseUrl}api/vendor/products`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            allProducts = await res.json();
            currentPage = 1;
            renderTable();
        } catch (err) {
            console.error("❌ loadVendorProducts:", err);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-danger py-3">Lỗi tải dữ liệu</td>
                </tr>`;
        }
    };

    // 🔄 Cập nhật trạng thái sản phẩm
    const updateProductStatus = async (id, status) => {
        try {
            const res = await fetch(`${baseUrl}api/vendor/products/${id}/status?status=${status}`, {
                method: "PATCH"
            });
            if (res.ok) {
                showAlert("✅ Cập nhật trạng thái thành công");
            } else {
                showAlert("❌ Cập nhật thất bại", "danger");
            }
        } catch (e) {
            console.error("❌ updateProductStatus:", e);
            showAlert("Lỗi kết nối máy chủ", "danger");
        }
    };

    // 🔍 Xem chi tiết sản phẩm
    const viewProduct = async (id) => {
        try {
            const res = await fetch(`${baseUrl}api/vendor/products/${id}`);
            if (!res.ok) throw new Error("Không thể tải chi tiết sản phẩm");
            const p = await res.json();

            // Ảnh sản phẩm
            detailImages.innerHTML = (p.imageUrls?.length)
                ? p.imageUrls.map(url => `
                        <img src="${url}" class="rounded border"
                             style="height:100px;width:100px;object-fit:cover;">
                    `).join("")
                : `<img src="${baseUrl}images/default-product.png" height="100" class="rounded border">`;

            const statusBadge = p.status === "ACTIVE"
                ? `<span class="badge bg-success">Đang bán</span>`
                : `<span class="badge bg-secondary">Tạm ẩn</span>`;

            detailInfo.innerHTML = `
                <h5 class="text-primary mb-2">${p.name}</h5>
                <p><strong>Trạng thái:</strong> ${statusBadge}</p>
                <p><strong>Mô tả:</strong> ${p.description || "Không có mô tả."}</p>
            `;

            // Biến thể sản phẩm
            detailVariants.innerHTML = (p.variants?.length)
                ? p.variants.map(v => `
                        <tr>
                            <td>${v.sizeName}</td>
                            <td>${Number(v.price).toLocaleString("vi-VN")}₫</td>
                            <td><span class="badge bg-success">Còn hàng</span></td>
                        </tr>
                    `).join("")
                : `<tr>
                        <td colspan="3" class="text-center text-muted">Không có biến thể.</td>
                   </tr>`;

            detailModal.show();

        } catch (e) {
            console.error("❌ viewProduct:", e);
            showAlert("Không thể tải chi tiết sản phẩm.", "danger");
        }
    };

    // 🖱 Sự kiện click vào nút xem chi tiết
    tableBody.addEventListener("click", e => {
        const btn = e.target.closest(".view-btn");
        if (!btn) return;
        const id = btn.dataset.id;
        viewProduct(id);
    });

    // 🚀 Khởi chạy
    loadVendorProducts();
});
