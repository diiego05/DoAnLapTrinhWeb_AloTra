"use strict";

document.addEventListener("DOMContentLoaded", async function() {
    if (!window.location.pathname.includes("/profile")) return;

    console.log("📄 Trang hồ sơ người dùng khởi chạy...");

    const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
    if (!token) {
        alert("Vui lòng đăng nhập để xem hồ sơ cá nhân.");
        window.location.href = "/alotra-website/login";
        return;
    }

    const avatarPreview = document.getElementById("avatarPreview");
    const avatarInput = document.getElementById("avatarInput");
    const addressModal = new bootstrap.Modal(document.getElementById("addressModal"));
    const addressBody = document.getElementById("addressTableBody");

    // === XEM TRƯỚC ẢNH ===
    avatarInput?.addEventListener("change", e => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = ev => avatarPreview.src = ev.target.result;
            reader.readAsDataURL(file);
        }
    });

    // === HÀM LOAD PROFILE ===
    async function loadProfile() {
        try {
            const res = await fetch("/alotra-website/api/profile", {
                headers: { "Authorization": `Bearer ${token}` }
            });
            if (!res.ok) throw new Error("Lỗi tải hồ sơ");
            const user = await res.json();

            document.getElementById("name").value = user.fullName || "";
            document.getElementById("phone").value = user.phone || "";
            document.getElementById("gender").value = user.gender || "";
            if (user.dateOfBirth) document.getElementById("dob").value = user.dateOfBirth;
            document.getElementById("idCardNumber").value = user.idCardNumber || "";
            avatarPreview.src = user.avatarUrl || "/alotra-website/images/avatardefault.jpg";
        } catch (err) {
            console.error("❌ Lỗi khi tải hồ sơ:", err);
        }
    }
    await loadProfile();

    // === CẬP NHẬT HỒ SƠ + ẢNH ===
    document.getElementById("btnSaveProfile").addEventListener("click", async () => {
        try {
            const file = avatarInput.files[0];
            const data = {
                fullName: document.getElementById("name").value,
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
                alert("✅ Cập nhật thông tin cá nhân thành công!");
                await loadProfile();
                if (window.loadNotifications) await window.loadNotifications(); // 🔔 Cập nhật chuông
            } else {
                alert("❌ Cập nhật thất bại!");
            }
        } catch (err) {
            console.error("❌ Lỗi khi cập nhật:", err);
        }
    });

    // === LOAD DANH SÁCH ĐỊA CHỈ ===
    async function loadAddresses() {
        try {
            const res = await fetch("/alotra-website/api/profile/addresses", {
                headers: { "Authorization": `Bearer ${token}` }
            });
            if (!res.ok) throw new Error("Lỗi tải địa chỉ");
            const list = await res.json();

            if (!list || list.length === 0) {
                addressBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Chưa có địa chỉ</td></tr>';
                return;
            }

            addressBody.innerHTML = list.map(a => `
                <tr class="${a.isDefault ? 'table-success' : ''}">
                    <td>${a.recipient}</td>
                    <td>${a.phone}</td>
                    <td>${a.line1}, ${a.ward || ''}, ${a.district || ''}, ${a.city || ''}</td>
                    <td class="text-center">${a.isDefault ? '<i class="fas fa-star text-warning"></i>' : ''}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-primary me-1" onclick="editAddress(${a.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-outline-danger me-1" onclick="deleteAddress(${a.id})"><i class="fas fa-trash"></i></button>
                        ${!a.isDefault ? `<button class="btn btn-sm btn-outline-success" onclick="setDefault(${a.id})"><i class="fas fa-check"></i></button>` : ''}
                    </td>
                </tr>
            `).join("");
        } catch (err) {
            console.error("❌ Lỗi khi tải danh sách địa chỉ:", err);
        }
    }
    await loadAddresses();

    // === THÊM / SỬA ĐỊA CHỈ ===
    document.getElementById("btnShowAddModal").addEventListener("click", () => {
        document.getElementById("addressId").value = "";
        document.querySelectorAll("#recip, #addrPhone, #line1, #ward, #district, #city").forEach(i => i.value = "");
        document.getElementById("isDefault").checked = false;
        addressModal.show();
    });

    document.getElementById("btnSaveAddress").addEventListener("click", async () => {
        const address = {
            id: document.getElementById("addressId").value || null,
            recipient: document.getElementById("recip").value,
            phone: document.getElementById("addrPhone").value,
            line1: document.getElementById("line1").value,
            ward: document.getElementById("ward").value,
            district: document.getElementById("district").value,
            city: document.getElementById("city").value,
            isDefault: document.getElementById("isDefault").checked
        };

        await fetch("/alotra-website/api/profile/addresses", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(address)
        });

        addressModal.hide();
        await loadAddresses();
        if (window.loadNotifications) await window.loadNotifications(); // 🔔 Cập nhật chuông
    });

    // === XÓA / MẶC ĐỊNH / SỬA ===
    window.deleteAddress = async function(id) {
        if (!confirm("Bạn có chắc muốn xóa địa chỉ này không?")) return;
        await fetch(`/alotra-website/api/profile/addresses/${id}`, {
            method: "DELETE", headers: { "Authorization": `Bearer ${token}` }
        });
        await loadAddresses();
        if (window.loadNotifications) await window.loadNotifications();
    };

    window.setDefault = async function(id) {
        await fetch(`/alotra-website/api/profile/addresses/${id}/default`, {
            method: "PUT", headers: { "Authorization": `Bearer ${token}` }
        });
        await loadAddresses();
        if (window.loadNotifications) await window.loadNotifications();
    };

    window.editAddress = async function(id) {
        const res = await fetch("/alotra-website/api/profile/addresses", {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const list = await res.json();
        const addr = list.find(a => a.id === id);
        if (!addr) return;

        document.getElementById("addressId").value = addr.id;
        document.getElementById("recip").value = addr.recipient;
        document.getElementById("addrPhone").value = addr.phone;
        document.getElementById("line1").value = addr.line1;
        document.getElementById("ward").value = addr.ward;
        document.getElementById("district").value = addr.district;
        document.getElementById("city").value = addr.city;
        document.getElementById("isDefault").checked = addr.isDefault;
        addressModal.show();
    };
});
