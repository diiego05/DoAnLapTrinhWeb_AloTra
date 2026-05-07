"use strict";

// 🟢 Context Path
const contextPath = detectContextPath();
const DEFAULT_FROM = "2025-01-01";
const DEFAULT_TO = "2030-12-31";

// 🔹 Biến giữ instance chart để tránh vẽ chồng
let revenueChartInstance = null;
let orderStatusChartInstance = null;
const STATUS_MAP = {
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    WAITING_FOR_PICKUP: "Chờ lấy hàng",
    SHIPPING: "Đang giao",
    COMPLETED: "Hoàn thành",
    CANCELED: "Đã hủy",
    CANCELLED: "Đã hủy",
    DELIVERED: "Đã giao"
};
document.addEventListener("DOMContentLoaded", async () => {
    showLoadingState(true);
    try {
        await Promise.all([
            loadSummary(),
            loadRevenueChart(),
            loadOrderStatusChart(),
            loadTopProducts(),
            loadTopCustomers(),
            loadLatestOrders()
        ]);
    } finally {
        showLoadingState(false);
    }
});

// =============================
// 📍 Lấy context path
// =============================
function detectContextPath() {
    const parts = window.location.pathname.split('/').filter(Boolean);
    return parts.length > 0 ? '/' + parts[0] : '';
}

