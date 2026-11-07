/**
 * Renaissance - Teacher Dashboard Analytics & Stats
 */

document.addEventListener('DOMContentLoaded', async () => {
    // Ensure teacher is logged in
    const teacher = await Auth.requireAuth();
    if (!teacher) return;

    let weeklyChartInstance = null;
    let subjectChartInstance = null;

    const loadDashboardData = async () => {
        try {
            const res = await API.get('/api/reports/dashboard');
            if (res && res.success && res.data) {
                renderStats(res.data);
                renderCharts(res.data);
                renderActiveSessions(res.data.recentActiveSessions);
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

    await loadDashboardData();

    // Auto-refresh stats periodically only when tab is active (preserves bandwidth and Atlas connections)
    setInterval(() => {
        if (!document.hidden) {
            loadDashboardData();
        }
    }, 30000);
});
