"use strict";

const contextPath = "/alotra-website";
const fmt = v => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);

// 📌 Biến toàn cục
let selectedAddressId = null;
let selectedBranchId = null;
let selectedCarrierId = null;
let couponCode = null;
let subtotal = 0;
let discount = 0;
let shippingFee = 0;
let cartItems = [];

// 🗺️ Toạ độ cho modal thêm địa chỉ (checkout)
let newAddressLat = null;
let newAddressLng = null;

// ========================= 📥 INIT =========================
document.addEventListener("DOMContentLoaded", async () => {
    await loadCheckoutItems();
    await loadAddresses();
    await loadBranches();
    await loadCarriers();

    document.getElementById("apply-coupon-btn").onclick = applyCoupon;
    document.getElementById("btn-confirm-order").onclick = confirmOrder;
    document.getElementById("branch-select").onchange = handleBranchChange;
    document.getElementById("carrier-select").onchange = handleCarrierChange;
    document.getElementById("btn-add-address").onclick = showAddAddressModal;
    document.getElementById("btn-save-address").onclick = saveNewAddress;
    
    // 🗺️ Load Google Maps using centralized loader
    window.googleMapsLoader.load();
});

// ========================= ⏳ LOADING OVERLAY =========================
function showLoading() {
    let overlay = document.getElementById("loading-overlay");
    if (!overlay) {
        overlay = document.createElement("div");
        overlay.id = "loading-overlay";
        overlay.innerHTML = `
            <div class="loading-spinner">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        const style = document.createElement("style");
        style.innerHTML = `
            #loading-overlay {
                position: fixed;
                top: 0; left: 0; right: 0; bottom: 0;
                background: rgba(255, 255, 255, 0.6);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 2000;
            }
        `;
        document.head.appendChild(style);
    }
    overlay.style.display = "flex";
}

function hideLoading() {
    const overlay = document.getElementById("loading-overlay");
    if (overlay) overlay.style.display = "none";
}

// ========================= 🧭 API HELPER =========================
async function api(url, method = 'GET', data) {
    const opt = { method, headers: { 'Content-Type': 'application/json' } };
    if (data) opt.body = JSON.stringify(data);
    const res = await fetch(contextPath + url, opt);
    if (!res.ok) throw new Error(`❌ Lỗi API: ${url}`);
    return res.json();
}

// ========================= 🛒 LOAD GIỎ HÀNG =========================
async function loadCheckoutItems() {
    const ids = JSON.parse(localStorage.getItem("checkoutItems") || "[]");
    if (ids.length === 0) {
        document.getElementById("checkout-item-list").innerHTML =
            `<div class="alert alert-warning">Không có sản phẩm nào để thanh toán</div>`;
        return;
    }

    cartItems = await api("/api/orders/cart-items/by-ids", "POST", ids);
    subtotal = cartItems.reduce((sum, it) => sum + ((it.unitPrice + it.toppingTotalEach) * it.quantity), 0);
    renderCheckoutItems();
    updateSummary();
}

function renderCheckoutItems() {
    const list = document.getElementById("checkout-item-list");
    list.innerHTML = cartItems.map(it => {
        const toppingHtml = it.toppings?.length
            ? `<div class="small text-muted">Topping: ${it.toppings.map(t => `${fmt(t.price)}`).join(", ")}</div>`
            : "";
        const noteHtml = it.note ? `<div class="small text-info">Ghi chú: ${it.note}</div>` : "";
        return `
            <div class="d-flex justify-content-between mb-2 border-bottom pb-2">
                <div>
                    <strong>${it.productName}</strong>
                    <div class="small text-muted">${it.sizeName ?? ""}</div>
                    ${toppingHtml}
                    ${noteHtml}
                </div>
                <div class="text-end">
                    ${fmt(it.unitPrice + it.toppingTotalEach)} x ${it.quantity}
                </div>
            </div>
        `;
    }).join("");
}

// ========================= 💰 CẬP NHẬT TỔNG =========================
function updateSummary() {
    document.getElementById("subtotal").innerText = fmt(subtotal);
    document.getElementById("discount").innerText = fmt(discount);
    document.getElementById("ship-fee-summary").innerText = fmt(shippingFee);
    document.getElementById("grand-total").innerText = fmt(subtotal - discount + shippingFee);
}

// ========================= 🏠 ĐỊA CHỈ =========================
async function loadAddresses() {
    const list = await api("/api/addresses");
    const container = document.getElementById("address-list");
    container.innerHTML = "";

    list.forEach(addr => {
        const div = document.createElement("div");
        div.className = "form-check";
        div.innerHTML = `
            <input class="form-check-input" type="radio" name="address" value="${addr.id}" ${addr.isDefault ? "checked" : ""}>
            <label class="form-check-label">
                <strong>${addr.recipient}</strong> - ${addr.phone}<br>
                ${addr.line1}, ${addr.ward || ''}, ${addr.city || ''}
                ${addr.isDefault ? '<span class="badge bg-success ms-2">Mặc định</span>' : ''}
            </label>
        `;
        container.appendChild(div);
    });

    document.querySelectorAll("input[name='address']").forEach(r => {
        r.onchange = e => { selectedAddressId = parseInt(e.target.value); suggestNearestBranch(); };
        if (r.checked) selectedAddressId = parseInt(r.value);
    });

    // Gợi ý chi nhánh gần nhất khi đã có địa chỉ mặc định
    if (selectedAddressId) suggestNearestBranch();
}

function showAddAddressModal() {
    const modalEl = document.getElementById("addAddressModal");
    const modal = new bootstrap.Modal(modalEl);

    // Reset fields and coords on open
    ["new-recipient","new-phone","new-line1","new-ward","new-city"].forEach(id=>{
        const el = document.getElementById(id);
        if (el) el.value = "";
    });
    newAddressLat = null;
    newAddressLng = null;

    modal.show();

    // 🗺️ Initialize autocomplete when modal is shown (align with profile)
    modalEl.addEventListener('shown.bs.modal', async () => {
        const input = document.getElementById('new-line1');
        if (!input) return;
        
        const autocomplete = await window.googleMapsLoader.createAutocomplete(input, { types: ['geocode'] });
        if (!autocomplete) return;
        
        // ✅ Google Places Autocomplete — use centralized parser like profile
        if (autocomplete.addListener) {
            autocomplete.addListener('place_changed', () => {
                const place = autocomplete.getPlace();
                if (!place || !place.address_components) return;

                // Clear current fields before fill
                const wardEl = document.getElementById('new-ward');
                const cityEl = document.getElementById('new-city');
                if (wardEl) wardEl.value = '';
                if (cityEl) cityEl.value = '';

                const parsed = window.googleMapsLoader.parseVietnameseAddress(place.address_components);

                // ✅ Only street to line1; keep ward/city identical to profile
                document.getElementById('new-line1').value = (parsed.street || '').trim();
                document.getElementById('new-ward').value = (parsed.ward || '').trim();
                document.getElementById('new-city').value = (parsed.city || '').trim();

                // ✅ Save coordinates if available
                if (place.geometry && place.geometry.location) {
                    try {
                        newAddressLat = place.geometry.location.lat();
                        newAddressLng = place.geometry.location.lng();
                    } catch (_) { /* ignore */ }
                }
                console.log('✅ Checkout parsed address:', parsed, newAddressLat, newAddressLng);
            });
        } else if (autocomplete.nominatim) {
            // ✅ Nominatim autocomplete – mirror profile.js logic
            input.addEventListener('nominatim-select', (e) => {
                const detail = e.detail;
                console.log('📍 Nominatim address selected (checkout):', detail.address);

                // Parse địa chỉ Nominatim theo format Việt Nam
                const parts = detail.address.split(',').map(p => p.trim());

                // ✅ Lọc bỏ postal code và 'Việt Nam'/'Vietnam'
                const filtered = parts.filter(part => {
                    if (/^\d{5,6}$/.test(part)) return false;
                    const lower = part.toLowerCase();
                    if (lower === 'việt nam' || lower === 'vietnam') return false;
                    return true;
                });

                // ✅ Tìm index của phường/xã/thị trấn
                const wardIndex = filtered.findIndex(p =>
                    p.includes('Phường') ||
                    p.includes('Xã') ||
                    p.includes('Thị trấn')
                );

                if (wardIndex > 0) {
                    const line1Parts = filtered.slice(0, wardIndex);
                    document.getElementById('new-line1').value = line1Parts.join(', ');
                    document.getElementById('new-ward').value = filtered[wardIndex] || '';
                    document.getElementById('new-city').value = filtered[filtered.length - 1] || '';
                } else if (wardIndex === 0) {
                    document.getElementById('new-line1').value = '';
                    document.getElementById('new-ward').value = filtered[0] || '';
                    document.getElementById('new-city').value = filtered[1] || '';
                } else {
                    document.getElementById('new-line1').value = filtered[0] || '';
                    document.getElementById('new-ward').value = '';
                    document.getElementById('new-city').value = filtered[1] || '';
                }

                // ✅ Lưu toạ độ nếu có
                if (detail && (detail.lat || detail.lon || detail.lng)) {
                    newAddressLat = Number(detail.lat ?? detail.latitude ?? null);
                    newAddressLng = Number(detail.lon ?? detail.lng ?? detail.longitude ?? null);
                }
            });
        }
    }, { once: true });
}

async function saveNewAddress() {
    const body = {
        recipient: document.getElementById("new-recipient").value.trim(),
        phone: document.getElementById("new-phone").value.trim(),
        line1: document.getElementById("new-line1").value.trim(),
        ward: document.getElementById("new-ward").value.trim(),
        city: document.getElementById("new-city").value.trim(),
        isDefault: document.getElementById("new-default").checked,
        // ✅ gửi toạ độ nếu có
        latitude: newAddressLat,
        longitude: newAddressLng
    };

    if (!body.recipient || !body.phone || !body.line1) {
        alert("⚠️ Vui lòng nhập đủ thông tin bắt buộc");
        return;
    }

    try {
        await api("/api/addresses", "POST", body);
        bootstrap.Modal.getInstance(document.getElementById("addAddressModal")).hide();
        await loadAddresses();
    } catch (e) {
        console.error(e);
        alert("❌ Không thể thêm địa chỉ. Vui lòng thử lại.");
    }
}

// ========================= 🏪 CHI NHÁNH =========================
async function loadBranches() {
    const select = document.getElementById("branch-select");
    select.innerHTML = `<option value="">-- Chọn chi nhánh --</option>`;
    try {
        const branches = await api("/api/public/branches/active");
        branches.forEach(b => {
            const opt = document.createElement("option");
            opt.value = b.id;
            opt.textContent = b.name;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error("❌ Không thể tải danh sách chi nhánh:", e);
        select.innerHTML = `<option value="">(Không thể tải chi nhánh)</option>`;
    }
}

async function suggestNearestBranch() {
    try {
        if (!selectedAddressId) return;
        const res = await fetch(`${contextPath}/api/public/branches/nearest?addressId=${selectedAddressId}`);
        if (!res.ok) return;
        const branch = await res.json();
        if (branch && branch.id) {
            const select = document.getElementById('branch-select');
            select.value = String(branch.id);
            // Trigger change side-effects
            select.dispatchEvent(new Event('change'));
        }
    } catch (e) { /* ignore */ }
}

async function handleBranchChange(e) {
    selectedBranchId = parseInt(e.target.value) || null;
    const branchWarning = document.getElementById("branch-warning");

    if (selectedBranchId) {
        const unavailable = await api(`/api/public/branches/${selectedBranchId}/check-availability`, "POST",
            cartItems.map(it => it.cartItemId)
        );
        if (unavailable.length > 0) {
            branchWarning.style.display = "block";
            branchWarning.textContent = `⚠️ Có ${unavailable.length} sản phẩm không khả dụng tại chi nhánh này.`;
        } else branchWarning.style.display = "none";
    } else branchWarning.style.display = "none";
}

// ========================= 🚚 VẬN CHUYỂN =========================
async function loadCarriers() {
    const carriers = await api("/api/public/shipping-carriers");
    const select = document.getElementById("carrier-select");
    select.innerHTML = `<option value="">-- Chọn đơn vị vận chuyển --</option>`;
    carriers.forEach(c => {
        const opt = document.createElement("option");
        opt.value = c.id;
        opt.textContent = c.name;
        select.appendChild(opt);
    });
}

async function handleCarrierChange(e) {
    selectedCarrierId = parseInt(e.target.value) || null;
    shippingFee = 0;
    if (selectedCarrierId) {
        const carrier = await api(`/api/public/shipping-carriers/${selectedCarrierId}/fee`);
        shippingFee = carrier.discountedFee;
    }
    document.getElementById("shipping-fee").innerText = fmt(shippingFee);
    document.getElementById("ship-fee-summary").innerText = fmt(shippingFee);
    updateSummary();
}

// ========================= 🎟️ COUPON =========================
async function applyCoupon() {
    const code = document.getElementById("coupon-code").value.trim();
    if (!code) return;

    const productIds = cartItems.map(it => it.productId);
    try {
        const res = await api(`/api/public/coupons/validate/${code}?orderTotal=${subtotal}`, "POST", productIds);
        discount = res;
        couponCode = code;
        document.getElementById("coupon-msg").innerText = `✅ Áp dụng mã thành công - Giảm ${fmt(discount)}`;
    } catch (e) {
        console.error(e);
        document.getElementById("coupon-msg").innerText = `❌ Mã không hợp lệ hoặc không áp dụng cho sản phẩm`;
        discount = 0;
        couponCode = null;
    }
    updateSummary();
}

// ========================= 🧾 XÁC NHẬN ĐẶT HÀNG =========================
async function confirmOrder() {
    const btn = document.getElementById("btn-confirm-order");
    btn.disabled = true;
    showLoading();

    try {
        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;

		if (!selectedAddressId && paymentMethod !== "PICKUP") {
		    showAlert("⚠️ Vui lòng chọn địa chỉ giao hàng");
		    return;
		}
		if (!selectedBranchId) {
		    showAlert("⚠️ Vui lòng chọn chi nhánh");
		    return;
		}


        const unavailable = await api(`/api/public/branches/${selectedBranchId}/check-availability`, "POST",
            cartItems.map(it => it.cartItemId)
        );
        if (unavailable.length > 0) {
            alert(`⚠️ Có ${unavailable.length} sản phẩm không khả dụng tại chi nhánh này.`);
            return;
        }

        const body = {
            cartItemIds: cartItems.map(it => it.cartItemId),
            branchId: selectedBranchId,
            shippingCarrierId: selectedCarrierId,
            couponCode: couponCode,
            paymentMethod: paymentMethod,
            addressId: selectedAddressId
        };

        const res = await api("/api/orders", "POST", body);

        if (paymentMethod === "BANK") {
            const paymentRes = await fetch(`${contextPath}/api/payment/vnpay/create?orderId=${res.orderId}`, {
                method: "POST"
            });
            if (!paymentRes.ok) throw new Error("Không thể tạo link thanh toán VNPay");
            const paymentUrl = await paymentRes.text();
            localStorage.removeItem("checkoutItems");
            window.location.href = paymentUrl;
        } else {
            showSuccessModal(res.code);
        }
    } catch (e) {
        console.error(e);
        alert("❌ Không thể đặt hàng. Vui lòng thử lại.");
    } finally {
        hideLoading();
        btn.disabled = false;
    }
}

// ========================= ✅ MODAL THÀNH CÔNG =========================
function showSuccessModal(orderCode) {
    const modalHTML = `
        <div class="success-modal-overlay" id="successModalOverlay">
            <div class="success-modal">
                <div class="success-modal-header">
                    <div class="success-modal-icon">
                        <i class="fas fa-check"></i>
                    </div>
                    <h2 class="success-modal-title">Đặt hàng thành công!</h2>
                    <p class="success-modal-subtitle">Cảm ơn bạn đã tin tưởng AloTra</p>
                </div>
                <div class="success-modal-body">
                    <div class="order-code-box">
                        <div class="order-code-label">Mã đơn hàng của bạn</div>
                        <div class="order-code-value">${orderCode}</div>
                    </div>
                    <p class="success-modal-message">
                        <i class="fas fa-info-circle"></i>
                        Đơn hàng của bạn đang được xử lý. Chúng tôi sẽ gửi thông báo khi đơn hàng được xác nhận.
                    </p>
                </div>
                <div class="success-modal-footer">
                    <button class="success-modal-btn success-modal-btn-secondary" id="btnGoHome">
                        <i class="fas fa-home"></i> Về trang chủ
                    </button>
                    <button class="success-modal-btn success-modal-btn-primary" id="btnGoOrders">
                        <i class="fas fa-receipt"></i> Xem đơn hàng
                    </button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);

    document.getElementById('btnGoHome').onclick = () => {
        localStorage.removeItem("checkoutItems");
        window.location.href = contextPath + "/";
    };
    document.getElementById('btnGoOrders').onclick = () => {
        localStorage.removeItem("checkoutItems");
        window.location.href = contextPath + "/orders";
    };
    document.getElementById('successModalOverlay').onclick = (e) => {
        if (e.target === e.currentTarget) {
            localStorage.removeItem("checkoutItems");
            window.location.href = contextPath + "/orders";
        }
    };
}