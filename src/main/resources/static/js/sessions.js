/**
 * Renaissance - Class Session Management & Real-Time Live Session
 */

document.addEventListener('DOMContentLoaded', async () => {
    const teacher = await Auth.requireAuth();
    if (!teacher) return;

    let allDepartments = [];
    let allClasses = [];
    let allSubjects = [];
    let activeSessionId = null;
    let stompClient = null;

    const deptSelect = document.getElementById('session-department');
    const classSelect = document.getElementById('session-class');
    const subjectSelect = document.getElementById('session-subject');
    const durationInput = document.getElementById('session-duration');
    const startSessionBtn = document.getElementById('start-session-btn');

    const activeSessionCard = document.getElementById('active-session-card');
    const liveCodeDisplay = document.getElementById('live-session-code');
    const liveClassDisplay = document.getElementById('live-session-class');
    const liveSubjectDisplay = document.getElementById('live-session-subject');
    const livePresentCount = document.getElementById('live-present-count');
    const liveTotalEnrolled = document.getElementById('live-total-enrolled');
    const livePercentage = document.getElementById('live-percentage');
    const copyLinkBtn = document.getElementById('copy-session-link-btn');
    const endSessionBtn = document.getElementById('end-session-btn');
    const liveRecordsTable = document.getElementById('live-records-table-body');
    const gpsLocationStatus = document.getElementById('gps-location-status');

    const sessionsHistoryTable = document.getElementById('sessions-history-table-body');

    // 1. Load Dropdowns
    const loadDropdownData = async () => {
        try {
            const [deptRes, classRes, subRes] = await Promise.all([
                API.get('/api/teacher/departments'),
                API.get('/api/teacher/classes'),
                API.get('/api/teacher/subjects')
            ]);

            if (deptRes.data) {
                allDepartments = deptRes.data;
                deptSelect.innerHTML = '<option value="">Select Department</option>' +
                    allDepartments.map(d => `<option value="${d.id}">${d.code} - ${d.name}</option>`).join('');
            }

            if (classRes.data) {
                allClasses = classRes.data;
            }

            if (subRes.data) {
                allSubjects = subRes.data;
            }
        } catch (e) {
            console.error('Failed to load metadata', e);
        }
    };

    deptSelect.addEventListener('change', () => {
        const deptId = deptSelect.value;
        const filteredClasses = deptId ? allClasses.filter(c => c.departmentId == deptId) : [];
        classSelect.innerHTML = '<option value="">Select Class</option>' +
            filteredClasses.map(c => `<option value="${c.id}">${c.displayName}</option>`).join('');

        const filteredSubjects = deptId ? allSubjects.filter(s => s.departmentId == deptId) : [];
        subjectSelect.innerHTML = '<option value="">Select Subject</option>' +
            filteredSubjects.map(s => `<option value="${s.id}">${s.code} - ${s.name}</option>`).join('');
    });

    // 2. Start Session Handler
    startSessionBtn.addEventListener('click', async () => {
        const classId = classSelect.value;
        const subjectId = subjectSelect.value;
        const duration = parseInt(durationInput.value, 10) || 15;

        if (!classId) {
            App.showToast('Please select a class.', 'warning');
            classSelect.focus();
            return;
        }

        if (!subjectId) {
            App.showToast('Please select a subject.', 'warning');
            subjectSelect.focus();
            return;
        }

        startSessionBtn.disabled = true;
        startSessionBtn.innerHTML = '<span>📡 Requesting GPS Coordinates...</span>';

        try {
            // Step 1: Capture Teacher's GPS location via browser
            const coords = await LocationService.getCurrentLocation();
            startSessionBtn.innerHTML = '<span>🚀 Activating 15m Geofence...</span>';

            // Step 2: Call backend to create and activate session
            const payload = {
                classId: parseInt(classId, 10),
                subjectId: parseInt(subjectId, 10),
                latitude: coords.latitude,
                longitude: coords.longitude,
                accuracy: coords.accuracy,
                durationMinutes: duration
            };

            const res = await API.post('/api/sessions/start', payload);
            if (res && res.data) {
                App.showToast('Attendance Session Activated!', 'success');
                showActiveSession(res.data);
                loadSessionsHistory();
            }
        } catch (err) {
            App.showToast(err.message || 'Failed to start session', 'danger');
        } finally {
            startSessionBtn.disabled = false;
            startSessionBtn.innerHTML = '<span>🎯 Start Attendance Session</span>';
        }
    });

    // 3. Show Active Session View
    const showActiveSession = (session) => {
        activeSessionId = session.id;
        activeSessionCard.style.display = 'block';

        liveCodeDisplay.textContent = session.sessionCode;
        liveClassDisplay.textContent = session.className;
        liveSubjectDisplay.textContent = `${session.subjectCode} - ${session.subjectName}`;
        livePresentCount.textContent = session.presentCount || 0;
        liveTotalEnrolled.textContent = session.totalEnrolled || 0;

        const pct = session.totalEnrolled > 0 ? ((session.presentCount / session.totalEnrolled) * 100).toFixed(1) : '0.0';
        livePercentage.textContent = `${pct}%`;

        if (gpsLocationStatus) {
            gpsLocationStatus.textContent = `Lat: ${session.teacherLatitude.toFixed(6)}, Lon: ${session.teacherLongitude.toFixed(6)} (Radius: 15m)`;
        }

        // Setup Copy Link
        copyLinkBtn.onclick = () => {
            const fullUrl = `${window.location.origin}/student-attendance.html?code=${session.sessionCode}`;
            navigator.clipboard.writeText(fullUrl).then(() => {
                App.showToast('Attendance link copied to clipboard!', 'success');
                copyLinkBtn.textContent = '✓ Copied!';
                setTimeout(() => copyLinkBtn.textContent = '🔗 Copy Link', 2000);
            }).catch(() => {
                prompt('Copy this link for your students:', fullUrl);
            });
        };

        // Setup End Session
        endSessionBtn.onclick = async () => {
            if (confirm(`Are you sure you want to end session ${session.sessionCode}? No more attendance submissions will be accepted.`)) {
                try {
                    await API.post(`/api/sessions/${session.id}/end`);
                    App.showToast('Session ended successfully', 'info');
                    activeSessionCard.style.display = 'none';
                    activeSessionId = null;
                    if (stompClient) stompClient.disconnect();
                    loadSessionsHistory();
                } catch (err) {
                    App.showToast(err.message, 'danger');
                }
            }
        };

        // Load current records
        loadSessionRecords(session.id);

        // Connect STOMP WebSocket or fallback poll
        initWebSocket(session.id);
    };

    // 4. WebSocket STOMP Live Subscription
    const initWebSocket = (sessionId) => {
        // Lightweight polling fallback + STOMP integration
        const pollInterval = setInterval(() => {
            if (activeSessionId === sessionId) {
                loadSessionRecords(sessionId);
            } else {
                clearInterval(pollInterval);
            }
        }, 5000);
    };

    const loadSessionRecords = async (sessionId) => {
        try {
            const res = await API.get(`/api/attendance/session/${sessionId}/records`);
            if (res && res.data) {
                renderLiveRecords(res.data);
            }
        } catch (e) {
            // ignore
        }
    };

    const renderLiveRecords = (records) => {
        livePresentCount.textContent = records.length;
        const total = parseInt(liveTotalEnrolled.textContent, 10) || 1;
        const pct = ((records.length / total) * 100).toFixed(1);
        livePercentage.textContent = `${pct}%`;

        if (records.length === 0) {
            liveRecordsTable.innerHTML = `
                <tr><td colspan="4" style="text-align:center; color:var(--text-muted); padding:1.5rem;">
                    Waiting for students to submit attendance within 15 meters...
                </td></tr>
            `;
            return;
        }

        liveRecordsTable.innerHTML = records.map(r => `
            <tr>
                <td><strong>${App.escapeHtml(r.student.usn)}</strong></td>
                <td>${App.escapeHtml(r.student.name)}</td>
                <td><span class="badge badge-success">PRESENT</span></td>
                <td>${r.distanceFromTeacher ? `${r.distanceFromTeacher.toFixed(1)} m` : '-'}</td>
            </tr>
        `).join('');
    };

    // 5. Load Past Sessions History
    const loadSessionsHistory = async () => {
        try {
            const res = await API.get('/api/sessions/my-sessions');
            if (res && res.data) {
                renderSessionsHistory(res.data);
                // If there's an active session, show it automatically
                const active = res.data.find(s => s.status === 'ACTIVE');
                if (active && !activeSessionId) {
                    showActiveSession(active);
                }
            }
        } catch (e) {
            console.error('Failed to load past sessions', e);
        }
    };

    const renderSessionsHistory = (sessions) => {
        if (!sessions || sessions.length === 0) {
            sessionsHistoryTable.innerHTML = `
                <tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:2rem;">
                    No sessions created yet. Click "Start Attendance Session" above.
                </td></tr>
            `;
            return;
        }

        sessionsHistoryTable.innerHTML = sessions.map(s => `
            <tr>
                <td><strong style="color:var(--accent-600); font-family:monospace;">${App.escapeHtml(s.sessionCode)}</strong></td>
                <td>${App.escapeHtml(s.className)}</td>
                <td>${App.escapeHtml(s.subjectName)}</td>
                <td>${App.formatDateTime(s.startTime)}</td>
                <td>${s.presentCount} / ${s.totalEnrolled}</td>
                <td>
                    <span class="badge badge-${s.status === 'ACTIVE' ? 'success badge-pulse' : (s.status === 'COMPLETED' ? 'neutral' : 'warning')}">
                        ${s.status}
                    </span>
                </td>
            </tr>
        `).join('');
    };

    await loadDropdownData();
    await loadSessionsHistory();
});
