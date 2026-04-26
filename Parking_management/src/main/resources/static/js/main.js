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