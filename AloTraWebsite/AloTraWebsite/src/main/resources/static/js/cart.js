// ====================== 🧭 CẤU HÌNH CHUNG ======================
const contextPath = "/alotra-website";
const fmt = v => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);

async function api(url, method = 'GET', data) {
    const opt = { method, headers: { 'Content-Type': 'application/json' } };
    if (data) opt.body = JSON.stringify(data);
    const res = await fetch(contextPath + url, opt);
    if (!res.ok) throw new Error(`Lỗi API: ${url}`);
    return res.json();
}

// ====================== 🍞 TOAST TÙY CHỈNH ======================
function showCartToast(message = 'Thao tác thành công') {
    const toast = document.getElementById('cart-toast');
    toast.textContent = message;
    toast.classList.add('show');

    setTimeout(() => {
        toast.classList.remove('show');
    }, 2500);
}

// ====================== 🛍️ HIỆU ỨNG RUNG ICON CART ======================
function bounceCartIcon() {
    const icon = document.querySelector('.cart-icon i') || document.getElementById('cart-icon');
    if (!icon) return;
    icon.style.animation = 'cart-bounce 0.5s ease';
    icon.addEventListener('animationend', () => {
        icon.style.animation = '';
    }, { once: true });
}

// ====================== 🛍️ CẬP NHẬT ICON CART ======================
async function updateCartIconCount() {
    try {
        const cart = await api('/api/cart');
        const count = cart.items.length;
        const badge = document.getElementById('cart-count');
        if (badge) {
            badge.innerText = count;
            badge.style.display = count > 0 ? 'inline-block' : 'none';
        }
        bounceCartIcon();
    } catch (e) {
        console.error('Không thể cập nhật số cart:', e);
    }
}

