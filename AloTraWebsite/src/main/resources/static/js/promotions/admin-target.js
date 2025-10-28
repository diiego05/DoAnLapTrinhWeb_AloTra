"use strict";

import { apiFetch } from '/alotra-website/js/auth-helper.js';

const ctx = (() => {
	const path = window.location.pathname.split('/');
	return path.length > 1 && path[1] ? `/${path[1]}` : '';
})();

document.addEventListener('DOMContentLoaded', () => {
	const campaignSelect = document.getElementById('campaignSelect');
	const categorySelect = document.getElementById('categorySelect');
	const productCheckboxList = document.getElementById('productCheckboxList');
	const targetTableBody = document.getElementById('targetTableBody');
	const paginationContainer = document.getElementById('targetPagination');

	const categorySelectWrapper = document.getElementById('categorySelectWrapper');
	const productSelectWrapper = document.getElementById('productSelectWrapper');

	const btnSave = document.getElementById('btnSaveTarget');
	const btnCancel = document.getElementById('btnCancelTarget');

	let productCache = [];
	let categoryCache = [];
	let currentCampaignId = null;
	let targetData = [];

	const rowsPerPage = 5;
	let currentPage = 1;

	// ========================== LOAD DỮ LIỆU ==========================
	async function loadCampaigns() {
		const res = await apiFetch(`/api/admin/promotions/campaigns`);
		const data = await res.json();
		campaignSelect.innerHTML = `<option value="" disabled selected>— Vui lòng chọn chiến dịch khuyến mãi —</option>` +
			data.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
	}

	async function loadCategories() {
		const res = await apiFetch(`/api/products/aux-data`);
		const data = await res.json();
		categoryCache = data.categories;
		categorySelect.innerHTML = `<option value="" disabled selected>— Chọn danh mục —</option>` +
			categoryCache.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
	}

	async function loadProducts() {
		const res = await apiFetch(`/api/products`);
		productCache = await res.json();
		productCheckboxList.innerHTML = productCache.map(p => `
            <div class="col">
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" id="product-${p.id}" value="${p.id}">
                    <label class="form-check-label" for="product-${p.id}">${p.name}</label>
                </div>
            </div>
        `).join('');
	}

	// ========================== EVENT: Radio chọn loại target ==========================
	document.querySelectorAll('input[name="targetType"]').forEach(radio => {
		radio.addEventListener('change', () => {
			categorySelectWrapper.classList.toggle('d-none', radio.value !== 'CATEGORY');
			productSelectWrapper.classList.toggle('d-none', radio.value !== 'PRODUCTS');
		});
	});

	// ========================== EVENT: chọn campaign ==========================
	campaignSelect.addEventListener('change', async () => {
		currentCampaignId = campaignSelect.value;
		if (!currentCampaignId) return;
		await loadTargetsForCampaign(currentCampaignId);
	});

	// ========================== LƯU ĐỐI TƯỢNG ==========================
	btnSave.addEventListener('click', async () => {
		if (!currentCampaignId) {
			showAlert('Vui lòng chọn chiến dịch trước');
			return;
		}

		const type = document.querySelector('input[name="targetType"]:checked').value;
		const body = { targetType: type };

		if (type === 'CATEGORY') {
			const categoryId = categorySelect.value;
			if (!categoryId) {
				showAlert('Vui lòng chọn danh mục');
				return;
			}
			body.categoryId = categoryId;
		}

		if (type === 'PRODUCTS') {
			const selectedProducts = Array.from(productCheckboxList.querySelectorAll('input[type="checkbox"]:checked'))
				.map(cb => Number(cb.value));
			if (selectedProducts.length === 0) {
				showAlert('Vui lòng chọn ít nhất 1 sản phẩm');
				return;
			}
			body.productIds = selectedProducts;
		}

		const res = await fetch(`${ctx}/api/admin/promotions/targets/${currentCampaignId}`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(body)
		});

		if (!res.ok) {
			showAlert('❌ Lưu thất bại');
			return;
		}

		await loadTargetsForCampaign(currentCampaignId);
		showAlert('✅ Lưu thành công');
	});

	btnCancel.addEventListener('click', () => {
		campaignSelect.value = '';
		targetTableBody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">Vui lòng chọn chiến dịch để xem đối tượng áp dụng</td></tr>`;
		paginationContainer.innerHTML = '';
		currentCampaignId = null;
		targetData = [];
	});

	// ========================== LOAD TARGET TABLE ==========================
	async function loadTargetsForCampaign(campaignId) {
		const res = await apiFetch(`/api/admin/promotions/targets/by-campaign/${campaignId}`);
		targetData = await res.json();

		if (!Array.isArray(targetData) || targetData.length === 0) {
			targetTableBody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">Chưa có đối tượng áp dụng nào</td></tr>`;
			paginationContainer.innerHTML = '';
			return;
		}

		currentPage = 1;
		renderTargetTable();
		renderPagination();
	}

	// ========================== HIỂN THỊ BẢNG + PHÂN TRANG ==========================
	function renderTargetTable() {
		const start = (currentPage - 1) * rowsPerPage;
		const end = start + rowsPerPage;
		const pageData = targetData.slice(start, end);

		targetTableBody.innerHTML = pageData.map(t => `
            <tr>
                <td>${t.id}</td>
                <td>${t.campaignName || '(Không có tên chiến dịch)'}</td>
                <td>${t.targetType}</td>
                <td>${t.productId ? (productCache.find(p => p.id === t.productId)?.name || '—') : '(Tất cả)'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteTarget(${t.id})">
                        <i class="fa fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
	}
	function renderPagination() {
	        paginationContainer.innerHTML = '';

	        const totalPages = Math.ceil(targetData.length / rowsPerPage);
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

	        // Prev
	        paginationContainer.appendChild(makeItem('«', currentPage === 1, false, () => {
	            currentPage--;
	            renderTargetTable();
	            renderPagination();
	        }));

	        const maxButtons = 5;
	        let start = Math.max(1, currentPage - 2);
	        let end = Math.min(totalPages, start + maxButtons - 1);
	        if (end - start < maxButtons - 1) start = Math.max(1, end - maxButtons + 1);

	        if (start > 1) {
	            paginationContainer.appendChild(makeItem('1', false, currentPage === 1, () => {
	                currentPage = 1;
	                renderTargetTable();
	                renderPagination();
	            }));
	            if (start > 2) paginationContainer.appendChild(makeItem('...', true, false, () => {}));
	        }

	        for (let i = start; i <= end; i++) {
	            paginationContainer.appendChild(makeItem(i, false, i === currentPage, () => {
	                currentPage = i;
	                renderTargetTable();
	                renderPagination();
	            }));
	        }

	        if (end < totalPages) {
	            if (end < totalPages - 1) paginationContainer.appendChild(makeItem('...', true, false, () => {}));
	            paginationContainer.appendChild(makeItem(totalPages, false, currentPage === totalPages, () => {
	                currentPage = totalPages;
	                renderTargetTable();
	                renderPagination();
	            }));
	        }

	        // Next
	        paginationContainer.appendChild(makeItem('»', currentPage === totalPages, false, () => {
	            currentPage++;
	            renderTargetTable();
	            renderPagination();
	        }));
	    }
	// ========================== XOÁ ==========================
	window.deleteTarget = async (id) => {
		if (!confirm('Bạn có chắc muốn xóa đối tượng này?')) return;
		const res = await fetch(`${ctx}/api/admin/promotions/targets/single/${id}`, { method: 'DELETE' });
		if (res.ok) {
			await loadTargetsForCampaign(currentCampaignId);
		} else {
			showAlert('❌ Không thể xóa');
		}
	};

	// ========================== INIT ==========================
	(async function init() {
		await Promise.all([loadCampaigns(), loadCategories(), loadProducts()]);
	})();
});
