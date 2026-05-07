"use strict";
import { apiFetch } from '/alotra-website/js/auth-helper.js';

const ordersList = document.getElementById('vendorOrdersList');
const paginationContainer = document.getElementById('vendorOrdersPagination');
const filterButtons = document.querySelectorAll('[data-status]');
const searchInput = document.getElementById('vendorOrderSearch');
const reloadBtn = document.getElementById('vendorOrderReload');
const applyBtn = document.getElementById('vendorOrderApply');
const fromInput = document.getElementById('vendorOrderFrom');
const toInput = document.getElementById('vendorOrderTo');

let currentStatus = '';
let allOrders = [];
let currentPage = 1;
const rowsPerPage = 5;

const fmtVND = v => (Number(v) || 0).toLocaleString('vi-VN') + ' ₫';

function mapStatusColor(status) {
	switch (status) {
		case 'PENDING': return 'warning';
		case 'CONFIRMED': return 'secondary';
		case 'SHIPPING': return 'info';
		case 'COMPLETED': return 'success';
		case 'CANCELED': return 'danger';
		case 'WAITING_FOR_PICKUP':return 'primary';
		default: return 'secondary';
	}
}

function mapStatusText(status) {
	switch (status) {
		case 'PENDING': return 'Chờ xác nhận';
		case 'CONFIRMED': return 'Đã xác nhận';
		case 'SHIPPING': return 'Đang giao';
		case 'COMPLETED': return 'Hoàn thành';
		case 'CANCELED': return 'Đã hủy';
		case 'WAITING_FOR_PICKUP': return 'Chờ lấy hàng';
		default: return status;
	}
}
function buildApiUrl() {
	const params = new URLSearchParams();
	if (currentStatus) params.set('status', currentStatus);
	const from = fromInput?.value?.trim();
	const to = toInput?.value?.trim();
	const q = searchInput?.value?.trim();
	if (from) params.set('from', from);
	if (to) params.set('to', to);
	if (q) params.set('q', q);
	const query = params.toString();
	return `/api/vendor/orders${query ? ('?' + query) : ''}`;
}

// =================== 📥 Lọc trạng thái + tìm kiếm ===================
filterButtons.forEach(btn => {
	btn.addEventListener('click', () => {
		filterButtons.forEach(b => b.classList.remove('active'));
		btn.classList.add('active');
		currentStatus = btn.dataset.status;
		loadVendorOrders();
	});
});

applyBtn?.addEventListener('click', () => loadVendorOrders());



searchInput?.addEventListener('keypress', (e) => {
	if (e.key === 'Enter') {
		e.preventDefault();
		loadVendorOrders();
	}
});

reloadBtn?.addEventListener('click', () => {
	currentStatus = '';
	filterButtons.forEach(b => b.classList.remove('active'));
	const allBtn = Array.from(filterButtons).find(b => b.dataset.status === '');
	if (allBtn) allBtn.classList.add('active');
	if (searchInput) searchInput.value = '';
	if (fromInput) fromInput.value = '';
	if (toInput) toInput.value = '';
	loadVendorOrders();
});

// =================== 📜 Load danh sách đơn hàng ===================
async function loadVendorOrders() {
	ordersList.innerHTML = `<div class="text-center text-muted py-4">
        <div class="spinner-border spinner-border-sm me-2"></div>Đang tải dữ liệu...
    </div>`;
	paginationContainer.innerHTML = '';
	try {
		const url = buildApiUrl();
		const res = await apiFetch(url);
		if (!res.ok) throw new Error();
		allOrders = await res.json();

		if (!allOrders || allOrders.length === 0) {
			ordersList.innerHTML = `<div class="text-center text-muted py-4">Không có đơn hàng</div>`;
			return;
		}

		currentPage = 1;
		renderOrders();
		renderPagination();

	} catch (e) {
		console.error(e);
		ordersList.innerHTML = `<div class="text-center text-danger py-4">⚠️ Lỗi tải đơn hàng</div>`;
	}
}

function mapPaymentText(method){
	switch(method){
		case 'COD':return 'Thanh toán khi nhận hàng';
		case 'PICKUP':return 'Nhận tại cửa hàng';
		case 'BANK':return 'Chuyển khoản ngân hàng';
		default:return method||'—';
	}
}

function mapPaymentColor(method){
	switch(method){
		case 'COD':return 'warning';
		case 'PICKUP':return 'info';
		case 'BANK':return 'primary';
		default:return 'secondary';
	}
}

