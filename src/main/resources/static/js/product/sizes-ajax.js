// --- KHỞI ĐỘNG SAU KHI TOÀN BỘ TRANG HTML ĐÃ LOAD ---
document.addEventListener('DOMContentLoaded', function () {

    // --- KHAI BÁO BIẾN ---
    let contextPath = document.body.dataset.contextPath;

    // Nếu không có contextPath trong <body>, tự động xác định từ URL
    if (!contextPath) {
        const pathParts = window.location.pathname.split('/');
        contextPath = pathParts.length > 1 && pathParts[1] ? `/${pathParts[1]}/` : '/';
    }

    if (!contextPath.endsWith('/')) contextPath += '/';
    console.log('✅ contextPath:', contextPath);

    // Lấy các phần tử cần dùng
    const sizeModal = new bootstrap.Modal(document.getElementById('sizeModal'));
    const modalTitle = document.getElementById('modalTitle');
    const sizeForm = document.getElementById('size-form');
    const sizeIdInput = document.getElementById('sizeId');
    const sizeNameInput = document.getElementById('sizeName');
    const tableBody = document.getElementById('size-table-body');
    const addNewBtn = document.getElementById('add-new-size-btn');
    const nameError = document.getElementById('name-error');
    const alertContainer = document.getElementById('alert-container');

    // --- 1️⃣ HIỂN THỊ THÔNG BÁO ---
    const showAlert = (message, type = 'success') => {
        const alertHTML = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>`;
        alertContainer.innerHTML = alertHTML;
        setTimeout(() => {
            const alertEl = document.querySelector('.alert');
            if (alertEl) bootstrap.Alert.getOrCreateInstance(alertEl).close();
        }, 3500);
    };

    // --- 2️⃣ LOAD DANH SÁCH KÍCH THƯỚC ---
    const loadSizes = async () => {
        let response;
        try {
            const apiUrl = `${contextPath}api/sizes`;
            console.log('🔍 Gọi API:', apiUrl);
            response = await fetch(apiUrl);

            if (!response.ok) throw new Error(`Server responded with ${response.status}`);

            const sizes = await response.json();
            tableBody.innerHTML = '';

            if (!sizes || sizes.length === 0) {
                tableBody.innerHTML = `
                    <tr><td colspan="3" class="text-center text-muted">Chưa có kích thước nào.</td></tr>`;
                return;
            }

            sizes.forEach(size => {
                const row = `
                    <tr>
                        <td>${size.name || ''}</td>
                        <td>${size.code || '-'}</td>
                        <td>
                            <button class="btn btn-info btn-sm edit-btn" data-id="${size.id}">
                                <i class="fas fa-edit"></i> Sửa
                            </button>
                            <button class="btn btn-danger btn-sm delete-btn" data-id="${size.id}">
                                <i class="fas fa-trash"></i> Xóa
                            </button>
                        </td>
                    </tr>`;
                tableBody.insertAdjacentHTML('beforeend', row);
            });
        } catch (error) {
            console.error("❌ Lỗi khi tải danh sách kích thước:", error);
            const msg = response ? `Không thể tải dữ liệu (Mã lỗi: ${response.status})` : 'Không thể kết nối tới máy chủ!';
            showAlert(msg, 'danger');
        }
    };

    // --- 3️⃣ MỞ MODAL THÊM MỚI ---
    const openAddModal = () => {
        sizeForm.reset();
        sizeIdInput.value = '';
        sizeNameInput.classList.remove('is-invalid');
        nameError.textContent = '';
        modalTitle.textContent = 'Thêm Kích thước mới';
        sizeModal.show();
    };

    // --- 4️⃣ MỞ MODAL CHỈNH SỬA ---
    const openEditModal = async (id) => {
        try {
            const url = `${contextPath}api/sizes/${id}`;
            console.log('✏️ Gọi API GET:', url);
            const response = await fetch(url);
            if (!response.ok) throw new Error('Không tìm thấy kích thước.');

            const size = await response.json();
            sizeIdInput.value = size.id;
            sizeNameInput.value = size.name;
            sizeNameInput.classList.remove('is-invalid');
            nameError.textContent = '';
            modalTitle.textContent = 'Chỉnh sửa Kích thước';
            sizeModal.show();
        } catch (error) {
            console.error("❌ Lỗi khi mở modal sửa:", error);
            showAlert('Không thể lấy dữ liệu để chỉnh sửa!', 'danger');
        }
    };

    // --- 5️⃣ XOÁ KÍCH THƯỚC ---
    const handleDelete = async (id) => {
        if (!confirm('Bạn có chắc chắn muốn xóa kích thước này?')) return;

        try {
            const url = `${contextPath}api/sizes/${id}`;
            console.log('🗑️ Gọi API DELETE:', url);
            const response = await fetch(url, { method: 'DELETE' });

            if (response.ok) {
                showAlert('Xóa kích thước thành công!');
                loadSizes();
            } else if (response.status === 404) {
                showAlert('Không tìm thấy kích thước để xóa.', 'danger');
            } else {
                showAlert('Không thể xóa kích thước (có thể đang được sử dụng).', 'danger');
            }
        } catch (error) {
            console.error("❌ Lỗi khi xóa:", error);
            showAlert('Lỗi kết nối tới máy chủ!', 'danger');
        }
    };

    // --- 6️⃣ LƯU (THÊM HOẶC CẬP NHẬT) ---
    const handleFormSubmit = async (event) => {
        event.preventDefault();

        const id = sizeIdInput.value;
        const name = sizeNameInput.value.trim();

        if (!name) {
            sizeNameInput.classList.add('is-invalid');
            nameError.textContent = 'Tên kích thước không được để trống.';
            return;
        }

        const url = id ? `${contextPath}api/sizes/${id}` : `${contextPath}api/sizes`;
        const method = id ? 'PUT' : 'POST';

        try {
            console.log(`${method} → ${url}`);
            const response = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name })
            });

            if (response.ok) {
                showAlert(id ? 'Cập nhật thành công!' : 'Thêm mới thành công!');
                sizeModal.hide();
                loadSizes();
            } else if (response.status === 409) {
                const errorMsg = await response.text();
                sizeNameInput.classList.add('is-invalid');
                nameError.textContent = errorMsg || 'Tên kích thước đã tồn tại.';
            } else if (response.status === 404) {
                showAlert('Không tìm thấy kích thước để cập nhật.', 'danger');
            } else {
                showAlert('Lưu thất bại, lỗi máy chủ.', 'danger');
            }
        } catch (error) {
            console.error("❌ Lỗi khi lưu:", error);
            showAlert('Không thể kết nối tới máy chủ!', 'danger');
        }
    };

    // --- 7️⃣ GẮN SỰ KIỆN ---
    addNewBtn.addEventListener('click', openAddModal);
    tableBody.addEventListener('click', (event) => {
        const target = event.target.closest('button');
        if (!target) return;

        if (target.classList.contains('edit-btn')) openEditModal(target.dataset.id);
        if (target.classList.contains('delete-btn')) handleDelete(target.dataset.id);
    });
    sizeForm.addEventListener('submit', handleFormSubmit);

    // --- 8️⃣ KHỞI CHẠY ---
    loadSizes();
});
