/**
 * Renaissance - Location & Geolocation Service
 */

const LocationService = (() => {

    /**
     * Obtains the current device GPS coordinates using the browser Geolocation API.
     * High accuracy is enabled to capture precise satellite coordinates.
     */
    const getCurrentLocation = (onStatusChange = null) => {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                if (onStatusChange) onStatusChange('Geolocation is not supported by your browser.', 'error');
                reject(new Error('Geolocation is not supported by this browser.'));
                return;
            }

            if (onStatusChange) {
                onStatusChange('Acquiring high-precision GPS lock...', 'scanning');
            }

            const options = {
                enableHighAccuracy: true,
                timeout: 12000,
                maximumAge: 0
            };

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    const coords = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude,
                        accuracy: position.coords.accuracy
                    };

                    if (onStatusChange) {
                        onStatusChange(`GPS Locked (±${Math.round(coords.accuracy)}m accuracy)`, 'success');
                    }

                    resolve(coords);
                },
                (error) => {
                    let msg = 'Unable to verify your location. Please try again.';
                    switch (error.code) {
                        case error.PERMISSION_DENIED:
                            msg = 'Your location permission is disabled. Please allow location access in your browser settings.';
                            break;
                        case error.POSITION_UNAVAILABLE:
                            msg = 'Location information is unavailable. Ensure your GPS is enabled.';
                            break;
                        case error.TIMEOUT:
                            msg = 'Location request timed out. Please retry in an open area.';
                            break;
                    }

                    if (onStatusChange) {
                        onStatusChange(msg, 'error');
                    }

                    reject(new Error(msg));
                },
                options
            );
        });
    };

    return {
        getCurrentLocation
    };
})();
