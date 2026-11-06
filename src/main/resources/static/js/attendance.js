/**
 * Renaissance - Student Attendance Logic
 */

document.addEventListener('DOMContentLoaded', async () => {
    const classSelect = document.getElementById('class-select');
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

    // 1. Check URL parameters for prefilled code or class
    const urlParams = new URLSearchParams(window.location.search);
    const prefillCode = urlParams.get('code');
    if (prefillCode && codeInput) {
        codeInput.value = prefillCode.trim().toUpperCase();
    }

    // 2. Load classes for "Choose Your Class" dropdown
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

    // 3. Handle Attendance Submission
    const handleSubmit = async (e) => {
        if (e) e.preventDefault();

        const usn = usnInput.value.trim().toUpperCase();
        const code = codeInput.value.trim().toUpperCase();
        const classId = classSelect.value;

        if (!classId) {
            App.showToast('Please choose your class from the dropdown.', 'warning');
            classSelect.focus();
            return;
        }

        if (!usn) {
            App.showToast('Please enter your USN.', 'warning');
            usnInput.focus();
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
            // Step 1: Capture GPS Coordinates with browser permission
            const coords = await LocationService.getCurrentLocation((msg, state) => {
                statusText.textContent = msg;
            });

            statusText.textContent = 'Verifying 15-meter geofence with server...';

            // Step 2: Send payload to backend
            const payload = {
                usn: usn,
                attendanceCode: code,
                classId: parseInt(classId, 10),
                latitude: coords.latitude,
                longitude: coords.longitude,
                accuracy: coords.accuracy
            };

            const response = await API.post('/api/attendance/mark', payload);

            // Step 3: Success! Render Success Card
            loadingSection.style.display = 'none';
            renderSuccess(response.data);

        } catch (err) {
            loadingSection.style.display = 'none';
            renderError(err.message || 'Unable to verify your location. Please try again.');
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