// ====================== 🛒 LOAD GIỎ HÀNG ======================
async function loadCart() {
    const cart = await api('/api/cart');
    const container = document.getElementById('cart-container');
    container.innerHTML = '';

    if (cart.items.length === 0) {
        container.innerHTML = `<div class="alert alert-info text-center">Giỏ hàng trống.</div>`;
        document.getElementById('total').innerText = fmt(0);
        document.querySelector('.btn-delete-selected').disabled = true;
        document.getElementById('select-all').checked = false;
        updateCartIconCount();
        return;
    }

    // 👉 đảo ngược danh sách để item mới nhất nằm trên đầu
    cart.items.slice().reverse().forEach(item => {
        const toppings = item.toppings.map(t => `
            <span class="badge bg-light text-dark me-1">${t.name} (+${fmt(t.price)})</span>
        `).join('');

        const row = document.createElement('div');
        row.className = 'card mb-3';
        row.dataset.id = item.id;
        row.dataset.variant = item.variantId;
        row.dataset.product = item.productId;
        row.dataset.lineTotal = item.lineTotal;

        row.innerHTML = `
            <div class="card-body d-flex flex-wrap align-items-center">
                <div class="me-3">
                    <input type="checkbox" class="select-item form-check-input" style="transform: scale(1.4);">
                </div>
                <img src="${item.imageUrl || contextPath + '/images/placeholder.png'}"
                     class="rounded me-3 mb-2"
                     style="width:80px;height:80px;object-fit:cover">
                <div class="flex-grow-1">
                    <h5 class="mb-1">${item.productName}</h5>
                    <div class="text-muted small mb-1">Size: ${item.sizeName || 'Không có'}</div>

                    <div class="mb-2">
                        <strong>Đá:</strong>
                        <div class="btn-group btn-group-sm ice-group ms-2" role="group">
                            ${['Ít','Bình thường','Nhiều'].map(opt => `
                                <button class="btn btn-outline-success ice-btn ${item.note?.includes('Đá: '+opt) ? 'active' : ''}" data-value="${opt}">${opt}</button>
                            `).join('')}
                        </div>
                    </div>

                    <div class="mb-2">
                        <strong>Ngọt:</strong>
                        <div class="btn-group btn-group-sm sugar-group ms-2" role="group">
                            ${['Ít','Bình thường','Nhiều','Không'].map(opt => `
                                <button class="btn btn-outline-success sugar-btn ${item.note?.includes('Ngọt: '+opt) ? 'active' : ''}" data-value="${opt}">${opt}</button>
                            `).join('')}
                        </div>
                    </div>

                    <div class="small">Topping: ${toppings || '<span class="text-muted">Không</span>'}</div>
                    <input class="form-control form-control-sm note-input mt-2"
                           value="${item.note || ''}"
                           placeholder="Ghi chú thêm (tự cập nhật khi chọn đá/ngọt)">
                </div>

                <div class="text-end ms-auto" style="width:220px">
                    <div>Đơn giá: <strong>${fmt(item.unitPrice)}</strong></div>
                    <div class="input-group input-group-sm my-2">
                        <button class="btn btn-outline-secondary btn-dec">-</button>
                        <input type="text" class="form-control text-center qty-input" value="${item.quantity}">
                        <button class="btn btn-outline-secondary btn-inc">+</button>
                    </div>
                    <div class="fw-bold text-success line-total">${fmt(item.lineTotal)}</div>
                    <div class="mt-2">
                        <button class="btn btn-sm btn-outline-primary btn-size me-1">Đổi size</button>
                        <button class="btn btn-sm btn-outline-secondary btn-top me-1">Topping</button>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(row);
    });

    bindEvents();
    updateTotal();
    updateSelectAllState();
    updateCartIconCount();
    container.scrollTo({ top: 0, behavior: 'smooth' });
}

// ====================== 📌 GẮN SỰ KIỆN ======================
function bindEvents() {
    document.querySelectorAll('.btn-inc').forEach(btn => btn.onclick = e => changeQty(e, 1));
    document.querySelectorAll('.btn-dec').forEach(btn => btn.onclick = e => changeQty(e, -1));
    document.querySelectorAll('.note-input').forEach(input => input.onchange = updateNote);
    document.querySelectorAll('.btn-size').forEach(btn => btn.onclick = changeSize);
    document.querySelectorAll('.btn-top').forEach(btn => btn.onclick = changeTopping);
    document.querySelectorAll('.select-item').forEach(cb => cb.onchange = () => {
        updateTotal();
        updateSelectAllState();
    });
    document.querySelectorAll('.ice-btn').forEach(btn => btn.onclick = onIceSelect);
    document.querySelectorAll('.sugar-btn').forEach(btn => btn.onclick = onSugarSelect);
}

// ====================== 🧊 CHỌN ĐÁ / NGỌT ======================
function onIceSelect(e) {
    const card = e.target.closest('.card');
    card.querySelectorAll('.ice-btn').forEach(b => b.classList.remove('active'));
    e.target.classList.add('active');
    updateNoteAuto(card);
}

function onSugarSelect(e) {
    const card = e.target.closest('.card');
    card.querySelectorAll('.sugar-btn').forEach(b => b.classList.remove('active'));
    e.target.classList.add('active');
    updateNoteAuto(card);
}

async function updateNoteAuto(card) {
    const ice = card.querySelector('.ice-btn.active')?.dataset.value || 'Bình thường';
    const sugar = card.querySelector('.sugar-btn.active')?.dataset.value || 'Bình thường';
    const note = `Đá: ${ice} | Ngọt: ${sugar}`;
    card.querySelector('.note-input').value = note;
    await fetch(`${contextPath}/api/cart/items/${card.dataset.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ note })
    });
}

// ====================== 🧮 TÍNH TỔNG ======================
function updateTotal() {
    let total = 0;
    document.querySelectorAll('.select-item:checked').forEach(cb => {
        const card = cb.closest('.card');
        total += parseFloat(card.dataset.lineTotal);
    });
    document.getElementById('total').innerText = fmt(total);
}

// ====================== ✅ CHỌN TẤT CẢ ======================
const selectAll = document.getElementById('select-all');
const deleteSelectedBtn = document.querySelector('.btn-delete-selected');

selectAll.addEventListener('change', () => {
    const checked = selectAll.checked;
    document.querySelectorAll('.select-item').forEach(cb => cb.checked = checked);
    updateTotal();
    updateDeleteButtonState();
});

function updateSelectAllState() {
    const all = document.querySelectorAll('.select-item').length;
    const checked = document.querySelectorAll('.select-item:checked').length;
    selectAll.checked = all > 0 && checked === all;
    updateDeleteButtonState();
}

function updateDeleteButtonState() {
    const checkedCount = document.querySelectorAll('.select-item:checked').length;
    deleteSelectedBtn.disabled = checkedCount === 0;
}

// ====================== 🗑 XÓA NHIỀU ITEM (Không xác nhận) ======================
deleteSelectedBtn.addEventListener('click', async () => {
    const ids = Array.from(document.querySelectorAll('.select-item:checked'))
        .map(cb => cb.closest('.card').dataset.id);

    if (ids.length === 0) return;

    for (const id of ids) {
        await fetch(`${contextPath}/api/cart/items/${id}`, { method: 'DELETE' });
    }

    showCartToast(`Đã xóa ${ids.length} sản phẩm khỏi giỏ hàng 🛍️`);
    loadCart();
});

