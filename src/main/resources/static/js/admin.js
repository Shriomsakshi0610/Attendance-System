/**
 * Renaissance - Admin Portal JavaScript
 * Comprehensive academic and administrative management
 */

const AdminApp = (() => {
    let currentAdmin = null;
    let departmentsCache = [];
    let classesCache = [];
    let teachersCache = [];
    let subjectsCache = [];
    let searchDebounceTimer = null;
    let pollingTimer = null;

    // ----------------------------------------------------
    // INITIALIZATION & AUTH
    // ----------------------------------------------------

    async function init() {
        try {
            // Require Admin authentication
            currentAdmin = await Auth.requireAdminAuth();
            if (!currentAdmin) return;

            document.getElementById('admin-name-display').textContent = currentAdmin.name || 'Administrator';
            document.getElementById('admin-email-display').textContent = currentAdmin.email || 'admin@renaissance.edu';
            if (currentAdmin.name) {
                document.getElementById('admin-avatar-display').textContent = currentAdmin.name.charAt(0).toUpperCase();
            }

            setupNav();
            setupLogout();
            setupMobileToggle();

            // Initial load of reference metadata
            await Promise.all([
                loadDepartments(),
                loadClasses(),
                loadTeachers(),
                loadSubjects()
            ]);

            // Initial load of active view data
            loadOverviewStats();
            loadStudents();
            loadTimetable();
            loadActiveSessions();

            // Auto-refresh active sessions every 15s
            pollingTimer = setInterval(() => {
                const activeTab = document.querySelector('.sidebar-nav .nav-link.active')?.dataset?.tab;
                if (activeTab === 'tab-overview' || activeTab === 'tab-sessions') {
                    loadActiveSessions();
                    loadOverviewStats();
                }
            }, 15000);

        } catch (err) {
            console.error('Initialization error:', err);
            showToast('Failed to initialize Admin Portal. Please log in again.', 'error');
        }
    }

    function setupNav() {
        document.querySelectorAll('.sidebar-nav .nav-link[data-tab]').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const tabId = link.getAttribute('data-tab');
                switchTab(tabId);
            });
        });
    }

    function switchTab(tabId) {
        document.querySelectorAll('.sidebar-nav .nav-link[data-tab]').forEach(l => l.classList.remove('active'));
        const activeLink = document.querySelector(`.sidebar-nav .nav-link[data-tab="${tabId}"]`);
        if (activeLink) activeLink.classList.add('active');

        document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
        const targetPane = document.getElementById(tabId);
        if (targetPane) targetPane.classList.add('active');

        // Update title
        const titles = {
            'tab-overview': 'Institution Management Dashboard',
            'tab-departments': 'Academic Departments Management',
            'tab-classes': 'Classes & Sections Management',
            'tab-subjects': 'Subjects & Courses Directory',
            'tab-teachers': 'Faculty & Teacher Directory',
            'tab-students': 'Student Roster Management',
            'tab-timetable': 'Class Timetable & Schedule Management',
            'tab-sessions': 'Live Campus Attendance Sessions'
        };
        document.getElementById('current-page-title').textContent = titles[tabId] || 'Admin Portal';

        // Refresh tab-specific data
        if (tabId === 'tab-overview') loadOverviewStats();
        if (tabId === 'tab-departments') loadDepartments();
        if (tabId === 'tab-classes') loadClasses();
        if (tabId === 'tab-subjects') loadSubjects();
        if (tabId === 'tab-teachers') loadTeachers();
        if (tabId === 'tab-students') loadStudents();
        if (tabId === 'tab-timetable') loadTimetable();
        if (tabId === 'tab-sessions') loadActiveSessions();

        // Close mobile sidebar if open
        const sidebar = document.getElementById('admin-sidebar');
        if (sidebar) sidebar.classList.remove('open');
    }

    function setupLogout() {
        const btn = document.getElementById('btn-admin-logout');
        if (btn) {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                if (confirm('Are you sure you want to log out from the Admin Portal?')) {
                    clearInterval(pollingTimer);
                    await Auth.logout();
                }
            });
        }
    }

    function setupMobileToggle() {
        const toggle = document.getElementById('sidebar-toggle');
        const sidebar = document.getElementById('admin-sidebar');
        if (toggle && sidebar) {
            toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
        }
    }

    // ----------------------------------------------------
    // TOAST NOTIFICATIONS
    // ----------------------------------------------------

    function showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) {
            alert(message);
            return;
        }
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<span>${escapeHtml(message)}</span>`;
        container.appendChild(toast);
        setTimeout(() => toast.remove(), 4000);
    }

    function escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // ----------------------------------------------------
    // MODAL HELPERS
    // ----------------------------------------------------

    function openModal(id) {
        const el = document.getElementById(id);
        if (el) el.classList.add('active');
    }

    function closeModal(id) {
        const el = document.getElementById(id);
        if (el) el.classList.remove('active');
    }

    function showConfirmModal(title, message, onConfirm) {
        document.getElementById('confirm-modal-title').textContent = title;
        document.getElementById('confirm-modal-msg').textContent = message;
        const btn = document.getElementById('confirm-modal-btn');
        btn.onclick = async () => {
            closeModal('modal-confirm');
            await onConfirm();
        };
        openModal('modal-confirm');
    }

    // ----------------------------------------------------
    // OVERVIEW & STATS
    // ----------------------------------------------------

    async function loadOverviewStats() {
        try {
            const res = await fetch('/api/admin/stats');
            const data = await res.json();
            if (data.success && data.data) {
                const s = data.data;
                document.getElementById('stat-departments').textContent = s.totalDepartments ?? 0;
                document.getElementById('stat-classes').textContent = s.totalClasses ?? 0;
                document.getElementById('stat-subjects').textContent = s.totalSubjects ?? 0;
                document.getElementById('stat-teachers').textContent = s.totalTeachers ?? 0;
                document.getElementById('stat-students').textContent = s.totalStudents ?? 0;
                document.getElementById('stat-active-sessions').textContent = s.activeSessionsCount ?? 0;

                const countBadge = document.getElementById('sidebar-active-sessions-count');
                if (s.activeSessionsCount > 0) {
                    countBadge.style.display = 'inline-block';
                    countBadge.textContent = s.activeSessionsCount;
                } else {
                    countBadge.style.display = 'none';
                }

                renderOverviewLiveSessions(s.activeSessions || []);
            }
        } catch (err) {
            console.error('Error loading stats:', err);
        }
    }

    function renderOverviewLiveSessions(sessions) {
        const tbody = document.getElementById('overview-live-sessions-tbody');
        if (!tbody) return;

        if (sessions.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2rem;">No active attendance sessions currently running on campus.</td></tr>`;
            return;
        }

        tbody.innerHTML = sessions.map(s => `
            <tr>
                <td><strong style="color: var(--accent-600); font-family: monospace; font-size: 1rem;">${s.sessionCode}</strong></td>
                <td>${escapeHtml(s.className || 'General')}</td>
                <td><strong>${escapeHtml(s.subjectName || 'Subject')}</strong> (${s.subjectCode || ''})</td>
                <td>${escapeHtml(s.teacherName || 'Faculty')}</td>
                <td>${formatTime(s.startTime)}</td>
                <td>
                    <span class="badge" style="background:#ecfdf5; color:#065f46; font-weight:700;">
                        ${s.presentCount ?? 0} / ${s.totalEnrolled ?? 0} present
                    </span>
                </td>
                <td>
                    <button class="btn btn-danger btn-sm" onclick="AdminApp.terminateSession('${s.id}', '${s.sessionCode}')">
                        ⛔ Terminate
                    </button>
                </td>
            </tr>
        `).join('');
    }

    // ----------------------------------------------------
    // DEPARTMENTS
    // ----------------------------------------------------

    async function loadDepartments() {
        try {
            const res = await fetch('/api/admin/departments');
            const data = await res.json();
            if (data.success && data.data) {
                departmentsCache = data.data;
                renderDepartmentsTable(departmentsCache);
                populateDepartmentDropdowns();
            }
        } catch (err) {
            console.error('Error loading departments:', err);
        }
    }

    function renderDepartmentsTable(list) {
        const tbody = document.getElementById('departments-tbody');
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; padding: 2rem; color: var(--text-muted);">No departments configured.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(d => {
            const isActive = d.status === 'ACTIVE';
            return `
                <tr>
                    <td><strong style="color: var(--accent-600);">${escapeHtml(d.code)}</strong></td>
                    <td><strong>${escapeHtml(d.name)}</strong></td>
                    <td style="color: var(--text-secondary); max-width: 300px;">${escapeHtml(d.description || '—')}</td>
                    <td>
                        <span class="status-pill ${isActive ? 'status-active' : 'status-inactive'}">
                            ${isActive ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 0.5rem;">
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.openDepartmentModal('${d.id}')">Edit</button>
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.toggleDepartmentStatus('${d.id}')">
                                ${isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="AdminApp.deleteDepartment('${d.id}', '${escapeHtml(d.code)}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function populateDepartmentDropdowns() {
        const dropdownIds = [
            'filter-class-department',
            'class-dept-select',
            'filter-subject-dept',
            'subject-dept-select',
            'teacher-dept-select',
            'filter-student-dept',
            'student-dept-select'
        ];

        dropdownIds.forEach(id => {
            const select = document.getElementById(id);
            if (!select) return;
            const currentVal = select.value;
            const isFilter = id.startsWith('filter-');
            let html = isFilter ? '<option value="">All Departments</option>' : '<option value="">Select Department...</option>';
            departmentsCache.forEach(d => {
                html += `<option value="${d.id}">${escapeHtml(d.code)} – ${escapeHtml(d.name)}</option>`;
            });
            select.innerHTML = html;
            if (currentVal) select.value = currentVal;
        });
    }

    function openDepartmentModal(deptId = null) {
        document.getElementById('form-department').reset();
        document.getElementById('dept-id').value = '';
        if (deptId) {
            const d = departmentsCache.find(item => item.id === deptId);
            if (d) {
                document.getElementById('dept-id').value = d.id;
                document.getElementById('dept-code').value = d.code;
                document.getElementById('dept-name').value = d.name;
                document.getElementById('dept-desc').value = d.description || '';
                document.getElementById('modal-dept-title').textContent = 'Edit Department';
            }
        } else {
            document.getElementById('modal-dept-title').textContent = 'Add Department';
        }
        openModal('modal-department');
    }

    async function saveDepartment(e) {
        e.preventDefault();
        const id = document.getElementById('dept-id').value;
        const payload = {
            code: document.getElementById('dept-code').value.trim().toUpperCase(),
            name: document.getElementById('dept-name').value.trim(),
            description: document.getElementById('dept-desc').value.trim()
        };

        try {
            const method = id ? 'PUT' : 'POST';
            const url = id ? `/api/admin/departments/${id}` : '/api/admin/departments';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                showToast(data.message || 'Department saved successfully', 'success');
                closeModal('modal-department');
                await loadDepartments();
                loadOverviewStats();
            } else {
                showToast(data.message || 'Failed to save department', 'error');
            }
        } catch (err) {
            console.error(err);
            showToast('Error saving department', 'error');
        }
    }

    async function toggleDepartmentStatus(id) {
        try {
            const res = await fetch(`/api/admin/departments/${id}/toggle-status`, { method: 'PATCH' });
            const data = await res.json();
            if (data.success) {
                showToast('Department status toggled', 'success');
                await loadDepartments();
            } else {
                showToast(data.message || 'Failed to toggle status', 'error');
            }
        } catch (err) {
            showToast('Network error toggling status', 'error');
        }
    }

    function deleteDepartment(id, code) {
        showConfirmModal(
            'Delete Department',
            `Are you sure you want to delete department ${code}? This will fail if classes, subjects, faculty, or students are assigned to it.`,
            async () => {
                try {
                    const res = await fetch(`/api/admin/departments/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (data.success) {
                        showToast('Department deleted successfully', 'success');
                        await loadDepartments();
                        loadOverviewStats();
                    } else {
                        showToast(data.message || 'Cannot delete department: dependent entities exist', 'error');
                    }
                } catch (err) {
                    showToast('Network error deleting department', 'error');
                }
            }
        );
    }

    // ----------------------------------------------------
    // CLASSES & SECTIONS
    // ----------------------------------------------------

    async function loadClasses() {
        try {
            const filterDept = document.getElementById('filter-class-department')?.value || '';
            const url = filterDept ? `/api/admin/classes?departmentId=${filterDept}` : '/api/admin/classes';
            const res = await fetch(url);
            const data = await res.json();
            if (data.success && data.data) {
                classesCache = data.data;
                renderClassesTable(classesCache);
                populateClassDropdowns();
            }
        } catch (err) {
            console.error('Error loading classes:', err);
        }
    }

    function renderClassesTable(list) {
        const tbody = document.getElementById('classes-tbody');
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted);">No classes configured.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(c => {
            const isActive = c.status === 'ACTIVE';
            return `
                <tr>
                    <td><strong>${escapeHtml(c.departmentCode || '—')}</strong></td>
                    <td><strong>${escapeHtml(c.className || `Sem ${c.semester}`)}</strong></td>
                    <td><span class="badge" style="background:#ede9fe; color:#581c87; font-weight:700;">Section ${escapeHtml(c.section)}</span></td>
                    <td>${escapeHtml(c.academicYear || c.batchYear || '2026-2027')}</td>
                    <td>${escapeHtml(c.classTeacherName || 'Unassigned')}</td>
                    <td>
                        <span class="status-pill ${isActive ? 'status-active' : 'status-inactive'}">
                            ${isActive ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 0.5rem;">
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.viewClassRoster('${c.id}', '${escapeHtml(c.displayName)}')">👥 Roster</button>
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.openClassModal('${c.id}')">Edit</button>
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.toggleClassStatus('${c.id}')">
                                ${isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="AdminApp.deleteClass('${c.id}', '${escapeHtml(c.displayName)}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function populateClassDropdowns() {
        const dropdownIds = [
            'filter-student-class',
            'student-class-select',
            'filter-timetable-class',
            'tt-class-select',
            'subject-class-select'
        ];

        dropdownIds.forEach(id => {
            const select = document.getElementById(id);
            if (!select) return;
            const currentVal = select.value;
            const isFilter = id.startsWith('filter-');
            let html = isFilter ? '<option value="">All Classes & Sections</option>' : '<option value="">Select Class / Section...</option>';
            classesCache.forEach(c => {
                html += `<option value="${c.id}">${escapeHtml(c.displayName)}</option>`;
            });
            select.innerHTML = html;
            if (currentVal) select.value = currentVal;
        });

        // Populate Class Teacher dropdown in Class modal
        const ctSelect = document.getElementById('class-teacher-select');
        if (ctSelect) {
            let html = '<option value="">None / Unassigned</option>';
            teachersCache.forEach(t => {
                html += `<option value="${t.id}">${escapeHtml(t.name)} (${escapeHtml(t.departmentCode || 'Faculty')})</option>`;
            });
            ctSelect.innerHTML = html;
        }
    }

    function openClassModal(classId = null) {
        document.getElementById('form-class').reset();
        document.getElementById('class-id').value = '';
        populateClassDropdowns();

        if (classId) {
            const c = classesCache.find(item => item.id === classId);
            if (c) {
                document.getElementById('class-id').value = c.id;
                document.getElementById('class-dept-select').value = c.departmentId || '';
                document.getElementById('class-semester').value = c.semester || '5';
                document.getElementById('class-section').value = c.section || 'A';
                document.getElementById('class-year').value = c.academicYear || c.batchYear || '2026-2027';
                document.getElementById('class-name-custom').value = c.className || '';
                document.getElementById('class-teacher-select').value = c.classTeacherId || '';
                document.getElementById('modal-class-title').textContent = 'Edit Class & Section';
            }
        } else {
            document.getElementById('modal-class-title').textContent = 'Add Class & Section';
        }
        openModal('modal-class');
    }

    async function saveClass(e) {
        e.preventDefault();
        const id = document.getElementById('class-id').value;
        const payload = {
            departmentId: document.getElementById('class-dept-select').value,
            semester: parseInt(document.getElementById('class-semester').value, 10),
            section: document.getElementById('class-section').value.trim().toUpperCase(),
            academicYear: document.getElementById('class-year').value.trim(),
            className: document.getElementById('class-name-custom').value.trim(),
            classTeacherId: document.getElementById('class-teacher-select').value || null
        };

        try {
            const method = id ? 'PUT' : 'POST';
            const url = id ? `/api/admin/classes/${id}` : '/api/admin/classes';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                showToast(data.message || 'Class saved successfully', 'success');
                closeModal('modal-class');
                await loadClasses();
                loadOverviewStats();
            } else {
                showToast(data.message || 'Failed to save class', 'error');
            }
        } catch (err) {
            showToast('Error saving class', 'error');
        }
    }

    async function toggleClassStatus(id) {
        try {
            const res = await fetch(`/api/admin/classes/${id}/toggle-status`, { method: 'PATCH' });
            const data = await res.json();
            if (data.success) {
                showToast('Class status toggled', 'success');
                await loadClasses();
            } else {
                showToast(data.message || 'Failed to toggle status', 'error');
            }
        } catch (err) {
            showToast('Network error toggling status', 'error');
        }
    }

    function deleteClass(id, name) {
        showConfirmModal(
            'Delete Class',
            `Are you sure you want to delete ${name}? Deletion will be rejected if students, timetable schedules, or attendance sessions exist for this class.`,
            async () => {
                try {
                    const res = await fetch(`/api/admin/classes/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (data.success) {
                        showToast('Class deleted successfully', 'success');
                        await loadClasses();
                        loadOverviewStats();
                    } else {
                        showToast(data.message || 'Cannot delete class: dependencies exist', 'error');
                    }
                } catch (err) {
                    showToast('Network error deleting class', 'error');
                }
            }
        );
    }

    async function viewClassRoster(classId, className) {
        document.getElementById('roster-class-title').textContent = `Enrolled Roster: ${className}`;
        const tbody = document.getElementById('class-roster-tbody');
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 2rem;">Loading roster...</td></tr>`;
        openModal('modal-class-roster');

        try {
            const res = await fetch(`/api/admin/classes/${classId}/students`);
            const data = await res.json();
            if (data.success && data.data) {
                const students = data.data;
                document.getElementById('roster-class-sub').textContent = `${students.length} student(s) enrolled in this section`;
                if (students.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 2rem; color: var(--text-muted);">No students enrolled in this section yet.</td></tr>`;
                    return;
                }
                tbody.innerHTML = students.map((s, idx) => `
                    <tr>
                        <td>${idx + 1}</td>
                        <td><strong style="color: var(--accent-600);">${escapeHtml(s.usn)}</strong></td>
                        <td><strong>${escapeHtml(s.name)}</strong></td>
                        <td>${escapeHtml(s.email || '—')}</td>
                        <td>
                            <span class="status-pill ${s.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">
                                ${s.status || 'ACTIVE'}
                            </span>
                        </td>
                    </tr>
                `).join('');
            }
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: red;">Failed to load roster.</td></tr>`;
        }
    }

    // ----------------------------------------------------
    // SUBJECTS
    // ----------------------------------------------------

    async function loadSubjects() {
        try {
            const dept = document.getElementById('filter-subject-dept')?.value || '';
            const sem = document.getElementById('filter-subject-sem')?.value || '';
            let url = '/api/admin/subjects?';
            if (dept) url += `departmentId=${dept}&`;
            if (sem) url += `semester=${sem}&`;

            const res = await fetch(url);
            const data = await res.json();
            if (data.success && data.data) {
                subjectsCache = data.data;
                renderSubjectsTable(subjectsCache);
                populateSubjectDropdowns();
            }
        } catch (err) {
            console.error('Error loading subjects:', err);
        }
    }

    function renderSubjectsTable(list) {
        const tbody = document.getElementById('subjects-tbody');
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 2rem; color: var(--text-muted);">No subjects found matching criteria.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(s => {
            const isActive = s.status === 'ACTIVE';
            return `
                <tr>
                    <td><strong style="color: var(--accent-600); font-family: monospace;">${escapeHtml(s.code)}</strong></td>
                    <td><strong>${escapeHtml(s.name)}</strong></td>
                    <td>${escapeHtml(s.departmentCode || '—')}</td>
                    <td>Sem ${s.semester}</td>
                    <td>${escapeHtml(s.className || 'All Sections')}</td>
                    <td>${escapeHtml(s.assignedTeacherName || 'Unassigned')}</td>
                    <td>${s.weeklyHours ?? 4} hrs/wk</td>
                    <td>
                        <span class="status-pill ${isActive ? 'status-active' : 'status-inactive'}">
                            ${isActive ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 0.5rem;">
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.openSubjectModal('${s.id}')">Edit</button>
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.toggleSubjectStatus('${s.id}')">
                                ${isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="AdminApp.deleteSubject('${s.id}', '${escapeHtml(s.code)}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function populateSubjectDropdowns() {
        const select = document.getElementById('tt-subject-select');
        if (select) {
            let html = '<option value="">Select Subject...</option>';
            subjectsCache.forEach(s => {
                html += `<option value="${s.id}">${escapeHtml(s.code)} – ${escapeHtml(s.name)} (Sem ${s.semester})</option>`;
            });
            select.innerHTML = html;
        }
    }

    function openSubjectModal(subjectId = null) {
        document.getElementById('form-subject').reset();
        document.getElementById('subject-id').value = '';

        // Populate teachers in subject modal
        const tSelect = document.getElementById('subject-teacher-select');
        if (tSelect) {
            let html = '<option value="">None / Unassigned</option>';
            teachersCache.forEach(t => {
                html += `<option value="${t.id}">${escapeHtml(t.name)} (${escapeHtml(t.departmentCode || 'Faculty')})</option>`;
            });
            tSelect.innerHTML = html;
        }

        if (subjectId) {
            const s = subjectsCache.find(item => item.id === subjectId);
            if (s) {
                document.getElementById('subject-id').value = s.id;
                document.getElementById('subject-code').value = s.code;
                document.getElementById('subject-name').value = s.name;
                document.getElementById('subject-dept-select').value = s.departmentId || '';
                document.getElementById('subject-semester').value = s.semester || '5';
                document.getElementById('subject-class-select').value = s.classId || '';
                document.getElementById('subject-teacher-select').value = s.assignedTeacherId || '';
                document.getElementById('subject-weekly-hours').value = s.weeklyHours || 4;
                document.getElementById('modal-subject-title').textContent = 'Edit Subject';
            }
        } else {
            document.getElementById('modal-subject-title').textContent = 'Add Course / Subject';
        }
        openModal('modal-subject');
    }

    async function saveSubject(e) {
        e.preventDefault();
        const id = document.getElementById('subject-id').value;
        const payload = {
            code: document.getElementById('subject-code').value.trim().toUpperCase(),
            name: document.getElementById('subject-name').value.trim(),
            departmentId: document.getElementById('subject-dept-select').value,
            semester: parseInt(document.getElementById('subject-semester').value, 10),
            classId: document.getElementById('subject-class-select').value || null,
            assignedTeacherId: document.getElementById('subject-teacher-select').value || null,
            weeklyHours: parseInt(document.getElementById('subject-weekly-hours').value || 4, 10)
        };

        try {
            const method = id ? 'PUT' : 'POST';
            const url = id ? `/api/admin/subjects/${id}` : '/api/admin/subjects';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                showToast(data.message || 'Subject saved successfully', 'success');
                closeModal('modal-subject');
                await loadSubjects();
                loadOverviewStats();
            } else {
                showToast(data.message || 'Failed to save subject', 'error');
            }
        } catch (err) {
            showToast('Error saving subject', 'error');
        }
    }

    async function toggleSubjectStatus(id) {
        try {
            const res = await fetch(`/api/admin/subjects/${id}/toggle-status`, { method: 'PATCH' });
            const data = await res.json();
            if (data.success) {
                showToast('Subject status toggled', 'success');
                await loadSubjects();
            } else {
                showToast(data.message || 'Failed to toggle status', 'error');
            }
        } catch (err) {
            showToast('Network error toggling status', 'error');
        }
    }

    function deleteSubject(id, code) {
        showConfirmModal(
            'Delete Subject',
            `Are you sure you want to delete subject ${code}? Deletion is blocked if timetable schedules, active attendance sessions, or student enrollments exist.`,
            async () => {
                try {
                    const res = await fetch(`/api/admin/subjects/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (data.success) {
                        showToast('Subject deleted successfully', 'success');
                        await loadSubjects();
                        loadOverviewStats();
                    } else {
                        showToast(data.message || 'Cannot delete subject: dependencies exist', 'error');
                    }
                } catch (err) {
                    showToast('Network error deleting subject', 'error');
                }
            }
        );
    }

    function onSubjectDeptChange() {
        const deptId = document.getElementById('subject-dept-select').value;
        const classSelect = document.getElementById('subject-class-select');
        let html = '<option value="">All Sections / General</option>';
        classesCache.filter(c => !deptId || c.departmentId === deptId).forEach(c => {
            html += `<option value="${c.id}">${escapeHtml(c.displayName)}</option>`;
        });
        classSelect.innerHTML = html;
    }

    // ----------------------------------------------------
    // FACULTY / TEACHERS
    // ----------------------------------------------------

    async function loadTeachers() {
        try {
            const res = await fetch('/api/admin/teachers');
            const data = await res.json();
            if (data.success && data.data) {
                teachersCache = data.data;
                renderTeachersTable(teachersCache);
                populateTeacherDropdowns();
            }
        } catch (err) {
            console.error('Error loading teachers:', err);
        }
    }

    function renderTeachersTable(list) {
        const tbody = document.getElementById('teachers-tbody');
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; padding: 2rem; color: var(--text-muted);">No faculty members registered.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(t => {
            const isActive = t.status === 'ACTIVE';
            const subjectNames = (t.assignedSubjectNames && t.assignedSubjectNames.length > 0)
                ? t.assignedSubjectNames.join(', ') : 'None assigned';
            return `
                <tr>
                    <td><strong>${escapeHtml(t.name)}</strong></td>
                    <td>${escapeHtml(t.email)}</td>
                    <td><strong style="color: var(--accent-600);">${escapeHtml(t.employeeId)}</strong></td>
                    <td>${escapeHtml(t.departmentName || t.departmentCode || '—')}</td>
                    <td>${escapeHtml(t.phone || '—')}</td>
                    <td style="max-width: 200px; font-size: 0.825rem; color: var(--text-secondary);">${escapeHtml(subjectNames)}</td>
                    <td>
                        <span class="status-pill ${isActive ? 'status-active' : 'status-inactive'}">
                            ${isActive ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.openTeacherModal('${t.id}')">Edit</button>
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.openPasswordResetModal('${t.id}', '${escapeHtml(t.name)}')">🔑 Reset</button>
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.toggleTeacherStatus('${t.id}')">
                                ${isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="AdminApp.deleteTeacher('${t.id}', '${escapeHtml(t.name)}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function populateTeacherDropdowns() {
        const dropdownIds = ['filter-timetable-teacher', 'tt-teacher-select', 'class-teacher-select', 'subject-teacher-select'];
        dropdownIds.forEach(id => {
            const select = document.getElementById(id);
            if (!select) return;
            const currentVal = select.value;
            const isFilter = id.startsWith('filter-');
            let html = isFilter ? '<option value="">All Faculty Members</option>' : '<option value="">Select Faculty...</option>';
            teachersCache.forEach(t => {
                html += `<option value="${t.id}">${escapeHtml(t.name)} (${escapeHtml(t.departmentCode || 'Faculty')})</option>`;
            });
            select.innerHTML = html;
            if (currentVal) select.value = currentVal;
        });
    }

    function openTeacherModal(teacherId = null) {
        document.getElementById('form-teacher').reset();
        document.getElementById('teacher-id').value = '';

        // Populate multiple select options for Classes and Subjects
        const classesSelect = document.getElementById('teacher-classes-select');
        classesSelect.innerHTML = classesCache.map(c => `<option value="${c.id}">${escapeHtml(c.displayName)}</option>`).join('');

        const subjectsSelect = document.getElementById('teacher-subjects-select');
        subjectsSelect.innerHTML = subjectsCache.map(s => `<option value="${s.id}">${escapeHtml(s.code)} - ${escapeHtml(s.name)}</option>`).join('');

        const pwdGroup = document.getElementById('teacher-password-group');

        if (teacherId) {
            const t = teachersCache.find(item => item.id === teacherId);
            if (t) {
                document.getElementById('teacher-id').value = t.id;
                document.getElementById('teacher-name').value = t.name;
                document.getElementById('teacher-empid').value = t.employeeId;
                document.getElementById('teacher-email').value = t.email;
                document.getElementById('teacher-dept-select').value = t.departmentId || '';
                document.getElementById('teacher-phone').value = t.phone || '';
                if (pwdGroup) pwdGroup.style.display = 'none';

                if (t.assignedClassIds) {
                    Array.from(classesSelect.options).forEach(opt => {
                        opt.selected = t.assignedClassIds.includes(opt.value);
                    });
                }
                if (t.assignedSubjectIds) {
                    Array.from(subjectsSelect.options).forEach(opt => {
                        opt.selected = t.assignedSubjectIds.includes(opt.value);
                    });
                }
                document.getElementById('modal-teacher-title').textContent = 'Edit Faculty Member';
            }
        } else {
            if (pwdGroup) pwdGroup.style.display = 'block';
            document.getElementById('modal-teacher-title').textContent = 'Register Faculty Member';
        }
        openModal('modal-teacher');
    }

    async function saveTeacher(e) {
        e.preventDefault();
        const id = document.getElementById('teacher-id').value;

        const selectedClassIds = Array.from(document.getElementById('teacher-classes-select').selectedOptions).map(o => o.value);
        const selectedSubjectIds = Array.from(document.getElementById('teacher-subjects-select').selectedOptions).map(o => o.value);

        const payload = {
            name: document.getElementById('teacher-name').value.trim(),
            employeeId: document.getElementById('teacher-empid').value.trim().toUpperCase(),
            email: document.getElementById('teacher-email').value.trim().toLowerCase(),
            departmentId: document.getElementById('teacher-dept-select').value,
            phone: document.getElementById('teacher-phone').value.trim(),
            initialPassword: document.getElementById('teacher-password')?.value || null,
            assignedClassIds: selectedClassIds,
            assignedSubjectIds: selectedSubjectIds
        };

        try {
            const method = id ? 'PUT' : 'POST';
            const url = id ? `/api/admin/teachers/${id}` : '/api/admin/teachers';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                showToast(data.message || 'Teacher saved successfully', 'success');
                closeModal('modal-teacher');
                await loadTeachers();
                loadOverviewStats();
            } else {
                showToast(data.message || 'Failed to save teacher', 'error');
            }
        } catch (err) {
            showToast('Error saving teacher', 'error');
        }
    }

    async function toggleTeacherStatus(id) {
        try {
            const res = await fetch(`/api/admin/teachers/${id}/toggle-status`, { method: 'PATCH' });
            const data = await res.json();
            if (data.success) {
                showToast('Teacher status updated', 'success');
                await loadTeachers();
            } else {
                showToast(data.message || 'Failed to toggle status', 'error');
            }
        } catch (err) {
            showToast('Network error toggling status', 'error');
        }
    }

    function openPasswordResetModal(teacherId, teacherName) {
        document.getElementById('form-reset-password').reset();
        document.getElementById('reset-teacher-id').value = teacherId;
        document.getElementById('reset-teacher-name').textContent = teacherName;
        openModal('modal-reset-password');
    }

    async function submitPasswordReset(e) {
        e.preventDefault();
        const id = document.getElementById('reset-teacher-id').value;
        const newPwd = document.getElementById('reset-new-password').value;
        const confirmPwd = document.getElementById('reset-confirm-password').value;

        if (newPwd !== confirmPwd) {
            showToast('Passwords do not match', 'error');
            return;
        }

        try {
            const res = await fetch(`/api/admin/teachers/${id}/reset-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ newPassword: newPwd })
            });
            const data = await res.json();
            if (data.success) {
                showToast('Password reset successfully', 'success');
                closeModal('modal-reset-password');
            } else {
                showToast(data.message || 'Failed to reset password', 'error');
            }
        } catch (err) {
            showToast('Network error resetting password', 'error');
        }
    }

    function deleteTeacher(id, name) {
        showConfirmModal(
            'Delete Faculty Member',
            `Are you sure you want to delete ${name}? Deletion will be rejected if historical sessions, scheduled classes, or mentorship duties exist. Deactivation is recommended instead.`,
            async () => {
                try {
                    const res = await fetch(`/api/admin/teachers/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (data.success) {
                        showToast('Teacher deleted successfully', 'success');
                        await loadTeachers();
                        loadOverviewStats();
                    } else {
                        showToast(data.message || 'Cannot delete teacher: associations exist', 'error');
                    }
                } catch (err) {
                    showToast('Network error deleting teacher', 'error');
                }
            }
        );
    }

    // ----------------------------------------------------
    // STUDENTS
    // ----------------------------------------------------

    async function loadStudents() {
        const dept = document.getElementById('filter-student-dept')?.value || '';
        const classId = document.getElementById('filter-student-class')?.value || '';
        const keyword = document.getElementById('search-student-keyword')?.value?.trim() || '';

        let url = '/api/admin/students?';
        if (dept) url += `departmentId=${dept}&`;
        if (classId) url += `classId=${classId}&`;
        if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;

        try {
            const res = await fetch(url);
            const data = await res.json();
            if (data.success && data.data) {
                renderStudentsTable(data.data);
            }
        } catch (err) {
            console.error('Error loading students:', err);
        }
    }

    function debouncedLoadStudents() {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            loadStudents();
        }, 300);
    }

    function onStudentDeptFilterChange() {
        const deptId = document.getElementById('filter-student-dept').value;
        const classSelect = document.getElementById('filter-student-class');
        let html = '<option value="">All Classes & Sections</option>';
        classesCache.filter(c => !deptId || c.departmentId === deptId).forEach(c => {
            html += `<option value="${c.id}">${escapeHtml(c.displayName)}</option>`;
        });
        classSelect.innerHTML = html;
        loadStudents();
    }

    function renderStudentsTable(list) {
        const tbody = document.getElementById('students-tbody');
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted);">No student records found.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(s => {
            const isActive = s.status === 'ACTIVE';
            return `
                <tr>
                    <td><strong style="color: var(--accent-600); font-family: monospace;">${escapeHtml(s.usn)}</strong></td>
                    <td><strong>${escapeHtml(s.name)}</strong></td>
                    <td>${escapeHtml(s.email || '—')}</td>
                    <td>${escapeHtml(s.departmentCode || '—')}</td>
                    <td>${escapeHtml(s.className || `Sem ${s.semester} (${s.section})`)}</td>
                    <td>
                        <span class="status-pill ${isActive ? 'status-active' : 'status-inactive'}">
                            ${isActive ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 0.4rem;">
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.openStudentModal('${s.id}')">Edit</button>
                            <button class="btn btn-outline btn-sm" onclick="AdminApp.toggleStudentStatus('${s.id}')">
                                ${isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="AdminApp.deleteStudent('${s.id}', '${escapeHtml(s.usn)}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function openStudentModal(studentId = null) {
        document.getElementById('form-student').reset();
        document.getElementById('student-id').value = '';

        if (studentId) {
            fetch(`/api/admin/students/${studentId}`)
                .then(r => r.json())
                .then(data => {
                    if (data.success && data.data) {
                        const s = data.data;
                        document.getElementById('student-id').value = s.id;
                        document.getElementById('student-usn').value = s.usn;
                        document.getElementById('student-name').value = s.name;
                        document.getElementById('student-email').value = s.email || '';
                        document.getElementById('student-dept-select').value = s.departmentId || '';
                        onStudentModalDeptChange(s.classId);
                        document.getElementById('modal-student-title').textContent = 'Edit Student';
                        openModal('modal-student');
                    }
                });
        } else {
            document.getElementById('modal-student-title').textContent = 'Add Student';
            onStudentModalDeptChange();
            openModal('modal-student');
        }
    }

    function onStudentModalDeptChange(preselectClassId = null) {
        const deptId = document.getElementById('student-dept-select').value;
        const classSelect = document.getElementById('student-class-select');
        let html = '<option value="">Select Class / Section...</option>';
        classesCache.filter(c => !deptId || c.departmentId === deptId).forEach(c => {
            html += `<option value="${c.id}">${escapeHtml(c.displayName)}</option>`;
        });
        classSelect.innerHTML = html;
        if (preselectClassId) classSelect.value = preselectClassId;
    }

    async function saveStudent(e) {
        e.preventDefault();
        const id = document.getElementById('student-id').value;
        const payload = {
            usn: document.getElementById('student-usn').value.trim().toUpperCase(),
            name: document.getElementById('student-name').value.trim(),
            email: document.getElementById('student-email').value.trim() || null,
            departmentId: document.getElementById('student-dept-select').value,
            classId: document.getElementById('student-class-select').value
        };

        try {
            const method = id ? 'PUT' : 'POST';
            const url = id ? `/api/admin/students/${id}` : '/api/admin/students';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                showToast(data.message || 'Student saved successfully', 'success');
                closeModal('modal-student');
                await loadStudents();
                loadOverviewStats();
            } else {
                showToast(data.message || 'Failed to save student', 'error');
            }
        } catch (err) {
            showToast('Error saving student', 'error');
        }
    }

    async function toggleStudentStatus(id) {
        try {
            const res = await fetch(`/api/admin/students/${id}/toggle-status`, { method: 'PATCH' });
            const data = await res.json();
            if (data.success) {
                showToast('Student status toggled', 'success');
                await loadStudents();
            } else {
                showToast(data.message || 'Failed to toggle status', 'error');
            }
        } catch (err) {
            showToast('Network error toggling status', 'error');
        }
    }

    function deleteStudent(id, usn) {
        showConfirmModal(
            'Delete Student Record',
            `Are you sure you want to delete student ${usn}? If attendance history exists, deletion will be rejected to protect data integrity.`,
            async () => {
                try {
                    const res = await fetch(`/api/admin/students/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (data.success) {
                        showToast('Student record deleted successfully', 'success');
                        await loadStudents();
                        loadOverviewStats();
                    } else {
                        showToast(data.message || 'Cannot delete student: historical attendance records exist', 'error');
                    }
                } catch (err) {
                    showToast('Network error deleting student', 'error');
                }
            }
        );
    }

    function openCsvImportModal() {
        document.getElementById('form-csv-import').reset();
        document.getElementById('csv-import-results').style.display = 'none';
        openModal('modal-csv-import');
    }

    async function submitCsvImport(e) {
        e.preventDefault();
        const fileInput = document.getElementById('csv-file-input');
        if (!fileInput.files || fileInput.files.length === 0) {
            showToast('Please select a CSV file to upload', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('file', fileInput.files[0]);

        const uploadBtn = document.getElementById('btn-upload-csv');
        uploadBtn.disabled = true;
        uploadBtn.textContent = 'Importing...';

        try {
            const res = await fetch('/api/admin/students/import', {
                method: 'POST',
                body: formData
            });
            const data = await res.json();
            const resultsBox = document.getElementById('csv-import-results');
            resultsBox.style.display = 'block';

            if (data.success && data.data) {
                const r = data.data;
                resultsBox.innerHTML = `
                    <div style="color: #065f46; font-weight: 700; margin-bottom: 0.5rem;">
                        ✔ Import Complete: ${r.importedCount} student(s) imported.
                    </div>
                    <div style="color: #92400e;">Skipped: ${r.skippedCount}</div>
                    ${r.errors && r.errors.length > 0 ? `<div style="color: #991b1b; font-size: 0.8rem; margin-top: 0.5rem; max-height: 120px; overflow-y: auto;">${r.errors.join('<br>')}</div>` : ''}
                `;
                showToast(`Successfully imported ${r.importedCount} student(s)`, 'success');
                await loadStudents();
                loadOverviewStats();
            } else {
                resultsBox.innerHTML = `<div style="color: #991b1b; font-weight: 700;">❌ Import Failed: ${escapeHtml(data.message || 'Unknown error')}</div>`;
                showToast(data.message || 'Import failed', 'error');
            }
        } catch (err) {
            showToast('Network error during CSV upload', 'error');
        } finally {
            uploadBtn.disabled = false;
            uploadBtn.textContent = 'Upload & Import';
        }
    }

    // ----------------------------------------------------
    // TIMETABLE & SCHEDULING
    // ----------------------------------------------------

    async function loadTimetable() {
        const classId = document.getElementById('filter-timetable-class')?.value || '';
        const teacherId = document.getElementById('filter-timetable-teacher')?.value || '';

        let url = '/api/admin/timetable?';
        if (classId) url += `classId=${classId}&`;
        if (teacherId) url += `teacherId=${teacherId}&`;

        try {
            const res = await fetch(url);
            const data = await res.json();
            if (data.success && data.data) {
                const schedules = data.data;
                renderTimetableWeeklyGrid(schedules);
                renderTimetableList(schedules);
            }
        } catch (err) {
            console.error('Error loading timetable:', err);
        }
    }

    function renderTimetableWeeklyGrid(schedules) {
        const grid = document.getElementById('timetable-weekly-grid');
        if (!grid) return;

        const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
        const dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

        let headerHtml = `<div class="timetable-header-cell">Timeline</div>`;
        dayNames.forEach(d => {
            headerHtml += `<div class="timetable-header-cell">${d}</div>`;
        });

        // Group by day
        const grouped = {};
        days.forEach(d => grouped[d] = []);
        schedules.forEach(s => {
            const d = (s.dayName || s.dayOfWeek || '').toUpperCase();
            if (grouped[d]) grouped[d].push(s);
        });

        // Sort each day's schedules by startTime
        days.forEach(d => {
            grouped[d].sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''));
        });

        let bodyHtml = `<div class="timetable-time-cell">Classes & Labs</div>`;
        days.forEach(d => {
            const daySchedules = grouped[d];
            bodyHtml += `<div class="timetable-day-col">`;
            if (daySchedules.length === 0) {
                bodyHtml += `<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; padding: 1.5rem 0.5rem; background: #fcfcfd; border-radius: 6px; border: 1px dashed var(--surface-border);">No sessions</div>`;
            } else {
                daySchedules.forEach(s => {
                    bodyHtml += `
                        <div class="schedule-card">
                            <div class="schedule-card-actions">
                                <button onclick="AdminApp.deleteTimetableSchedule('${s.id}')" title="Delete slot" style="background:none; border:none; color:#ef4444; cursor:pointer; font-size:0.9rem;">&times;</button>
                            </div>
                            <div class="schedule-card-title">${escapeHtml(s.subjectCode || '')} – ${escapeHtml(s.subjectName || 'Subject')}</div>
                            <div class="schedule-card-time">⏰ ${formatTime(s.startTime)} – ${formatTime(s.endTime)}</div>
                            <div class="schedule-card-meta">
                                <span>🏫 ${escapeHtml(s.className || 'Class')}</span>
                                <span>👨‍🏫 ${escapeHtml(s.teacherName || 'Faculty')}</span>
                                ${s.roomNumber ? `<span>🚪 ${escapeHtml(s.roomNumber)}</span>` : ''}
                            </div>
                        </div>
                    `;
                });
            }
            bodyHtml += `</div>`;
        });

        grid.innerHTML = headerHtml + bodyHtml;
    }

    function renderTimetableList(list) {
        const tbody = document.getElementById('timetable-list-tbody');
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted);">No timetable schedules found.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(s => `
            <tr>
                <td><strong>${escapeHtml(s.dayName || s.dayOfWeek)}</strong></td>
                <td><strong style="color: var(--accent-600);">${formatTime(s.startTime)} – ${formatTime(s.endTime)}</strong></td>
                <td>${escapeHtml(s.className || '—')}</td>
                <td><strong>${escapeHtml(s.subjectCode || '')}</strong> – ${escapeHtml(s.subjectName || '')}</td>
                <td>${escapeHtml(s.teacherName || '—')}</td>
                <td>${escapeHtml(s.roomNumber || 'General')}</td>
                <td>
                    <button class="btn btn-danger btn-sm" onclick="AdminApp.deleteTimetableSchedule('${s.id}')">Delete</button>
                </td>
            </tr>
        `).join('');
    }

    function openTimetableModal(scheduleId = null) {
        document.getElementById('form-timetable').reset();
        document.getElementById('timetable-id').value = '';
        const pInput = document.getElementById('tt-period');
        if (pInput) pInput.value = '';
        document.getElementById('modal-timetable-conflict-box').style.display = 'none';

        populateClassDropdowns();
        populateSubjectDropdowns();
        populateTeacherDropdowns();

        openModal('modal-timetable');
    }

    function onTimetableClassChange() {
        const classId = document.getElementById('tt-class-select').value;
        const selectedClass = classesCache.find(c => c.id === classId);
        if (!selectedClass) return;

        // Auto-filter subjects matching this department and semester
        const sSelect = document.getElementById('tt-subject-select');
        let html = '<option value="">Select Subject...</option>';
        subjectsCache.filter(s => s.departmentId === selectedClass.departmentId && s.semester === selectedClass.semester).forEach(s => {
            html += `<option value="${s.id}">${escapeHtml(s.code)} – ${escapeHtml(s.name)}</option>`;
        });
        sSelect.innerHTML = html;
    }

    async function saveTimetableSchedule(e) {
        e.preventDefault();
        const conflictBox = document.getElementById('modal-timetable-conflict-box');
        conflictBox.style.display = 'none';

        const id = document.getElementById('timetable-id').value;
        const periodVal = document.getElementById('tt-period')?.value;
        const payload = {
            classId: document.getElementById('tt-class-select').value,
            subjectId: document.getElementById('tt-subject-select').value,
            teacherId: document.getElementById('tt-teacher-select').value,
            dayOfWeek: document.getElementById('tt-day').value,
            startTime: document.getElementById('tt-start-time').value,
            endTime: document.getElementById('tt-end-time').value,
            periodNumber: periodVal ? parseInt(periodVal, 10) : null,
            roomNumber: document.getElementById('tt-room').value.trim() || null
        };
        const method = id ? 'PUT' : 'POST';
        const url = id ? `/api/admin/timetable/${id}` : '/api/admin/timetable';

        try {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                showToast('Timetable slot saved successfully', 'success');
                closeModal('modal-timetable');
                await loadTimetable();
            } else {
                conflictBox.style.display = 'block';
                conflictBox.innerHTML = `<strong>⚠️ Scheduling Conflict:</strong> ${escapeHtml(data.message || 'Validation error')}`;
                showToast(data.message || 'Scheduling conflict detected', 'error');
            }
        } catch (err) {
            showToast('Network error saving timetable schedule', 'error');
        }
    }

    function deleteTimetableSchedule(id) {
        showConfirmModal(
            'Delete Timetable Slot',
            'Are you sure you want to remove this scheduled slot from the timetable?',
            async () => {
                try {
                    const res = await fetch(`/api/admin/timetable/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (data.success) {
                        showToast('Timetable schedule slot removed', 'success');
                        await loadTimetable();
                    } else {
                        showToast(data.message || 'Failed to remove schedule slot', 'error');
                    }
                } catch (err) {
                    showToast('Network error removing timetable slot', 'error');
                }
            }
        );
    }

    // ----------------------------------------------------
    // ACTIVE SESSIONS MONITOR & TERMINATION
    // ----------------------------------------------------

    async function loadActiveSessions() {
        try {
            const res = await fetch('/api/admin/sessions/active');
            const data = await res.json();
            if (data.success && data.data) {
                renderActiveSessionsTable(data.data);
                renderOverviewLiveSessions(data.data);
                const countBadge = document.getElementById('sidebar-active-sessions-count');
                if (data.data.length > 0) {
                    countBadge.style.display = 'inline-block';
                    countBadge.textContent = data.data.length;
                } else {
                    countBadge.style.display = 'none';
                }
            }
        } catch (err) {
            console.error('Error loading active sessions:', err);
        }
    }

    function renderActiveSessionsTable(sessions) {
        const tbody = document.getElementById('active-sessions-tbody');
        if (!tbody) return;

        if (sessions.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 2rem; color: var(--text-muted);">No active attendance sessions currently running on campus.</td></tr>`;
            return;
        }

        tbody.innerHTML = sessions.map(s => `
            <tr>
                <td><strong style="color: var(--accent-600); font-family: monospace; font-size: 1.05rem;">${s.sessionCode}</strong></td>
                <td><strong>${escapeHtml(s.className || 'General')}</strong></td>
                <td>${escapeHtml(s.subjectName || '')} (${escapeHtml(s.subjectCode || '')})</td>
                <td>${escapeHtml(s.teacherName || 'Faculty')}</td>
                <td>${formatTime(s.startTime)}</td>
                <td style="color: #f59e0b; font-weight: 600;">${formatTime(s.endTime)}</td>
                <td>
                    <span class="badge" style="background:#ecfdf5; color:#065f46; font-weight:700;">
                        ${s.presentCount ?? 0} / ${s.totalEnrolled ?? 0} present
                    </span>
                </td>
                <td style="font-family: monospace; font-size: 0.8rem; color: var(--text-muted);">
                    ${s.teacherLatitude?.toFixed(4)}, ${s.teacherLongitude?.toFixed(4)} (±${s.geofenceRadiusMeters ? Math.round(s.geofenceRadiusMeters) : 40}m)
                </td>
                <td>
                    <button class="btn btn-danger btn-sm" onclick="AdminApp.terminateSession('${s.id}', '${s.sessionCode}')">
                        ⛔ Terminate
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function terminateSession(sessionId, sessionCode) {
        showConfirmModal(
            'Terminate Attendance Session',
            `Are you sure you want to immediately terminate attendance session [${sessionCode}]? No further attendance will be accepted.`,
            async () => {
                try {
                    const res = await fetch(`/api/admin/sessions/${sessionId}/terminate`, { method: 'POST' });
                    const data = await res.json();
                    if (data.success) {
                        showToast(`Session ${sessionCode} terminated by administrator.`, 'success');
                        await loadActiveSessions();
                        loadOverviewStats();
                    } else {
                        showToast(data.message || 'Failed to terminate session', 'error');
                    }
                } catch (err) {
                    showToast('Network error terminating session', 'error');
                }
            }
        );
    }

    function refreshAllData() {
        loadDepartments();
        loadClasses();
        loadTeachers();
        loadSubjects();
        loadStudents();
        loadTimetable();
        loadActiveSessions();
        loadOverviewStats();
        showToast('All administrative caches refreshed', 'info');
    }

    function formatTime(isoOrTimeStr) {
        if (!isoOrTimeStr) return '—';
        if (isoOrTimeStr.length === 5 || isoOrTimeStr.length === 8) {
            return isoOrTimeStr.substring(0, 5);
        }
        try {
            const d = new Date(isoOrTimeStr);
            if (!isNaN(d.getTime())) {
                return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            }
        } catch (e) {
            // fallback
        }
        return isoOrTimeStr;
    }

    // Public API
    return {
        init,
        switchTab,
        refreshAllData,
        closeModal,
        openDepartmentModal,
        saveDepartment,
        toggleDepartmentStatus,
        deleteDepartment,
        loadClasses,
        openClassModal,
        saveClass,
        toggleClassStatus,
        deleteClass,
        viewClassRoster,
        loadSubjects,
        openSubjectModal,
        saveSubject,
        toggleSubjectStatus,
        deleteSubject,
        onSubjectDeptChange,
        loadTeachers,
        openTeacherModal,
        saveTeacher,
        toggleTeacherStatus,
        openPasswordResetModal,
        submitPasswordReset,
        deleteTeacher,
        loadStudents,
        debouncedLoadStudents,
        onStudentDeptFilterChange,
        openStudentModal,
        onStudentModalDeptChange,
        saveStudent,
        toggleStudentStatus,
        deleteStudent,
        openCsvImportModal,
        submitCsvImport,
        loadTimetable,
        openTimetableModal,
        onTimetableClassChange,
        saveTimetableSchedule,
        deleteTimetableSchedule,
        loadActiveSessions,
        terminateSession
    };
})();

document.addEventListener('DOMContentLoaded', AdminApp.init);
