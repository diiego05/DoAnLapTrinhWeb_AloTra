/**
 * Google Maps API Loader - Centralized loader for Google Maps API
 * Tải Google Maps API một lần cho toàn bộ website
 */

class GoogleMapsLoader {
    constructor() {
        this.apiKey = null;
        this.loadPromise = null;
        this.isLoaded = false;
        this.callbacks = [];
    }

    /**
     * Load Google Maps API
     * @returns {Promise<boolean>} true nếu load thành công
     */
    async load() {
        // Nếu đã load rồi, return ngay
        if (this.isLoaded) {
            return true;
        }

        // Nếu đang load, đợi promise hiện tại
        if (this.loadPromise) {
            return this.loadPromise;
        }

        // Bắt đầu load mới
        this.loadPromise = this._loadAPI();
        return this.loadPromise;
    }

    async _loadAPI() {
        try {
            // 1. Lấy API key từ server
            const res = await fetch('/alotra-website/api/public/config/maps-key');
            if (!res.ok || res.status === 204) {
                console.info('ℹ️ Google Maps API key not configured - using fallback Nominatim');
                return false;
            }

            this.apiKey = await res.text();
            if (!this.apiKey || this.apiKey.trim() === '') {
                console.info('ℹ️ Google Maps API key is empty - using fallback Nominatim');
                return false;
            }

            // 2. Kiểm tra xem script đã được load chưa
            if (window.google && window.google.maps) {
                this.isLoaded = true;
                this._triggerCallbacks();
                return true;
            }

            // 3. Load script
            await this._loadScript();
            this.isLoaded = true;
            this._triggerCallbacks();
            return true;

        } catch (error) {
            console.info('ℹ️ Google Maps API not available - using fallback Nominatim:', error.message);
            return false;
        }
    }

