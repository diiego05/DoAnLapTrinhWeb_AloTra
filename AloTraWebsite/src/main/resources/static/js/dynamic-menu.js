"use strict";

/**
 * 🍔 Dynamic MegaMenu Loader
 * Load categories từ API và render mega menu
 */

const contextPath = window.location.pathname.split('/')[1] ? '/' + window.location.pathname.split('/')[1] : '';

// ======================= 📥 LOAD CATEGORIES FOR MEGA MENU ======================
async function loadCategoryMenu() {
    const container = document.getElementById('categoryMenuContainer');
    
    try {
        // Show loading spinner
        container.innerHTML = `
            <div class="col-12 text-center py-4">
                <div class="spinner-border text-success" role="status">
                    <span class="visually-hidden">Đang tải...</span>
                </div>
                <p class="text-muted mt-2 mb-0">Đang tải danh mục...</p>
            </div>
        `;

        const res = await fetch(`${contextPath}/api/categories/tree`);
        
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
        }

        const categories = await res.json();
        
        if (!categories || categories.length === 0) {
            container.innerHTML = `
                <div class="col-12 text-center text-muted py-5">
                    <i class="fas fa-folder-open fa-3x mb-3 opacity-50"></i>
                    <p class="mb-0">Chưa có danh mục nào</p>
                </div>
            `;
            return;
        }

        renderMegaMenu(categories);
        initDropdownHover();
        
        console.log('✅ MegaMenu loaded successfully with', categories.length, 'categories');
        
    } catch (error) {
        console.error('❌ Lỗi load categories:', error);
        container.innerHTML = `
            <div class="col-12 text-center text-danger py-5">
                <i class="fas fa-exclamation-triangle fa-3x mb-3"></i>
                <p class="mb-2 fw-bold">Không thể tải danh mục sản phẩm</p>
                <p class="small text-muted mb-0">${error.message}</p>
            </div>
        `;
    }
}

// ======================= 🎨 RENDER MEGA MENU ======================
function renderMegaMenu(categories) {
    const container = document.getElementById('categoryMenuContainer');
    if (!container) {
        console.warn('⚠️ Không tìm thấy #categoryMenuContainer');
        return;
    }

    // Render từng category với children
    const html = categories.map(cat => {
        const hasChildren = cat.children && cat.children.length > 0;
        const categoryUrl = cat.url || `${contextPath}/products?category=${cat.id}`;
        
        return `
            <div class="col-lg-3 col-md-6 mb-4">
                <h6 class="mega-menu-title text-success fw-bold mb-3">
                    ${cat.icon ? `<i class="${cat.icon} me-2"></i>` : '<i class="fas fa-mug-hot me-2"></i>'}
                    <a href="${categoryUrl}" class="text-decoration-none text-success">
                        ${cat.title}
                    </a>
                </h6>
                ${hasChildren ? `
                    <ul class="list-unstyled">
                        ${cat.children.map(child => {
                            const childUrl = child.url || `${contextPath}/products?category=${child.id}`;
                            return `
                                <li class="mb-2">
                                    <a href="${childUrl}" 
                                       class="mega-menu-item d-flex align-items-center text-decoration-none text-dark p-2 rounded">
                                        ${child.icon ? `<i class="${child.icon} me-2 text-success"></i>` : '<i class="fas fa-chevron-right me-2 text-success" style="font-size: 10px;"></i>'}
                                        <span>${child.title}</span>
                                    </a>
                                </li>
                            `;
                        }).join('')}
                    </ul>
                ` : ''}
            </div>
        `;
    }).join('');

    container.innerHTML = html;
}

// ======================= 🖱️ DROPDOWN HOVER EFFECT ======================
function initDropdownHover() {
    const menuDropdown = document.querySelector('.navbar-bottom .dropdown.position-static');
    if (!menuDropdown) {
        console.warn('⚠️ Không tìm thấy menu dropdown');
        return;
    }

    let hoverTimeout;
    let isMouseInside = false;
    
    const dropdownToggle = menuDropdown.querySelector('.dropdown-toggle');
    const dropdownMenu = menuDropdown.querySelector('.dropdown-menu');

    if (!dropdownToggle || !dropdownMenu) {
        console.warn('⚠️ Không tìm thấy dropdown elements');
        return;
    }

    // Helper function to show menu
    const showMenu = () => {
        clearTimeout(hoverTimeout);
        dropdownMenu.style.display = 'block';
        
        // Force reflow for animation
        void dropdownMenu.offsetWidth;
        
        requestAnimationFrame(() => {
            dropdownMenu.classList.add('show');
            dropdownToggle.setAttribute('aria-expanded', 'true');
            menuDropdown.classList.add('show');
        });
    };

    // Helper function to hide menu
    const hideMenu = () => {
        hoverTimeout = setTimeout(() => {
            if (!isMouseInside) {
                dropdownMenu.classList.remove('show');
                dropdownToggle.setAttribute('aria-expanded', 'false');
                menuDropdown.classList.remove('show');
                
                setTimeout(() => {
                    if (!menuDropdown.classList.contains('show')) {
                        dropdownMenu.style.display = 'none';
                    }
                }, 300); // Wait for animation
            }
        }, 200); // Delay before hiding
    };

    // Mouse enter on dropdown container
    menuDropdown.addEventListener('mouseenter', function() {
        isMouseInside = true;
        showMenu();
    });

    // Mouse leave from dropdown container
    menuDropdown.addEventListener('mouseleave', function() {
        isMouseInside = false;
        hideMenu();
    });

    // Click on toggle - redirect to products page
    dropdownToggle.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        window.location.href = `${contextPath}/products`;
    });

    // Prevent closing when clicking inside menu
    dropdownMenu.addEventListener('click', function(e) {
        // Only prevent if clicking on container, not links
        if (e.target === dropdownMenu || e.target.closest('.container')) {
            e.stopPropagation();
        }
    });

    console.log('✅ Dropdown hover initialized');
}

// ======================= 🚀 INIT ======================
document.addEventListener('DOMContentLoaded', () => {
    console.log('✅ Dynamic MegaMenu initializing...');
    
    // Small delay to ensure DOM is fully ready
    setTimeout(() => {
        loadCategoryMenu();
    }, 100);
});

// ======================= 🔄 RELOAD ON VISIBILITY CHANGE ======================
// Reload menu when tab becomes visible (useful for development)
document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
        const container = document.getElementById('categoryMenuContainer');
        if (container && container.innerHTML.includes('Không thể tải')) {
            console.log('🔄 Retrying to load menu...');
            loadCategoryMenu();
        }
    }
});