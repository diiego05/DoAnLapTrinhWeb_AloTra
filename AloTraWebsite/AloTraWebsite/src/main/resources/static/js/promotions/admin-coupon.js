"use strict";

import { apiFetch } from '/alotra-website/js/auth-helper.js';

// ========================== 🧭 CONTEXT PATH HELPER ==========================
function getContextPath() {
    const path = window.location.pathname.split('/');
    return path.length > 1 && path[1] ? `/${path[1]}` : '';
}
const ctx = getContextPath();

// ========================== 📅 FORMAT HELPER ==========================
const formatDate = (d) => d ? new Date(d).toLocaleDateString('vi-VN') : '';
const formatDateInput = (d) => d ? new Date(d).toISOString().split('T')[0] : '';

// ========================== ✨ FORMAT VALUE ==========================
const formatValue = (value, type) => {
    if (value == null) return '-';
    if (type && type.endsWith('PERCENT')) return `${value}%`;
    return `${Number(value).toLocaleString('vi-VN')} đ`;
};

// ========================== 📌 DISPLAY TYPE ==========================
const displayType = (type) => {
    switch (type) {
        case 'ORDER_PERCENT': return 'Giảm % trên đơn hàng';
        case 'ORDER_FIXED': return 'Giảm tiền trên đơn hàng';
        case 'SHIPPING_PERCENT': return 'Giảm % phí vận chuyển';
        case 'SHIPPING_FIXED': return 'Giảm tiền phí vận chuyển';
        default: return type || '-';
    }
};

