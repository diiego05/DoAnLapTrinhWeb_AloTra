"use strict";

import { apiFetch } from "/alotra-website/js/auth-helper.js";

// ========================== 🧭 Detect Context Path ==========================
function getContextPath() {
  const path = window.location.pathname;
  const parts = path.split('/');
  return parts.length > 1 ? '/' + parts[1] : '';
}
const contextPath = getContextPath();

// ========================== DOM Elements ==========================
const fromInput = document.getElementById("filterFrom");
const toInput = document.getElementById("filterTo");

const revenueTableBody = document.getElementById("revenueTableBody");
const productsTableBody = document.getElementById("productsTableBody");
const customersTableBody = document.getElementById("customersTableBody");

const btnApply = document.getElementById("btnApplyFilter");
const btnExportRevenue = document.getElementById("btnExportRevenue");
const btnExportProducts = document.getElementById("btnExportProducts");
const btnExportCustomers = document.getElementById("btnExportCustomers");

let revenueChart;

// ========================== Helper ==========================
function fmtCurrency(v) {
  return (Number(v) || 0).toLocaleString("vi-VN") + " ₫";
}

function setDefaultDateRange() {
  const today = new Date();
  const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
  fromInput.value = firstDay.toISOString().slice(0, 10);
  toInput.value = today.toISOString().slice(0, 10);
}

function apiUrl(endpoint, from, to) {
  return `/api/vendor/statistics/${endpoint}?from=${from}&to=${to}`;
}

// ========================== 📊 Load Revenue ==========================
async function loadRevenue(from, to) {
  try {
    revenueTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Đang tải...</td></tr>`;
    const res = await apiFetch(apiUrl("revenue", from, to));
    const data = await res.json();

    if (!data.length) {
      revenueTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Không có dữ liệu</td></tr>`;
      if (revenueChart) revenueChart.destroy();
      return;
    }

    const labels = data.map(d => d.date);
    const values = data.map(d => d.totalRevenue);

    // Render table
    revenueTableBody.innerHTML = data.map(row => `
      <tr>
        <td>${row.date}</td>
        <td>${row.totalOrders}</td>
        <td>${fmtCurrency(row.totalRevenue)}</td>
        <td>${fmtCurrency(row.avgPerOrder)}</td>
      </tr>
    `).join("");

    // Render chart
    if (revenueChart) revenueChart.destroy();
    const ctx = document.getElementById("revenueChart").getContext("2d");
    revenueChart = new Chart(ctx, {
      type: "line",
      data: {
        labels,
        datasets: [{
          label: "Doanh thu (VNĐ)",
          data: values,
          borderColor: "#28a745",
          backgroundColor: "rgba(40,167,69,0.1)",
          tension: 0.4,
          fill: true,
          pointRadius: 3
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: ctx => fmtCurrency(ctx.parsed.y) } }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { callback: v => fmtCurrency(v) }
          }
        }
      }
    });
  } catch (err) {
    console.error("❌ Lỗi loadRevenue:", err);
  }
}

// ========================== 🧋 Load Top Products ==========================
async function loadTopProducts(from, to) {
  try {
    productsTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Đang tải...</td></tr>`;
    const res = await apiFetch(apiUrl("products", from, to));
    const data = await res.json();

    if (!data.length) {
      productsTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Không có dữ liệu</td></tr>`;
      return;
    }

    productsTableBody.innerHTML = data.map(row => `
      <tr>
        <td>${row.productId}</td>
        <td>${row.productName}</td>
        <td>${row.totalQuantity}</td>
        <td>${fmtCurrency(row.totalRevenue)}</td>
      </tr>
    `).join("");
  } catch (err) {
    console.error("❌ Lỗi loadTopProducts:", err);
  }
}

// ========================== 👤 Load Top Customers ==========================
async function loadTopCustomers(from, to) {
  try {
    customersTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Đang tải...</td></tr>`;
    const res = await apiFetch(apiUrl("customers", from, to));
    const data = await res.json();

    if (!data.length) {
      customersTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Không có dữ liệu</td></tr>`;
      return;
    }

    customersTableBody.innerHTML = data.map(row => `
      <tr>
        <td>${row.customerId}</td>
        <td>${row.customerName}</td>
        <td>${row.totalOrders}</td>
        <td>${fmtCurrency(row.totalSpent)}</td>
      </tr>
    `).join("");
  } catch (err) {
    console.error("❌ Lỗi loadTopCustomers:", err);
  }
}
// ========================== 📤 Export Excel (thủ công) ==========================
async function exportExcel(endpoint, filename) {
  try {
    const from = fromInput.value;
    const to = toInput.value;

    // 📌 Lấy contextPath thủ công
    const contextPath = window.location.pathname.split('/')[1]
      ? '/' + window.location.pathname.split('/')[1]
      : '';

    // 📌 Gắn contextPath trực tiếp vào URL API
    const api = `${contextPath}/api/vendor/statistics/${endpoint}/export-excel?from=${from}&to=${to}`;

    const res = await fetch(api, { credentials: "include" });

    if (!res.ok) {
      console.error(`❌ Lỗi exportExcel: ${res.status} ${res.statusText}`);
      if (res.status === 404) {
        alert("Không tìm thấy endpoint export Excel (404)");
      } else {
        alert("Lỗi khi xuất file Excel.");
      }
      return;
    }

    const blob = await res.blob();

    // 📥 Tạo link tải xuống
    const fileUrl = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = fileUrl;
    a.download = `${filename}_${from}_${to}.xlsx`;
    a.style.display = "none";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(fileUrl);

  } catch (err) {
    console.error("❌ Lỗi exportExcel:", err);
    alert("Không thể xuất file Excel. Vui lòng thử lại.");
  }
}



// ========================== 🔁 Reload All ==========================
async function reloadAll() {
  const from = fromInput.value;
  const to = toInput.value;
  await Promise.all([
    loadRevenue(from, to),
    loadTopProducts(from, to),
    loadTopCustomers(from, to)
  ]);
}

// ========================== Events ==========================
btnApply.addEventListener("click", reloadAll);
btnExportRevenue.addEventListener("click", () => exportExcel("revenue", "DoanhThu"));
btnExportProducts.addEventListener("click", () => exportExcel("products", "TopSanPham"));
btnExportCustomers.addEventListener("click", () => exportExcel("customers", "TopKhachHang"));

// ========================== Init ==========================
setDefaultDateRange();
reloadAll();
