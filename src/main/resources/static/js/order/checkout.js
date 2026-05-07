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
let discountItems = 0;
let discountShipping = 0;
let shippingFeeBase = 0;

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

function setBranchNotice(msg){
  let el=document.getElementById("branch-notice");
  if(!el){
    const select=document.getElementById("branch-select");
    el=document.createElement("div");
    el.id="branch-notice";
    el.className="alert alert-warning mt-2 py-2";
    el.style.display="none";
    select.parentElement.appendChild(el);
  }
  if(msg){
    el.innerText=msg;
    el.style.display="block";
  }else{
    el.innerText="";
    el.style.display="none";
  }
}

function setConfirmEnabled(enabled){
  const btn=document.getElementById("btn-confirm-order");
  if(btn) btn.disabled=!enabled;
}

function markBranchSelectInvalid(invalid){
  const select=document.getElementById("branch-select");
  if(!select) return;
  if(invalid){
    select.classList.add("is-invalid");
  }else{
    select.classList.remove("is-invalid");
  }
}

function renderCheckoutItems() {
	const list = document.getElementById("checkout-item-list");
	list.innerHTML = cartItems.map(it => {
		const toppingHtml = it.toppings?.length
		? `<div class="small text-muted">
		                   Topping: ${it.toppings.map(t => `${t.name} (${fmt(t.price)})`).join(", ")}
		               </div>`
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
	document.getElementById("ship-fee-summary").innerText = fmt(shippingFeeBase);
	document.getElementById("grand-total").innerText = fmt(subtotal - discount + shippingFeeBase);
}

// ========================= 🏠 ĐỊA CHỈ =========================
async function loadAddresses(){
  const list=await api("/api/addresses");
  const container=document.getElementById("address-list");
  container.innerHTML="";

  list.forEach(addr=>{
    const div=document.createElement("div");
    div.className="form-check";
    div.innerHTML=`
      <input class="form-check-input" type="radio" name="address" value="${addr.id}" ${addr.isDefault?"checked":""}>
      <label class="form-check-label">
        <strong>${addr.recipient}</strong> - ${addr.phone}<br>
        ${addr.line1}, ${addr.ward||""}, ${addr.city||""}
        ${addr.isDefault?'<span class="badge bg-success ms-2">Mặc định</span>':""}
      </label>
    `;
    container.appendChild(div);
  });

  document.querySelectorAll("input[name='address']").forEach(r=>{
    r.onchange = e => {
      selectedAddressId=parseInt(e.target.value);

      // ✅ mở lại dropdown trước, vì địa chỉ cũ có thể đã bị khóa do >20km
      setBranchSelectable(true);

      // (tuỳ chọn) reset UI cũ
      setBranchNotice(null);
      markBranchSelectInvalid(false);

      suggestNearestBranch();
    };

    if(r.checked) selectedAddressId=parseInt(r.value);
  });

  if(selectedAddressId){
    setBranchSelectable(true);
    setBranchNotice(null);
    markBranchSelectInvalid(false);
    suggestNearestBranch();
  }
}

function showAddAddressModal() {
	const modalEl = document.getElementById("addAddressModal");
	const modal = new bootstrap.Modal(modalEl);

	// Reset fields and coords on open
	["new-recipient", "new-phone", "new-line1", "new-ward", "new-city"].forEach(id => {
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
function setBranchSelectable(enabled){
  const select=document.getElementById("branch-select");
  if(select) select.disabled=!enabled;
}

async function suggestNearestBranch(){
  try{
    if(!selectedAddressId) return;

    const res=await fetch(`${contextPath}/api/public/branches/nearest?addressId=${selectedAddressId}`);

    if(!res.ok){
      let msg="Hiện chưa có chi nhánh trong bán kính 20km từ địa chỉ này. Rất tiếc về sự bất tiện này.";
      try{
        const data=await res.json();
        if(data && data.message) msg=data.message;
      }catch(_){}

      const select=document.getElementById("branch-select");
      select.value="";
      select.dispatchEvent(new Event("change"));

      setBranchNotice(msg);
      markBranchSelectInvalid(true);

      setConfirmEnabled(false);
      setBranchSelectable(false); // ✅ KHÓA luôn dropdown, không cho chọn thủ công
      return;
    }

    const branch=await res.json();
    if(branch && branch.id){
      setBranchNotice(null);
      markBranchSelectInvalid(false);

      setBranchSelectable(true);  // ✅ có chi nhánh hợp lệ thì mở lại dropdown

      const select=document.getElementById("branch-select");
      select.value=String(branch.id);
      select.dispatchEvent(new Event("change"));

      setConfirmEnabled(true);
    }
  }catch(e){
    console.error(e);
  }
}



async function handleBranchChange(e){
  selectedBranchId=parseInt(e.target.value)||null;

  setBranchNotice(null);
  markBranchSelectInvalid(false);

  const branchWarning=document.getElementById("branch-warning");

  if(selectedBranchId){
    setConfirmEnabled(true);

    const unavailable=await api(`/api/public/branches/${selectedBranchId}/check-availability`,"POST",
      cartItems.map(it=>it.cartItemId)
    );

    if(unavailable.length>0){
      branchWarning.style.display="block";
      branchWarning.textContent=`⚠️ Có ${unavailable.length} sản phẩm không khả dụng tại chi nhánh này.`;
    }else branchWarning.style.display="none";
  }else{
    branchWarning.style.display="none";
    setConfirmEnabled(false);
  }
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

  shippingFeeBase = 0;
  discountShipping = 0;

  if (selectedCarrierId) {
    const carrier = await api(`/api/public/shipping-carriers/${selectedCarrierId}/fee`);
    shippingFeeBase = Number(carrier.discountedFee ?? carrier.fee ?? 0);
  }

  shippingFee = Math.max(0, shippingFeeBase - (discountShipping || 0));

  // ✅ CẬP NHẬT CẢ 2 VỊ TRÍ HIỂN THỊ PHÍ SHIP
  document.getElementById("shipping-fee").innerText = fmt(shippingFeeBase);
  document.getElementById("ship-fee-summary").innerText = fmt(shippingFeeBase);

  // ✅ Nếu đã có coupon, tính lại (vì trước đó ship base=0 sẽ cho giảm ship=0)
  if (couponCode) {
    await applyCoupon();   // im lặng re-calc dựa trên shippingFeeBase mới
  } else {
    updateSummary();
  }
}

// ========================= 🎟️ COUPON =========================
async function applyCoupon() {
    const code = document.getElementById("coupon-code").value.trim();
    if (!code) return;

    const productIds = cartItems.map(it => it.productId);

    try {
        const res = await api(`/api/public/coupons/validate/${code}?orderTotal=${subtotal}`, "POST", productIds);

        // bảo đảm các biến mới tồn tại (nếu file cũ chưa khai báo)
        if (typeof discountItems === "undefined") window.discountItems = 0;
        if (typeof discountShipping === "undefined") window.discountShipping = 0;
        if (typeof shippingFeeBase === "undefined") window.shippingFeeBase = Number(shippingFee || 0);

        // reset trước khi tính lại
        discountItems = 0;
        discountShipping = 0;

        const clamp = (v, maxBase) => Math.max(0, Math.min(Math.round(v), Math.round(maxBase)));
        const applyPercent = (base, p, cap) => {
            const raw = (base * (p ?? 0)) / 100;
            return cap != null ? Math.min(raw, cap) : raw;
        };
        const ensureTargets = (t) => {
            if (Array.isArray(t)) return t;
            if (typeof t === "string") return [t];
            return ["ORDER"]; // mặc định nếu BE không trả
        };

        if (typeof res === "number") {
            // LEGACY: (0,100) là %, còn lại là số tiền → áp cho ORDER
            const computed = res > 0 && res < 100 ? (subtotal * res / 100) : res;
            discountItems = clamp(computed, subtotal);
        } else if (res && typeof res === "object") {
            const type = res.type || res.kind || null;                // "PERCENT" | "AMOUNT"
            const percent = res.percent ?? res.percentage ?? null;
            const amount = res.amount ?? res.value ?? null;
            const maxDiscount = res.maxDiscount ?? res.cap ?? null;
            const targets = ensureTargets(res.target || res.appliesTo); // "ORDER" | "SHIPPING" | ["ORDER","SHIPPING"]

            if (type === "PERCENT") {
                if (targets.includes("ORDER")) {
                    discountItems = clamp(applyPercent(subtotal, percent, maxDiscount), subtotal);
                }
                if (targets.includes("SHIPPING")) {
                    discountShipping = clamp(applyPercent(shippingFeeBase, percent, maxDiscount), shippingFeeBase);
                }
                if (targets.includes("BOTH")) {
                    const dItems = applyPercent(subtotal, percent, maxDiscount);
                    const dShip  = applyPercent(shippingFeeBase, percent, maxDiscount);
                    discountItems    = clamp(dItems, subtotal);
                    discountShipping = clamp(dShip, shippingFeeBase);
                }
            } else if (type === "AMOUNT") {
                if (targets.length === 1) {
                    if (targets[0] === "ORDER") {
                        discountItems = clamp(amount ?? 0, subtotal);
                    } else if (targets[0] === "SHIPPING") {
                        discountShipping = clamp(amount ?? 0, shippingFeeBase);
                    }
                } else {
                    // BOTH: ưu tiên trừ hàng rồi mới trừ ship
                    let remain = amount ?? 0;
                    const dItems = Math.min(remain, subtotal);
                    remain -= dItems;
                    const dShip = Math.min(remain, shippingFeeBase);
                    discountItems    = clamp(dItems, subtotal);
                    discountShipping = clamp(dShip, shippingFeeBase);
                }
            } else if (res.computed && typeof res.computed === "object") {
                // BE đã tính sẵn
                discountItems    = clamp(res.computed.items ?? 0, subtotal);
                discountShipping = clamp(res.computed.shipping ?? 0, shippingFeeBase);
            } else {
                // fallback: coi như legacy số
                const v = Number(res.amount ?? res.percent ?? 0);
                if (v > 0 && v < 100) {
                    discountItems = clamp(subtotal * v / 100, subtotal);
                } else {
                    discountItems = clamp(v, subtotal);
                }
            }
        } else {
            throw new Error("Phản hồi mã giảm giá không hợp lệ");
        }

        // tính lại phí ship hiển thị sau khi trừ giảm ship
        shippingFee = Math.max(0, shippingFeeBase - (discountShipping || 0));
        document.getElementById("shipping-fee").innerText = fmt(shippingFee);

        // thông báo
        const parts = [];
        if (discountItems > 0) parts.push(`hàng: ${fmt(discountItems)}`);
        if (discountShipping > 0) parts.push(`vận chuyển: ${fmt(discountShipping)}`);
        const msg = parts.length > 0 ? parts.join(" + ") : fmt(0);

        couponCode = code;
        document.getElementById("coupon-msg").innerText = `✅ Áp dụng mã thành công - Giảm ${msg}`;
    } catch (e) {
        console.error(e);
        document.getElementById("coupon-msg").innerText = `❌ Mã không hợp lệ hoặc không áp dụng cho sản phẩm`;
        couponCode = null;
        discountItems = 0;
        discountShipping = 0;

        // trả lại ship gốc
        shippingFee = shippingFeeBase || Number(shippingFee || 0);
        document.getElementById("shipping-fee").innerText = fmt(shippingFee);
    }

    // cập nhật tổng hợp (discount=items+shipping, grand= subtotal - items + shippingFee)
    if (typeof discount !== "undefined") {
        discount = (discountItems || 0) + (discountShipping || 0);
    }
    // đồng bộ phần tóm tắt bên phải
    document.getElementById("ship-fee-summary").innerText = fmt(shippingFee);
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
