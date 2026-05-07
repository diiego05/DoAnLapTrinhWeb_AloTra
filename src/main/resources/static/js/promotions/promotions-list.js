"use strict";

// ✅ Lấy context path tự động (ví dụ: /alotra-website)
const ctx = (() => {
    const path = window.location.pathname.split('/');
    return path.length > 1 && path[1] ? `/${path[1]}` : '';
})();

// ========================= 📌 Cấu hình phân trang =========================
let currentPage = 0;
const pageSize = 4;

document.addEventListener('DOMContentLoaded', async () => {
    await loadCampaignPage(0);
});

// ========================= 📥 Load danh sách khuyến mãi =========================
async function loadCampaignPage(page) {
    const campaignList = document.getElementById('campaignList');
    const paginationContainer = document.getElementById('paginationContainer');

    // 🌀 Loading
    campaignList.innerHTML = `
        <div class="col-12 text-center text-muted py-4">
            <div class="spinner-border text-success" role="status"></div>
            <div>Đang tải dữ liệu...</div>
        </div>
    `;

    try {
        const res = await fetch(`${ctx}/api/public/promotions?page=${page}&size=${pageSize}`);
        if (!res.ok) {
            campaignList.innerHTML = `
                <div class="col-12 text-center text-danger fw-bold py-3">
                    Không thể tải dữ liệu khuyến mãi (Lỗi ${res.status}).
                </div>`;
            return;
        }

        const data = await res.json();
        const content = data.content;

        if (!Array.isArray(content) || content.length === 0) {
            campaignList.innerHTML = `
                <div class="col-12 text-center text-muted py-3">
                    Chưa có chiến dịch nào
                </div>`;
            paginationContainer.innerHTML = '';
            return;
        }

        // 🏷️ Render danh sách chiến dịch
        campaignList.innerHTML = content.map(c => `
            <div class="col-12 col-sm-6 col-lg-3">
                <div class="card h-100 shadow-sm promo-card"
                     onclick="window.location.href='${ctx}/promotions/${c.id}'"
                     style="cursor:pointer;">
                    <img src="${c.banner}" class="card-img-top" alt="${c.name}" style="height:200px;object-fit:cover;">
                    <div class="card-body">
                        <div class="small text-muted mb-1">
                            ${formatDate(c.startAt)} - ${formatDate(c.endAt)}
                            <span class="float-end">
                                <i class="fa fa-eye"></i> ${c.viewCount}
                            </span>
                        </div>
                        <h6 class="card-title fw-semibold text-truncate">${c.name}</h6>
                    </div>
                </div>
            </div>
        `).join('');

        // 📄 Render nút phân trang
        renderPagination(data);

    } catch (error) {
        console.error("❌ Lỗi khi tải khuyến mãi:", error);
        campaignList.innerHTML = `
            <div class="col-12 text-center text-danger fw-bold py-3">
                Đã xảy ra lỗi khi tải dữ liệu.
            </div>`;
    }
}

// ========================= 📑 Phân trang =========================
function renderPagination(data) {
    const paginationContainer = document.getElementById('paginationContainer');
    paginationContainer.innerHTML = '';

    if (data.totalPages <= 1) return;

    for (let i = 0; i < data.totalPages; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === data.number ? 'active' : ''}`;

        const btn = document.createElement('button');
        btn.textContent = i + 1;
        btn.className = 'page-link';
        btn.addEventListener('click', () => {
            currentPage = i;
            loadCampaignPage(currentPage);
        });

        li.appendChild(btn);
        paginationContainer.appendChild(li);
    }
}

// ========================= 🧭 Format ngày =========================
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN');
}
