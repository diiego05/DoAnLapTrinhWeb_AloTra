/**
 * Google Maps Utilities
 * Các hàm tiện ích bổ sung cho Google Maps API
 */

class MapsUtils {
    /**
     * Tìm chi nhánh gần nhất dựa trên vị trí hiện tại
     * @param {Array} branches - Danh sách chi nhánh [{id, name, lat, lng, ...}]
     * @param {number} userLat - Vĩ độ người dùng
     * @param {number} userLng - Kinh độ người dùng
     * @returns {Object} Chi nhánh gần nhất với thuộc tính distance
     */
    static findNearestBranch(branches, userLat, userLng) {
        if (!branches || branches.length === 0) return null;

        return branches
            .map(branch => ({
                ...branch,
                distance: window.googleMapsLoader.calculateDistance(
                    userLat, userLng,
                    branch.latitude || branch.lat,
                    branch.longitude || branch.lng
                )
            }))
            .sort((a, b) => a.distance - b.distance)[0];
    }

    /**
     * Lấy vị trí hiện tại của người dùng
     * @returns {Promise<{lat: number, lng: number}|null>}
     */
    static getCurrentPosition() {
        return new Promise((resolve) => {
            if (!navigator.geolocation) {
                console.warn('⚠️ Geolocation not supported');
                resolve(null);
                return;
            }

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    resolve({
                        lat: position.coords.latitude,
                        lng: position.coords.longitude
                    });
                },
                (error) => {
                    console.warn('⚠️ Cannot get current position:', error.message);
                    resolve(null);
                }
            );
        });
    }

    /**
     * Tạo info window cho marker
     * @param {google.maps.Map} map
     * @param {google.maps.Marker} marker
     * @param {string} content - HTML content
     * @returns {google.maps.InfoWindow}
     */
    static createInfoWindow(map, marker, content) {
        if (!window.google || !window.google.maps) return null;

        const infoWindow = new google.maps.InfoWindow({
            content: content
        });

        marker.addListener('click', () => {
            infoWindow.open(map, marker);
        });

        return infoWindow;
    }

    /**
     * Vẽ đường đi giữa 2 điểm
     * @param {google.maps.Map} map
     * @param {Object} origin - {lat, lng}
     * @param {Object} destination - {lat, lng}
     * @param {string} color - Màu đường (default: #4285F4)
     * @returns {google.maps.Polyline}
     */
    static drawRoute(map, origin, destination, color = '#4285F4') {
        if (!window.google || !window.google.maps) return null;

        const route = new google.maps.Polyline({
            path: [origin, destination],
            geodesic: true,
            strokeColor: color,
            strokeOpacity: 0.8,
            strokeWeight: 3,
            map: map
        });

        return route;
    }

    /**
     * Tính toán và hiển thị hướng đi (Directions)
     * @param {google.maps.Map} map
     * @param {Object} origin - {lat, lng}
     * @param {Object} destination - {lat, lng}
     * @param {HTMLElement} panel - Element để hiển thị hướng dẫn (optional)
     * @returns {Promise<Object>} Kết quả directions
     */
    static async calculateRoute(map, origin, destination, panel = null) {
        if (!window.google || !window.google.maps) return null;

        return new Promise((resolve, reject) => {
            const directionsService = new google.maps.DirectionsService();
            const directionsRenderer = new google.maps.DirectionsRenderer({
                map: map,
                panel: panel
            });

            directionsService.route(
                {
                    origin: origin,
                    destination: destination,
                    travelMode: google.maps.TravelMode.DRIVING
                },
                (result, status) => {
                    if (status === 'OK') {
                        directionsRenderer.setDirections(result);

                        // Lấy thông tin khoảng cách và thời gian
                        const route = result.routes[0];
                        const leg = route.legs[0];

                        resolve({
                            distance: leg.distance.text,
                            duration: leg.duration.text,
                            distanceValue: leg.distance.value,
                            durationValue: leg.duration.value
                        });
                    } else {
                        console.error('⚠️ Directions request failed:', status);
                        reject(status);
                    }
                }
            );
        });
    }

    /**
     * Fit map bounds để hiển thị tất cả markers
     * @param {google.maps.Map} map
     * @param {Array<google.maps.Marker>} markers
     */
    static fitBounds(map, markers) {
        if (!window.google || !window.google.maps || !markers || markers.length === 0) return;

        const bounds = new google.maps.LatLngBounds();
        markers.forEach(marker => {
            bounds.extend(marker.getPosition());
        });
        map.fitBounds(bounds);
    }

    /**
     * Tạo custom marker icon
     * @param {string} color - Màu (hex hoặc tên màu)
     * @param {string} label - Text hiển thị trên marker
     * @returns {Object} Icon configuration
     */
    static createCustomIcon(color = '#FF0000', label = '') {
        return {
            path: google.maps.SymbolPath.CIRCLE,
            fillColor: color,
            fillOpacity: 0.8,
            strokeColor: '#FFFFFF',
            strokeWeight: 2,
            scale: 10,
            labelOrigin: new google.maps.Point(0, 0)
        };
    }

    /**
     * Format khoảng cách
     * @param {number} distanceInKm - Khoảng cách tính bằng km
     * @returns {string} Khoảng cách đã format (vd: "1.5 km" hoặc "500 m")
     */
    static formatDistance(distanceInKm) {
        if (distanceInKm < 1) {
            return Math.round(distanceInKm * 1000) + ' m';
        }
        return distanceInKm.toFixed(1) + ' km';
    }

    /**
     * Kiểm tra xem điểm có nằm trong vùng không
     * @param {Object} point - {lat, lng}
     * @param {google.maps.Circle} circle
     * @returns {boolean}
     */
    static isPointInCircle(point, circle) {
        if (!window.google || !window.google.maps) return false;

        const distance = google.maps.geometry.spherical.computeDistanceBetween(
            new google.maps.LatLng(point.lat, point.lng),
            circle.getCenter()
        );

        return distance <= circle.getRadius();
    }

    /**
     * Tạo heatmap layer
     * @param {google.maps.Map} map
     * @param {Array<Object>} data - [{lat, lng, weight}]
     * @returns {google.maps.visualization.HeatmapLayer}
     */
    static createHeatmap(map, data) {
        if (!window.google || !window.google.maps || !window.google.maps.visualization) {
            console.warn('⚠️ Visualization library not loaded');
            return null;
        }

        const heatmapData = data.map(point => ({
            location: new google.maps.LatLng(point.lat, point.lng),
            weight: point.weight || 1
        }));

        return new google.maps.visualization.HeatmapLayer({
            data: heatmapData,
            map: map
        });
    }
}

// Export
window.MapsUtils = MapsUtils;

if (typeof module !== 'undefined' && module.exports) {
    module.exports = MapsUtils;
}