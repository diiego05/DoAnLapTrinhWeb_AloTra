import { apiFetch } from '/alotra-website/js/auth-helper.js';

const ordersList = document.getElementById('ordersList');
const filterButtons = document.querySelectorAll('[data-status]');
let currentStatus = '';

/* ======================= 📌 Helpers ======================= */
const toNum = v => Number.isNaN(Number(v)) ? 0 : Number(v);
const fmtVND = v => toNum(v).toLocaleString('vi-VN') + ' ₫';

function mapStatusColor(status) {
    switch (status) {
        case 'PENDING': return 'warning';
        case 'CONFIRMED': return 'secondary';
        case 'AWAITING_PAYMENT': return 'info';
        case 'PAID': return 'primary';
        case 'SHIPPING': return 'info';
        case 'COMPLETED': return 'success';
        case 'CANCELED': return 'danger';
        case 'FAILED': return 'dark';
        default: return 'secondary';
    }
}
function mapStatusText(status) {
    switch (status) {
        case 'PENDING': return 'Chờ xác nhận';
        case 'CONFIRMED': return 'Đã xác nhận';
        case 'AWAITING_PAYMENT': return 'Chờ thanh toán';
        case 'PAID': return 'Đã thanh toán';
        case 'SHIPPING': return 'Đang giao';
        case 'COMPLETED': return 'Hoàn thành';
        case 'CANCELED': return 'Đã hủy';
        case 'FAILED': return 'Thất bại';
        default: return status;
    }
}
function mapPaymentMethodText(method) {
    if (!method) return '—';
    switch (method) {
        case 'PICKUP': return 'Nhận tại cửa hàng';
        case 'COD': return 'Thanh toán khi nhận hàng';
        case 'BANK': return 'Chuyển khoản ngân hàng';
        default: return method;
    }
}

/* ======================= 🧭 Bộ lọc trạng thái ======================= */
filterButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        filterButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentStatus = btn.dataset.status;
        loadOrders();
    });
});

