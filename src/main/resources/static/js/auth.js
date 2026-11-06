/**
 * Renaissance - Authentication Module for Teacher & Administrator
 */

const Auth = (() => {
    // Check if current user is logged in
    const checkAuthStatus = async () => {
        try {
            const res = await API.get('/api/auth/status');
            if (res && res.success && res.data) {
                return res.data;
            }
            return null;
        } catch (e) {
            return null;
        }
    };

    // Ensure teacher/admin is authenticated on faculty pages
    const requireAuth = async () => {
        const user = await checkAuthStatus();
        if (!user) {
            window.location.href = '/teacher-login.html?expired=true';
            return null;
        }

        // Populate user info in sidebar/topbar if elements exist
        const nameEl = document.getElementById('teacher-name-display');
        const roleEl = document.getElementById('teacher-role-display');
        const avatarEl = document.getElementById('teacher-avatar-display');

        if (nameEl) nameEl.textContent = user.name;
        if (roleEl) roleEl.textContent = user.role === 'ROLE_ADMIN' ? 'Administrator' : `${user.departmentCode || 'CSE'} Faculty`;
        if (avatarEl) {
            const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
            avatarEl.textContent = initials;
        }

        return user;
    };

    // Ensure administrator is authenticated on admin portal pages
    const requireAdminAuth = async () => {
        const user = await checkAuthStatus();
        if (!user) {
            window.location.href = '/admin-login.html?error=unauthorized';
            return null;
        }

        if (user.role !== 'ROLE_ADMIN') {
            window.location.href = '/admin-login.html?error=forbidden';
            return null;
        }

        const nameEl = document.getElementById('admin-name-display');
        const roleEl = document.getElementById('admin-role-display');
        const avatarEl = document.getElementById('admin-avatar-display');

        if (nameEl) nameEl.textContent = user.name;
        if (roleEl) roleEl.textContent = 'System Administrator';
        if (avatarEl) {
            const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
            avatarEl.textContent = initials;
        }

        return user;
    };

    // Login function
    const login = async (email, password) => {
        return await API.post('/api/auth/login', { email, password });
    };

    // Logout function
    const logout = async (targetLoginUrl) => {
        try {
            await API.post('/api/auth/logout');
        } catch (e) {
            // ignore
        } finally {
            const redirectUrl = targetLoginUrl || (window.location.pathname.includes('admin') ? '/admin-login.html?logout=true' : '/teacher-login.html?logout=true');
            window.location.href = redirectUrl;
        }
    };

    // Attach logout buttons
    const bindLogoutButtons = () => {
        document.querySelectorAll('.btn-logout').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                logout();
            });
        });
    };

    return {
        checkAuthStatus,
        requireAuth,
        requireAdminAuth,
        login,
        logout,
        bindLogoutButtons
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    Auth.bindLogoutButtons();
});
