"use strict";

// 📌 Lấy contextPath tự động (dùng chung cho toàn project)
function detectContextPath() {
    const parts = window.location.pathname.split('/').filter(Boolean);
    return parts.length > 0 ? '/' + parts[0] : '';
}

const contextPath = detectContextPath();

// Thời gian mặc định để lấy biểu đồ đơn hàng theo ngày (7 ngày gần nhất)
const DEFAULT_FROM = new Date(new Date().setDate(new Date().getDate() - 7)).toISOString();
const DEFAULT_TO = new Date().toISOString();

document.addEventListener("DOMContentLoaded", async () => {
    showLoadingState(true);
    try {
        await Promise.all([
            loadSummary(),
            loadOrderStatusChart(),
            loadOrdersByDateChart(),
            loadRecentOrders()
        ]);
    } catch (err) {
        console.error("❌ Lỗi khi tải dữ liệu Dashboard Shipper:", err);
    } finally {
        showLoadingState(false);
    }
});


// =============================
// ⏳ Loading Overlay
// =============================
function showLoadingState(isLoading) {
    const spinnerId = "shipperDashboardSpinner";
    if (isLoading) {
        if (!document.getElementById(spinnerId)) {
            const overlay = document.createElement("div");
            overlay.id = spinnerId;
            overlay.innerHTML = `
                <div class="d-flex justify-content-center align-items-center"
                     style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(255,255,255,0.7);z-index:9999;">
                    <div class="spinner-border text-success" role="status" style="width:3rem;height:3rem;">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>`;
            document.body.appendChild(overlay);
        }
    } else {
        const el = document.getElementById(spinnerId);
        if (el) el.remove();
    }
}


// =============================
// 📊 1. Tổng quan
// =============================
async function loadSummary() {
    const res = await fetch(`${contextPath}/api/shipper/dashboard/summary`);
    if (!res.ok) throw new Error("Không thể tải Summary");
    const data = await res.json();

    document.getElementById("todayOrders").textContent = data.todayOrders ?? 0;
    document.getElementById("inProgressOrders").textContent = data.inProgressOrders ?? 0;
    document.getElementById("completedOrders").textContent = data.completedOrders ?? 0;
    document.getElementById("totalEarnings").textContent = formatCurrency(data.totalEarnings ?? 0);
}


// =============================
// 📈 2. Biểu đồ trạng thái đơn hàng
// =============================
async function loadOrderStatusChart() {
    const res = await fetch(`${contextPath}/api/shipper/dashboard/order-status`);
    if (!res.ok) throw new Error("Không thể tải Order Status Chart");
    const data = await res.json();

    const labels = data.map(i => i.status);
    const counts = data.map(i => i.count);

    const ctx = document.getElementById("orderStatusChart").getContext("2d");
    new Chart(ctx, {
        type: "doughnut",
        data: {
            labels,
            datasets: [{
                data: counts,
                backgroundColor: [
                    "#28a745",
                    "#0dcaf0",
                    "#ffc107",
                    "#dc3545",
                    "#6c757d"
                ]
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: "bottom" } },
            cutout: "70%"
        }
    });
}


// =============================
// 📉 3. Biểu đồ đơn theo ngày
// =============================
async function loadOrdersByDateChart() {
    const res = await fetch(`${contextPath}/api/shipper/dashboard/orders-by-date?from=${DEFAULT_FROM}&to=${DEFAULT_TO}`);
    if (!res.ok) throw new Error("Không thể tải Orders by Date Chart");
    const data = await res.json();

    const labels = data.map(i => i.date);
    const counts = data.map(i => i.orderCount);  // ✅ Đúng field trả về

    const ctx = document.getElementById("ordersByDateChart").getContext("2d");
    new Chart(ctx, {
        type: "line",
        data: {
            labels,
            datasets: [{
                label: "Số đơn",
                data: counts,
                borderColor: "#28a745",
                backgroundColor: "rgba(40,167,69,0.1)",
                tension: 0.4,
                fill: true,
                pointRadius: 3,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true },
                x: { ticks: { autoSkip: true, maxTicksLimit: 10 } }
            }
        }
    });
}


// =============================
// 🧾 4. Đơn hàng gần đây
// =============================
async function loadRecentOrders() {
    const res = await fetch(`${contextPath}/api/shipper/dashboard/recent-orders?limit=5`);
    if (!res.ok) throw new Error("Không thể tải Recent Orders");
    const data = await res.json();

    const tbody = document.getElementById("recentOrders");
    tbody.innerHTML = "";

    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-3">Không có đơn hàng gần đây</td></tr>`;
        return;
    }

    data.forEach(o => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${o.code}</td>
            <td>${o.customerName}</td>
            <td>${o.address}</td>
            <td><span class="badge bg-${getStatusColor(o.status)}">${o.status}</span></td>
            <td>${new Date(o.createdAt).toLocaleString('vi-VN')}</td>
        `;
        tbody.appendChild(tr);
    });
}


// =============================
// 🛠️ Helper functions
// =============================
function formatCurrency(v) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v ?? 0);
}

function getStatusColor(status) {
    switch (status) {
        case "DELIVERED":
        case "COMPLETED": return "success";
        case "SHIPPING": return "info";
        case "PENDING": return "warning";
        case "CANCELLED": return "danger";
        default: return "secondary";
    }
}