/* ======================= 📥 Load danh sách đơn ======================= */
async function loadOrders() {
    ordersList.innerHTML = `<div class="text-center text-muted py-5">Đang tải dữ liệu...</div>`;
    try {
        const res = await apiFetch(`/api/orders${currentStatus ? `?status=${currentStatus}` : ''}`);
        if (!res.ok) throw new Error();
        const orders = await res.json();

        if (!orders || orders.length === 0) {
            ordersList.innerHTML = `<div class="text-center text-muted py-5">Không có đơn hàng</div>`;
            return;
        }

        ordersList.innerHTML = orders.map(o => `
            <div class="card shadow-sm border-0 order-card">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <div>
                            <div class="fw-bold text-dark fs-5">#${o.code}</div>
                            <small class="text-muted">${new Date(o.createdAt).toLocaleString('vi-VN')}</small>
                        </div>
                        <span class="badge bg-${mapStatusColor(o.status)}">${mapStatusText(o.status)}</span>
                    </div>
                    <div class="mb-2">
                        <strong>Tổng tiền:</strong>
                        <span class="text-success fw-bold">${fmtVND(o.total)}</span>
                    </div>
                    <div><strong>Phương thức:</strong> ${mapPaymentMethodText(o.paymentMethod)}</div>

                    <div class="mt-3 border-top pt-2">
                        ${o.items && o.items.length > 0
                            ? o.items.map(it => `
                                <div class="d-flex justify-content-between small mb-1">
                                    <div>${it.productName} (${it.sizeName || '-'}) x ${it.quantity}</div>
                                    <div>${fmtVND(it.lineTotal)}</div>
                                </div>
                            `).join('')
                            : '<div class="text-muted small fst-italic">Không có sản phẩm</div>'}
                    </div>

                    <div class="mt-3 d-flex justify-content-end gap-2">
                        ${(o.status === 'PENDING' || o.status === 'AWAITING_PAYMENT')
                        ? `<button class="btn btn-sm btn-danger" onclick="cancelOrder(${o.id})">
                                <i class="fas fa-times"></i> Hủy
                           </button>` : ''}
                        <button class="btn btn-sm btn-primary" onclick="showOrderDetail(${o.id})">
                            <i class="fas fa-eye"></i> Xem chi tiết
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch {
        ordersList.innerHTML = `<div class="text-center text-danger py-5">Lỗi tải đơn hàng</div>`;
    }
}

/* ======================= 📜 Modal Chi tiết ======================= */
window.showOrderDetail = async function(orderId) {
    const modal = new bootstrap.Modal(document.getElementById("orderDetailModal"));
    const loadingEl = document.getElementById("orderModalLoading");
    const contentEl = document.getElementById("orderModalContent");
    const cancelBtn = document.getElementById("btnCancelOrder");

    modal.show();
    loadingEl.style.display = "block";
    contentEl.style.display = "none";
    cancelBtn.classList.add('d-none');

    try {
        const res = await apiFetch(`/api/orders/${orderId}`);
        if (!res.ok) throw new Error();
        const order = await res.json();

        // 🧾 Thông tin chung
        document.getElementById("modalOrderCode").textContent = `#${order.code}`;
        document.getElementById("modalOrderDate").textContent = new Date(order.createdAt).toLocaleString('vi-VN');
        document.getElementById("modalOrderStatus").textContent = mapStatusText(order.status);
        document.getElementById("modalOrderStatus").className = `badge bg-${mapStatusColor(order.status)}`;
        document.getElementById("modalOrderPayment").textContent = mapPaymentMethodText(order.paymentMethod);
        document.getElementById("modalOrderAddress").textContent = order.deliveryAddress || '—';

        // 💰 Tổng tiền
        document.getElementById("modalSubtotal").textContent = fmtVND(order.subtotal);
        document.getElementById("modalDiscount").textContent = fmtVND(order.discount);
        document.getElementById("modalShipping").textContent = fmtVND(order.shippingFee);
        document.getElementById("modalOrderTotal").textContent = fmtVND(order.total);

        // 🛍 Sản phẩm
        const items = order.items || [];
        document.getElementById("modalOrderItems").innerHTML = items.length
            ? items.map(it => `
                <tr>
                    <td>
                        <div><strong>${it.productName}</strong></div>
                        ${it.toppings && it.toppings.length > 0
                            ? `<div class="small text-muted">${it.toppings.map(t => `${t.toppingName} (+${fmtVND(t.priceAtAddition)})`).join(', ')}</div>`
                            : ''}
                        ${it.note ? `<div class="small fst-italic text-muted">Ghi chú: ${it.note}</div>` : ''}
                    </td>
                    <td>${it.sizeName || '-'}</td>
                    <td>${it.quantity}</td>
                    <td>${fmtVND(it.unitPrice)}</td>
                    <td>${fmtVND(it.lineTotal)}</td>
                </tr>`).join('')
            : `<tr><td colspan="5" class="text-center text-muted">Không có sản phẩm</td></tr>`;

        // 🕒 Lịch sử
        const history = order.statusHistory || [];
        document.getElementById("modalOrderHistory").innerHTML = history.length
            ? history.map(h => `
                <li class="mb-3">
                    <div class="d-flex align-items-center">
                        <div class="timeline-dot bg-${mapStatusColor(h.status)} me-3"></div>
                        <div>
                            <div class="fw-bold">${mapStatusText(h.status)}</div>
                            <div class="text-muted small">${new Date(h.changedAt).toLocaleString('vi-VN')}</div>
                            ${h.note ? `<div class="text-muted fst-italic small">${h.note}</div>` : ''}
                        </div>
                    </div>
                </li>`).join('')
            : `<li class="text-center text-muted">Chưa có lịch sử</li>`;

        // 🟥 Nút hủy đơn
        if (order.status === 'PENDING' || order.status === 'AWAITING_PAYMENT') {
            cancelBtn.classList.remove('d-none');
            cancelBtn.onclick = () => cancelOrder(orderId, modal);
        }

        loadingEl.style.display = "none";
        contentEl.style.display = "block";
    } catch {
        loadingEl.textContent = "⚠️ Lỗi tải dữ liệu đơn hàng!";
    }
};

/* ======================= ❌ Hủy đơn ======================= */
window.cancelOrder = async function(orderId, modalInstance) {
    if (!confirm("Bạn có chắc muốn hủy đơn hàng này không?")) return;
    const res = await apiFetch(`/api/orders/${orderId}/cancel`, { method: 'PUT' });
    if (res.ok) {
        alert("✅ Hủy đơn hàng thành công!");
        if (modalInstance) modalInstance.hide();
        loadOrders();
    } else {
        alert("❌ Không thể hủy đơn hàng.");
    }
};

/* ======================= ✨ Style ======================= */
const style = document.createElement('style');
style.textContent = `
    .timeline-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
    .order-card:hover { background-color: #f9f9f9; transition: all 0.2s ease; }
`;
document.head.appendChild(style);

/* ======================= 🚀 Init ======================= */
loadOrders();
