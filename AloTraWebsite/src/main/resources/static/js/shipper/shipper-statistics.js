"use strict";

import { apiFetch } from "/alotra-website/js/auth-helper.js";

// ========================== 🧭 Detect Context Path ==========================
function getContextPath() {
  const path = window.location.pathname;
  const parts = path.split('/');
  return parts.length > 1 ? '/' + parts[1] : '';
}
const contextPath = getContextPath();

// ========================== 📌 DOM Elements ==========================
const summaryCompleted = document.getElementById("summaryCompleted");
const summaryInProgress = document.getElementById("summaryInProgress");
const summaryEarnings = document.getElementById("summaryEarnings");

const historyTableBody = document.getElementById("historyTableBody");
const historyTotalEarnings = document.getElementById("historyTotalEarnings");

const fromInput = document.getElementById("filterFrom");
const toInput = document.getElementById("filterTo");
const btnApply = document.getElementById("btnApplyFilter");
const btnExport = document.getElementById("btnExportExcel");

let revenueChart, statusChart;

// ========================== 🧮 Helper ==========================
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
  return `/api/shipper/statistics/${endpoint}?from=${from}T00:00:00&to=${to}T23:59:59`;
}


// ========================== 📊 Load Summary ==========================
async function loadSummary(from, to) {
  try {
    const res = await apiFetch(apiUrl("summary", from, to));
    if (!res.ok) throw new Error("Tải dữ liệu thất bại");
    const data = await res.json();
    summaryCompleted.textContent = data.completedOrders ?? 0;
    summaryInProgress.textContent = data.inProgressOrders ?? 0;
    summaryEarnings.textContent = fmtCurrency(data.totalEarnings ?? 0);
  } catch (err) {
    console.error("❌ Lỗi loadSummary:", err);
    summaryCompleted.textContent = 0;
    summaryInProgress.textContent = 0;
    summaryEarnings.textContent = "0 ₫";
  }
}

// ========================== 📈 Revenue Chart ==========================
async function loadRevenueChart(from, to) {
  try {
    const res = await apiFetch(apiUrl("revenue", from, to));
    if (!res.ok) throw new Error();
    const data = await res.json();

    const labels = data.map(d => d.date);
    const values = data.map(d => d.revenue);

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
          borderWidth: 2,
          tension: 0.4,
          fill: true,
          pointRadius: 4,
          pointBackgroundColor: "#28a745"
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: ctx => fmtCurrency(ctx.parsed.y)
            }
          }
        },
        scales: {
          x: {
            ticks: { autoSkip: true, maxRotation: 0 }
          },
          y: {
            beginAtZero: true,
            ticks: { callback: value => fmtCurrency(value) }
          }
        }
      }
    });

  } catch (err) {
    console.error("❌ Lỗi loadRevenueChart:", err);
    if (revenueChart) revenueChart.destroy();
  }
}

// ========================== 🥧 Status Chart ==========================
async function loadStatusChart(from, to) {
  try {
    const res = await apiFetch(apiUrl("status-chart", from, to));
    if (!res.ok) throw new Error();
    const data = await res.json();

    const labels = data.map(d => d.status);
    const values = data.map(d => d.count);

    if (statusChart) statusChart.destroy();
    const ctx = document.getElementById("statusChart").getContext("2d");
    statusChart = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [
            "#28a745", "#0dcaf0", "#ffc107", "#dc3545", "#6c757d"
          ]
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { position: "bottom" } },
        cutout: "65%"
      }
    });
  } catch (err) {
    console.error("❌ Lỗi loadStatusChart:", err);
    if (statusChart) statusChart.destroy();
  }
}

// ========================== 🧾 History Table ==========================
async function loadHistory(from, to) {
  try {
    historyTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Đang tải...</td></tr>`;
    const res = await apiFetch(apiUrl("history", from, to));
    if (!res.ok) throw new Error();
    const data = await res.json();

    if (!data.length) {
      historyTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Không có dữ liệu</td></tr>`;
      historyTotalEarnings.textContent = "0 ₫";
      return;
    }

    let total = 0;
    historyTableBody.innerHTML = data.map(item => {
      total += Number(item.earnings) || 0;
      return `
        <tr>
          <td>#${item.orderCode}</td>
          <td>${new Date(item.deliveredAt).toLocaleString("vi-VN")}</td>
          <td>${fmtCurrency(item.shippingFee)}</td>
          <td class="text-success fw-bold">${fmtCurrency(item.earnings)}</td>
        </tr>
      `;
    }).join("");

    historyTotalEarnings.textContent = fmtCurrency(total);
  } catch (err) {
    console.error("❌ Lỗi loadHistory:", err);
    historyTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-danger py-4">Lỗi tải dữ liệu</td></tr>`;
    historyTotalEarnings.textContent = "0 ₫";
  }
}
// ========================== 📤 Export Excel ==========================
async function exportExcel(from, to) {
  try {
    const url = `${contextPath}${apiUrl("export-excel", from, to)}`;
    const res = await fetch(url, {
      method: "GET",
      headers: {
        "Accept": "application/octet-stream"
      },
      credentials: "include"
    });

    if (!res.ok) throw new Error("Lỗi xuất Excel");

    const blob = await res.blob();
    const fileUrl = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = fileUrl;
    a.download = `BaoCaoShipper_${from}_${to}.xlsx`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(fileUrl);

  } catch (err) {
    console.error("❌ Lỗi exportExcel:", err);
    showAlert("Không thể xuất file Excel.");
  }
}


// ========================== 🔁 Reload All ==========================
async function reloadAll() {
  const from = fromInput.value;
  const to = toInput.value;
  await Promise.all([
    loadSummary(from, to),
    loadRevenueChart(from, to),
    loadStatusChart(from, to),
    loadHistory(from, to)
  ]);
}

// ========================== 🧭 Events ==========================
btnApply.addEventListener("click", reloadAll);
btnExport.addEventListener("click", () => exportExcel(fromInput.value, toInput.value));

// ========================== 🚀 Init ==========================
setDefaultDateRange();
reloadAll();