    _loadScript() {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = `https://maps.googleapis.com/maps/api/js?key=${this.apiKey}&libraries=places&language=vi&region=VN`;
            script.async = true;
            script.defer = true;

            script.onload = () => {
                console.log('✅ Google Maps API loaded successfully');
                resolve();
            };

            script.onerror = () => {
                console.error('❌ Failed to load Google Maps API');
                reject(new Error('Failed to load Google Maps API'));
            };

            document.head.appendChild(script);
        });
    }

    /**
     * Đăng ký callback khi API đã sẵn sàng
     * @param {Function} callback
     */
    onReady(callback) {
        if (this.isLoaded) {
            callback();
        } else {
            this.callbacks.push(callback);
        }
    }

    _triggerCallbacks() {
        this.callbacks.forEach(cb => {
            try {
                cb();
            } catch (e) {
                console.error('❌ Error in Google Maps callback:', e);
            }
        });
        this.callbacks = [];
    }

    /**
     * Tạo autocomplete cho input element
     * @param {HTMLElement} input
     * @param {Object} options
     * @returns {Promise<google.maps.places.Autocomplete|null>}
     */
    async createAutocomplete(input, options = {}) {
        const loaded = await this.load();
        if (!loaded || !window.google || !window.google.maps || !window.google.maps.places) {
            console.info('ℹ️ Using Nominatim autocomplete (free alternative)');
            return this.createNominatimAutocomplete(input);
        }

        const defaultOptions = {
            componentRestrictions: { country: 'vn' },
            fields: ['address_components', 'formatted_address', 'geometry'],
            language: 'vi'
        };

        return new google.maps.places.Autocomplete(input, { ...defaultOptions, ...options });
    }

    /**
     * 🆕 Tạo autocomplete dùng Nominatim (OpenStreetMap) - MIỄN PHÍ
     */
    createNominatimAutocomplete(input) {
        if (!input) return null;

        let timeout = null;
        let dropdown = null;

        // ✅ FIX: Định nghĩa hideDropdown như một closure function thay vì method
        const hideDropdown = () => {
            if (dropdown) {
                dropdown.remove();
                dropdown = null;
            }
        };

        const showDropdown = (suggestions) => {
            hideDropdown(); // ✅ Gọi trực tiếp hideDropdown

            if (suggestions.length === 0) return;

            dropdown = document.createElement('div');
            dropdown.className = 'nominatim-autocomplete-dropdown';
            dropdown.style.cssText = `
                position: absolute;
                background: white;
                border: 1px solid #ddd;
                border-radius: 4px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                max-height: 300px;
                overflow-y: auto;
                z-index: 9999;
                width: ${input.offsetWidth}px;
            `;

            suggestions.forEach(item => {
                const div = document.createElement('div');
                div.className = 'nominatim-suggestion-item';
                div.style.cssText = `
                    padding: 10px;
                    cursor: pointer;
                    border-bottom: 1px solid #f0f0f0;
                `;
                div.textContent = item.display_name;
                div.onmouseover = () => div.style.background = '#f5f5f5';
                div.onmouseout = () => div.style.background = 'white';
                div.onclick = () => {
                    input.value = item.display_name;
                    
                    // Trigger custom event để các handler khác xử lý
                    const event = new CustomEvent('nominatim-select', {
                        detail: {
                            address: item.display_name,
                            lat: parseFloat(item.lat),
                            lng: parseFloat(item.lon)
                        }
                    });
                    input.dispatchEvent(event);
                    
                    hideDropdown(); // ✅ Gọi trực tiếp hideDropdown
                };
                dropdown.appendChild(div);
            });

            const rect = input.getBoundingClientRect();
            dropdown.style.position = 'fixed';
            dropdown.style.top = (rect.bottom + 2) + 'px';
            dropdown.style.left = rect.left + 'px';

            document.body.appendChild(dropdown);
        };

        input.addEventListener('input', (e) => {
            clearTimeout(timeout);
            const query = e.target.value.trim();

            if (query.length < 3) {
                hideDropdown(); // ✅ Gọi trực tiếp hideDropdown
                return;
            }

            timeout = setTimeout(async () => {
                try {
                    const res = await fetch(
                        `https://nominatim.openstreetmap.org/search?` +
                        `q=${encodeURIComponent(query)}&` +
                        `format=json&` +
                        `countrycodes=vn&` +
                        `limit=5&` +
                        `addressdetails=1`,
                        { headers: { 'User-Agent': 'AloTraWebsite/1.0' } }
                    );
                    const data = await res.json();
                    showDropdown(data);
                } catch (err) {
                    console.warn('Nominatim autocomplete error:', err);
                }
            }, 500);
        });

        // ✅ Đóng dropdown khi click ra ngoài
        const clickOutsideHandler = (e) => {
            if (e.target !== input && !dropdown?.contains(e.target)) {
                hideDropdown();
            }
        };
        document.addEventListener('click', clickOutsideHandler);

        // ✅ Đóng dropdown khi nhấn ESC
        const escapeHandler = (e) => {
            if (e.key === 'Escape') {
                hideDropdown();
            }
        };
        input.addEventListener('keydown', escapeHandler);

        // ✅ Cleanup khi input bị remove khỏi DOM
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.removedNodes.forEach((node) => {
                    if (node === input || node.contains(input)) {
                        hideDropdown();
                        document.removeEventListener('click', clickOutsideHandler);
                        observer.disconnect();
                    }
                });
            });
        });
        
        if (input.parentNode) {
            observer.observe(input.parentNode, { childList: true, subtree: true });
        }

        console.log('✅ Nominatim autocomplete initialized');
        return { nominatim: true }; // Return object để indicate success
    }

    /**
     * Geocode địa chỉ thành tọa độ
     * @param {string} address
     * @returns {Promise<{lat: number, lng: number}|null>}
     */
    async geocode(address) {
        const loaded = await this.load();
        if (!loaded || !window.google || !window.google.maps) {
            console.warn('⚠️ Google Maps not available for geocoding');
            return null;
        }

        return new Promise((resolve) => {
            const geocoder = new google.maps.Geocoder();
            geocoder.geocode(
                {
                    address: address,
                    region: 'vn',
                    language: 'vi'
                },
                (results, status) => {
                    if (status === 'OK' && results[0]) {
                        const location = results[0].geometry.location;
                        resolve({
                            lat: location.lat(),
                            lng: location.lng()
                        });
                    } else {
                        console.warn('⚠️ Geocoding failed:', status);
                        resolve(null);
                    }
                }
            );
        });
    }

    /**
     * Parse địa chỉ Việt Nam từ address_components
     * @param {Array} components
     * @returns {{ward: string, district: string, city: string, street: string}}
     */
    parseVietnameseAddress(components) {
        const result = {
            street: '',
            ward: '',
            district: '',
            city: '',
            postalCode: ''
        };

        console.log('🗺️ [DEBUG] Parsing address components:', JSON.stringify(components, null, 2));

        // 📋 Tạo map để dễ truy xuất
        const componentMap = {};
        components.forEach(comp => {
            comp.types.forEach(type => {
                if (!componentMap[type]) {
                    componentMap[type] = comp.long_name;
                }
            });
        });

        console.log('🔍 [DEBUG] Component map:', JSON.stringify(componentMap, null, 2));

        // 🔍 Bước 1: Phân loại từng component theo priority
        components.forEach(component => {
            const types = component.types || [];
            const longName = component.long_name || '';

            // 📮 Postal code - LƯU RIÊNG, KHÔNG DÙNG
            if (types.includes('postal_code')) {
                result.postalCode = longName;
                console.log('📮 Postal code found:', longName);
                return;
            }

            // 🛣️ Street number và route
            if (types.includes('street_number')) {
                result.street = longName + (result.street ? ' ' : '');
                console.log('🏠 Street number:', longName);
            } 
            else if (types.includes('route')) {
                result.street = (result.street || '') + longName;
                console.log('🛣️ Route:', longName);
            }
            // 🏘️ Neighborhood (Khu phố)
            else if (types.includes('neighborhood')) {
                if (longName.includes('Khu phố') || longName.includes('KP')) {
                    result.street = (result.street ? result.street + ', ' : '') + longName;
                    console.log('🏘️ Neighborhood added to street:', longName);
                }
            }
        });

        // 🏘️ Bước 2: Tìm Ward (Phường/Xã) - ƯU TIÊN CAO NHẤT
        if (componentMap['sublocality_level_1']) {
            result.ward = componentMap['sublocality_level_1'];
            console.log('✅ Ward from sublocality_level_1:', result.ward);
        } else if (componentMap['sublocality_level_2']) {
            result.ward = componentMap['sublocality_level_2'];
            console.log('✅ Ward from sublocality_level_2:', result.ward);
        } else if (componentMap['administrative_area_level_3']) {
            result.ward = componentMap['administrative_area_level_3'];
            console.log('✅ Ward from administrative_area_level_3:', result.ward);
        }

        // 🏙️ Bước 3: Tìm District (Quận/Huyện/Thành phố cấp huyện)
        // ✅ ƯU TIÊN: locality > administrative_area_level_2
        if (componentMap['locality']) {
            // locality thường là Quận/Huyện/Thành phố cấp huyện ở VN
            result.district = componentMap['locality'];
            console.log('✅ District from locality:', result.district);
        } else if (componentMap['administrative_area_level_2']) {
            result.district = componentMap['administrative_area_level_2'];
            console.log('✅ District from administrative_area_level_2:', result.district);
        }

        // 🌆 Bước 4: Tìm City (Tỉnh/Thành phố trực thuộc TW)
        if (componentMap['administrative_area_level_1']) {
            result.city = componentMap['administrative_area_level_1'];
            console.log('✅ City from administrative_area_level_1:', result.city);
        }

        // 🔧 Bước 5: Post-processing & validation
        
        // ❌ Loại bỏ postal code nếu nhầm vào ward/district/city
        [result.ward, result.district, result.city].forEach((value, idx) => {
            const field = ['ward', 'district', 'city'][idx];
            if (value && /^\d{5,6}$/.test(value.trim())) {
                console.warn(`⚠️ Removing postal code from ${field}:`, value);
                if (field === 'ward') result.ward = '';
                if (field === 'district') result.district = '';
                if (field === 'city') result.city = '';
            }
        });

        // ❌ Loại bỏ country name nếu nhầm vào city/district/ward
        const countryNames = ['Việt Nam', 'Vietnam', 'VN'];
        [result.city, result.district, result.ward].forEach((value, idx) => {
            const field = ['city', 'district', 'ward'][idx];
            if (value && countryNames.includes(value)) {
                console.warn(`⚠️ Removing country name from ${field}:`, value);
                if (field === 'city') result.city = '';
                if (field === 'district') result.district = '';
                if (field === 'ward') result.ward = '';
            }
        });

        // 🔄 Nếu city bị xóa, tìm lại từ administrative_area_level_1
        if (!result.city && componentMap['administrative_area_level_1']) {
            const city = componentMap['administrative_area_level_1'];
            if (!countryNames.includes(city)) {
                result.city = city;
                console.log('🔄 Recovered city:', result.city);
            }
        }

        // ✅ Nếu không có street, thử lấy từ premise
        if (!result.street && componentMap['premise']) {
            result.street = componentMap['premise'];
            console.log('🔄 Street from premise:', result.street);
        }

        // 🔍 Debug log kết quả cuối cùng
        console.log('✅ [FINAL RESULT] Parsed address:', {
            street: result.street || '(empty)',
            ward: result.ward || '(empty)',
            district: result.district || '(empty)',
            city: result.city || '(empty)',
            postalCode: result.postalCode || '(empty)'
        });

        // ⚠️ Cảnh báo nếu thiếu thông tin
        const warnings = [];
        if (!result.street) warnings.push('street');
        if (!result.ward) warnings.push('ward');
        if (!result.district) warnings.push('district');
        if (!result.city) warnings.push('city');
        
        if (warnings.length > 0) {
            console.warn('⚠️ [WARNING] Missing address components:', warnings.join(', '));
        }

        // ✅ Trả về kết quả (không bao gồm postalCode)
        return {
            street: result.street,
            ward: result.ward,
            district: result.district,
            city: result.city
        };
    }

    /**
     * Tính khoảng cách giữa 2 điểm (Haversine formula)
     * @param {number} lat1 
     * @param {number} lng1 
     * @param {number} lat2 
     * @param {number} lng2 
     * @returns {number} Khoảng cách tính bằng km
     */
    calculateDistance(lat1, lng1, lat2, lng2) {
        const R = 6371; // Bán kính trái đất (km)
        const dLat = this._toRad(lat2 - lat1);
        const dLng = this._toRad(lng2 - lng1);
        
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(this._toRad(lat1)) * Math.cos(this._toRad(lat2)) *
                  Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    _toRad(degrees) {
        return degrees * (Math.PI / 180);
    }

    /**
     * Hiển thị bản đồ trên element
     * @param {HTMLElement} element 
     * @param {Object} options 
     * @returns {Promise<google.maps.Map|null>}
     */
    async createMap(element, options = {}) {
        const loaded = await this.load();
        if (!loaded || !window.google || !window.google.maps) {
            console.warn('⚠️ Google Maps not available');
            return null;
        }

        const defaultOptions = {
            center: { lat: 10.762622, lng: 106.660172 }, // TP.HCM
            zoom: 13,
            language: 'vi'
        };

        return new google.maps.Map(element, { ...defaultOptions, ...options });
    }

    /**
     * Thêm marker vào bản đồ
     * @param {google.maps.Map} map 
     * @param {Object} options 
     * @returns {google.maps.Marker|null}
     */
    createMarker(map, options = {}) {
        if (!window.google || !window.google.maps) {
            console.warn('⚠️ Google Maps not available');
            return null;
        }

        return new google.maps.Marker({ map, ...options });
    }
}

// Tạo instance global
window.googleMapsLoader = new GoogleMapsLoader();

// Export cho ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = GoogleMapsLoader;
}