function renderOrders() {
	const start = (currentPage - 1) * rowsPerPage;
	const end = start + rowsPerPage;
	const pageOrders = allOrders.slice(start, end);

	ordersList.innerHTML = pageOrders.map(o => `
		        <div class="card shadow-sm border-0 order-card">
		            <div class="card-body">
		                <div class="d-flex justify-content-between align-items-start">
		                    <div>
		                        <div class="fw-bold fs-5">#${o.code}</div>
		                        <small class="text-muted">${new Date(o.createdAt).toLocaleString('vi-VN')}</small>
                    </div>
					<span class="badge bg-${mapStatusColor(o.status)}">${mapStatusText(o.status)}</span>
					                </div>
					                <div class="mt-2">
					                    <strong>Tổng tiền:</strong> <span class="text-success fw-bold">${fmtVND(o.total)}</span>
					                </div>
					                <div class="mt-2 border-top pt-2">
					                    ${o.items.map(it => `
					                        <div class="d-flex justify-content-between small mb-1">
					                            <div>${it.productName} (${it.sizeName || '-'}) x ${it.quantity}</div>
					                            <div>${fmtVND(it.lineTotal)}</div>
					                        </div>
					                    `).join('')}
					                </div>
					                <div class="mt-3 d-flex gap-2 justify-content-end flex-wrap">
					                    ${o.status === 'PENDING' ? `
					                        <button class="btn btn-sm btn-success" onclick="vendorUpdateStatus(${o.id}, 'confirm')">
					                            <i class="fas fa-check"></i> Duyệt
                        </button>
						<button class="btn btn-sm btn-danger" onclick="vendorUpdateStatus(${o.id}, 'cancel')">
						                            <i class="fas fa-times"></i> Hủy
						                        </button>
						                    ` : ''}
						                    ${o.status === 'CONFIRMED' ? `
						                        <button class="btn btn-sm btn-primary" onclick="vendorUpdateStatus(${o.id}, 'ship')">
						                            <i class="fas fa-truck"></i> Giao hàng
						                        </button>
						                    ` : ''}
						                    <button class="btn btn-sm btn-outline-secondary" onclick="showVendorOrderDetail(${o.id})">
						                        <i class="fas fa-eye"></i> Chi tiết
						                    </button>
                </div>
            </div>
			</div>
			    `).join('');
}


// =================== 📑 Thanh phân trang ===================
function renderPagination() {
	paginationContainer.innerHTML = '';
	const totalPages = Math.ceil(allOrders.length / rowsPerPage);
	if (totalPages <= 1) return;

	const makeItem = (label, disabled, active, onClick) => {
		const li = document.createElement('li');
		li.className = `page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}`;
		const btn = document.createElement('button');
		btn.className = 'page-link';
		btn.textContent = label;
		btn.addEventListener('click', e => {
			e.preventDefault();
			if (!disabled) onClick();
		});
		li.appendChild(btn);
		return li;
	};

	paginationContainer.appendChild(makeItem('«', currentPage === 1, false, () => {
		currentPage--;
		renderOrders();
		renderPagination();
	}));

	const maxButtons = 5;
	let start = Math.max(1, currentPage - 2);
	let end = Math.min(totalPages, start + maxButtons - 1);
	if (end - start < maxButtons - 1) start = Math.max(1, end - maxButtons + 1);

	if (start > 1) {
		paginationContainer.appendChild(makeItem('1', false, currentPage === 1, () => { currentPage = 1; renderOrders(); renderPagination(); }));
		if (start > 2) paginationContainer.appendChild(makeItem('...', true, false, () => { }));
	}

	for (let i = start; i <= end; i++) {
		paginationContainer.appendChild(makeItem(i, false, i === currentPage, () => {
			currentPage = i;
			renderOrders();
			renderPagination();
		}));
	}

	if (end < totalPages) {
		if (end < totalPages - 1) paginationContainer.appendChild(makeItem('...', true, false, () => { }));
		paginationContainer.appendChild(makeItem(totalPages, false, currentPage === totalPages, () => {
			currentPage = totalPages;
			renderOrders();
			renderPagination();
		}));
	}

	paginationContainer.appendChild(makeItem('»', currentPage === totalPages, false, () => {
		currentPage++;
		renderOrders();
		renderPagination();
	}));
}

