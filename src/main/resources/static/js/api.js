/**
 * Renaissance - API Communication Layer using Fetch API
 */

const API = (() => {
    const request = async (url, options = {}) => {
        const defaultHeaders = {
            'Accept': 'application/json'
        };

        if (options.body && !(options.body instanceof FormData)) {
            defaultHeaders['Content-Type'] = 'application/json';
            options.body = JSON.stringify(options.body);
        }

        const config = {
            credentials: 'include', // Ensures HTTP-only session cookie is sent
            headers: {
                ...defaultHeaders,
                ...(options.headers || {})
            },
            ...options
        };

        try {
            const response = await fetch(url, config);

            // Handle downloadable files (e.g. CSV)
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('text/csv')) {
                const blob = await response.blob();
                return { isFile: true, blob, response };
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                const errorMessage = (data && data.message) ? data.message : `HTTP error: ${response.status}`;
                const errorCode = (data && data.errorCode) ? data.errorCode : 'UNKNOWN_ERROR';
                const error = new Error(errorMessage);
                error.status = response.status;
                error.errorCode = errorCode;
                error.data = data;
                throw error;
            }

            return data;
        } catch (error) {
            console.error(`API Error on ${url}:`, error);
            throw error;
        }
    };

    return {
        get: (url, options = {}) => request(url, { method: 'GET', ...options }),
        post: (url, body, options = {}) => request(url, { method: 'POST', body, ...options }),
        put: (url, body, options = {}) => request(url, { method: 'PUT', body, ...options }),
        delete: (url, options = {}) => request(url, { method: 'DELETE', ...options })
    };
})();
