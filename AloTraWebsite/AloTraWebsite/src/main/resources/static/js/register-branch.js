"use strict";

import { apiFetch } from '/alotra-website/js/auth-helper.js';

document.addEventListener("DOMContentLoaded", async () => {
    console.log("🏪 Trang đăng ký chi nhánh khởi chạy...");

    // ========= ELEMENTS =========
    const avatarPreview = document.getElementById("avatarPreview");
    const avatarInput = document.getElementById("avatarInput");
    const fullName = document.getElementById("fullName");
    const phone = document.getElementById("phone");
    const email = document.getElementById("email");
    const idCardNumber = document.getElementById("idCardNumber");
    const gender = document.getElementById("gender");
    const dob = document.getElementById("dob");
    const btnSaveProfile = document.getElementById("btnSaveProfile");

    const registerType = document.getElementById('registerType');
    const branchJoinGroup = document.getElementById('branch-join-group');
    const branchCreateGroup = document.getElementById('branch-create-group');
    const branchSelect = document.getElementById('branchSelect');
    const btnSubmitBranch = document.getElementById('btnSubmitBranch');
    const historyList = document.getElementById('historyList');

    // ========= Modal sửa =========
    const editModalEl = document.getElementById('editBranchModal');
    const editBranchName = document.getElementById('editBranchName');
    const editBranchPhone = document.getElementById('editBranchPhone');
    const editBranchAddress = document.getElementById('editBranchAddress');
    const editRequestId = document.getElementById('editRequestId');
    const btnSaveEditBranch = document.getElementById('btnSaveEditBranch');
    const editModal = new bootstrap.Modal(editModalEl);

    // ========= LOAD PROFILE =========
    async function loadProfile() {
        const res = await fetch("/alotra-website/api/profile", {
            headers: { "Authorization": `Bearer ${localStorage.getItem("jwtToken")}` }
        });
        if (!res.ok) return;
        const user = await res.json();
        fullName.value = user.fullName || "";
        phone.value = user.phone || "";
        email.value = user.email || "";
        idCardNumber.value = user.idCardNumber || "";
        gender.value = user.gender || "";
        dob.value = user.dateOfBirth || "";
        avatarPreview.src = user.avatarUrl || "/alotra-website/images/avatar-default.jpg";
    }

    // ========= CẬP NHẬT PROFILE =========
    btnSaveProfile.addEventListener("click", async () => {
        const file = avatarInput.files[0];
        const data = {
            fullName: fullName.value,
            phone: phone.value,
            gender: gender.value,
            dateOfBirth: dob.value,
            idCardNumber: idCardNumber.value
        };

        const formData = new FormData();
        formData.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
        if (file) formData.append("file", file);

        const res = await fetch("/alotra-website/api/profile", {
            method: "PUT",
            headers: { "Authorization": `Bearer ${localStorage.getItem("jwtToken")}` },
            body: formData
        });

		if (res.ok) {
		               alert("✅ Cập nhật thông tin cá nhân thành công!");
		               await loadProfile();
		               if (window.loadNotifications) await window.loadNotifications(); // 🔔 Cập nhật chuông
		           } else {
		               alert("❌ Cập nhật thất bại!");
		           }
    });

    // ========= HIỂN THỊ HÌNH ĐẠI DIỆN =========
    avatarInput.addEventListener("change", e => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = ev => avatarPreview.src = ev.target.result;
            reader.readAsDataURL(file);
        }
    });

	// ========= XỬ LÝ ĐĂNG KÝ CHI NHÁNH =========
	registerType.addEventListener('change', () => {
	    if (registerType.value === 'JOIN') {
	        branchJoinGroup.classList.remove('d-none');
	        branchCreateGroup.classList.add('d-none');
	        loadBranches();
	    } else {
	        branchJoinGroup.classList.add('d-none');
	        branchCreateGroup.classList.remove('d-none');
	    }
	});

	btnSubmitBranch.addEventListener('click', async () => {
	    const payload = { type: registerType.value };

	    if (payload.type === 'JOIN') {
	        if (!branchSelect.value) {
	            alert('⚠️ Vui lòng chọn chi nhánh muốn tham gia.');
	            return;
	        }
	        payload.branchId = branchSelect.value;
	    } else {
	        const name = document.getElementById('branchName').value.trim();
	        const phone = document.getElementById('branchPhone').value.trim();
	        const address = document.getElementById('address').value.trim();

	        if (!name || !phone || !address) {
	            alert('⚠️ Vui lòng nhập đầy đủ thông tin chi nhánh.');
	            return;
	        }

	        payload.name = name;
	        payload.phone = phone;
	        payload.address = address;
	    }

	    try {
	        const res = await apiFetch(`/api/register/branch`, {
	            method: 'POST',
	            headers: { 'Content-Type': 'application/json' },
	            body: JSON.stringify(payload)
	        });

	        if (res.ok) {
	            alert('✅ Gửi yêu cầu thành công!');
	            loadHistory();
	        } else {
	            const text = await res.text();
	            let message = text;
	            try {
	                const json = JSON.parse(text);
	                message = json.message || json.error || 'Có lỗi xảy ra.';
	            } catch (_) {}
	            alert(`❌ ${message}`);
	        }
	    } catch (err) {
	        alert('❌ Không thể kết nối tới máy chủ.');
	        console.error(err);
	    }
	});

    // ========= TẢI DANH SÁCH CHI NHÁNH =========
    async function loadBranches() {
        const res = await apiFetch(`/api/register/list-branches`);
        if (!res.ok) return;
        const branches = await res.json();
        branchSelect.innerHTML = branches.map(b =>
            `<option value="${b.id}">${b.name} - ${b.address}</option>`
        ).join('');
    }

    // ========= SỬA YÊU CẦU =========
    historyList.addEventListener('click', async (e) => {
        if (e.target.classList.contains('btn-edit-request')) {
            const id = e.target.dataset.id;
            const name = e.target.dataset.name;
            const phone = e.target.dataset.phone;
            const address = e.target.dataset.address;
            editRequestId.value = id;
            editBranchName.value = name;
            editBranchPhone.value = phone;
            editBranchAddress.value = address;
            editModal.show();
        }

        if (e.target.classList.contains('btn-delete-request')) {
            const id = e.target.dataset.id;
            if (confirm('⚠️ Bạn có chắc muốn xóa yêu cầu này?')) {
                const res = await apiFetch(`/api/register/branch/${id}`, { method: 'DELETE' });
                if (res.ok) {
                    alert('🗑️ Xóa yêu cầu thành công!');
                    loadHistory();
                } else {
                    alert('❌ Xóa thất bại.');
                }
            }
        }
    });

	btnSaveEditBranch.addEventListener('click', async () => {
	    const id = editRequestId.value;
	    const payload = {
	        type: "CREATE", // 👈 Thêm type để không bị lỗi validation
	        name: editBranchName.value.trim(),
	        phone: editBranchPhone.value.trim(),
	        address: editBranchAddress.value.trim()
	    };

	    if (!payload.name || !payload.phone || !payload.address) {
	        alert('⚠️ Vui lòng nhập đầy đủ thông tin.');
	        return;
	    }

	    const res = await apiFetch(`/api/register/branch/${id}`, {
	        method: 'PUT',
	        headers: { 'Content-Type': 'application/json' },
	        body: JSON.stringify(payload)
	    });

	    if (res.ok) {
	        alert('✅ Cập nhật yêu cầu thành công!');
	        editModal.hide();
	        loadHistory();
	    } else {
	        alert('❌ Cập nhật thất bại.');
	    }
	});

    // ========= LỊCH SỬ YÊU CẦU =========
    async function loadHistory() {
        const res = await apiFetch(`/api/register/branch/my-requests`);
        if (!res.ok) {
            historyList.innerHTML = `<p class="text-center text-danger">Không thể tải lịch sử</p>`;
            return;
        }
        const data = await res.json();
        if (data.length === 0) {
            historyList.innerHTML = `<p class="text-center text-muted">Chưa có yêu cầu nào</p>`;
            return;
        }

		historyList.innerHTML = data.map(r => `
		    <div class="border-bottom py-2 d-flex justify-content-between align-items-start">
		        <div>
		            <div><b>Hình thức:</b> ${r.type === 'CREATE' ? 'Tạo mới' : 'Tham gia'}</div>
		            <div><b>Tên chi nhánh:</b> ${r.branchName || '(Chưa có)'}</div>
		            <div><b>Địa chỉ:</b> ${r.address || '(Chưa có)'}</div>
		            <div><b>SĐT:</b> ${r.phone || '(Chưa có)'}</div>
		            <div><b>Trạng thái:</b>
		                <span class="badge ${r.status === 'PENDING' ? 'bg-warning' : (r.status === 'APPROVED' ? 'bg-success' : 'bg-danger')}">
		                    ${r.status}
		                </span>
		            </div>
		            <div><b>Ngày yêu cầu:</b> ${new Date(r.createdAt).toLocaleString('vi-VN')}</div>
		            ${r.note ? `<div><b>Ghi chú:</b> ${r.note}</div>` : ''}
		        </div>
		        ${(r.status === 'PENDING' || r.status === 'REJECTED') && r.type === 'CREATE' ? `
		        <div class="d-flex flex-column gap-2 ms-2">
		            <button class="btn btn-sm btn-outline-primary btn-edit-request"
		                    data-id="${r.id}"
		                    data-name="${r.branchName}"
		                    data-phone="${r.phone}"
		                    data-address="${r.address}">
		                ✏️
		            </button>
		            <button class="btn btn-sm btn-outline-danger btn-delete-request" data-id="${r.id}">🗑️</button>
		        </div>`
		        : (r.status === 'PENDING' || r.status === 'REJECTED') && r.type === 'JOIN' ? `
		        <div class="d-flex flex-column gap-2 ms-2">
		            <button class="btn btn-sm btn-outline-danger btn-delete-request" data-id="${r.id}">🗑️</button>
		        </div>` : ''}
		    </div>
		`).join('');


    }

    // ========= KHỞI TẠO =========
    await loadProfile();
    await loadHistory();
});