// =============================
// ⏳ Loading overlay
// =============================
function showLoadingState(isLoading) {
    const spinnerId = "vendorDashboardSpinner";
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
    try {
        const res = await fetch(`${contextPath}/api/vendor/dashboard/summary`);
        if (!res.ok) throw new Error("Lỗi khi tải dữ liệu tổng quan");
        const data = await res.json();

        document.getElementById("totalCustomers").textContent = data.totalUsers ?? 0;
        document.getElementById("totalOrders").textContent = data.totalOrders ?? 0;
        document.getElementById("totalRevenue").textContent = formatCurrency(data.totalRevenue ?? 0);
    } catch (err) {
        console.error("❌ Lỗi khi tải Summary:", err);
    }
}
// =============================
// 📈 2. Biểu đồ doanh thu
// =============================
async function loadRevenueChart() {
    try {
        const res = await fetch(`${contextPath}/api/vendor/dashboard/revenue?from=${DEFAULT_FROM}&to=${DEFAULT_TO}`);
        if (!res.ok) throw new Error("Lỗi khi tải dữ liệu biểu đồ doanh thu");
        const data = await res.json();

        const canvas = document.getElementById("revenueChart");
        const ctx = canvas.getContext("2d");

        if (!data || data.length === 0) {
            if (revenueChartInstance) revenueChartInstance.destroy();
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.font = "16px Arial";
            ctx.fillStyle = "#999";
            ctx.textAlign = "center";
            ctx.fillText("⚠️ Không có dữ liệu doanh thu", canvas.width / 2, canvas.height / 2);
            return;
        }

        // ✅ Sửa ở đây
        const labels = data.map(p => p.date);
        const values = data.map(p => p.revenue);

        if (revenueChartInstance) revenueChartInstance.destroy();

        revenueChartInstance = new Chart(ctx, {
            type: "line",
            data: {
                labels,
                datasets: [{
                    label: "Doanh thu",
                    data: values,
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
    } catch (err) {
        console.error("❌ Lỗi khi tải biểu đồ doanh thu:", err);
    }
}


// =============================
// 📊 3. Biểu đồ trạng thái đơn hàng
// =============================
async function loadOrderStatusChart() {
    try {
        const res = await fetch(`${contextPath}/api/vendor/dashboard/order-status?from=${DEFAULT_FROM}&to=${DEFAULT_TO}`);
        if (!res.ok) throw new Error("Lỗi khi tải dữ liệu trạng thái đơn hàng");
        const data = await res.json();

        const canvas = document.getElementById("orderStatusChart");
        const ctx = canvas.getContext("2d");

        // ✅ Nếu không có dữ liệu
        if (!data || data.length === 0) {
            if (orderStatusChartInstance) orderStatusChartInstance.destroy();
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.font = "16px Arial";
            ctx.fillStyle = "#999";
            ctx.textAlign = "center";
            ctx.fillText("⚠️ Không có dữ liệu trạng thái đơn", canvas.width / 2, canvas.height / 2);
            return;
        }

        const labels = data.map(item => STATUS_MAP[item.status] || item.status);
        const counts = data.map(item => item.count);

        if (orderStatusChartInstance) orderStatusChartInstance.destroy();

        orderStatusChartInstance = new Chart(ctx, {
            type: "doughnut",
            data: {
                labels,
                datasets: [{
                    data: counts,
                    backgroundColor: ["#ffc107", "#0d6efd", "#0dcaf0", "#17a2b8", "#28a745", "#dc3545"]
                }]
            },
            options: {
                responsive: true,
				plugins: {
				                   legend: { position: "bottom" },
				                   tooltip: {
				                       callbacks: {
				                           label: ctx => `${ctx.label}: ${ctx.parsed}`
				                       }
				                   }
				               },
                cutout: "70%"
            }
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải biểu đồ trạng thái đơn hàng:", err);
    }
}

// =============================
// 🏆 4. Top sản phẩm bán chạy
// =============================
async function loadTopProducts() {
    try {
        const res = await fetch(`${contextPath}/api/vendor/dashboard/top-products?from=${DEFAULT_FROM}&to=${DEFAULT_TO}&limit=5`);
        if (!res.ok) throw new Error("Lỗi khi tải top sản phẩm");
        const data = await res.json();

        console.log("🏆 Top Products:", data);

        const list = document.getElementById("topProducts");
        list.innerHTML = "";
        if (data.length === 0) {
            list.innerHTML = `<li class="list-group-item text-muted text-center">Không có dữ liệu</li>`;
            return;
        }

        data.forEach(p => {
            const li = document.createElement("li");
            li.className = "list-group-item d-flex justify-content-between align-items-center";
            li.innerHTML = `
                <span><i class="fas fa-box text-success me-2"></i>${p.name}</span>
                <span class="fw-bold text-success">${p.quantity} sp · ${formatCurrency(p.revenue)}</span>
            `;
            list.appendChild(li);
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải Top sản phẩm:", err);
    }
}

// =============================
// 🧑‍🤝‍🧑 5. Top khách hàng
// =============================
async function loadTopCustomers() {
    try {
        const res = await fetch(`${contextPath}/api/vendor/dashboard/top-customers?from=${DEFAULT_FROM}&to=${DEFAULT_TO}&limit=5`);
        if (!res.ok) throw new Error("Lỗi khi tải top khách hàng");
        const data = await res.json();

        console.log("👥 Top Customers:", data);

        const list = document.getElementById("topCustomers");
        list.innerHTML = "";
        if (data.length === 0) {
            list.innerHTML = `<li class="list-group-item text-muted text-center">Không có dữ liệu</li>`;
            return;
        }

        data.forEach(c => {
            const li = document.createElement("li");
            li.className = "list-group-item d-flex justify-content-between align-items-center";
            li.innerHTML = `
                <span><i class="fas fa-user text-success me-2"></i>${c.fullName}</span>
                <span class="fw-bold text-success">${formatCurrency(c.totalSpent)}</span>
            `;
            list.appendChild(li);
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải Top khách hàng:", err);
    }
}

// =============================
// 🧾 6. Đơn hàng mới nhất
// =============================
async function loadLatestOrders() {
    try {
        const res = await fetch(`${contextPath}/api/vendor/dashboard/latest-orders?limit=5`);
        if (!res.ok) throw new Error("Lỗi khi tải đơn hàng mới nhất");
        const data = await res.json();

        console.log("🧾 Latest Orders:", data);

        const tbody = document.getElementById("latestOrders");
        tbody.innerHTML = "";
        if (data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-3">Không có đơn hàng</td></tr>`;
            return;
        }

        data.forEach(o => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${o.code}</td>
                <td>${o.customerName}</td>
                <td>${formatCurrency(o.total)}</td>
				<td><span class="badge bg-${getStatusColor(o.status)}">${getStatusText(o.status)}</span></td>

                <td>${new Date(o.createdAt).toLocaleString('vi-VN')}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải đơn hàng mới nhất:", err);
    }
}

// =============================
// 🛠️ Helper
// =============================
function formatCurrency(v) {
    if (v == null) return "0₫";
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v);
}

function getStatusText(status){
	return STATUS_MAP[status]||status||'—';
}

function getStatusColor(status){
	switch(status){
		case "PENDING":return "warning";
		case "CONFIRMED":return "secondary";
		case "WAITING_FOR_PICKUP":return "primary";
		case "SHIPPING":return "info";
		case "COMPLETED":return "success";
		case "CANCELED":
		case "CANCELLED":return "danger";
		case "DELIVERED":return "success";
		default:return "secondary";
	}
}

