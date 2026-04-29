// Auto-refresh occupancy and predictions every 30 seconds
setInterval(function() {
    fetch('/api/occupancy')
        .then(response => response.json())
        .then(data => {
            // Update occupancy cards
            const container = document.getElementById('occupancy');
            if (container) {
                // Assuming we have a structure that can be updated
                // For simplicity, we'll just reload the page or use more sophisticated DOM updates
                // Here we'll just show a message; you can implement dynamic update
                console.log('Occupancy updated', data);
            }
        });

    fetch('/api/predictions')
        .then(response => response.json())
        .then(data => {
            const predContainer = document.getElementById('predictions');
            if (predContainer) {
                console.log('Predictions updated', data);
            }
        });
}, 30000);

// Hamburger menu toggle logic
document.addEventListener('DOMContentLoaded', () => {
    const menuToggle = document.querySelector('.menu-toggle');
    const navMenu = document.getElementById('nav-menu');

    if (menuToggle && navMenu) {
        menuToggle.addEventListener('click', () => {
            navMenu.classList.toggle('active');
            menuToggle.classList.toggle('active');
        });
    }

    // Add staggered animation classes to grid items on load
    const animateElements = document.querySelectorAll('.level-card, .pred-enhanced-card, .fee-card');
    animateElements.forEach((el, index) => {
        el.style.animationDelay = `${index * 0.08}s`;
        el.classList.add('staggered-entry');
    });
});

