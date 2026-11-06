/**
 * Renaissance - Student Management
 */

document.addEventListener('DOMContentLoaded', async () => {
    const teacher = await Auth.requireAuth();
    if (!teacher) return;

    let allDepartments = [];
    let allClasses = [];

    const searchInput = document.getElementById('student-search-input');
    const filterDept = document.getElementById('filter-department');
    const filterClass = document.getElementById('filter-class');
    const studentsTableBody = document.getElementById('students-table-body');
    const addStudentBtn = document.getElementById('add-student-btn');
    const importCsvBtn = document.getElementById('import-csv-btn');

    const studentModal = document.getElementById('student-modal');
    const studentForm = document.getElementById('student-form');
    const studentModalTitle = document.getElementById('student-modal-title');
    const studentIdInput = document.getElementById('student-id');
    const studentUsnInput = document.getElementById('student-usn');
    const studentNameInput = document.getElementById('student-name');
    const studentEmailInput = document.getElementById('student-email');
    const studentDeptSelect = document.getElementById('student-department');
    const studentClassSelect = document.getElementById('student-class');

    const csvModal = document.getElementById('csv-modal');
    const csvFileInput = document.getElementById('csv-file-input');
    const uploadCsvBtn = document.getElementById('upload-csv-btn');

    // Load dropdown options
    const loadMetadata = async () => {
        try {
            const [deptRes, classRes] = await Promise.all([
                API.get('/api/teacher/departments'),
                API.get('/api/teacher/classes')
            ]);

            if (deptRes.data) {
                allDepartments = deptRes.data;
                const deptOptions = '<option value="">All Departments</option>' +
                    allDepartments.map(d => `<option value="${d.id}">${d.code} - ${d.name}</option>`).join('');
                filterDept.innerHTML = deptOptions;
                studentDeptSelect.innerHTML = '<option value="">Select Department</option>' +
                    allDepartments.map(d => `<option value="${d.id}">${d.code} - ${d.name}</option>`).join('');
            }

            if (classRes.data) {
                allClasses = classRes.data;
                renderClassSelects();
            }
        } catch (e) {
            console.error('Failed to load metadata', e);
        }
    };

    const renderClassSelects = (deptId = null) => {
        const filtered = deptId ? allClasses.filter(c => c.departmentId == deptId) : allClasses;
        const options = '<option value="">All Classes</option>' +
            filtered.map(c => `<option value="${c.id}">${c.displayName}</option>`).join('');
        filterClass.innerHTML = options;

        studentClassSelect.innerHTML = '<option value="">Select Class</option>' +
            filtered.map(c => `<option value="${c.id}">${c.displayName}</option>`).join('');
    };

    // Filter class options when department changes
    filterDept.addEventListener('change', () => {
        renderClassSelects(filterDept.value);
        loadStudents();
    });

    studentDeptSelect.addEventListener('change', () => {
        const deptId = studentDeptSelect.value;
        const filtered = deptId ? allClasses.filter(c => c.departmentId == deptId) : allClasses;
        studentClassSelect.innerHTML = '<option value="">Select Class</option>' +
            filtered.map(c => `<option value="${c.id}">${c.displayName}</option>`).join('');
    });

    filterClass.addEventListener('change', () => loadStudents());
    searchInput.addEventListener('input', () => loadStudents());

    // Load Students List
    const loadStudents = async () => {
        try {
            const deptId = filterDept.value;
            const classId = filterClass.value;
            const keyword = searchInput.value.trim();

            let url = '/api/students?';
            if (deptId) url += `departmentId=${deptId}&`;
            if (classId) url += `classId=${classId}&`;
            if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;

            const res = await API.get(url);
            if (res && res.data) {
                renderStudents(res.data);
            }
        } catch (e) {
            console.error('Failed to load students', e);
            App.showToast('Failed to load students: ' + e.message, 'danger');
        }
    };

    const renderStudents = (students) => {
        if (!students || students.length === 0) {
            studentsTableBody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align:center; padding: 2.5rem; color: var(--text-muted);">
                        No students found matching current criteria.
                    </td>
                </tr>
            `;
            return;
        }

        studentsTableBody.innerHTML = students.map((s, idx) => `
            <tr>
                <td><strong>${App.escapeHtml(s.usn)}</strong></td>
                <td>
                    <div style="font-weight:600;">${App.escapeHtml(s.name)}</div>
                    <div style="font-size:0.8rem; color:var(--text-muted);">${App.escapeHtml(s.email || '-')}</div>
                </td>
                <td><span class="badge badge-primary">${App.escapeHtml(s.departmentCode || '-')}</span></td>
                <td>Sem ${s.semester} (${App.escapeHtml(s.section)})</td>
                <td><span class="badge badge-${s.status === 'ACTIVE' ? 'success' : 'neutral'}">${s.status}</span></td>
                <td style="text-align:right;">
                    <button class="btn btn-secondary btn-sm edit-student-btn" data-id="${s.id}">Edit</button>
                    <button class="btn btn-outline btn-sm delete-student-btn" data-id="${s.id}" data-name="${App.escapeHtml(s.name)}" style="color:var(--danger-accent); border-color:var(--danger-accent);">Delete</button>
                </td>
            </tr>
        `).join('');

        // Bind Edit buttons
        document.querySelectorAll('.edit-student-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = btn.getAttribute('data-id');
                const student = students.find(s => s.id == id);
                if (student) openEditModal(student);
            });
        });

        // Bind Delete buttons
        document.querySelectorAll('.delete-student-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-id');
                const name = btn.getAttribute('data-name');
                if (confirm(`Are you sure you want to delete student "${name}"? This action cannot be undone.`)) {
                    try {
                        await API.delete(`/api/students/${id}`);
                        App.showToast(`Student ${name} deleted successfully`, 'success');
                        loadStudents();
                    } catch (err) {
                        App.showToast(`Error deleting student: ${err.message}`, 'danger');
                    }
                }
            });
        });
    };

    // Modal Actions
    const openAddModal = () => {
        studentModalTitle.textContent = 'Add New Student';
        studentIdInput.value = '';
        studentForm.reset();
        App.openModal('student-modal');
    };

    const openEditModal = (s) => {
        studentModalTitle.textContent = 'Edit Student Profile';
        studentIdInput.value = s.id;
        studentUsnInput.value = s.usn;
        studentNameInput.value = s.name;
        studentEmailInput.value = s.email || '';
        studentDeptSelect.value = s.departmentId;

        // Populate class options for this department
        const filtered = allClasses.filter(c => c.departmentId == s.departmentId);
        studentClassSelect.innerHTML = '<option value="">Select Class</option>' +
            filtered.map(c => `<option value="${c.id}">${c.displayName}</option>`).join('');
        studentClassSelect.value = s.classId;

        App.openModal('student-modal');
    };

    addStudentBtn.addEventListener('click', openAddModal);

    // Save Student (Create or Update)
    studentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = studentIdInput.value;
        const payload = {
            usn: studentUsnInput.value.trim().toUpperCase(),
            name: studentNameInput.value.trim(),
            email: studentEmailInput.value.trim(),
            departmentId: studentDeptSelect.value,
            classId: studentClassSelect.value,
            status: 'ACTIVE'
        };

        try {
            if (id) {
                await API.put(`/api/students/${id}`, payload);
                App.showToast('Student updated successfully', 'success');
            } else {
                await API.post('/api/students', payload);
                App.showToast('Student created successfully', 'success');
            }
            App.closeModal('student-modal');
            loadStudents();
        } catch (err) {
            App.showToast(err.message, 'danger');
        }
    });

    // CSV Import Handlers
    importCsvBtn.addEventListener('click', () => {
        csvFileInput.value = '';
        App.openModal('csv-modal');
    });

    uploadCsvBtn.addEventListener('click', async () => {
        if (!csvFileInput.files || csvFileInput.files.length === 0) {
            App.showToast('Please select a CSV file to upload', 'warning');
            return;
        }

        const formData = new FormData();
        formData.append('file', csvFileInput.files[0]);

        uploadCsvBtn.disabled = true;
        uploadCsvBtn.textContent = 'Importing...';

        try {
            const res = await API.post('/api/students/import', formData);
            if (res && res.data) {
                const { importedCount, skippedCount } = res.data;
                App.showToast(`Import finished: ${importedCount} added, ${skippedCount} skipped.`, 'success');
                App.closeModal('csv-modal');
                loadStudents();
            }
        } catch (err) {
            App.showToast('Import failed: ' + err.message, 'danger');
        } finally {
            uploadCsvBtn.disabled = false;
            uploadCsvBtn.textContent = 'Upload & Import';
        }
    });

    // Close buttons for modals
    document.querySelectorAll('.modal-close-btn, .btn-modal-cancel').forEach(btn => {
        btn.addEventListener('click', () => {
            App.closeModal('student-modal');
            App.closeModal('csv-modal');
        });
    });

    await loadMetadata();
    await loadStudents();
});
