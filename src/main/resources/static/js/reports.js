/**
 * Renaissance - Attendance Records, Profile Modal & CSV Export
 */

document.addEventListener('DOMContentLoaded', async () => {
    const teacher = await Auth.requireAuth();
    if (!teacher) return;

    let allDepartments = [];
    let allClasses = [];
    let allSubjects = [];
    let currentSummaries = [];

    const filterDept = document.getElementById('report-department');
    const filterClass = document.getElementById('report-class');
    const filterSubject = document.getElementById('report-subject');
    const exportCsvBtn = document.getElementById('export-summary-csv-btn');
    const exportRegisterExcelBtn = document.getElementById('export-register-excel-btn');
    const recordsTableBody = document.getElementById('attendance-summary-table-body');

    // Student Profile Modal Elements
    const profileModal = document.getElementById('profile-modal');
    const profileName = document.getElementById('profile-name');
    const profileUsn = document.getElementById('profile-usn');
    const profileDept = document.getElementById('profile-dept');
    const profileSemSec = document.getElementById('profile-sem-sec');
    const profileConducted = document.getElementById('profile-conducted');
    const profilePresent = document.getElementById('profile-present');
    const profileAbsent = document.getElementById('profile-absent');
    const profilePercentage = document.getElementById('profile-percentage');
    const profileHistoryBody = document.getElementById('profile-history-table-body');
    const exportStudentCsvBtn = document.getElementById('export-student-csv-btn');

    let selectedStudentId = null;

    // 1. Load Filter Dropdowns
    const loadMetadata = async () => {
        try {
            const [deptRes, classRes, subRes] = await Promise.all([
                API.get('/api/teacher/departments'),
                API.get('/api/teacher/classes'),
                API.get('/api/teacher/subjects')
            ]);

            if (deptRes.data) {
                allDepartments = deptRes.data;
                filterDept.innerHTML = '<option value="">All Departments</option>' +
                    allDepartments.map(d => `<option value="${d.id}">${d.code} - ${d.name}</option>`).join('');
            }

            if (classRes.data) allClasses = classRes.data;
            if (subRes.data) allSubjects = subRes.data;

            renderClassAndSubjectFilters();
        } catch (e) {
            console.error('Failed to load metadata', e);
        }
    };

    const renderClassAndSubjectFilters = (deptId = null) => {
        const classes = deptId ? allClasses.filter(c => c.departmentId == deptId) : allClasses;
        filterClass.innerHTML = '<option value="">All Classes</option>' +
            classes.map(c => `<option value="${c.id}">${c.displayName}</option>`).join('');

        const subjects = deptId ? allSubjects.filter(s => s.departmentId == deptId) : allSubjects;
        filterSubject.innerHTML = '<option value="">All Subjects</option>' +
            subjects.map(s => `<option value="${s.id}">${s.code} - ${s.name}</option>`).join('');
    };

    filterDept.addEventListener('change', () => {
        renderClassAndSubjectFilters(filterDept.value);
        loadAttendanceData();
    });

    filterClass.addEventListener('change', () => loadAttendanceData());
    filterSubject.addEventListener('change', () => loadAttendanceData());

    // 2. Load Attendance Summary Table
    const loadAttendanceData = async () => {
        const classId = filterClass.value;
        const subjectId = filterSubject.value;

        let url = '/api/reports/attendance-summary?';
        if (classId) url += `classId=${classId}&`;
        if (subjectId) url += `subjectId=${subjectId}&`;

        try {
            const res = await API.get(url);
            if (res && res.data) {
                currentSummaries = res.data;
                renderSummaryTable(res.data);
            }
        } catch (e) {
            console.error('Failed to load attendance summary', e);
            App.showToast('Failed to load records: ' + e.message, 'danger');
        }
    };

    const renderSummaryTable = (records) => {
        if (!records || records.length === 0) {
            recordsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align:center; padding: 2.5rem; color: var(--text-muted);">
                        No attendance records found for the selected filters.
                    </td>
                </tr>
            `;
            return;
        }

        recordsTableBody.innerHTML = records.map(r => {
            const isLow = r.below75;
            return `
                <tr class="${isLow ? 'highlight-danger' : ''}">
                    <td><strong>${App.escapeHtml(r.usn)}</strong></td>
                    <td>
                        <a href="javascript:void(0)" class="student-profile-link" data-id="${r.studentId}" style="font-weight:700; color:var(--accent-600);">
                            ${App.escapeHtml(r.studentName)}
                        </a>
                    </td>
                    <td>${App.escapeHtml(r.department)} - Sem ${r.semester} (${App.escapeHtml(r.section)})</td>
                    <td style="text-align:center;">${r.totalClasses}</td>
                    <td style="text-align:center; color:var(--success-accent); font-weight:700;">${r.attended}</td>
                    <td style="text-align:center; color:var(--danger-accent); font-weight:700;">${r.absent}</td>
                    <td style="text-align:right;">
                        <span class="badge badge-${isLow ? 'danger' : 'success'}" style="font-size:0.85rem; padding: 0.35rem 0.75rem;">
                            ${r.percentage.toFixed(1)}% ${isLow ? '⚠ < 75%' : ''}
                        </span>
                    </td>
                </tr>
            `;
        }).join('');

        // Bind profile clicks
        document.querySelectorAll('.student-profile-link').forEach(link => {
            link.addEventListener('click', () => {
                const id = link.getAttribute('data-id');
                openProfileModal(id);
            });
        });
    };

    // 3. Open Detailed Student Profile Modal
    const openProfileModal = async (studentId) => {
        selectedStudentId = studentId;
        const classId = filterClass ? filterClass.value : '';
        const subjectId = filterSubject ? filterSubject.value : '';
        const selectedSubjectName = (filterSubject && filterSubject.selectedIndex >= 0 && filterSubject.value)
            ? filterSubject.options[filterSubject.selectedIndex].text
            : '';

        let url = `/api/reports/student-profile/${encodeURIComponent(studentId)}`;
        const queryParams = [];
        if (classId) queryParams.push(`classId=${encodeURIComponent(classId)}`);
        if (subjectId) queryParams.push(`subjectId=${encodeURIComponent(subjectId)}`);
        if (queryParams.length > 0) url += `?${queryParams.join('&')}`;

        try {
            const res = await API.get(url);
            if (res && res.data) {
                const p = res.data;
                const subjectBadge = p.filteredSubjectName || selectedSubjectName;
                profileName.textContent = subjectBadge ? `${p.name} — ${subjectBadge}` : p.name;
                profileUsn.textContent = p.usn;
                profileDept.textContent = p.department;
                profileSemSec.textContent = `Sem ${p.semester} (Sec ${p.section})`;
                profileConducted.textContent = p.totalConducted;
                profilePresent.textContent = p.totalPresent;
                profileAbsent.textContent = p.totalAbsent;
                profilePercentage.textContent = `${p.overallPercentage.toFixed(1)}%`;

                if (p.below75) {
                    profilePercentage.style.color = 'var(--danger-accent)';
                } else {
                    profilePercentage.style.color = 'var(--success-accent)';
                }

                // Render Attendance History
                if (!p.attendanceHistory || p.attendanceHistory.length === 0) {
                    const emptySubjectText = subjectBadge || 'the selected subject';
                    profileHistoryBody.innerHTML = `
                        <tr><td colspan="5" style="text-align:center; padding:1.5rem; color:var(--text-muted);">
                            ${subjectId ? `No attendance records found for ${App.escapeHtml(emptySubjectText)}.` : 'No sessions attended yet.'}
                        </td></tr>
                    `;
                } else {
                    profileHistoryBody.innerHTML = p.attendanceHistory.map(h => `
                        <tr>
                            <td>${App.escapeHtml(h.sessionDate)}</td>
                            <td><strong>${App.escapeHtml(h.subjectName)}</strong> (${App.escapeHtml(h.subjectCode)})</td>
                            <td>${App.escapeHtml(h.markedTime)}</td>
                            <td>${App.escapeHtml(h.distanceFromTeacher)}</td>
                            <td><span class="badge badge-success">${App.escapeHtml(h.status)}</span></td>
                        </tr>
                    `).join('');
                }

                App.openModal('profile-modal');
            }
        } catch (e) {
            App.showToast('Failed to load student profile: ' + e.message, 'danger');
        }
    };

    // 4. Export CSV & Excel Register Handlers
    if (exportRegisterExcelBtn) {
        exportRegisterExcelBtn.addEventListener('click', async () => {
            const classId = filterClass.value;
            const subjectId = filterSubject.value;

            let url = '/api/reports/export/register-excel?';
            if (classId) url += `classId=${encodeURIComponent(classId)}&`;
            if (subjectId) url += `subjectId=${encodeURIComponent(subjectId)}&`;

            try {
                exportRegisterExcelBtn.disabled = true;
                exportRegisterExcelBtn.textContent = 'Generating Excel...';

                const res = await API.get(url);
                if (res.isFile) {
                    const downloadUrl = window.URL.createObjectURL(res.blob);
                    const a = document.createElement('a');
                    a.href = downloadUrl;
                    a.download = `Theory_Attendance_Register.xlsx`;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    window.URL.revokeObjectURL(downloadUrl);
                    App.showToast('Theory Attendance Register (.xlsx) downloaded!', 'success');
                }
            } catch (err) {
                App.showToast('Failed to export Excel register: ' + err.message, 'danger');
            } finally {
                exportRegisterExcelBtn.disabled = false;
                exportRegisterExcelBtn.textContent = '📊 Download Register (.xlsx)';
            }
        });
    }

    exportCsvBtn.addEventListener('click', async () => {
        const classId = filterClass.value;
        const subjectId = filterSubject.value;

        let url = '/api/reports/export/summary-csv?';
        if (classId) url += `classId=${classId}&`;
        if (subjectId) url += `subjectId=${subjectId}&`;

        try {
            exportCsvBtn.disabled = true;
            exportCsvBtn.textContent = 'Generating CSV...';

            const res = await API.get(url);
            if (res.isFile) {
                const downloadUrl = window.URL.createObjectURL(res.blob);
                const a = document.createElement('a');
                a.href = downloadUrl;
                a.download = `renaissance_attendance_summary.csv`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(downloadUrl);
                App.showToast('Attendance summary CSV downloaded!', 'success');
            }
        } catch (err) {
            App.showToast('Failed to export CSV: ' + err.message, 'danger');
        } finally {
            exportCsvBtn.disabled = false;
            exportCsvBtn.textContent = '📥 Export Summary CSV';
        }
    });

    if (exportStudentCsvBtn) {
        exportStudentCsvBtn.addEventListener('click', async () => {
            if (!selectedStudentId) return;
            try {
                const classId = filterClass ? filterClass.value : '';
                const subjectId = filterSubject ? filterSubject.value : '';
                let url = `/api/reports/export/student-csv/${encodeURIComponent(selectedStudentId)}`;
                const queryParams = [];
                if (classId) queryParams.push(`classId=${encodeURIComponent(classId)}`);
                if (subjectId) queryParams.push(`subjectId=${encodeURIComponent(subjectId)}`);
                if (queryParams.length > 0) url += `?${queryParams.join('&')}`;

                const res = await API.get(url);
                if (res.isFile) {
                    const downloadUrl = window.URL.createObjectURL(res.blob);
                    const a = document.createElement('a');
                    a.href = downloadUrl;
                    a.download = `renaissance_student_${selectedStudentId}_history.csv`;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    window.URL.revokeObjectURL(downloadUrl);
                    App.showToast('Student attendance CSV downloaded!', 'success');
                }
            } catch (err) {
                App.showToast('Failed to download student CSV: ' + err.message, 'danger');
            }
        });
    }

    // Modal close
    document.querySelectorAll('.modal-close-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            App.closeModal('profile-modal');
        });
    });

    // Load filter dropdown metadata and initial attendance summary concurrently in parallel
    await Promise.all([loadMetadata(), loadAttendanceData()]);
});