// =================== 📜 Modal chi tiết ===================
window.showVendorOrderDetail = async function(orderId) {
	const modal = new bootstrap.Modal(document.getElementById("vendorOrderModal"));
	modal.show();

	const loadingEl = document.getElementById("vendorModalLoading");
	const contentEl = document.getElementById("vendorModalContent");

	const btnCancel = document.getElementById("btnVendorCancelOrder");
	const btnConfirm = document.getElementById("btnVendorConfirmOrder");
	const btnShip = document.getElementById("btnVendorShipOrder");

	loadingEl.style.display = "block";
	contentEl.style.display = "none";
	btnCancel.classList.add("d-none");
	btnConfirm.classList.add("d-none");
	btnShip.classList.add("d-none");

	const res = await apiFetch(`/api/orders/${orderId}`);
	if (!res.ok) {
		loadingEl.textContent = "⚠️ Lỗi tải dữ liệu!";
		return;
	}
	const order = await res.json();

	document.getElementById("vendorModalOrderCode").textContent = `#${order.code}`;
	document.getElementById("vendorModalOrderDate").textContent = new Date(order.createdAt).toLocaleString('vi-VN');
	document.getElementById("vendorModalOrderStatus").textContent = mapStatusText(order.status);
	document.getElementById("vendorModalOrderStatus").className = `badge fs-6 bg-${mapStatusColor(order.status)}`;
	const payEl=document.getElementById("vendorModalPayment");
	payEl.textContent=mapPaymentText(order.paymentMethod);
	payEl.className=`badge fs-6 bg-${mapPaymentColor(order.paymentMethod)}`;

	document.getElementById("vendorModalAddress").textContent = order.deliveryAddress || '—';

	document.getElementById("vendorModalSubtotal").textContent = fmtVND(order.subtotal);
	document.getElementById("vendorModalDiscount").textContent = fmtVND(order.discount);
	document.getElementById("vendorModalShipping").textContent = fmtVND(order.shippingFee);
	document.getElementById("vendorModalTotal").textContent = fmtVND(order.total);

	document.getElementById("vendorModalOrderItems").innerHTML = order.items.map(it => `
        <tr>
            <td>${it.productName}</td>
            <td>${it.sizeName || '-'}</td>
            <td>${it.quantity}</td>
            <td>${fmtVND(it.unitPrice)}</td>
            <td>${fmtVND(it.lineTotal)}</td>
        </tr>
    `).join('');

	document.getElementById("vendorModalOrderHistory").innerHTML = order.statusHistory.length
		? order.statusHistory.map(h => `
            <li class="mb-2 d-flex align-items-start">
                <div class="timeline-dot bg-${mapStatusColor(h.status)} me-2"></div>
                <div>
                    <div class="fw-bold">${mapStatusText(h.status)}</div>
                    <div class="text-muted small">${new Date(h.changedAt).toLocaleString('vi-VN')}</div>
                    ${h.note ? `<div class="small fst-italic">${h.note}</div>` : ''}
                </div>
            </li>
        `).join('')
		: '<li class="text-muted">Không có lịch sử</li>';

	// Nút hành động modal
	if (order.status === 'PENDING') {
		btnCancel.classList.remove('d-none');
		btnConfirm.classList.remove('d-none');
		btnCancel.onclick = () => vendorUpdateStatus(orderId, 'cancel');
		btnConfirm.onclick = () => vendorUpdateStatus(orderId, 'confirm');
	} else if (order.status === 'CONFIRMED') {
		btnShip.classList.remove('d-none');
		btnShip.onclick = () => vendorUpdateStatus(orderId, 'ship');
	}

	loadingEl.style.display = "none";
	contentEl.style.display = "block";
};

// =================== ⚡ Cập nhật trạng thái ===================
window.vendorUpdateStatus = async function(orderId, action) {
	let endpoint = '';
	if (action === 'cancel') endpoint = `/api/vendor/orders/${orderId}/cancel`;
	if (action === 'confirm') endpoint = `/api/vendor/orders/${orderId}/confirm`;
	if (action === 'ship') endpoint = `/api/vendor/orders/${orderId}/ship`;

	const res = await apiFetch(endpoint, { method: 'PUT' });
	if (res.ok) {
		showAlert("✅ Cập nhật trạng thái thành công!");
		loadVendorOrders();
		const modalInstance = bootstrap.Modal.getInstance(document.getElementById("vendorOrderModal"));
		if (modalInstance) modalInstance.hide();
	} else {
		showAlert("❌ Không thể cập nhật trạng thái!");
	}
};

// =================== 🚀 Khởi chạy ===================
loadVendorOrders();

const style = document.createElement('style');
style.textContent = `
.order-card:hover { background-color: #f8f9fa; transition: 0.2s; }
.timeline-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
`;
document.head.appendChild(style);