document.addEventListener('DOMContentLoaded', () => {
    // ========================== 📌 DOM ELEMENTS ==========================
    const tableBody = document.getElementById('couponTableBody');
    const modal = new bootstrap.Modal(document.getElementById('couponModal'));
    const btnAdd = document.getElementById('btnAddCoupon');
    const btnSave = document.getElementById('btnSaveCoupon');
    const formEl = document.getElementById('couponForm');

    const campaignSelect = document.getElementById('couponCampaign');
    const typeInput = document.getElementById('couponType');            // hiển thị tiếng Việt
    const typeHidden = document.getElementById('couponTypeHidden');     // gửi enum gốc
    const valueInput = document.getElementById('couponValue');
    const maxDiscountGroup = document.getElementById('maxDiscountGroup');

    const modalTitle = document.getElementById('couponModalTitle') ?? document.querySelector('.modal-title');
    const campaignStartAt = document.getElementById('campaignStartAt');
    const campaignEndAt = document.getElementById('campaignEndAt');

    let campaignList = [];
    let couponCache = [];
    let editingId = null;

    // ========================== 📥 LOAD CAMPAIGN ==========================
    async function loadCampaignOptions() {
        try {
            const res = await apiFetch(`/api/admin/promotions/campaigns`);
            if (!res.ok) throw new Error(`Lỗi tải campaign: ${res.status}`);
            campaignList = await res.json();
            campaignSelect.innerHTML = `
                <option value="" disabled selected>— Vui lòng chọn chiến dịch —</option>
                ${campaignList.map(c => `<option value="${c.id}">${c.name}</option>`).join('')}
            `;
        } catch (err) {
            console.error("❌ Lỗi load campaign:", err);
            campaignSelect.innerHTML = `<option value="">(Không tải được dữ liệu)</option>`;
        }
    }

    // ========================== 📥 LOAD COUPON ==========================
    async function loadCoupons() {
        try {
            const res = await apiFetch(`/api/admin/promotions/coupons`);
            if (!res.ok) throw new Error(`Lỗi tải coupon: ${res.status}`);
            couponCache = await res.json();

            if (!Array.isArray(couponCache) || couponCache.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-muted">Không có dữ liệu</td></tr>`;
                return;
            }

            tableBody.innerHTML = couponCache.map(c => `
                <tr id="coupon-row-${c.id}">
                    <td>${c.id}</td>
                    <td><strong>${c.code}</strong></td>
                    <td>${c.campaignName || '<em class="text-muted">-</em>'}</td>
                    <td>${c.startAt ? formatDate(c.startAt) : '-'}</td>
                    <td>${c.endAt ? formatDate(c.endAt) : '-'}</td>
                    <td>${displayType(c.type)}</td>
                    <td class="text-end">${formatValue(c.value, c.type)}</td>
                    <td class="text-center">${c.usageLimit ?? '<em class="text-muted">Không giới hạn</em>'}</td>
                    <td class="text-center">${c.usedCount ?? 0}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary me-2" onclick="editCoupon(${c.id})">
                            <i class="fa fa-pen"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteCoupon(${c.id}, '${c.code}')">
                            <i class="fa fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `).join('');
        } catch (err) {
            console.error("❌ Lỗi load coupon:", err);
            tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-danger">Không thể tải dữ liệu</td></tr>`;
        }
    }

    // ========================== ⚡ SET CAMPAIGN INFO ==========================
    function setCampaignInfo(selected) {
        if (!selected) {
            campaignStartAt.value = '';
            campaignEndAt.value = '';
            typeInput.value = '';
            if (typeHidden) typeHidden.value = '';
            valueInput.value = '';
            maxDiscountGroup.style.display = 'none';
            return;
        }
        campaignStartAt.value = formatDateInput(selected.startAt);
        campaignEndAt.value = formatDateInput(selected.endAt);
        typeInput.value = displayType(selected.type);
        if (typeHidden) typeHidden.value = selected.type;
        valueInput.value = selected.value;
        maxDiscountGroup.style.display = (selected.type === 'ORDER_PERCENT') ? 'block' : 'none';
    }

    // ========================== 📅 EVENT: chọn campaign ==========================
    campaignSelect.addEventListener('change', () => {
        const selectedId = Number(campaignSelect.value);
        const selected = campaignList.find(c => c.id === selectedId);
        setCampaignInfo(selected);
    });

    // ========================== 🆕 THÊM MỚI ==========================
    btnAdd.addEventListener('click', () => {
        editingId = null;
        formEl.reset();
        setCampaignInfo(null);
        modalTitle.textContent = 'Thêm mã giảm giá';
        modal.show();
    });

    // ========================== ✏️ SỬA COUPON ==========================
    window.editCoupon = (id) => {
        const c = couponCache.find(x => x.id === id);
        if (!c) {
            alert('Không tìm thấy coupon để sửa. Vui lòng tải lại trang.');
            return;
        }

        editingId = id;
        modalTitle.textContent = 'Sửa mã giảm giá';

        document.getElementById('couponId').value = c.id;
        document.getElementById('couponCode').value = c.code;
        document.getElementById('couponMaxDiscount').value = c.maxDiscount ?? '';
        document.getElementById('couponMinOrderTotal').value = c.minOrderTotal ?? '';
        document.getElementById('couponUsageLimit').value = c.usageLimit ?? '';

        if (c.campaignName) {
            const selectedCampaign = campaignList.find(x => x.name === c.campaignName);
            if (selectedCampaign) {
                campaignSelect.value = selectedCampaign.id;
                setCampaignInfo(selectedCampaign);
            }
        }

        modal.show();
    };

    // ========================== 💾 LƯU / CẬP NHẬT ==========================
    formEl.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!campaignSelect.value) {
            alert('Vui lòng chọn chiến dịch trước khi lưu.');
            return;
        }

        const originalText = btnSave.innerHTML;
        btnSave.disabled = true;
        btnSave.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Đang lưu...`;

        try {
            const formData = new FormData(formEl);

            // Xóa field rỗng để tránh lỗi parse ở backend
            if (!formData.get('maxDiscount')) formData.delete('maxDiscount');
            if (!formData.get('minOrderTotal')) formData.delete('minOrderTotal');
            if (!formData.get('usageLimit')) formData.delete('usageLimit');

            let method = 'POST';
            let apiUrl = `${ctx}/api/admin/promotions/coupons`;
            if (editingId) {
                method = 'PUT';
                apiUrl = `${ctx}/api/admin/promotions/coupons/${editingId}`;
            }

            const res = await fetch(apiUrl, { method, body: formData });
            if (!res.ok) {
                const text = await res.text();
                console.error('❌ Server error:', text);
                throw new Error(`Lỗi ${res.status}`);
            }

            modal.hide();
            await loadCoupons();
        } catch (err) {
            console.error('❌ Lỗi khi lưu coupon:', err);
            alert(`Không thể lưu: ${err.message}`);
        } finally {
            btnSave.disabled = false;
            btnSave.innerHTML = originalText;
        }
    });

    // ========================== 🗑️ XÓA ==========================
    window.deleteCoupon = async (id, code) => {
        if (!confirm(`Bạn có chắc muốn xóa mã "${code}" không?`)) return;

        const row = document.getElementById(`coupon-row-${id}`);
        const deleteButton = row.querySelector('.btn-outline-danger');
        deleteButton.disabled = true;

        try {
            const apiUrl = `${ctx}/api/admin/promotions/coupons/${id}`;
            const res = await fetch(apiUrl, { method: 'DELETE' });
            if (!res.ok) {
                const text = await res.text();
                console.error('❌ Server error:', text);
                throw new Error(`Lỗi ${res.status}`);
            }
            row.remove();
            couponCache = couponCache.filter(c => c.id !== id);
        } catch (err) {
            console.error("❌ Lỗi xóa coupon:", err);
            alert(`Không thể xóa mã: ${err.message}`);
            deleteButton.disabled = false;
        }
    };

    // ========================== 🚀 INIT ==========================
    async function init() {
        await Promise.all([
            loadCampaignOptions(),
            loadCoupons()
        ]);
    }
    init();
});
