"use strict";

const contextPath = "/alotra-website";

document.addEventListener("DOMContentLoaded", () => {
    loadSummary();
    loadRevenueChart();
    loadOrderStatusChart();
    loadTopProducts();
    loadTopBranches();
    loadTopCampaigns();
    loadLatestOrders();
});

// 🔹 Format tiền VND
const fmt = v => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);

// ================== 📊 Tổng quan ==================
async function loadSummary() {
    const res = await fetch(`${contextPath}/api/admin/dashboard/summary`);
    const data = await res.json();

    document.getElementById("totalUsers").innerText = data.totalUsers;
    document.getElementById("totalOrders").innerText = data.totalOrders;
    document.getElementById("totalBranches").innerText = data.totalBranches;
    document.getElementById("totalShippers").innerText = data.totalShippers;
    document.getElementById("totalRevenue").innerText = fmt(data.totalRevenue);
    document.getElementById("activePromotions").innerText = data.activePromotions;
}

// ================== 💰 Biểu đồ doanh thu ==================
async function loadRevenueChart() {
    const today = new Date().toISOString().split("T")[0];
    const yearStart = new Date(new Date().getFullYear(), 0, 1).toISOString().split("T")[0];

    const res = await fetch(`${contextPath}/api/admin/dashboard/revenue-chart?from=${yearStart}&to=${today}`);
    const data = await res.json();

    const labels = data.map(d => d.date);
    const values = data.map(d => d.revenue);

    new Chart(document.getElementById("revenueChart"), {
        type: "line",
        data: {
            labels,
            datasets: [{
                label: "Doanh thu (VND)",
                data: values,
                borderColor: "#0d6efd",
                backgroundColor: "rgba(13,110,253,0.2)",
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true } }
        }
    });
}

// ================== 📦 Biểu đồ trạng thái đơn hàng ==================
async function loadOrderStatusChart() {
    const today = new Date().toISOString().split("T")[0];
    const yearStart = new Date(new Date().getFullYear(), 0, 1).toISOString().split("T")[0];

    const res = await fetch(`${contextPath}/api/admin/dashboard/order-status-chart?from=${yearStart}&to=${today}`);
    const data = await res.json();

    const labels = data.map(d => d.status);
    const values = data.map(d => d.count);

    new Chart(document.getElementById("orderStatusChart"), {
        type: "doughnut",
        data: {
            labels,
            datasets: [{
                data: values,
                backgroundColor: ["#0d6efd", "#198754", "#dc3545", "#ffc107", "#6c757d"]
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: "bottom" } }
        }
    });
}

// ================== 🏆 Top sản phẩm ==================
async function loadTopProducts() {
    const today = new Date().toISOString().split("T")[0];
    const yearStart = new Date(new Date().getFullYear(), 0, 1).toISOString().split("T")[0];

    const res = await fetch(`${contextPath}/api/admin/dashboard/top-products?from=${yearStart}&to=${today}`);
    const data = await res.json();

    const list = document.getElementById("topProducts");
    list.innerHTML = data.map(p => `
        <li class="list-group-item d-flex justify-content-between">
            <span>${p.name}</span>
            <span class="fw-bold">${p.quantity} sp</span>
        </li>
    `).join("");
}

// ================== 🏪 Top chi nhánh ==================
async function loadTopBranches() {
    const today = new Date().toISOString().split("T")[0];
    const yearStart = new Date(new Date().getFullYear(), 0, 1).toISOString().split("T")[0];

    const res = await fetch(`${contextPath}/api/admin/dashboard/top-branches?from=${yearStart}&to=${today}`);
    const data = await res.json();

    const list = document.getElementById("topBranches");
    list.innerHTML = data.map(b => `
        <li class="list-group-item d-flex justify-content-between">
            <span>${b.branchName}</span>
            <span class="fw-bold">${fmt(b.revenue)}</span>
        </li>
    `).join("");
}

// ================== 📢 Top chiến dịch ==================
async function loadTopCampaigns() {
    const res = await fetch(`${contextPath}/api/admin/dashboard/top-campaigns?limit=5`);
    const data = await res.json();

    const list = document.getElementById("topCampaigns");
    list.innerHTML = data.map(c => `
        <li class="list-group-item d-flex justify-content-between">
            <span>${c.name}</span>
            <span class="fw-bold">${c.viewCount} view</span>
        </li>
    `).join("");
}

// ================== 🧾 Đơn hàng mới nhất ==================
async function loadLatestOrders() {
    const res = await fetch(`${contextPath}/api/admin/dashboard/latest-orders?limit=5`);
    const data = await res.json();

    const table = document.getElementById("latestOrders");
    table.innerHTML = data.map(o => `
        <tr>
            <td>${o.code}</td>
            <td>${o.customerName}</td>
            <td>${fmt(o.total)}</td>
            <td><span class="badge bg-secondary">${o.status}</span></td>
            <td>${o.createdAt.replace("T", " ")}</td>
        </tr>
    `).join("");
}
