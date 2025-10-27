"use strict";

import { apiFetch } from '/alotra-website/js/auth-helper.js';

// ========================== 🧭 CONTEXT PATH HELPER ==========================
function getContextPath() {
    const path = window.location.pathname.split('/');
    return path.length > 1 && path[1] ? `/${path[1]}` : '';
}
const ctx = getContextPath();

document.addEventListener('DOMContentLoaded', () => {
    // ========================== 📌 DOM ELEMENTS ==========================
    const tableBody = document.getElementById('campaignTableBody');
    const modalEl = document.getElementById('campaignModal');
    const modal = new bootstrap.Modal(modalEl);
    const btnAdd = document.getElementById('btnAddCampaign');
    const btnSave = document.getElementById('btnSaveCampaign');
    const formEl = document.getElementById('campaignForm');
    const previewBanner = document.getElementById('previewBanner');
    const bannerInput = formEl.querySelector('#campaignBanner');
    const detailModal = new bootstrap.Modal(document.getElementById('campaignDetailModal'));

    const formatTime = t => t ? new Date(t).toLocaleString('vi-VN') : '';
    const statusColor = status => ({
        ACTIVE: 'success',
        EXPIRED: 'secondary',
        SCHEDULED: 'warning'
    }[status] || 'dark');

    const displayType = (type) => {
        switch (type) {
            case 'ORDER_PERCENT': return 'Giảm % trên đơn hàng';
            case 'ORDER_FIXED': return 'Giảm tiền trên đơn hàng';
            case 'SHIPPING_PERCENT': return 'Giảm % phí vận chuyển';
            case 'SHIPPING_FIXED': return 'Giảm tiền phí vận chuyển';
            default: return type || '-';
        }
    };

    const formatValue = (type, value) => {
        if (value == null) return '-';
        if (type.endsWith('PERCENT')) {
            return `${value}%`;
        } else {
            return `${Number(value).toLocaleString('vi-VN')} đ`;
        }
    };

    let campaignCache = [];

    // ========================== 📥 LOAD DANH SÁCH ==========================
    async function loadCampaigns() {
        try {
            const res = await apiFetch(`/api/admin/promotions/campaigns`);
            if (!res.ok) throw new Error(`Lỗi tải campaign: ${res.status}`);
            campaignCache = await res.json();

            if (campaignCache.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="9" class="text-center text-muted">Không có dữ liệu</td></tr>`;
                return;
            }

            tableBody.innerHTML = campaignCache.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.name}</td>
                    <td>${displayType(c.type)}</td>
                    <td>${formatValue(c.type, c.value)}</td>
                    <td>${formatTime(c.startAt)}<br>→ ${formatTime(c.endAt)}</td>
                    <td class="text-center fw-bold">${c.viewCount ?? 0}</td> <!-- 🆕 Lượt xem -->
                    <td>${c.bannerUrl ? `<img src="${c.bannerUrl}" class="img-fluid rounded" style="max-height:60px;">` : '-'}</td>
                    <td><span class="badge bg-${statusColor(c.status)}">${c.status}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-success me-1" onclick="viewCampaign(${c.id})">
                            <i class="fa fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-primary me-1" onclick="editCampaign(${c.id})">
                            <i class="fa fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteCampaign(${c.id})">
                            <i class="fa fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `).join('');
        } catch (err) {
            console.error("❌ Lỗi load danh sách chiến dịch:", err);
            tableBody.innerHTML = `<tr><td colspan="9" class="text-center text-danger">Không thể tải dữ liệu</td></tr>`;
        }
    }

    // ========================== 🆕 THÊM MỚI ==========================
    btnAdd.addEventListener('click', () => {
        formEl.reset();
        formEl.querySelector('#campaignId').value = '';
        previewBanner.classList.add('d-none');
        previewBanner.src = '';
        modal.show();
    });

    // ========================== 💾 LƯU ==========================
    btnSave.addEventListener('click', async () => {
        try {
            const formData = new FormData(formEl);
            const id = formData.get('id');
            const url = id
                ? `${ctx}/api/admin/promotions/campaigns/${id}`
                : `${ctx}/api/admin/promotions/campaigns`;
            const method = id ? 'PUT' : 'POST';

            const response = await fetch(url, { method, body: formData });
            if (!response.ok) throw new Error(`Lỗi ${method}: ${response.status}`);

            modal.hide();
            document.activeElement.blur();
            await loadCampaigns();
        } catch (err) {
            console.error("❌ Lỗi khi lưu chiến dịch:", err);
            showAlert("Đã xảy ra lỗi khi lưu chiến dịch!");
        }
    });

    // ========================== ✏️ SỬA ==========================
    window.editCampaign = (id) => {
        const c = campaignCache.find(x => x.id === id);
        if (!c) return;

        formEl.querySelector('#campaignId').value = c.id;
        formEl.querySelector('#campaignName').value = c.name;
        formEl.querySelector('#campaignDescription').value = c.description ?? '';
        formEl.querySelector('#campaignType').value = c.type;
        formEl.querySelector('#campaignValue').value = c.value;
        formEl.querySelector('#campaignStart').value = c.startAt?.substring(0, 16) || '';
        formEl.querySelector('#campaignEnd').value = c.endAt?.substring(0, 16) || '';

        if (c.bannerUrl) {
            previewBanner.src = c.bannerUrl;
            previewBanner.classList.remove('d-none');
        } else {
            previewBanner.classList.add('d-none');
        }

        modal.show();
    };

    // ========================== 🗑️ XÓA ==========================
    window.deleteCampaign = async (id) => {
        if (!confirm('Xác nhận xóa chiến dịch này?')) return;
        try {
            const res = await fetch(`${ctx}/api/admin/promotions/campaigns/${id}`, { method: 'DELETE' });
            if (!res.ok) throw new Error(`Lỗi xóa: ${res.status}`);
            await loadCampaigns();
        } catch (err) {
            console.error("❌ Lỗi khi xóa chiến dịch:", err);
            showAlert("Không thể xóa chiến dịch!");
        }
    };

    // ========================== 🖼️ XEM TRƯỚC BANNER ==========================
    bannerInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = () => {
                previewBanner.src = reader.result;
                previewBanner.classList.remove('d-none');
            };
            reader.readAsDataURL(file);
        }
    });

    // ========================== 👁️ XEM CHI TIẾT ==========================
    window.viewCampaign = (id) => {
        const c = campaignCache.find(x => x.id === id);
        if (!c) return;

        document.getElementById('detailBanner').src = c.bannerUrl || '/images/placeholder.png';
        document.getElementById('detailTitle').textContent = c.name;
        document.getElementById('detailTime').textContent = `${formatTime(c.startAt)} → ${formatTime(c.endAt)}`;
        document.getElementById('detailDescription').textContent = c.description || '(Không có mô tả)';
        detailModal.show();
    };

    // ========================== 🚀 INIT ==========================
    loadCampaigns();
});
