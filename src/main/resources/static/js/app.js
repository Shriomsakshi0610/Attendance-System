/**
 * Renaissance - Global App Utilities
 */

const App = (() => {
    // Show a floating toast notification
    const showToast = (message, type = 'info', duration = 4000) => {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const icons = {
            success: '✓',
            danger: '✕',
            info: 'ℹ',
            warning: '⚠'
        };

        toast.innerHTML = `
            <span style="font-size:1.1rem; font-weight:bold;">${icons[type] || 'ℹ'}</span>
            <div style="flex:1;">${escapeHtml(message)}</div>
        `;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(15px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, duration);
    };

    // Modal Helper
    const openModal = (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
        }
    };

    const closeModal = (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
        }
    };

    // Date and time formatter
    const formatDateTime = (dateStr) => {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const escapeHtml = (str) => {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    };

    // Setup sidebar toggle for mobile
    const initSidebarToggle = () => {
        const btn = document.getElementById('sidebar-toggle');
        const sidebar = document.querySelector('.sidebar');
        if (btn && sidebar) {
            btn.addEventListener('click', () => {
                sidebar.classList.toggle('open');
            });
            document.addEventListener('click', (e) => {
                if (!sidebar.contains(e.target) && !btn.contains(e.target)) {
                    sidebar.classList.remove('open');
                }
            });
        }
    };

    return {
        showToast,
        openModal,
        closeModal,
        formatDateTime,
        escapeHtml,
        initSidebarToggle
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    App.initSidebarToggle();
});
