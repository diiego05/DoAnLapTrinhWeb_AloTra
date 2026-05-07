document.addEventListener('DOMContentLoaded', function () {
    const input = document.getElementById('addressLine1');
    if (!input || !window.google) return;

    const autocomplete = new google.maps.places.Autocomplete(input, {
        types: ['geocode'],
        componentRestrictions: { country: 'vn' }
    });

    autocomplete.addListener('place_changed', function () {
        const place = autocomplete.getPlace();
        if (!place.address_components) return;

        let ward = '', district = '', city = '';
        place.address_components.forEach(comp => {
            if (comp.types.includes('administrative_area_level_3')) ward = comp.long_name;
            if (comp.types.includes('administrative_area_level_2')) district = comp.long_name;
            if (comp.types.includes('administrative_area_level_1')) city = comp.long_name;
            if (comp.types.includes('locality') && !city) city = comp.long_name;
        });

        document.getElementById('ward')?.value = ward;
        document.getElementById('district')?.value = district;
        document.getElementById('city')?.value = city;

        // Nếu muốn lưu tọa độ:
        if (place.geometry) {
            document.getElementById('latitude')?.value = place.geometry.location.lat();
            document.getElementById('longitude')?.value = place.geometry.location.lng();
        }
    });
});