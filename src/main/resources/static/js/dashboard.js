/**
 * Renaissance - Teacher Dashboard Analytics & Stats
 */

document.addEventListener('DOMContentLoaded', async () => {
    // Ensure teacher is logged in
    const teacher = await Auth.requireAuth();
    if (!teacher) return;

    let weeklyChartInstance = null;
    let subjectChartInstance = null;
    let timetableList = [];
    const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const todayIndex = new Date().getDay();
    const todayDayName = dayNames[todayIndex];
    let activeDay = (todayDayName === 'SUNDAY') ? 'MONDAY' : todayDayName;

    const loadDashboardData = async () => {
        try {
            // Load dashboard stats and weekly timetable in parallel
            const [res, ttRes] = await Promise.all([
                API.get('/api/reports/dashboard').catch(err => ({ success: false, error: err })),
                API.get('/api/timetable/my-schedule').catch(err => ({ success: false, error: err }))
            ]);

            if (res && res.success && res.data) {
                renderStats(res.data);
                renderCharts(res.data);
                renderActiveSessions(res.data.recentActiveSessions);
            }

            if (ttRes && ttRes.success && Array.isArray(ttRes.data)) {
                timetableList = ttRes.data;
                renderTimetable();
            } else if (ttRes && ttRes.data) {
                timetableList = Array.isArray(ttRes.data) ? ttRes.data : [];
                renderTimetable();
            }
        } catch (e) {
            console.error('Failed to load dashboard data', e);
            App.showToast('Failed to load analytics: ' + e.message, 'danger');
        }
    };

    const renderStats = (data) => {
        document.getElementById('stat-total-students').textContent = data.totalStudents;
        document.getElementById('stat-total-classes').textContent = data.totalClassesConducted;
        document.getElementById('stat-avg-attendance').textContent = `${data.averageAttendancePercentage}%`;
        document.getElementById('stat-today-attendance').textContent = data.todayAttendance;
        document.getElementById('stat-active-sessions').textContent = data.activeSessions;
        const classesEl = document.getElementById('stat-assigned-classes');
        if (classesEl) {
            classesEl.textContent = data.assignedClasses != null ? data.assignedClasses : (data.classesCount || 0);
        }
        const subjectsEl = document.getElementById('stat-assigned-subjects');
        if (subjectsEl) {
            subjectsEl.textContent = data.assignedSubjects != null ? data.assignedSubjects : (data.subjectsCount || 0);
        }
    };

    const renderActiveSessions = (sessions) => {
        const container = document.getElementById('active-sessions-container');
        if (!container) return;

        if (!sessions || sessions.length === 0) {
            container.innerHTML = `
                <div style="text-align:center; padding: 2rem; color: var(--text-muted);">
                    <p>No active attendance sessions at the moment.</p>
                    <a href="/sessions.html" class="btn btn-primary btn-sm" style="margin-top:0.75rem;">+ Start New Session</a>
                </div>
            `;
            return;
        }

        container.innerHTML = sessions.map(s => `
            <div class="active-session-banner">
                <div>
                    <div style="display:flex; align-items:center; gap:0.5rem; margin-bottom:0.25rem;">
                        <span class="badge badge-success badge-pulse">LIVE NOW</span>
                        <strong style="font-size:1.1rem;">${App.escapeHtml(s.className)}</strong>
                    </div>
                    <p style="font-size:0.875rem; color:var(--text-secondary); margin:0.25rem 0;">
                        Subject: <strong>${App.escapeHtml(s.subjectName)}</strong> (${App.escapeHtml(s.subjectCode)}) • Geofence: <strong>${s.geofenceRadiusMeters ? Math.round(s.geofenceRadiusMeters) : 40}m</strong>
                    </p>
                    <p style="font-size:0.825rem; color:var(--text-muted);">
                        Present: <strong>${s.presentCount}</strong> / ${s.totalEnrolled} students
                    </p>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:0.75rem; text-transform:uppercase; color:var(--text-muted); margin-bottom:0.2rem;">Attendance Code</div>
                    <div class="session-live-code">${App.escapeHtml(s.sessionCode)}</div>
                    <div style="margin-top:0.5rem;">
                        <a href="/sessions.html?id=${s.id}" class="btn btn-secondary btn-sm">Manage Live Session →</a>
                    </div>
                </div>
            </div>
        `).join('');
    };

    const renderCharts = (data) => {
        // Chart 1: Weekly Attendance Trend
        const weeklyCanvas = document.getElementById('weeklyAttendanceChart');
        if (weeklyCanvas && data.weeklyAttendanceCounts && window.Chart) {
            const labels = Object.keys(data.weeklyAttendanceCounts);
            const values = Object.values(data.weeklyAttendanceCounts);

            if (weeklyChartInstance) weeklyChartInstance.destroy();

            weeklyChartInstance = new Chart(weeklyCanvas, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Students Present',
                        data: values,
                        borderColor: '#7c3aed',
                        backgroundColor: 'rgba(124, 58, 237, 0.1)',
                        borderWidth: 3,
                        fill: true,
                        tension: 0.35,
                        pointBackgroundColor: '#7c3aed',
                        pointRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: { precision: 0 }
                        }
                    }
                }
            });
        }

        // Chart 2: Subject Attendance Trends
        const subjectCanvas = document.getElementById('subjectAttendanceChart');
        if (subjectCanvas && data.subjectAttendanceTrends && window.Chart) {
            const labels = Object.keys(data.subjectAttendanceTrends);
            const values = Object.values(data.subjectAttendanceTrends);

            if (subjectChartInstance) subjectChartInstance.destroy();

            subjectChartInstance = new Chart(subjectCanvas, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Avg Attendance %',
                        data: values,
                        backgroundColor: '#4338ca',
                        borderRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            max: 100,
                            ticks: {
                                callback: val => `${val}%`
                            }
                        }
                    }
                }
            });
        }
    };

    const formatSlotTime = (s) => {
        if (s.timeSlot && s.timeSlot.includes('-')) return s.timeSlot;
        const start = s.startTimeFormatted || (typeof s.startTime === 'string' ? s.startTime.substring(0, 5) : (s.startTime ? `${s.startTime.hour || s.startTime[0] || ''}:${s.startTime.minute || s.startTime[1] || ''}` : ''));
        const end = s.endTimeFormatted || (typeof s.endTime === 'string' ? s.endTime.substring(0, 5) : (s.endTime ? `${s.endTime.hour || s.endTime[0] || ''}:${s.endTime.minute || s.endTime[1] || ''}` : ''));
        return (start && end) ? `${start} – ${end}` : (start || 'Scheduled');
    };

    const capitalize = (str) => {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    };

    const getClassTagClass = (className = '') => {
        const upper = (className || '').toUpperCase();
        if (upper.includes('ISE')) return 'class-tag ise';
        if (upper.includes('ECE')) return 'class-tag ece';
        if (upper.includes('5B') || upper.includes('SEC B')) return 'class-tag alt';
        return 'class-tag';
    };

    const setupTimetableTabs = () => {
        const todayBadge = document.getElementById('today-day-indicator');
        if (todayBadge) {
            todayBadge.textContent = todayDayName === 'SUNDAY' ? 'Today: Sunday (Showing Monday)' : `Today: ${capitalize(todayDayName)}`;
        }

        const tabButtons = document.querySelectorAll('#timetable-day-tabs .day-tab-btn');
        tabButtons.forEach(btn => {
            const day = btn.dataset.day;
            btn.classList.toggle('active', day === activeDay);
            btn.classList.toggle('is-today-tab', day === todayDayName);

            btn.onclick = () => {
                activeDay = day;
                tabButtons.forEach(b => b.classList.toggle('active', b.dataset.day === activeDay));
                renderTimetable();
            };
        });
    };

    const renderTimetable = () => {
        const container = document.getElementById('timetable-content-container');
        const upcomingBanner = document.getElementById('timetable-upcoming-banner');
        if (!container) return;

        // Check if teacher has any timetable at all across the entire week
        if (!timetableList || timetableList.length === 0) {
            if (upcomingBanner) upcomingBanner.style.display = 'none';
            container.innerHTML = `
                <div class="empty-timetable-state">
                    <div class="empty-icon">📅</div>
                    <h4>No timetable has been assigned to you yet.</h4>
                    <p>Please contact your department administrator to configure your teaching schedule.</p>
                </div>
            `;
            return;
        }

        // Render Today's Schedule Highlight Banner if today has lectures
        const todayLectures = timetableList.filter(s => (s.dayName || s.dayOfWeek || '').toUpperCase() === todayDayName);
        if (upcomingBanner) {
            if (todayLectures.length > 0) {
                upcomingBanner.style.display = 'block';
                const periodsList = todayLectures.map(l => `Period ${l.periodNumber || 1} (${formatSlotTime(l)})`).join(', ');
                upcomingBanner.innerHTML = `
                    <div class="timetable-today-banner">
                        <div style="display: flex; align-items: center; gap: 0.6rem;">
                            <span style="font-size: 1.25rem;">⚡</span>
                            <div>
                                <strong style="color: #4338ca; font-size: 0.95rem;">You have ${todayLectures.length} scheduled lecture${todayLectures.length > 1 ? 's' : ''} today (${capitalize(todayDayName)}):</strong>
                                <span style="font-size: 0.85rem; color: var(--text-secondary); margin-left: 0.35rem;">${periodsList}</span>
                            </div>
                        </div>
                        <button class="btn btn-primary btn-sm" onclick="activeDay='${todayDayName}'; setupTimetableTabs(); renderTimetable();">
                            View Today's Schedule →
                        </button>
                    </div>
                `;
            } else if (todayDayName !== 'SUNDAY') {
                upcomingBanner.style.display = 'block';
                upcomingBanner.innerHTML = `
                    <div class="timetable-today-banner" style="background: rgba(241, 245, 249, 0.7); border-color: #cbd5e1;">
                        <span style="font-size: 0.9rem; color: var(--text-secondary);">
                            ☕ No lectures scheduled for today (${capitalize(todayDayName)}). Showing timetable for selected days.
                        </span>
                    </div>
                `;
            } else {
                upcomingBanner.style.display = 'none';
            }
        }

        // Filter schedules for the active day
        const daySlots = timetableList.filter(s => (s.dayName || s.dayOfWeek || '').toUpperCase() === activeDay);

        if (daySlots.length === 0) {
            container.innerHTML = `
                <div class="empty-timetable-state">
                    <div class="empty-icon">☕</div>
                    <h4>No lectures scheduled for ${capitalize(activeDay)}</h4>
                    <p>You have no teaching periods allocated on ${capitalize(activeDay)}. Select another day above to view lectures.</p>
                </div>
            `;
            return;
        }

        // Render Day's Timetable Table
        container.innerHTML = `
            <div style="overflow-x: auto;">
                <table class="timetable-table">
                    <thead>
                        <tr>
                            <th style="width: 100px;">Period</th>
                            <th style="width: 170px;">Timing</th>
                            <th style="width: 160px;">Class & Section</th>
                            <th>Subject / Course</th>
                            <th style="width: 130px;">Classroom</th>
                            <th style="width: 150px; text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${daySlots.map((s, idx) => {
                            const pNum = s.periodNumber || (idx + 1);
                            const timeStr = formatSlotTime(s);
                            const classTag = getClassTagClass(s.className);
                            const roomStr = s.roomNumber ? `📍 ${App.escapeHtml(s.roomNumber)}` : '📍 General';
                            const launchUrl = `/sessions.html?classId=${encodeURIComponent(s.classId || '')}&subjectId=${encodeURIComponent(s.subjectId || '')}`;
                            return `
                                <tr>
                                    <td>
                                        <span class="period-pill">Period ${pNum}</span>
                                    </td>
                                    <td>
                                        <div class="time-slot-badge">
                                            <span>🕒</span>
                                            <span>${App.escapeHtml(timeStr)}</span>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="${classTag}">
                                            ${App.escapeHtml(s.className || 'Class')}
                                        </span>
                                    </td>
                                    <td>
                                        <div style="font-weight: 700; color: var(--text-primary); font-size: 0.925rem;">
                                            ${App.escapeHtml(s.subjectName || 'Subject')}
                                        </div>
                                        <div style="font-size: 0.775rem; color: var(--text-muted); font-family: monospace;">
                                            Code: ${App.escapeHtml(s.subjectCode || '—')}
                                        </div>
                                    </td>
                                    <td>
                                        <span class="room-chip">${roomStr}</span>
                                    </td>
                                    <td style="text-align: right;">
                                        <a href="${launchUrl}" class="btn btn-primary btn-sm" style="padding: 0.35rem 0.7rem; font-size: 0.8rem;">
                                            + Launch Session
                                        </a>
                                    </td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
        `;
    };

    setupTimetableTabs();
    await loadDashboardData();

    // Auto-refresh stats periodically only when tab is active (preserves bandwidth and Atlas connections)
    setInterval(() => {
        if (!document.hidden) {
            loadDashboardData();
        }
    }, 30000);
});
