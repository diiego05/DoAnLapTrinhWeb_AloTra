"use strict";

// ✅ Tự động lấy context path (VD: /alotra-website)
const ctx = `/${window.location.pathname.split('/')[1]}`;

document.addEventListener('DOMContentLoaded', async () => {
    // ✅ Lấy campaignId từ URL cuối cùng
    const campaignId = window.location.pathname.split('/').pop();

    // ✅ Gọi API có context path
    const res = await fetch(`${ctx}/api/public/promotions/${campaignId}`);
    if (!res.ok) {
        document.body.innerHTML = `
            <div class="text-center my-5 text-danger fw-bold">
                ❌ Chiến dịch không tồn tại hoặc đã hết hạn (${res.status})
            </div>`;
        return;
    }

    const data = await res.json();

    // =================== GÁN DỮ LIỆU VÀO TRANG ===================
    document.getElementById('campaignBanner').src = data.banner || '/images/placeholder.png';
    document.getElementById('campaignName').textContent = data.name || '—';
    document.getElementById('campaignViews').textContent = data.viewCount ?? 0;
    document.getElementById('campaignDate').textContent = `${formatDate(data.startAt)} - ${formatDate(data.endAt)}`;
    document.getElementById('campaignDescription').innerHTML = data.description || '';

    // =================== ĐỐI TƯỢNG ÁP DỤNG ===================
    const targetList = document.getElementById('targetList');
    if (data.targetDetails && data.targetDetails.length > 0) {
        targetList.innerHTML = data.targetDetails.map(t => `<li>${t}</li>`).join('');
    } else {
        targetList.innerHTML = `<li class="text-muted">Không có đối tượng cụ thể</li>`;
    }

    // =================== MÃ GIẢM GIÁ ===================
    const couponContainer = document.getElementById('couponContainer');
    if (!data.coupons || data.coupons.length === 0) {
        couponContainer.innerHTML = `<div class="text-muted">Không có mã giảm giá</div>`;
    } else {
        couponContainer.innerHTML = data.coupons.map(c => `
            <div class="border rounded px-3 py-2 bg-light mb-2">
                <span class="fw-bold">${c.code}</span>
                <span class="text-muted ms-2">
                    ${c.type === 'PERCENT'
                        ? `${c.value}%`
                        : `${Number(c.value).toLocaleString('vi-VN')}đ`}
                </span>
            </div>
        `).join('');
    }
});

// ✅ Hàm format ngày kiểu VN
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN');
}
