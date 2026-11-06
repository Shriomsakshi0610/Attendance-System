/**
 * Renaissance - Authentication Module for Teacher/Admin
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

    // Ensure user is authenticated on protected pages
    const requireAuth = async () => {
        const teacher = await checkAuthStatus();
        if (!teacher) {
            window.location.href = '/teacher-login.html?expired=true';
            return null;
        }

        // Populate teacher info in sidebar/topbar if elements exist
        const nameEl = document.getElementById('teacher-name-display');
        const roleEl = document.getElementById('teacher-role-display');
        const avatarEl = document.getElementById('teacher-avatar-display');

        if (nameEl) nameEl.textContent = teacher.name;
        if (roleEl) roleEl.textContent = `${teacher.departmentCode || 'CSE'} Department`;
        if (avatarEl) {
            const initials = teacher.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
            avatarEl.textContent = initials;
        }

        return teacher;
    };

    // Login function
    const login = async (email, password) => {
        return await API.post('/api/auth/login', { email, password });
    };

    // Logout function
    const logout = async () => {
        try {
            await API.post('/api/auth/logout');
        } catch (e) {
            // ignore
        } finally {
            window.location.href = '/teacher-login.html?logout=true';
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
        login,
        logout,
        bindLogoutButtons
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    Auth.bindLogoutButtons();
});
