window.addEventListener('DOMContentLoaded', event => {
    const sidebarToggle = document.body.querySelector('#sidebarToggle');
    const wrapper = document.getElementById('wrapper');

    if (sidebarToggle && wrapper) {
        sidebarToggle.addEventListener('click', event => {
            event.preventDefault();
            wrapper.classList.toggle('toggled');
            localStorage.setItem('sidebar-toggled', wrapper.classList.contains('toggled'));
        });
    }

    const isSidebarToggled = localStorage.getItem('sidebar-toggled');
    if (isSidebarToggled === 'true' && wrapper) {
        wrapper.classList.add('toggled');
    }
});