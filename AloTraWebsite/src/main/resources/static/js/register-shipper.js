"use strict";


document.addEventListener("DOMContentLoaded", async function () {
    console.log("🚚 Trang đăng ký Shipper khởi chạy...");

    const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
    if (!token) {
        showAlert("Vui lòng đăng nhập để sử dụng chức năng này.");
        window.location.href = "/alotra-website/login";
        return;
    }


    // ==== DOM ELEMENTS ====
    const avatarPreview = document.getElementById("avatarPreview");
    const avatarInput = document.getElementById("avatarInput");
    const carrierSelect = document.getElementById("carrierSelect");
    const vehicleType = document.getElementById("vehicleType");
    const vehiclePlate = document.getElementById("vehiclePlate");
    const historyContainer = document.getElementById("shipperHistoryContainer");

    const wardInput = document.getElementById("ward");
    const districtInput = document.getElementById("district");
    const cityInput = document.getElementById("city");

    // Modal edit
    const editModalEl = document.getElementById("editShipperModal");
    const editModal = new bootstrap.Modal(editModalEl);
    const editShipperId = document.getElementById("editShipperId");
    const editCarrierSelect = document.getElementById("editCarrierSelect");
    const editVehicleType = document.getElementById("editVehicleType");
    const editVehiclePlate = document.getElementById("editVehiclePlate");
    const editWard = document.getElementById("editWard");
    const editDistrict = document.getElementById("editDistrict");
    const editCity = document.getElementById("editCity");
    const btnSaveEditShipper = document.getElementById("btnSaveEditShipper");

    // ✅ Initialize autocomplete when modal is shown
    if (editModalEl) {
        editModalEl.addEventListener('shown.bs.modal', async () => {
            console.log('📝 Edit modal shown, initializing autocomplete...');
            await initEditModalAutocomplete();
        }, { once: false }); // Allow multiple initializations
    }

    // === ẢNH ĐẠI DIỆN ===
    avatarInput?.addEventListener("change", e => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = ev => avatarPreview.src = ev.target.result;
            reader.readAsDataURL(file);
        }
    });

    // === LOAD PROFILE ===
    async function loadProfile() {
        const res = await fetch("/alotra-website/api/profile", {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!res.ok) return;
        const user = await res.json();
        document.getElementById("fullName").value = user.fullName || "";
        document.getElementById("phone").value = user.phone || "";
        document.getElementById("email").value = user.email || "";
        document.getElementById("gender").value = user.gender || "";
        document.getElementById("dob").value = user.dateOfBirth || "";
        document.getElementById("idCardNumber").value = user.idCardNumber || "";
        avatarPreview.src = user.avatarUrl || "/alotra-website/images/avatar-default.jpg";
    }

    // === CẬP NHẬT PROFILE ===
    document.getElementById("btnSaveProfile").addEventListener("click", async () => {
        const file = avatarInput.files[0];
        const data = {
            fullName: document.getElementById("fullName").value,
            phone: document.getElementById("phone").value,
            gender: document.getElementById("gender").value,
            dateOfBirth: document.getElementById("dob").value,
            idCardNumber: document.getElementById("idCardNumber").value
        };

        const formData = new FormData();
        formData.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
        if (file) formData.append("file", file);

        const res = await fetch("/alotra-website/api/profile", {
            method: "PUT",
            headers: { "Authorization": `Bearer ${token}` },
            body: formData
        });

		if (res.ok) {
		               showAlert("✅ Cập nhật thông tin cá nhân thành công!");
		               await loadProfile();
		               if (window.loadNotifications) await window.loadNotifications(); // 🔔 Cập nhật chuông
		           } else {
		               showAlert("❌ Cập nhật thất bại!");
		           }
    });

    // === DANH SÁCH NHÀ VẬN CHUYỂN ===
    async function loadCarriers() {
        const res = await fetch(`/alotra-website/api/shipping-carriers/active`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!res.ok) return;
        const carriers = await res.json();
        carrierSelect.innerHTML = carriers.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        editCarrierSelect.innerHTML = carrierSelect.innerHTML;
    }

    // === GỬI YÊU CẦU SHIPPER ===
    document.getElementById("btnSubmitShipper").addEventListener("click", async () => {
        if (!carrierSelect.value || !vehicleType.value.trim() || !vehiclePlate.value.trim()) {
            showAlert("❌ Vui lòng nhập đầy đủ thông tin đăng ký.");
            return;
        }

        if (!wardInput.value.trim() || !districtInput.value.trim() || !cityInput.value.trim()) {
            showAlert("❌ Vui lòng nhập khu vực hoạt động.");
            return;
        }

        const payload = {
            carrierId: carrierSelect.value,
            vehicleType: vehicleType.value.trim(),
            vehiclePlate: vehiclePlate.value.trim(),
            ward: wardInput.value.trim(),
            district: districtInput.value.trim(),
            city: cityInput.value.trim()
        };

        const res = await fetch(`/alotra-website/api/register/shipper`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            showAlert('✅ Gửi yêu cầu thành công!');
            loadHistory();
        } else {
            const text = await res.text();
            showAlert(`❌ Gửi yêu cầu thất bại: ${text}`);
        }
    });

    // === LỊCH SỬ YÊU CẦU ===
    async function loadHistory() {
        const res = await fetch(`/alotra-website/api/register/shipper/my-request`, {
            headers: { "Authorization": `Bearer ${token}` }
        });

        // ✅ Xử lý 204 No Content
        if (res.status === 204) {
            historyContainer.innerHTML = `<p class="text-center text-muted">Chưa có yêu cầu nào</p>`;
            return;
        }

        if (!res.ok) {
            historyContainer.innerHTML = `<p class="text-center text-danger">Không thể tải lịch sử</p>`;
            return;
        }

        const shipper = await res.json();
        if (!shipper || !shipper.id) {
            historyContainer.innerHTML = `<p class="text-center text-muted">Chưa có yêu cầu nào</p>`;
            return;
        }

        const canEdit = shipper.status === 'PENDING' || shipper.status === 'REJECTED';

		historyContainer.innerHTML = `
		        <div class="border-bottom py-2 d-flex justify-content-between align-items-start">
		            <div>
		                <div><b>Nhà vận chuyển:</b> ${shipper.carrierName || 'N/A'}</div>
		                <div><b>Khu vực:</b> ${shipper.ward}, ${shipper.district}, ${shipper.city}</div>
		                <div><b>Phương tiện:</b> ${shipper.vehicleType || '(Chưa có)'}</div>
		                <div><b>Biển số:</b> ${shipper.vehiclePlate || '(Chưa có)'}</div>
		                <div><b>Trạng thái:</b>
		                    <span class="badge ${shipper.status === 'PENDING'
		                        ? 'bg-warning'
		                        : (shipper.status === 'APPROVED' ? 'bg-success' : 'bg-danger')
		                    }">${shipper.status}</span>
		                </div>
		                <div><b>Ghi chú:</b> ${shipper.adminNote || '(Không có)'}</div>
		                <small class="text-muted">${new Date(shipper.createdAt).toLocaleString('vi-VN')}</small>
		            </div>
		            ${canEdit ? `
		            <div class="d-flex flex-column gap-2 ms-2">
		                <button class="btn btn-sm btn-outline-primary" id="btnEditShipper">✏️</button>
		                <button class="btn btn-sm btn-outline-danger" id="btnDeleteShipper">🗑️</button>
		            </div>` : ''}
		        </div>
		    `;

        if (canEdit) {
            document.getElementById("btnEditShipper").addEventListener("click", () => openEditModal(shipper));
            document.getElementById("btnDeleteShipper").addEventListener("click", () => deleteShipper(shipper.id));
        }
    }

    // === MỞ MODAL CHỈNH SỬA ===
    function openEditModal(shipper) {
        editShipperId.value = shipper.id;
        editCarrierSelect.value = shipper.carrierId;
        editVehicleType.value = shipper.vehicleType;
        editVehiclePlate.value = shipper.vehiclePlate;
        editWard.value = shipper.ward;
        editDistrict.value = shipper.district;
        editCity.value = shipper.city;
        editModal.show();
    }

    // === LƯU CHỈNH SỬA ===
    btnSaveEditShipper.addEventListener("click", async () => {
        const id = editShipperId.value;
        const payload = {
            carrierId: editCarrierSelect.value,
            vehicleType: editVehicleType.value.trim(),
            vehiclePlate: editVehiclePlate.value.trim(),
            ward: editWard.value.trim(),
            district: editDistrict.value.trim(),
            city: editCity.value.trim()
        };

        const res = await fetch(`/alotra-website/api/register/shipper/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            showAlert("✅ Cập nhật yêu cầu thành công!");
            editModal.hide();
            loadHistory();
        } else {
            const text = await res.text();
            showAlert(`❌ Lỗi: ${text}`);
        }
    });

    // === XÓA YÊU CẦU ===
    async function deleteShipper(id) {
        if (!confirm("Bạn có chắc muốn xóa yêu cầu này không?")) return;

        const res = await fetch(`/alotra-website/api/register/shipper/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (res.ok) {
            showAlert("🗑️ Xóa yêu cầu thành công!");
            loadHistory();
        } else {
            const text = await res.text();
            showAlert(`❌ Không thể xóa: ${text}`);
        }
    }

    // === KHỞI TẠO ===
    async function initPage() {
        await loadProfile();
        await loadCarriers();
        await loadHistory();
    }

    await initPage();
});