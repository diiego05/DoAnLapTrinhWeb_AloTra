"use strict";

import { apiFetch } from '/alotra-website/js/auth-helper.js';
const SHIPPER_STATUS_VI={
  PENDING:"Đang chờ duyệt",
  APPROVED:"Đã duyệt",
  REJECTED:"Từ chối"
};

const shipperStatusToVi=s=>SHIPPER_STATUS_VI[s]||s||"(Không rõ)";

document.addEventListener("DOMContentLoaded", async () => {
    console.log("🚚 Quản lý yêu cầu shipper (Admin) khởi chạy...");

    const tableBody = document.getElementById("shipperRequestsTableBody");
    const statusFilter = document.getElementById("shipperStatusFilter");
    const reloadBtn = document.getElementById("reloadShipperRequests");

    // 🗃️ Cache dữ liệu để xem chi tiết
    const requestsCache = new Map();

    // ================= ❌ Modal từ chối =================
    const rejectModal = new bootstrap.Modal(document.getElementById("rejectShipperModal"));
    const rejectShipperId = document.getElementById("rejectShipperId");
    const rejectShipperNote = document.getElementById("rejectShipperNote");
    const btnRejectConfirm = document.getElementById("btnRejectShipperConfirm");

    // ================= 📄 Modal xem chi tiết =================
    const detailModal = new bootstrap.Modal(document.getElementById("shipperDetailModal"));

    // ================= 📥 LOAD DANH SÁCH =================
    async function loadShipperRequests() {
        tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-muted">Đang tải...</td></tr>`;

        const status = statusFilter.value;
        const url = status ? `/api/register/shipper?status=${status}` : `/api/register/shipper`;
        const res = await apiFetch(url);

        if (!res.ok) {
            tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-danger">Không thể tải dữ liệu</td></tr>`;
            return;
        }

        const data = await res.json();
        requestsCache.clear();
        data.forEach(r => requestsCache.set(String(r.id), r));

        if (!data || data.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-muted">Không có yêu cầu</td></tr>`;
            return;
        }

        tableBody.innerHTML = data.map((r, idx) => `
            <tr>
                <td>${idx + 1}</td>
                <td>${r.requesterName || '(N/A)'}</td>
                <td>${r.carrierName || '(N/A)'}</td>
                <td>${[r.ward, r.district, r.city].filter(Boolean).join(', ')}</td>
                <td>${r.vehicleType || ''}</td>
                <td>${r.vehiclePlate || ''}</td>
                <td>
                    <span class="badge ${r.status === 'PENDING' ? 'bg-warning' :
                                          r.status === 'APPROVED' ? 'bg-success' : 'bg-danger'}">
										  ${shipperStatusToVi(r.status)}

                    </span>
                </td>
                <td>${r.createdAt ? new Date(r.createdAt).toLocaleString('vi-VN') : ''}</td>
                <td>${r.adminNote || ''}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-info me-2 btn-view" data-id="${r.id}" title="Xem chi tiết">
                        👁️
                    </button>
                    ${r.status === 'PENDING' ? `
                        <button class="btn btn-sm btn-success me-2 btn-approve" data-id="${r.id}" title="Duyệt">
                            ✅
                        </button>
                        <button class="btn btn-sm btn-danger btn-reject" data-id="${r.id}" title="Từ chối">
                            ❌
                        </button>` : '-'}
                </td>
            </tr>
        `).join('');

        tableBody.querySelectorAll(".btn-approve").forEach(btn => {
            btn.addEventListener("click", () => approveShipper(btn.dataset.id));
        });

        tableBody.querySelectorAll(".btn-reject").forEach(btn => {
            btn.addEventListener("click", () => openRejectModal(btn.dataset.id));
        });

        tableBody.querySelectorAll(".btn-view").forEach(btn => {
            btn.addEventListener("click", () => openViewModal(btn.dataset.id));
        });
    }

    // ================= ✅ DUYỆT =================
    async function approveShipper(id) {
        if (!confirm("Xác nhận duyệt yêu cầu này?")) return;
        const res = await apiFetch(`/api/register/admin/shipper/${id}/approve`, { method: "PUT" });

        if (res.ok) {
            showAlert("✅ Đã duyệt yêu cầu thành công!");
            loadShipperRequests();
        } else {
            showAlert("❌ Lỗi duyệt yêu cầu.");
        }
    }

    // ================= ❌ TỪ CHỐI =================
    function openRejectModal(id) {
        rejectShipperId.value = id;
        rejectShipperNote.value = "";
        rejectModal.show();
    }

    btnRejectConfirm.addEventListener("click", async () => {
        const id = rejectShipperId.value;
        const note = encodeURIComponent(rejectShipperNote.value.trim());
        const res = await apiFetch(`/api/register/admin/shipper/${id}/reject?note=${note}`, { method: "PUT" });

        if (res.ok) {
            showAlert("❌ Đã từ chối yêu cầu.");
            rejectModal.hide();
            loadShipperRequests();
        } else {
            showAlert("❌ Lỗi từ chối yêu cầu.");
        }
    });

    // ================= 📄 XEM CHI TIẾT =================
    function openViewModal(id) {
        const dto = requestsCache.get(String(id));
        if (!dto) return;

        document.getElementById("detailUserName").textContent = dto.requesterName || 'N/A';
        document.getElementById("detailUserEmail").textContent = dto.requesterEmail || 'N/A';
        document.getElementById("detailUserPhone").textContent = dto.requesterPhone || 'N/A';
        document.getElementById("detailUserIdCard").textContent = dto.idCardNumber || 'N/A';
        document.getElementById("detailUserGender").textContent = dto.gender || 'N/A';
        document.getElementById("detailUserDob").textContent = dto.dob || 'N/A';

        document.getElementById("detailWard").textContent = dto.ward || 'N/A';
        document.getElementById("detailDistrict").textContent = dto.district || 'N/A';
        document.getElementById("detailCity").textContent = dto.city || 'N/A';

        document.getElementById("detailCarrier").textContent = dto.carrierName || 'N/A';
        document.getElementById("detailVehicle").textContent = dto.vehicleType || 'N/A';
        document.getElementById("detailPlate").textContent = dto.vehiclePlate || 'N/A';

        const statusEl = document.getElementById("detailStatus");
		statusEl.textContent = shipperStatusToVi(dto.status);

        statusEl.className = "badge " + (
            dto.status === "PENDING" ? "bg-warning" :
            dto.status === "APPROVED" ? "bg-success" :
            "bg-danger"
        );

        document.getElementById("detailNote").textContent = dto.adminNote || '';
        document.getElementById("detailCreated").textContent = dto.createdAt ? new Date(dto.createdAt).toLocaleString('vi-VN') : '';
        document.getElementById("detailUpdated").textContent = dto.updatedAt ? new Date(dto.updatedAt).toLocaleString('vi-VN') : '';

        detailModal.show();
    }

    // ================= 🧭 SỰ KIỆN =================
    statusFilter.addEventListener("change", loadShipperRequests);
    reloadBtn.addEventListener("click", loadShipperRequests);

    await loadShipperRequests();
});
