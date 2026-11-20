/**
 * Renaissance - Student Attendance Logic
 * Incorporates:
 * - Module 1: Smart USN Detection & Auto-fill
 * - Module 3: Strict 5-Minute Session Expiry & Live Countdown Timer
 * - Module 4: Student Device Registration & Verification
 */

document.addEventListener('DOMContentLoaded', async () => {
    const classSelect = document.getElementById('class-select');
    const usnDigitsInput = document.getElementById('usn-digits-input');
    const btnDetectUsn = document.getElementById('btn-detect-usn');
    const detectedCard = document.getElementById('detected-student-card');
    const detectedName = document.getElementById('detected-student-name');
    const detectedUsnBadge = document.getElementById('detected-student-usn');
    const detectedDeptBadge = document.getElementById('detected-student-dept');
    const detectedClassBadge = document.getElementById('detected-student-class');
    const btnChangeDigits = document.getElementById('btn-change-digits');
    const usnFeedbackText = document.getElementById('usn-feedback-text');

    const manualUsnGroup = document.getElementById('manual-usn-group');
    const usnInput = document.getElementById('usn-input');
    const codeInput = document.getElementById('code-input');
    const submitBtn = document.getElementById('submit-attendance-btn');
    const retryBtn = document.getElementById('retry-btn');
    const restartBtn = document.getElementById('restart-btn');

    const formSection = document.getElementById('attendance-form-section');
    const loadingSection = document.getElementById('attendance-loading-section');
    const successSection = document.getElementById('attendance-success-section');
    const errorSection = document.getElementById('attendance-error-section');

    const radarCircle = document.getElementById('radar-circle');
    const statusText = document.getElementById('location-status-text');
    const errorMessage = document.getElementById('error-message-text');

    // Timer elements
    const timerWidget = document.getElementById('session-timer-widget');
    const timerBadge = document.getElementById('timer-badge');
    const timerDot = document.getElementById('timer-dot');
    const timerStatusText = document.getElementById('timer-status-text');
    const timerCountdownText = document.getElementById('timer-countdown-text');

    // Device elements
    const deviceBanner = document.getElementById('device-banner');
    const deviceAlertBox = document.getElementById('device-alert-box');
    const deviceAlertTitle = document.getElementById('device-alert-title');
    const deviceStatusPill = document.getElementById('device-status-pill');
    const deviceAlertDesc = document.getElementById('device-alert-desc');
    const btnRequestDeviceReg = document.getElementById('btn-request-device-reg');

    let detectedStudentUsn = null;
    let timerInterval = null;
    let sessionRemainingSeconds = 0;
    let isSessionExpired = false;

    // ----------------------------------------------------
    // MODULE 4: PERSISTENT DEVICE TOKEN INITIALIZATION
    // ----------------------------------------------------
    const getOrCreateDeviceToken = () => {
        let token = localStorage.getItem('renaissance_device_token');
        if (!token) {
            token = 'dev_' + (crypto.randomUUID ? crypto.randomUUID().replace(/-/g, '') : Math.random().toString(36).substring(2)) + Date.now().toString(36);
            localStorage.setItem('renaissance_device_token', token);
        }
        return token;
    };

    const getDeviceMetadata = () => {
        const ua = navigator.userAgent;
        let os = 'Unknown OS';
        if (/Android/i.test(ua)) os = 'Android';
        else if (/iPhone|iPad|iPod/i.test(ua)) os = 'iOS';
        else if (/Windows/i.test(ua)) os = 'Windows';
        else if (/Mac/i.test(ua)) os = 'macOS';
        else if (/Linux/i.test(ua)) os = 'Linux';

        let browser = 'Unknown Browser';
        if (/Chrome/i.test(ua)) browser = 'Chrome';
        else if (/Safari/i.test(ua)) browser = 'Safari';
        else if (/Firefox/i.test(ua)) browser = 'Firefox';
        else if (/Edge/i.test(ua)) browser = 'Edge';

        return {
            deviceModel: `${browser} on ${os}`,
            operatingSystem: os,
            browser: browser,
            deviceFingerprint: `${screen.width}x${screen.height}_${navigator.language || 'en'}`
        };
    };

    const currentDeviceToken = getOrCreateDeviceToken();

    // Check device approval status with backend
    const checkDeviceStatus = async (usn) => {
        if (!usn) return;
        try {
            const res = await API.post('/api/public/device/check-status', {
                usn: usn,
                deviceToken: currentDeviceToken
            });

            if (res && res.data) {
                const d = res.data;
                if (d.approved) {
                    deviceBanner.style.display = 'none';
                } else if (d.status === 'PENDING') {
                    deviceBanner.style.display = 'block';
                    deviceAlertBox.className = 'alert alert-warning';
                    deviceAlertTitle.textContent = 'Device Registration Pending';
                    deviceStatusPill.textContent = 'Pending Approval';
                    deviceStatusPill.style.background = '#fef3c7';
                    deviceStatusPill.style.color = '#92400e';
                    deviceAlertDesc.textContent = 'Your device registration request has been submitted. Please ask your department administrator to approve it.';
                    btnRequestDeviceReg.style.display = 'none';
                } else if (d.status === 'REVOKED' || d.status === 'REJECTED') {
                    deviceBanner.style.display = 'block';
                    deviceAlertBox.className = 'alert alert-danger';
                    deviceAlertTitle.textContent = 'Device Authorization ' + d.status;
                    deviceStatusPill.textContent = d.status;
                    deviceStatusPill.style.background = '#fee2e2';
                    deviceStatusPill.style.color = '#991b1b';
                    deviceAlertDesc.textContent = d.message || 'This device is not authorized to mark attendance.';
                    btnRequestDeviceReg.style.display = 'block';
                    btnRequestDeviceReg.textContent = '📱 Submit New Registration Request';
                } else if (d.status === 'MISMATCH') {
                    deviceBanner.style.display = 'block';
                    deviceAlertBox.className = 'alert alert-danger';
                    deviceAlertTitle.textContent = 'Device Mismatch';
                    deviceStatusPill.textContent = 'Mismatch';
                    deviceStatusPill.style.background = '#fee2e2';
                    deviceStatusPill.style.color = '#991b1b';
                    deviceAlertDesc.textContent = d.message;
                    btnRequestDeviceReg.style.display = 'none';
                } else {
                    // Not registered
                    deviceBanner.style.display = 'block';
                    deviceAlertBox.className = 'alert alert-warning';
                    deviceAlertTitle.textContent = 'Device Registration Required';
                    deviceStatusPill.textContent = 'Unregistered';
                    deviceStatusPill.style.background = '#fef3c7';
                    deviceStatusPill.style.color = '#92400e';
                    deviceAlertDesc.textContent = 'Only registered and approved devices can mark attendance. Please register this device.';
                    btnRequestDeviceReg.style.display = 'block';
                    btnRequestDeviceReg.textContent = '📱 Request Device Registration';
                }
            }
        } catch (e) {
            console.warn('Unable to verify device registration status', e);
        }
    };

    // Handle "Request Device Registration" click
    if (btnRequestDeviceReg) {
        btnRequestDeviceReg.addEventListener('click', async () => {
            const usn = detectedStudentUsn || (usnInput ? usnInput.value.trim().toUpperCase() : null);
            if (!usn) {
                App.showToast('Please verify your 3-digit USN first before registering device.', 'warning');
                if (usnDigitsInput) usnDigitsInput.focus();
                return;
            }

            const meta = getDeviceMetadata();
            try {
                btnRequestDeviceReg.disabled = true;
                btnRequestDeviceReg.textContent = 'Submitting Request...';

                const res = await API.post('/api/public/device/register', {
                    usn: usn,
                    deviceToken: currentDeviceToken,
                    deviceModel: meta.deviceModel,
                    operatingSystem: meta.operatingSystem,
                    browser: meta.browser,
                    deviceFingerprint: meta.deviceFingerprint
                });

                if (res && res.success) {
                    App.showToast('Device registration request submitted to administrator!', 'success');
                    checkDeviceStatus(usn);
                } else {
                    App.showToast(res.message || 'Registration request failed.', 'danger');
                }
            } catch (err) {
                App.showToast(err.message || 'Unable to register device. Please try again.', 'danger');
            } finally {
                btnRequestDeviceReg.disabled = false;
            }
        });
    }

    // ----------------------------------------------------
    // MODULE 1: SMART USN DETECTION (3 DIGITS AUTO-FILL)
    // ----------------------------------------------------
    const performUsnDetection = async () => {
        const classId = classSelect.value;
        const digits = usnDigitsInput.value.trim();

        if (!classId) {
            usnFeedbackText.textContent = '⚠️ Please choose your class & section first.';
            usnFeedbackText.style.color = 'var(--danger-accent)';
            classSelect.focus();
            return;
        }

        if (!digits || !/^\d{3}$/.test(digits)) {
            usnFeedbackText.textContent = '⚠️ Please enter exactly 3 numeric digits (e.g. 045, 123).';
            usnFeedbackText.style.color = 'var(--danger-accent)';
            usnDigitsInput.focus();
            return;
        }

        usnFeedbackText.textContent = '🔍 Detecting student profile...';
        usnFeedbackText.style.color = 'var(--accent-600)';

        try {
            const res = await API.get(`/api/public/student/detect-usn?classId=${encodeURIComponent(classId)}&digits=${encodeURIComponent(digits)}`);
            if (res && res.success && res.data) {
                const data = res.data;
                if (data.success && data.code === 'EXACT_MATCH') {
                    detectedStudentUsn = data.usn;
                    detectedName.textContent = data.studentName;
                    detectedUsnBadge.textContent = data.usn;
                    detectedDeptBadge.textContent = data.departmentCode || data.departmentName || 'Dept';
                    detectedClassBadge.textContent = `Sem ${data.semester} (${data.section})`;

                    detectedCard.style.display = 'block';
                    manualUsnGroup.style.display = 'none';
                    if (usnInput) usnInput.value = data.usn;

                    usnDigitsInput.disabled = true;
                    if (btnDetectUsn) btnDetectUsn.style.display = 'none';
                    usnFeedbackText.textContent = '✓ Verified: ' + data.studentName;
                    usnFeedbackText.style.color = 'var(--success-accent)';

                    // Check device authorization for this student
                    checkDeviceStatus(data.usn);

                } else if (data.code === 'MULTIPLE_MATCHES') {
                    detectedStudentUsn = null;
                    detectedCard.style.display = 'none';
                    manualUsnGroup.style.display = 'block';
                    usnFeedbackText.textContent = '⚠️ Multiple students match. Please enter complete USN below.';
                    usnFeedbackText.style.color = 'var(--danger-accent)';
                    if (usnInput) usnInput.focus();

                } else {
                    detectedStudentUsn = null;
                    detectedCard.style.display = 'none';
                    usnFeedbackText.textContent = '✕ ' + (data.message || 'No registered student found.');
                    usnFeedbackText.style.color = 'var(--danger-accent)';
                }
            }
        } catch (err) {
            usnFeedbackText.textContent = '✕ ' + (err.message || 'Failed to detect student.');
            usnFeedbackText.style.color = 'var(--danger-accent)';
        }
    };

    if (btnDetectUsn) {
        btnDetectUsn.addEventListener('click', performUsnDetection);
    }

    if (usnDigitsInput) {
        usnDigitsInput.addEventListener('input', (e) => {
            // Filter non-numeric input
            e.target.value = e.target.value.replace(/\D/g, '');
            if (e.target.value.length === 3 && classSelect.value) {
                performUsnDetection();
            }
        });

        usnDigitsInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                performUsnDetection();
            }
        });
    }

    if (btnChangeDigits) {
        btnChangeDigits.addEventListener('click', () => {
            detectedStudentUsn = null;
            detectedCard.style.display = 'none';
            manualUsnGroup.style.display = 'none';
            usnDigitsInput.disabled = false;
            usnDigitsInput.value = '';
            if (btnDetectUsn) btnDetectUsn.style.display = 'inline-block';
            usnFeedbackText.textContent = 'Enter only 3 numeric digits. Full institutional USN will be auto-resolved.';
            usnFeedbackText.style.color = 'var(--text-muted)';
            deviceBanner.style.display = 'none';
            usnDigitsInput.focus();
        });
    }

    // ----------------------------------------------------
    // MODULE 3: STRICT 5-MINUTE SESSION TIMER
    // ----------------------------------------------------
    const formatTime = (secs) => {
        const m = Math.floor(secs / 60);
        const s = secs % 60;
        return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
    };

    const startSessionTimer = (remainingSecs) => {
        if (timerInterval) clearInterval(timerInterval);
        sessionRemainingSeconds = remainingSecs;
        timerWidget.style.display = 'block';

        const updateDisplay = () => {
            if (sessionRemainingSeconds <= 0) {
                isSessionExpired = true;
                timerBadge.className = 'timer-badge-container closed';
                timerDot.textContent = '🔴';
                timerStatusText.textContent = 'Window Closed:';
                timerCountdownText.textContent = '00:00';
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.style.opacity = '0.6';
                    submitBtn.style.cursor = 'not-allowed';
                }
                clearInterval(timerInterval);
            } else {
                isSessionExpired = false;
                timerBadge.className = 'timer-badge-container open';
                timerDot.textContent = '🟢';
                timerStatusText.textContent = 'Attendance Open:';
                timerCountdownText.textContent = formatTime(sessionRemainingSeconds);
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.style.opacity = '1';
                    submitBtn.style.cursor = 'pointer';
                }
            }
        };

        updateDisplay();
        if (sessionRemainingSeconds > 0) {
            timerInterval = setInterval(() => {
                sessionRemainingSeconds--;
                updateDisplay();
            }, 1000);
        }
    };

    const loadSessionInfo = async (code) => {
        if (!code || code.length < 4) return;
        try {
            const res = await API.get(`/api/attendance/session-info/${encodeURIComponent(code)}`);
            if (res && res.success && res.data) {
                const sess = res.data;
                // If session has remainingSeconds provided by backend
                if (sess.remainingSeconds !== undefined && sess.remainingSeconds !== null) {
                    startSessionTimer(sess.remainingSeconds);
                } else if (sess.status === 'EXPIRED' || sess.status === 'COMPLETED') {
                    startSessionTimer(0);
                }
            }
        } catch (e) {
            console.warn('Session info lookup non-blocking error:', e);
        }
    };

    if (codeInput) {
        codeInput.addEventListener('input', (e) => {
            const code = e.target.value.trim().toUpperCase();
            if (code.length === 6) {
                loadSessionInfo(code);
            }
        });
    }

    // ----------------------------------------------------
    // INITIALIZATION & URL PARAMS
    // ----------------------------------------------------
    const urlParams = new URLSearchParams(window.location.search);
    const prefillCode = urlParams.get('code');
    if (prefillCode && codeInput) {
        codeInput.value = prefillCode.trim().toUpperCase();
        loadSessionInfo(codeInput.value);
    }

    // Load classes for dropdown
    const loadClasses = async () => {
        try {
            const res = await API.get('/api/public/classes');
            if (res && res.success && res.data) {
                classSelect.innerHTML = '<option value="">-- Choose Your Class --</option>';
                res.data.forEach(c => {
                    const opt = document.createElement('option');
                    opt.value = c.id;
                    opt.textContent = `${c.departmentCode} - Semester ${c.semester} (Section ${c.section})`;
                    classSelect.appendChild(opt);
                });
            }
        } catch (e) {
            console.error('Failed to load classes', e);
        }
    };

    await loadClasses();

    // ----------------------------------------------------
    // ATTENDANCE SUBMISSION HANDLER
    // ----------------------------------------------------
    const handleSubmit = async (e) => {
        if (e) e.preventDefault();

        if (isSessionExpired) {
            App.showToast('Attendance window closed: The 5-minute period for this session has expired.', 'danger');
            return;
        }

        const classId = classSelect.value;
        const finalUsn = detectedStudentUsn || (usnInput ? usnInput.value.trim().toUpperCase() : null);
        const code = codeInput.value.trim().toUpperCase();

        if (!classId) {
            App.showToast('Please choose your class from the dropdown.', 'warning');
            classSelect.focus();
            return;
        }

        if (!finalUsn) {
            App.showToast('Please enter your 3-digit roll number and verify your student profile.', 'warning');
            if (usnDigitsInput) usnDigitsInput.focus();
            return;
        }

        if (!code || code.length < 4) {
            App.showToast('Please enter the 6-character attendance code.', 'warning');
            codeInput.focus();
            return;
        }

        // Show loading & radar
        formSection.style.display = 'none';
        errorSection.style.display = 'none';
        successSection.style.display = 'none';
        loadingSection.style.display = 'block';

        radarCircle.classList.add('scanning');

        try {
            // Step 1: Capture GPS Coordinates with high accuracy
            const coords = await LocationService.getCurrentLocation((msg, state) => {
                statusText.textContent = msg;
            });

            statusText.textContent = 'Verifying classroom perimeter (40m), approved device & window...';

            // Step 2: Send payload with deviceToken & USN
            const payload = {
                usn: finalUsn,
                attendanceCode: code,
                classId: classId,
                latitude: coords.latitude,
                longitude: coords.longitude,
                accuracy: coords.accuracy,
                deviceToken: currentDeviceToken
            };

            const response = await API.post('/api/attendance/mark', payload);

            // Step 3: Success! Render Success Card
            loadingSection.style.display = 'none';
            renderSuccess(response.data);

        } catch (err) {
            loadingSection.style.display = 'none';
            renderError(err.message || 'Unable to verify your attendance. Please try again.');
        }
    };

    const renderSuccess = (data) => {
        document.getElementById('res-student-name').textContent = data.studentName;
        document.getElementById('res-usn').textContent = data.usn;
        document.getElementById('res-department').textContent = data.department;
        document.getElementById('res-semester').textContent = `Sem ${data.semester} (${data.section})`;
        document.getElementById('res-subject').textContent = data.subject;
        document.getElementById('res-datetime').textContent = data.dateTime;
        document.getElementById('res-distance').textContent = data.distanceFromTeacher;
        document.getElementById('res-status').textContent = data.attendanceStatus;

        successSection.style.display = 'block';
    };

    const renderError = (msg) => {
        errorMessage.textContent = msg;
        errorSection.style.display = 'block';
    };

    if (submitBtn) {
        submitBtn.addEventListener('click', handleSubmit);
    }

    if (retryBtn) {
        retryBtn.addEventListener('click', handleSubmit);
    }

    if (restartBtn) {
        restartBtn.addEventListener('click', () => {
            errorSection.style.display = 'none';
            successSection.style.display = 'none';
            formSection.style.display = 'block';
        });
    }
});
