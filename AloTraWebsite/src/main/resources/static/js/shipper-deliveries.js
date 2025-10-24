"use strict";
import { apiFetch } from "/alotra-website/js/auth-helper.js";

// ===================== DOM ELEMENTS =====================
const tbody = document.getElementById("deliveryTableBody");
const searchInput = document.getElementById("shipperSearch");
const reloadBtn = document.getElementById("shipperReload");
const statusFilter = document.getElementById("statusFilter");

// Pagination
const prevPageBtn = document.getElementById("prevPage");
const nextPageBtn = document.getElementById("nextPage");
const paginationInfo = document.getElementById("paginationInfo");

// Detail panel
const detailCard = document.getElementById("detailCard");
const emptyDetailCard = document.getElementById("emptyDetailCard");
const detailLoading = document.getElementById("detailLoading");
const detailContent = document.getElementById("detailContent");

const btnAccept = document.getElementById("btnAccept");
const btnDelivered = document.getElementById("btnDelivered");
const btnCloseDetail = document.getElementById("btnCloseDetail");

// Field mapping
const el = {
  code: document.getElementById("detailCode"),
  status: document.getElementById("detailStatus"),
  date: document.getElementById("detailDate"),
  pay: document.getElementById("detailPayment"),
  addr: document.getElementById("detailAddress"),
  subtotal: document.getElementById("detailSubtotal"),
  discount: document.getElementById("detailDiscount"),
  shipping: document.getElementById("detailShipping"),
  total: document.getElementById("detailTotal"),
  items: document.getElementById("detailItems"),
  history: document.getElementById("detailHistory"),
};

// ===================== GLOBAL STATE =====================
let allOrders = [];
let filteredOrders = [];
let selectedId = null;
let currentPage = 1;
const pageSize = 5;

// ===================== HELPERS =====================
const fmtVND = v => (Number(v) || 0).toLocaleString("vi-VN") + " ₫";

function statusColor(s) {
  switch (s) {
    case "PENDING": return "secondary";
    case "CONFIRMED": return "warning";
    case "WAITING_FOR_PICKUP": return "warning";
    case "SHIPPING": return "info";
    case "COMPLETED": return "success";
    case "CANCELED": return "danger";
    case "PAID": return "primary";
    case "FAILED": return "dark";
    case "AWAITING_PAYMENT": return "secondary";
    default: return "dark";
  }
}

function statusText(s) {
  switch (s) {
    case "PENDING": return "Chờ xác nhận";
    case "CONFIRMED": return "Đã xác nhận";
    case "WAITING_FOR_PICKUP": return "Chờ shipper nhận đơn";
    case "SHIPPING": return "Đang giao hàng";
    case "COMPLETED": return "Hoàn thành";
    case "CANCELED": return "Đã hủy";
    case "PAID": return "Đã thanh toán";
    case "FAILED": return "Thanh toán thất bại";
    case "AWAITING_PAYMENT": return "Chờ thanh toán";
    default: return s;
  }
}

// ===================== LOAD LIST =====================
async function loadList() {
  tbody.innerHTML = `
    <tr><td colspan="5" class="text-center text-muted py-4">
      <div class="spinner-border spinner-border-sm me-2"></div>Đang tải...
    </td></tr>`;

  try {
    const res = await apiFetch("/api/shipper/orders");
    if (!res.ok) throw 0;
    allOrders = await res.json();
    applyFilter();
  } catch {
    tbody.innerHTML = `
      <tr><td colspan="5" class="text-center text-danger py-4">Lỗi tải đơn hàng</td></tr>`;
  }
}

// ===================== FILTER & PAGINATION =====================
function applyFilter() {
  const q = (searchInput.value || "").trim().toLowerCase();
  const status = statusFilter ? statusFilter.value : "";

  filteredOrders = allOrders.filter(o => {
    const matchSearch = !q || (o.code || "").toLowerCase().includes(q);
    const matchStatus = !status || o.status === status;
    return matchSearch && matchStatus;
  });

  renderTable();
}

function renderTable() {
  const total = filteredOrders.length;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  currentPage = Math.min(currentPage, totalPages);

  const start = (currentPage - 1) * pageSize;
  const pageOrders = filteredOrders.slice(start, start + pageSize);

  if (pageOrders.length === 0) {
    tbody.innerHTML = `
      <tr><td colspan="5" class="text-center text-muted py-4">Không có đơn hàng</td></tr>`;
  } else {
    tbody.innerHTML = pageOrders.map(o => `
      <tr class="order-row">
        <td class="fw-bold">#${o.code}</td>
        <td>${o.deliveryAddress ?? "—"}</td>
        <td><span class="badge bg-${statusColor(o.status)}">${statusText(o.status)}</span></td>
        <td>${new Date(o.createdAt).toLocaleString("vi-VN")}</td>
        <td class="text-center">
          <button class="btn btn-sm btn-outline-primary" data-view="${o.id}">
            <i class="fas fa-eye"></i>
          </button>
        </td>
      </tr>
    `).join("");

    tbody.querySelectorAll("[data-view]").forEach(btn => {
      btn.addEventListener("click", () => openDetail(btn.getAttribute("data-view")));
    });
  }

  paginationInfo.textContent = `Trang ${currentPage}/${totalPages} — Tổng ${total} đơn`;
  prevPageBtn.disabled = currentPage === 1;
  nextPageBtn.disabled = currentPage === totalPages;
}

