"use strict";

// === HÀM HIỂN THỊ CONFIRM ĐĂNG XUẤT ===
function showConfirm(message, submessage = '') {
    return new Promise((resolve) => {
        // Tạo overlay
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            z-index: 99998;
            opacity: 0;
            transition: opacity 0.2s ease-out;
        `;

        // Tạo dialog
        const dialog = document.createElement('div');
        dialog.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) scale(0.9);
            z-index: 99999;
            background: white;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
            min-width: 420px;
            max-width: 500px;
            opacity: 0;
            transition: all 0.3s ease-out;
        `;

        dialog.innerHTML = `
            <div style="background: linear-gradient(135deg, #dc3545 0%, #e83e8c 100%);
                        color: white;
                        padding: 20px 24px;
                        border-radius: 16px 16px 0 0;
                        display: flex;
                        align-items: center;
                        gap: 12px;">
                <div style="font-size: 32px; animation: pulse 1s infinite;">⚠️</div>
                <h5 style="margin: 0; font-weight: 600;">Xác nhận</h5>
            </div>
            <div style="padding: 24px; text-align: center;">
                <p style="font-size: 16px; color: #333; margin-bottom: 8px; font-weight: 500;">${message}</p>
                ${submessage ? `<p style="font-size: 14px; color: #6c757d; margin: 0;">${submessage}</p>` : ''}
            </div>
            <div style="padding: 16px 24px 24px; display: flex; gap: 12px; justify-content: center;">
                <button class="btn btn-secondary" id="confirmCancel" style="min-width: 120px; font-weight: 600; padding: 10px 20px; border-radius: 8px;">
                    <i class="fas fa-times me-1"></i>Hủy
                </button>
                <button class="btn btn-danger" id="confirmOk" style="min-width: 120px; font-weight: 600; padding: 10px 20px; border-radius: 8px;">
                    <i class="fas fa-check me-1"></i>Xác nhận
                </button>
            </div>
        `;

        // Thêm keyframe animation cho icon
        if (!document.getElementById('confirm-pulse-animation')) {
            const style = document.createElement('style');
            style.id = 'confirm-pulse-animation';
            style.textContent = `
                @keyframes pulse {
                    0%, 100% { transform: scale(1); }
                    50% { transform: scale(1.1); }
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(overlay);
        document.body.appendChild(dialog);

        // Trigger animation
        setTimeout(() => {
            overlay.style.opacity = '1';
            dialog.style.opacity = '1';
            dialog.style.transform = 'translate(-50%, -50%) scale(1)';
        }, 10);

        const handleClose = (result) => {
            overlay.style.opacity = '0';
            dialog.style.opacity = '0';
            dialog.style.transform = 'translate(-50%, -50%) scale(0.9)';

            setTimeout(() => {
                overlay.remove();
                dialog.remove();
            }, 200);

            resolve(result);
        };

        dialog.querySelector('#confirmOk').onclick = () => handleClose(true);
        dialog.querySelector('#confirmCancel').onclick = () => handleClose(false);
        overlay.onclick = () => handleClose(false);

        // Thêm ESC để đóng
        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                handleClose(false);
                document.removeEventListener('keydown', handleEsc);
            }
        };
        document.addEventListener('keydown', handleEsc);
    });
}

// === BIẾN TOKEN GLOBAL ===
let globalToken = null;

// 🗺️ Lưu toạ độ tạm thời khi user chọn từ autocomplete (profile address modal)
let modalAddressLat = null;
let modalAddressLng = null;

// 🗺️ Sử dụng Google Maps Loader
async function initAddressAutocomplete() {
    const line1Input = document.getElementById("line1");
    if (!line1Input) return;

    // Reset toạ độ tạm khi mở modal hoặc init
    modalAddressLat = null;
    modalAddressLng = null;

    // Sử dụng Google Maps Loader
    const autocomplete = await window.googleMapsLoader.createAutocomplete(line1Input, {
        types: ["geocode"]
    });

    if (!autocomplete) {
        console.warn('⚠️ Autocomplete initialization failed');
        return;
    }

    // ✅ XỬ LÝ GOOGLE PLACES AUTOCOMPLETE
    if (autocomplete.addListener) {
        autocomplete.addListener("place_changed", function () {
            const place = autocomplete.getPlace();
            if (!place || !place.address_components) return;

            // Sử dụng parser từ Google Maps Loader
            const parsed = window.googleMapsLoader.parseVietnameseAddress(place.address_components);

            // ✅ CHỈ lấy street cho Line1
            document.getElementById("line1").value = parsed.street || '';
            document.getElementById("ward").value = parsed.ward;
            document.getElementById("city").value = parsed.city;

            // ✅ Lưu toạ độ nếu Google trả về geometry
            if (place.geometry && place.geometry.location) {
                try {
                    modalAddressLat = place.geometry.location.lat();
                    modalAddressLng = place.geometry.location.lng();
                } catch (_) {}
            }
            console.log('✅ Address parsed and filled:', parsed, modalAddressLat, modalAddressLng);
        });
        console.log('✅ Google Places autocomplete initialized');
    } 
    // ✅ XỬ LÝ NOMINATIM AUTOCOMPLETE (fallback)
    else if (autocomplete.nominatim) {
        line1Input.addEventListener('nominatim-select', (e) => {
            const detail = e.detail;
            console.log('📍 Nominatim address selected:', detail.address);

            // Parse địa chỉ Nominatim theo format Việt Nam
            const parts = detail.address.split(',').map(p => p.trim());

            // ✅ Lọc bỏ postal code và VN
            const filtered = parts.filter(part => {
                if (/^\d{5,6}$/.test(part)) return false;
                if (part.toLowerCase() === 'việt nam' || part.toLowerCase() === 'vietnam') return false;
                return true;
            });

            const wardIndex = filtered.findIndex(p => 
                p.includes('Phường') || 
                p.includes('Xã') || 
                p.includes('Thị trấn')
            );

            if (wardIndex > 0) {
                const line1Parts = filtered.slice(0, wardIndex);
                document.getElementById("line1").value = line1Parts.join(', ');
                const ward = filtered[wardIndex] || '';
                document.getElementById("ward").value = ward;
                const city = filtered[filtered.length - 1] || '';
                document.getElementById("city").value = city;
            } else if (wardIndex === 0) {
                document.getElementById("line1").value = '';
                document.getElementById("ward").value = filtered[0] || '';
                document.getElementById("city").value = filtered[1] || '';
            } else {
                document.getElementById("line1").value = filtered[0] || '';
                document.getElementById("ward").value = '';
                document.getElementById("city").value = filtered[1] || '';
            }

            // ✅ Lưu toạ độ nếu có từ detail
            if (detail && (detail.lat || detail.lon || detail.lng)) {
                modalAddressLat = Number(detail.lat ?? detail.latitude ?? null);
                modalAddressLng = Number(detail.lon ?? detail.lng ?? detail.longitude ?? null);
            }
        });
        console.log('✅ Nominatim autocomplete initialized');
    }
}

document.addEventListener("DOMContentLoaded", async function() {
    if (!window.location.pathname.includes("/profile")) return;

    console.log("📄 Trang hồ sơ người dùng khởi chạy...");

    const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
    if (!token) {
        showAlert("Vui lòng đăng nhập để xem hồ sơ cá nhân.");
        window.location.href = "/alotra-website/login";
        return;
    }

    // Lưu token vào biến global
    globalToken = token;

    const avatarPreview = document.getElementById("avatarPreview");
    const avatarInput = document.getElementById("avatarInput");
    const addressModal = new bootstrap.Modal(document.getElementById("addressModal"));
    const addressBody = document.getElementById("addressTableBody");

    // 🔌 Load Google Maps khi trang khởi động
    window.googleMapsLoader.load().then(loaded => {
        if (loaded) {
            console.log('✅ Google Maps loaded for profile page');
        }
    });

    // Khi modal địa chỉ mở, khởi tạo autocomplete và reset toạ độ tạm
    const addressModalEl = document.getElementById('addressModal');
    if (addressModalEl) {
        addressModalEl.addEventListener('shown.bs.modal', () => {
            modalAddressLat = null;
            modalAddressLng = null;
            initAddressAutocomplete();
        }, { once: true });
    }

    // === HÀM HIỂN THỊ TOAST THÔNG BÁO ===
    window.showToast = function(message, type = 'success') {
        let toastContainer = document.getElementById('toastContainer');

        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            document.body.appendChild(toastContainer);
        }

        toastContainer.style.cssText = `
            position: fixed !important;
            top: 50% !important;
            left: 50% !important;
            transform: translate(-50%, -50%) !important;
            z-index: 99999 !important;
            display: flex !important;
            flex-direction: column !important;
            gap: 10px !important;
            align-items: center !important;
            pointer-events: none !important;
        `;

        const toastId = 'toast-' + Date.now();
        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : '⚠️';
        const title = type === 'success' ? 'Thành công' : type === 'error' ? 'Lỗi' : 'Cảnh báo';

        let headerBg = '';
        if (type === 'success') {
            headerBg = 'background: linear-gradient(135deg, #28a745 0%, #20c997 100%);';
        } else if (type === 'error') {
            headerBg = 'background: linear-gradient(135deg, #dc3545 0%, #e83e8c 100%);';
        } else {
            headerBg = 'background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);';
        }

        const toastHTML = `
            <div id="${toastId}" class="toast show" role="alert" aria-live="assertive" aria-atomic="true"
                 style="min-width: 400px;
                        border-radius: 12px;
                        box-shadow: 0 8px 24px rgba(0,0,0,0.3);
                        pointer-events: auto;
                        animation: slideIn 0.3s ease-out;">
                <div class="toast-header" style="${headerBg}
                                                  color: white;
                                                  font-weight: 600;
                                                  padding: 14px 18px;
                                                  border: none;
                                                  border-radius: 12px 12px 0 0;">
                    <strong class="me-auto">${icon} ${title}</strong>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"
                            style="filter: brightness(0) invert(1); opacity: 0.8;"></button>
                </div>
                <div class="toast-body" style="padding: 14px 18px;
                                              font-size: 15px;
                                              background-color: white;
                                              border-radius: 0 0 12px 12px;">
                    ${message}
                </div>
            </div>
        `;

        toastContainer.insertAdjacentHTML('beforeend', toastHTML);

        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 3000
        });

        toast.show();

        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    };

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
                showToast("Cập nhật thông tin cá nhân thành công!", "success");
                await loadProfile();
                if (window.loadNotifications) await window.loadNotifications();
            } else {
                showToast("Cập nhật thất bại!", "error");
            }
        } catch (err) {
            console.error("❌ Lỗi khi cập nhật:", err);
            showToast("Có lỗi xảy ra khi cập nhật!", "error");
        }
    });

    // === LOAD DANH SÁCH ĐỊA CHỈ ===
    window.loadAddresses = async function() {
        try {
            const res = await fetch("/alotra-website/api/profile/addresses", {
                headers: { "Authorization": `Bearer ${globalToken}` }
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
                    <td>${a.line1}, ${a.ward || ''}, ${a.city || ''}</td>
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
    };
    await loadAddresses();

    // === THÊM / SỬA ĐỊA CHỈ ===
    document.getElementById("btnShowAddModal").addEventListener("click", () => {
        document.getElementById("addressId").value = "";
        document.querySelectorAll("#recip, #addrPhone, #line1, #ward, #city").forEach(i => i.value = "");
        const defaultCheckbox = document.getElementById("isDefault");
        if (defaultCheckbox) defaultCheckbox.checked = false;
        // reset coords
        modalAddressLat = null;
        modalAddressLng = null;
        addressModal.show();
    });

    // === LƯU ĐỊA CHỈ (THÊM / SỬA) ===
    document.getElementById("btnSaveAddress").addEventListener("click", async () => {
        try {
            const addressId = document.getElementById("addressId").value;
            const address = {
                recipient: document.getElementById("recip").value,
                phone: document.getElementById("addrPhone").value,
                line1: document.getElementById("line1").value,
                ward: document.getElementById("ward").value,
                city: document.getElementById("city").value,
                isDefault: document.getElementById("isDefault")?.checked || false,
                // ✅ gửi toạ độ nếu user đã chọn từ autocomplete
                latitude: modalAddressLat,
                longitude: modalAddressLng
            };

            if (!address.recipient || !address.phone || !address.line1) {
                showToast("Vui lòng nhập đầy đủ thông tin người nhận, số điện thoại và địa chỉ chi tiết!", "warning");
                return;
            }

            let res;
            if (addressId) {
                res = await fetch(`/alotra-website/api/addresses/${addressId}`, {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`
                    },
                    body: JSON.stringify(address)
                });
            } else {
                res = await fetch("/alotra-website/api/addresses", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`
                    },
                    body: JSON.stringify(address)
                });
            }

            if (res.ok) {
                showToast(`${addressId ? 'Cập nhật' : 'Thêm'} địa chỉ thành công!`, "success");
                addressModal.hide();
                await loadAddresses();
                if (window.loadNotifications) await window.loadNotifications();
            } else {
                const error = await res.text();
                showToast(`Lỗi: ${error}`, "error");
            }
        } catch (err) {
            console.error("❌ Lỗi khi lưu địa chỉ:", err);
            showToast("Có lỗi xảy ra khi lưu địa chỉ. Vui lòng thử lại!", "error");
        }
    });

    // === SỬA ĐỊA CHỈ ===
    window.editAddress = async function(id) {
        try {
            const res = await fetch("/alotra-website/api/profile/addresses", {
                headers: { "Authorization": `Bearer ${globalToken}` }
            });
            const list = await res.json();
            const addr = list.find(a => a.id === id);

            if (!addr) {
                showToast("Không tìm thấy địa chỉ!", "error");
                return;
            }

            document.getElementById("addressId").value = addr.id;
            document.getElementById("recip").value = addr.recipient;
            document.getElementById("addrPhone").value = addr.phone;
            document.getElementById("line1").value = addr.line1;
            document.getElementById("ward").value = addr.ward || "";
            document.getElementById("city").value = addr.city || "";

            const defaultCheckbox = document.getElementById("isDefault");
            if (defaultCheckbox) {
                defaultCheckbox.checked = addr.isDefault;
            }

            // ✅ preset coords để nếu user không chọn lại autocomplete thì không bị null
            modalAddressLat = typeof addr.latitude === 'number' ? addr.latitude : null;
            modalAddressLng = typeof addr.longitude === 'number' ? addr.longitude : null;

            addressModal.show();
        } catch (err) {
            console.error("❌ Lỗi khi load địa chỉ:", err);
            showToast("Có lỗi xảy ra khi tải địa chỉ!", "error");
        }
    };
});

// === XÓA ĐỊA CHỈ (GLOBAL FUNCTION) ===
window.deleteAddress = async function(id) {
    console.log("🗑️ Delete address called with ID:", id);

    const confirmed = await showConfirm(
        "Bạn có chắc muốn xóa địa chỉ này không?",
        "Hành động này không thể hoàn tác"
    );

    console.log("✅ User confirmed:", confirmed);

    if (!confirmed) return;

    try {
        const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
        const res = await fetch(`/alotra-website/api/profile/addresses/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (res.ok) {
            window.showToast("Xóa địa chỉ thành công!", "success");
            await window.loadAddresses();
            if (window.loadNotifications) await window.loadNotifications();
        } else {
            window.showToast("Xóa địa chỉ thất bại!", "error");
        }
    } catch (err) {
        console.error("❌ Lỗi khi xóa địa chỉ:", err);
        window.showToast("Có lỗi xảy ra khi xóa địa chỉ!", "error");
    }
};

// === ĐẶT MẶC ĐỊNH (GLOBAL FUNCTION) ===
window.setDefault = async function(id) {
    try {
        const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");
        const res = await fetch(`/alotra-website/api/profile/addresses/${id}/default`, {
            method: "PUT",
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (res.ok) {
            window.showToast("Đặt địa chỉ mặc định thành công!", "success");
            await window.loadAddresses();
            if (window.loadNotifications) await window.loadNotifications();
        } else {
            window.showToast("Đặt địa chỉ mặc định thất bại!", "error");
        }
    } catch (err) {
        console.error("❌ Lỗi khi đặt mặc định:", err);
        window.showToast("Có lỗi xảy ra!", "error");
    }
};

// Call this after DOMContentLoaded (kept for backward-compat)
document.addEventListener("DOMContentLoaded", function() {
    // if Maps loaded early by cache, init immediately
    if (window.google && window.google.maps && window.google.maps.places) {
        initAddressAutocomplete();
    }
});