// ====================== ➕ THAY ĐỔI SỐ LƯỢNG ======================
async function changeQty(e, delta) {
    const card = e.target.closest('.card');
    const id = card.dataset.id;
    const input = card.querySelector('.qty-input');
    let qty = parseInt(input.value || '1') + delta;
    if (qty < 1) qty = 1;
    input.value = qty;
    await fetch(`${contextPath}/api/cart/items/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ quantity: qty })
    });
    loadCart();
}

// ====================== 📝 CẬP NHẬT GHI CHÚ ======================
async function updateNote(e) {
    const card = e.target.closest('.card');
    const id = card.dataset.id;
    const note = e.target.value;
    await fetch(`${contextPath}/api/cart/items/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ note })
    });
}

// ====================== 🔄 ĐỔI SIZE ======================
let currentCard = null;
async function changeSize(e) {
    currentCard = e.target.closest('.card');
    const productId = currentCard.dataset.product;
    const variantId = currentCard.dataset.variant;
    const list = await api(`/api/products/${productId}/variants`);
    const container = document.getElementById('size-options');
    container.innerHTML = '';

    list.forEach(v => {
        const btn = document.createElement('button');
        btn.className = `btn btn-outline-success`;
        btn.textContent = `${v.sizeName} (${fmt(v.price)})`;
        btn.dataset.id = v.id;
        if (v.id == variantId) btn.classList.add('active');
        btn.onclick = () => {
            container.querySelectorAll('button').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        };
        container.appendChild(btn);
    });

    new bootstrap.Modal(document.getElementById('sizeModal')).show();
}

document.getElementById('confirm-size').onclick = async () => {
    const active = document.querySelector('#size-options button.active');
    if (!active || !currentCard) return;
    const newVariantId = Number(active.dataset.id);
    await fetch(`${contextPath}/api/cart/items/${currentCard.dataset.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ variantId: newVariantId })
    });
    currentCard = null;
    bootstrap.Modal.getInstance(document.getElementById('sizeModal')).hide();
    loadCart();
};

// ====================== 🧋 ĐỔI TOPPING ======================
let currentCardTopping = null;
let allToppings = [];

async function changeTopping(e) {
    currentCardTopping = e.target.closest('.card');
    if (allToppings.length === 0) {
        allToppings = await api('/api/toppings');
    }

    const container = document.getElementById('topping-options');
    const selectedContainer = document.getElementById('selected-toppings');
    container.innerHTML = '';
    selectedContainer.innerHTML = '';

    allToppings.forEach(t => {
        const id = `topping-${t.id}`;
        const label = document.createElement('label');
        label.className = 'form-check d-flex align-items-center gap-2';
        label.innerHTML = `
            <input type="checkbox" class="form-check-input" id="${id}" value="${t.id}">
            <span>${t.name} (${fmt(t.price)})</span>
        `;
        container.appendChild(label);
    });

    container.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.onchange = updateSelectedTags;
    });

    new bootstrap.Modal(document.getElementById('toppingModal')).show();
}

function updateSelectedTags() {
    const selectedContainer = document.getElementById('selected-toppings');
    selectedContainer.innerHTML = '';
    document.querySelectorAll('#topping-options input:checked').forEach(cb => {
        const topping = allToppings.find(t => t.id == cb.value);
        const tag = document.createElement('span');
        tag.className = 'badge bg-success';
        tag.innerHTML = `${topping.name} <i class="fas fa-times"></i>`;
        tag.querySelector('i').onclick = () => {
            cb.checked = false;
            updateSelectedTags();
        };
        selectedContainer.appendChild(tag);
    });
}

document.getElementById('confirm-topping').onclick = async () => {
    if (!currentCardTopping) return;
    const ids = Array.from(document.querySelectorAll('#topping-options input:checked'))
        .map(cb => Number(cb.value));

    await fetch(`${contextPath}/api/cart/items/${currentCardTopping.dataset.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ toppingIds: ids })
    });

    currentCardTopping = null;
    bootstrap.Modal.getInstance(document.getElementById('toppingModal')).hide();
    loadCart();
};

// ====================== 🚀 KHỞI TẠO ======================
loadCart();
updateCartIconCount();