// ===================== DETAIL =====================
async function openDetail(orderId) {
  selectedId = Number(orderId);
  emptyDetailCard.style.display = "none";
  detailCard.style.display = "block";
  detailLoading.style.display = "block";
  detailContent.style.display = "none";

  try {
    const res = await apiFetch(`/api/orders/${orderId}`);
    if (!res.ok) throw 0;
    const order = await res.json();
    bindDetail(order);
    detailLoading.style.display = "none";
    detailContent.style.display = "block";
  } catch {
    detailLoading.innerHTML = `<div class="text-danger">⚠️ Không thể tải chi tiết</div>`;
  }
}

function bindDetail(order) {
  el.code.textContent = `#${order.code}`;
  el.status.textContent = statusText(order.status);
  el.status.className = `badge bg-${statusColor(order.status)}`;
  el.date.textContent = new Date(order.createdAt).toLocaleString("vi-VN");
  el.pay.textContent = order.paymentMethod || "—";
  el.addr.textContent = order.deliveryAddress || "—";

  el.subtotal.textContent = fmtVND(order.subtotal);
  el.discount.textContent = fmtVND(order.discount);
  el.shipping.textContent = fmtVND(order.shippingFee);
  el.total.textContent = fmtVND(order.total);

  el.items.innerHTML = (order.items || []).map(it => `
    <tr>
      <td>${it.productName}</td>
      <td>${it.sizeName || "-"}</td>
      <td>${it.quantity}</td>
      <td>${fmtVND(it.unitPrice)}</td>
      <td>${fmtVND(it.lineTotal)}</td>
    </tr>
  `).join("");

  el.history.innerHTML = (order.statusHistory || []).length
    ? order.statusHistory.map(h => `
        <li class="mb-2 d-flex align-items-start">
          <div class="timeline-dot bg-${statusColor(h.status)} me-2"></div>
          <div>
            <div class="fw-bold">${statusText(h.status)}</div>
            <div class="text-muted small">${new Date(h.changedAt).toLocaleString("vi-VN")}</div>
            ${h.note ? `<div class="small fst-italic">${h.note}</div>` : ""}
          </div>
        </li>
      `).join("")
    : `<li class="text-muted">Không có lịch sử</li>`;

  // Hiển thị nút hành động phù hợp
  btnAccept.classList.add("d-none");
  btnDelivered.classList.add("d-none");

  if (order.status === "WAITING_FOR_PICKUP") {
    btnAccept.classList.remove("d-none");
    btnAccept.onclick = () => updateStatus(order.id, "accept");
  } else if (order.status === "SHIPPING") {
    btnDelivered.classList.remove("d-none");
    btnDelivered.onclick = () => updateStatus(order.id, "delivered");
  }
}

// ===================== ACTION =====================
async function updateStatus(orderId, action) {
  const endpoint =
    action === "accept" ? `/api/shipper/orders/${orderId}/accept` :
    action === "delivered" ? `/api/shipper/orders/${orderId}/delivered` : null;

  if (!endpoint) return;

  btnAccept.disabled = true;
  btnDelivered.disabled = true;

  const res = await apiFetch(endpoint, { method: "PUT" });

  btnAccept.disabled = false;
  btnDelivered.disabled = false;

  if (res.ok) {
    await loadList();
    await openDetail(orderId);
  } else {
    alert("❌ Không thể cập nhật trạng thái!");
  }
}

// ===================== EVENTS =====================
searchInput.addEventListener("input", () => { currentPage = 1; applyFilter(); });
if (statusFilter) statusFilter.addEventListener("change", () => { currentPage = 1; applyFilter(); });
reloadBtn.addEventListener("click", () => { searchInput.value = ""; if (statusFilter) statusFilter.value = ""; loadList(); });

btnCloseDetail.addEventListener("click", () => {
  selectedId = null;
  detailCard.style.display = "none";
  emptyDetailCard.style.display = "block";
});

prevPageBtn.addEventListener("click", () => {
  if (currentPage > 1) {
    currentPage--;
    renderTable();
  }
});
nextPageBtn.addEventListener("click", () => {
  currentPage++;
  renderTable();
});

// ===================== INIT =====================
loadList();
