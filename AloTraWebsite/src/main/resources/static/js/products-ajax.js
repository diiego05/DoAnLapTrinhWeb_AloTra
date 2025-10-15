document.addEventListener('DOMContentLoaded', function () {

    // === LẤY CONTEXT PATH CHÍNH XÁC ===
    const detectContextPath = () => {
        let ctx = document.body?.dataset?.contextPath;
        if (!ctx || ctx === '/') {
            const parts = window.location.pathname.split('/').filter(Boolean);
            ctx = parts.length ? `/${parts[0]}/` : '/';
        }
        if (!ctx.endsWith('/')) ctx += '/';
        return ctx;
    };

    const contextPath = detectContextPath();
    const baseUrl = new URL(contextPath, window.location.origin).toString();

    // === DOM ELEMENTS ===
    const productModalEl = document.getElementById('productModal');
    const modal = productModalEl ? new bootstrap.Modal(productModalEl) : null;
    const detailModalEl = document.getElementById('productDetailModal');
    const detailModal = detailModalEl ? new bootstrap.Modal(detailModalEl) : null;

    const addNewBtn = document.getElementById('add-new-product-btn');
    const productForm = document.getElementById('product-form');
    const variantsContainer = document.getElementById('variants-container');
    const addVariantBtn = document.getElementById('add-variant-btn');
    const imageInput = document.getElementById('productImages');
    const imagePreviewContainer = document.getElementById('image-preview-container');
    const categorySelect = document.getElementById('productCategory');
    const statusSelect = document.getElementById('productStatus');
    const tableBody = document.getElementById('product-table-body');
    const searchInput = document.getElementById('searchInput');
    const filterCategory = document.getElementById('filterCategory');
    const filterBtn = document.getElementById('filterBtn');
    const alertContainer = document.getElementById('alert-container');
    const paginationContainer = document.getElementById('pagination-container');

    // Elements trong modal chi tiết
    const detailImages = document.getElementById('detailImages');
    const detailInfo = document.getElementById('detailInfo');
    const detailVariants = document.getElementById('detailVariants');

    let categories = [];
    let sizes = [];
    let allProducts = [];
    let filteredProducts = [];
    let currentPage = 1;
    const itemsPerPage = 5;

    // === HIỂN THỊ THÔNG BÁO ===
    const showAlert = (message, type = 'success') => {
        alertContainer.innerHTML = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>`;
    };

    // === PHÂN TRANG ===
    const renderPagination = (totalItems) => {
        const totalPages = Math.ceil(totalItems / itemsPerPage);
        paginationContainer.innerHTML = '';
        if (totalPages <= 1) return;

        let html = '<ul class="pagination pagination-sm justify-content-center mb-0">';
        for (let i = 1; i <= totalPages; i++) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a href="#" class="page-link" data-page="${i}">${i}</a>
                     </li>`;
        }
        html += '</ul>';
        paginationContainer.innerHTML = html;

        paginationContainer.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', e => {
                e.preventDefault();
                currentPage = parseInt(e.target.dataset.page);
                renderProducts();
            });
        });
    };

    // === HIỂN THỊ DANH SÁCH SẢN PHẨM ===
    const renderProducts = () => {
        tableBody.innerHTML = '';
        if (!Array.isArray(filteredProducts) || filteredProducts.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" class="text-center">Không tìm thấy sản phẩm nào.</td></tr>';
            paginationContainer.innerHTML = '';
            return;
        }

        const start = (currentPage - 1) * itemsPerPage;
        const end = start + itemsPerPage;
        const pageProducts = filteredProducts.slice(start, end);

        pageProducts.forEach(p => {
            const img = p.primaryImageUrl || new URL('images/default-product.png', baseUrl).toString();
            const status = p.status === 'ACTIVE'
                ? '<span class="badge bg-success">Đang bán</span>'
                : '<span class="badge bg-secondary">Tạm ẩn</span>';
            tableBody.insertAdjacentHTML('beforeend', `
                <tr>
                    <td><img src="${img}" alt="${p.name}" width="50" height="50" class="rounded border" style="object-fit:cover;"></td>
                    <td>${p.name}</td>
                    <td>${p.categoryName || '-'}</td>
                    <td>${status}</td>
                    <td>
                        <button class="btn btn-sm btn-primary view-btn" data-id="${p.id}">
                            <i class="fas fa-eye me-1"></i> Xem
                        </button>
                        <button class="btn btn-sm btn-info edit-btn" data-id="${p.id}">
                            <i class="fas fa-edit me-1"></i> Sửa
                        </button>
                        <button class="btn btn-sm btn-danger delete-btn" data-id="${p.id}">
                            <i class="fas fa-trash-alt me-1"></i> Xóa
                        </button>
                    </td>
                </tr>`);
        });

        renderPagination(filteredProducts.length);
    };

    // === TẢI DỮ LIỆU PHỤ ===
    const loadAuxData = async () => {
        try {
            const res = await fetch(new URL('api/products/aux-data', baseUrl));
            if (!res.ok) throw new Error('Không thể tải dữ liệu phụ');
            const data = await res.json();
            categories = data.categories || [];
            sizes = data.sizes || [];

            // Cập nhật combobox trong form + filter
            if (categorySelect) {
                categorySelect.innerHTML = '<option value="">-- Chọn danh mục --</option>';
                categories.forEach(c => {
                    categorySelect.insertAdjacentHTML('beforeend', `<option value="${c.id}">${c.name}</option>`);
                });
            }
            if (filterCategory) {
                filterCategory.innerHTML = '<option value="">-- Tất cả danh mục --</option>';
                categories.forEach(c => {
                    filterCategory.insertAdjacentHTML('beforeend', `<option value="${c.name}">${c.name}</option>`);
                });
            }
        } catch (e) {
            console.error('❌ Lỗi loadAuxData:', e);
            showAlert('Không thể tải danh mục hoặc size.', 'danger');
        }
    };

    // === TẢI DANH SÁCH SẢN PHẨM ===
    const loadProducts = async (keyword = '') => {
        try {
            const url = new URL('api/products', baseUrl);
            if (keyword) url.searchParams.set('keyword', keyword);

            const res = await fetch(url);
            if (!res.ok) throw new Error(`Lỗi server: ${res.status}`);
            const data = await res.json();

            allProducts = Array.isArray(data) ? data : [];
            filteredProducts = allProducts;
            currentPage = 1;
            renderProducts();
        } catch (e) {
            console.error('❌ loadProducts:', e);
            showAlert('Không thể tải danh sách sản phẩm.', 'danger');
        }
    };

    // === ÁP DỤNG LỌC (TÌM KIẾM + DANH MỤC) ===
    const applyFilter = () => {
        const keyword = searchInput.value.toLowerCase().trim();
        const selectedCategory = filterCategory.value;
        filteredProducts = allProducts.filter(p => {
            const matchName = p.name.toLowerCase().includes(keyword);
            const matchCategory = !selectedCategory || p.categoryName === selectedCategory;
            return matchName && matchCategory;
        });
        currentPage = 1;
        renderProducts();
    };

    // === THÊM BIẾN THỂ ===
    const addVariantRow = (selectedSizeId = '', price = '') => {
        if (!variantsContainer) return;
        if (sizes.length === 0) {
            showAlert('Chưa có Size. Vui lòng thêm trước.', 'warning');
            return;
        }
        const options = sizes.map(s =>
            `<option value="${s.id}" ${s.id == selectedSizeId ? 'selected' : ''}>${s.name}</option>`
        ).join('');
        variantsContainer.insertAdjacentHTML('beforeend', `
            <tr>
                <td><select class="form-select form-select-sm variant-size" required>${options}</select></td>
                <td><input type="number" class="form-control form-control-sm variant-price" placeholder="Giá tiền" value="${price}" required></td>
                <td><button type="button" class="btn btn-danger btn-sm remove-variant-btn">&times;</button></td>
            </tr>
        `);
    };

    // === MỞ MODAL THÊM MỚI ===
    const openAddModal = () => {
        productForm.reset();
        document.getElementById('productId').value = '';
        document.getElementById('productModalTitle').textContent = 'Thêm sản phẩm mới';
        variantsContainer.innerHTML = '';
        imagePreviewContainer.innerHTML = '';
        statusSelect.value = 'ACTIVE';
        addVariantRow();
        modal.show();
    };

    // === GỬI FORM (THÊM / SỬA) ===
    const handleFormSubmit = async (e) => {
        e.preventDefault();
        const id = document.getElementById('productId').value;
        const variantsData = [];
        variantsContainer.querySelectorAll('tr').forEach(row => {
            const sizeId = row.querySelector('.variant-size')?.value;
            const price = row.querySelector('.variant-price')?.value;
            if (sizeId && price) variantsData.push({ sizeId, price });
        });

        if (variantsData.length === 0) {
            showAlert('Cần ít nhất một biến thể.', 'warning');
            return;
        }

        const productData = {
            name: document.getElementById('productName').value,
            description: document.getElementById('productDescription').value,
            categoryId: document.getElementById('productCategory').value,
            status: statusSelect.value,
            variants: variantsData,
        };

        const formData = new FormData();
        formData.append('product', new Blob([JSON.stringify(productData)], { type: 'application/json' }));
        for (const f of imageInput.files) formData.append('files', f);

        try {
            const url = new URL(`api/products${id ? '/' + id : ''}`, baseUrl);
            const method = id ? 'PUT' : 'POST';
            const res = await fetch(url, { method, body: formData });
            if (res.ok) {
                showAlert(id ? 'Cập nhật sản phẩm thành công!' : 'Thêm sản phẩm thành công!');
                modal.hide();
                loadProducts();
            } else {
                const txt = await res.text();
                showAlert(`Lỗi khi lưu: ${txt}`, 'danger');
            }
        } catch (err) {
            console.error('❌ handleFormSubmit:', err);
            showAlert(`Lỗi kết nối: ${err.message}`, 'danger');
        }
    };

    // === SỬA / XEM / XÓA SẢN PHẨM ===
    const editProduct = async (id) => {
        try {
            const res = await fetch(new URL(`api/products/${id}`, baseUrl));
            if (!res.ok) throw new Error(`Không thể tải sản phẩm ${id}`);
            const p = await res.json();

            document.getElementById('productId').value = p.id;
            document.getElementById('productName').value = p.name;
            document.getElementById('productDescription').value = p.description || '';
            document.getElementById('productCategory').value = p.categoryId || '';
            statusSelect.value = p.status || 'ACTIVE';

            variantsContainer.innerHTML = '';
            (p.variants?.length ? p.variants : [{}]).forEach(v => addVariantRow(v.sizeId, v.price));
            imagePreviewContainer.innerHTML = (p.imageUrls || []).map(imgUrl =>
                `<img src="${imgUrl}" class="rounded border" style="height:80px;width:80px;object-fit:cover;">`
            ).join('');

            document.getElementById('productModalTitle').textContent = 'Cập nhật sản phẩm';
            modal.show();
        } catch (e) {
            console.error('❌ editProduct:', e);
            showAlert('Không thể tải dữ liệu sản phẩm.', 'danger');
        }
    };

    const deleteProduct = async (id) => {
        if (!confirm('Bạn có chắc muốn xóa sản phẩm này không?')) return;
        try {
            const res = await fetch(new URL(`api/products/${id}`, baseUrl), { method: 'DELETE' });
            if (res.ok) {
                showAlert('Đã xóa sản phẩm thành công!');
                loadProducts();
            } else {
                const txt = await res.text();
                showAlert(`Xóa thất bại: ${txt}`, 'danger');
            }
        } catch (e) {
            console.error('❌ deleteProduct:', e);
            showAlert('Không thể xóa sản phẩm.', 'danger');
        }
    };

    const viewProduct = async (id) => {
        try {
            const res = await fetch(new URL(`api/products/${id}/detail`, baseUrl));
            if (!res.ok) throw new Error(`Không thể tải chi tiết sản phẩm ${id}`);
            const p = await res.json();

            detailImages.innerHTML = (p.imageUrls?.length)
                ? p.imageUrls.map(url => `<img src="${url}" class="rounded border" style="height:100px;width:100px;object-fit:cover;">`).join('')
                : `<img src="${new URL('images/default-product.png', baseUrl)}" height="100" class="rounded border">`;

            const category = categories.find(c => c.id === p.categoryId)?.name || '-';
            const status = p.status === 'ACTIVE'
                ? '<span class="badge bg-success">Đang bán</span>'
                : '<span class="badge bg-secondary">Tạm ẩn</span>';
            detailInfo.innerHTML = `
                <h5 class="text-primary mb-2">${p.name}</h5>
                <p><strong>Danh mục:</strong> ${category}</p>
                <p><strong>Trạng thái:</strong> ${status}</p>
                <p><strong>Mô tả:</strong> ${p.description || 'Không có mô tả.'}</p>
            `;

            detailVariants.innerHTML = (p.variants?.length)
                ? p.variants.map(v => {
                    const sizeName = sizes.find(s => s.id === v.sizeId)?.name || 'N/A';
                    return `<tr>
                                <td>${sizeName}</td>
                                <td>${Number(v.price).toLocaleString('vi-VN')}₫</td>
                                <td><span class="badge bg-success">Còn hàng</span></td>
                            </tr>`;
                }).join('')
                : '<tr><td colspan="3" class="text-center text-muted">Không có biến thể.</td></tr>';
            detailModal.show();
        } catch (e) {
            console.error('❌ viewProduct:', e);
            showAlert('Không thể tải chi tiết sản phẩm.', 'danger');
        }
    };

    // === GẮN SỰ KIỆN ===
    addNewBtn?.addEventListener('click', openAddModal);
    addVariantBtn?.addEventListener('click', () => addVariantRow());
    variantsContainer?.addEventListener('click', e => {
        if (e.target.classList.contains('remove-variant-btn')) e.target.closest('tr').remove();
    });
    productForm?.addEventListener('submit', handleFormSubmit);
    tableBody?.addEventListener('click', e => {
        const btn = e.target.closest('button');
        if (!btn) return;
        const id = btn.dataset.id;
        if (btn.classList.contains('edit-btn')) editProduct(id);
        if (btn.classList.contains('delete-btn')) deleteProduct(id);
        if (btn.classList.contains('view-btn')) viewProduct(id);
    });
    searchInput?.addEventListener('input', () => {
        clearTimeout(searchInput._t);
        searchInput._t = setTimeout(applyFilter, 300);
    });
    filterBtn?.addEventListener('click', applyFilter);

    imageInput?.addEventListener('change', () => {
        imagePreviewContainer.innerHTML = '';
        for (const f of imageInput.files) {
            const reader = new FileReader();
            reader.onload = ev => {
                const img = document.createElement('img');
                img.src = ev.target.result;
                img.className = 'rounded border';
                img.style.cssText = 'height:80px;width:80px;object-fit:cover;';
                imagePreviewContainer.appendChild(img);
            };
            reader.readAsDataURL(f);
        }
    });

    // === KHỞI CHẠY ===
    loadAuxData().then(() => loadProducts());
});
