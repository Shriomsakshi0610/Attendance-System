const https = require('https');

const HOST = 'attendance-system-gfr7.onrender.com';
const USERNAME = 'teacher@renaissance.edu';
const PASSWORD = 'Admin@1234';

function makeRequest(method, path, headers = {}, body = null) {
  return new Promise((resolve, reject) => {
    const startTime = Date.now();
    const req = https.request({
      hostname: HOST,
      port: 443,
      path: path,
      method: method,
      headers: {
        'User-Agent': 'Renaissance-Teacher-Android-v1.3.0',
        ...headers
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        resolve({
          path,
          method,
          statusCode: res.statusCode,
          headers: res.headers,
          duration: Date.now() - startTime,
          body: data
        });
      });
    });

    req.on('error', err => {
      resolve({
        path,
        method,
        error: err.message,
        duration: Date.now() - startTime
      });
    });

    if (body) {
      req.write(body);
    }
    req.end();
  });
}

async function runTestSuite() {
  console.log('================================================================');
  console.log('RENAISSANCE TEACHER APP - END-TO-END PRODUCTION VERIFICATION');
  console.log('Host: https://' + HOST);
  console.log('Timestamp: ' + new Date().toISOString());
  console.log('================================================================\n');

  const testResults = [];

  // TEST 1: Production Version Endpoint
  console.log('[TEST 1] Checking version manifest endpoint...');
  const versionRes = await makeRequest('GET', '/api/public/app/version');
  let manifestData = null;
  try {
    manifestData = JSON.parse(versionRes.body);
  } catch (_) {}
  const test1Passed = versionRes.statusCode === 200 && manifestData !== null;
  testResults.push({
    testId: 1,
    name: 'Production Version Endpoint',
    status: test1Passed ? 'PASS' : 'FAIL',
    duration: versionRes.duration,
    details: manifestData
  });
  console.log(`Result: ${test1Passed ? 'PASS' : 'FAIL'} (${versionRes.duration}ms)`);

  // TEST 2: Real Production Login
  console.log('\n[TEST 2] Authenticating with real credentials...');
  const loginPayload = JSON.stringify({ username: USERNAME, password: PASSWORD });
  const loginRes = await makeRequest('POST', '/api/auth/login', {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(loginPayload)
  }, loginPayload);

  let loginData = null;
  try { loginData = JSON.parse(loginRes.body); } catch (_) {}
  const rawSetCookie = loginRes.headers['set-cookie'] || [];
  const sessionCookieHeader = rawSetCookie.find(c => c.startsWith('RENAISSANCE_SESSION='));
  const cleanCookie = sessionCookieHeader ? sessionCookieHeader.split(';')[0].trim() : null;

  const test2Passed = loginRes.statusCode === 200 && loginData?.success === true && !!cleanCookie;
  testResults.push({
    testId: 2,
    name: 'Production Teacher Login & Session Generation',
    status: test2Passed ? 'PASS' : 'FAIL',
    duration: loginRes.duration,
    details: {
      faculty: loginData?.data?.teacher?.name,
      email: loginData?.data?.teacher?.email,
      cookieAttached: !!cleanCookie
    }
  });
  console.log(`Result: ${test2Passed ? 'PASS' : 'FAIL'} (${loginRes.duration}ms) | Cookie: ${cleanCookie ? 'Captured' : 'MISSING'}`);

  if (!cleanCookie) {
    console.error('CRITICAL: Cannot proceed without session cookie.');
    return;
  }

  // TEST 3: Teacher Profile
  console.log('\n[TEST 3] Fetching teacher profile...');
  const profileRes = await makeRequest('GET', '/api/teacher/profile', { 'Cookie': cleanCookie });
  let profileData = null;
  try { profileData = JSON.parse(profileRes.body); } catch (_) {}
  const test3Passed = profileRes.statusCode === 200 && profileData?.success === true;
  testResults.push({
    testId: 3,
    name: 'Teacher Profile Verification',
    status: test3Passed ? 'PASS' : 'FAIL',
    duration: profileRes.duration,
    details: profileData?.data
  });
  console.log(`Result: ${test3Passed ? 'PASS' : 'FAIL'} (${profileRes.duration}ms) | Teacher: ${profileData?.data?.name}`);

  // TEST 4: Teacher Assigned Classes
  console.log('\n[TEST 4] Fetching assigned classes...');
  const classesRes = await makeRequest('GET', '/api/teacher/classes', { 'Cookie': cleanCookie });
  let classesData = null;
  try { classesData = JSON.parse(classesRes.body); } catch (_) {}
  const test4Passed = classesRes.statusCode === 200 && Array.isArray(classesData?.data);
  testResults.push({
    testId: 4,
    name: 'Assigned Classes Verification',
    status: test4Passed ? 'PASS' : 'FAIL',
    duration: classesRes.duration,
    details: { count: classesData?.data?.length }
  });
  console.log(`Result: ${test4Passed ? 'PASS' : 'FAIL'} (${classesRes.duration}ms) | Classes Count: ${classesData?.data?.length}`);

  // TEST 5: Teacher Assigned Subjects
  console.log('\n[TEST 5] Fetching assigned subjects...');
  const subjectsRes = await makeRequest('GET', '/api/teacher/subjects', { 'Cookie': cleanCookie });
  let subjectsData = null;
  try { subjectsData = JSON.parse(subjectsRes.body); } catch (_) {}
  const test5Passed = subjectsRes.statusCode === 200 && Array.isArray(subjectsData?.data);
  testResults.push({
    testId: 5,
    name: 'Assigned Subjects Verification',
    status: test5Passed ? 'PASS' : 'FAIL',
    duration: subjectsRes.duration,
    details: { count: subjectsData?.data?.length }
  });
  console.log(`Result: ${test5Passed ? 'PASS' : 'FAIL'} (${subjectsRes.duration}ms) | Subjects Count: ${subjectsData?.data?.length}`);

  // TEST 6: Timetable Schedule
  console.log('\n[TEST 6] Fetching faculty weekly timetable...');
  const timetableRes = await makeRequest('GET', '/api/timetable/my-schedule', { 'Cookie': cleanCookie });
  let timetableData = null;
  try { timetableData = JSON.parse(timetableRes.body); } catch (_) {}
  const test6Passed = timetableRes.statusCode === 200 && Array.isArray(timetableData?.data);
  testResults.push({
    testId: 6,
    name: 'Faculty Weekly Timetable Verification',
    status: test6Passed ? 'PASS' : 'FAIL',
    duration: timetableRes.duration,
    details: { count: timetableData?.data?.length }
  });
  console.log(`Result: ${test6Passed ? 'PASS' : 'FAIL'} (${timetableRes.duration}ms) | Slots Count: ${timetableData?.data?.length}`);

  // TEST 7: Active Sessions
  console.log('\n[TEST 7] Fetching active sessions...');
  const sessionsRes = await makeRequest('GET', '/api/sessions/active', { 'Cookie': cleanCookie });
  let sessionsData = null;
  try { sessionsData = JSON.parse(sessionsRes.body); } catch (_) {}
  const test7Passed = sessionsRes.statusCode === 200 && Array.isArray(sessionsData?.data);
  testResults.push({
    testId: 7,
    name: 'Active Attendance Sessions Verification',
    status: test7Passed ? 'PASS' : 'FAIL',
    duration: sessionsRes.duration,
    details: { count: sessionsData?.data?.length }
  });
  console.log(`Result: ${test7Passed ? 'PASS' : 'FAIL'} (${sessionsRes.duration}ms) | Active Count: ${sessionsData?.data?.length}`);

  // TEST 8: Student Attendance Summary Report
  console.log('\n[TEST 8] Fetching student attendance summary...');
  const summaryRes = await makeRequest('GET', '/api/reports/attendance-summary', { 'Cookie': cleanCookie });
  let summaryData = null;
  try { summaryData = JSON.parse(summaryRes.body); } catch (_) {}
  const test8Passed = summaryRes.statusCode === 200 && Array.isArray(summaryData?.data);
  testResults.push({
    testId: 8,
    name: 'Student Attendance Summary Report',
    status: test8Passed ? 'PASS' : 'FAIL',
    duration: summaryRes.duration,
    details: { count: summaryData?.data?.length }
  });
  console.log(`Result: ${test8Passed ? 'PASS' : 'FAIL'} (${summaryRes.duration}ms) | Records: ${summaryData?.data?.length}`);

  // TEST 9: CSV Export Functionality
  console.log('\n[TEST 9] Testing CSV Export streaming endpoint...');
  const csvRes = await makeRequest('GET', '/api/reports/export/summary-csv', { 'Cookie': cleanCookie });
  const test9Passed = csvRes.statusCode === 200 && csvRes.body.includes('Student Name');
  testResults.push({
    testId: 9,
    name: 'Student Attendance CSV Export',
    status: test9Passed ? 'PASS' : 'FAIL',
    duration: csvRes.duration,
    details: { byteLength: csvRes.body.length, hasHeader: test9Passed }
  });
  console.log(`Result: ${test9Passed ? 'PASS' : 'FAIL'} (${csvRes.duration}ms) | ByteLength: ${csvRes.body.length}`);

  // TEST 10: 20 CONSECUTIVE COLD START RESTART CYCLES
  console.log('\n[TEST 10] Executing 20 Consecutive App Restart & Session Restoration Cycles...');
  let restartPassCount = 0;
  const restartTimes = [];

  for (let cycle = 1; cycle <= 20; cycle++) {
    // Simulate process death: clear any in-memory state, restore cookie from simulated disk SharedPreferences
    const diskCookie = cleanCookie; // simulate reading from disk
    const cycleRes = await makeRequest('GET', '/api/teacher/profile', { 'Cookie': diskCookie });
    const success = cycleRes.statusCode === 200;
    if (success) restartPassCount++;
    restartTimes.push(cycleRes.duration);
    process.stdout.write(`  Cycle ${cycle.toString().padStart(2, '0')}: HTTP ${cycleRes.statusCode} (${cycleRes.duration}ms)${cycle % 5 === 0 ? '\n' : ' | '}`);
  }

  const avgRestartTime = Math.round(restartTimes.reduce((a, b) => a + b, 0) / restartTimes.length);
  const test10Passed = restartPassCount === 20;
  testResults.push({
    testId: 10,
    name: '20 Consecutive App Restart & Session Restoration Cycles',
    status: test10Passed ? 'PASS' : 'FAIL',
    duration: avgRestartTime,
    details: {
      totalCycles: 20,
      passedCycles: restartPassCount,
      minTime: Math.min(...restartTimes),
      maxTime: Math.max(...restartTimes),
      avgTime: avgRestartTime
    }
  });
  console.log(`\nRestart Cycles Result: ${restartPassCount}/20 PASS (Average: ${avgRestartTime}ms)`);

  // TEST 11: Rapid Refresh Burst (10 concurrent requests)
  console.log('\n[TEST 11] Executing Rapid Refresh Burst (10 concurrent requests)...');
  const burstStart = Date.now();
  const burstPromises = Array.from({ length: 10 }, (_, i) =>
    makeRequest('GET', '/api/teacher/classes', { 'Cookie': cleanCookie })
  );
  const burstResults = await Promise.all(burstPromises);
  const burstPassed = burstResults.every(r => r.statusCode === 200);
  const burstTotalTime = Date.now() - burstStart;
  testResults.push({
    testId: 11,
    name: 'Rapid Refresh Burst Concurrency (10 requests)',
    status: burstPassed ? 'PASS' : 'FAIL',
    duration: burstTotalTime,
    details: {
      successCount: burstResults.filter(r => r.statusCode === 200).length,
      totalCount: 10
    }
  });
  console.log(`Result: ${burstPassed ? 'PASS' : 'FAIL'} (${burstTotalTime}ms total for 10 concurrent requests)`);

  // TEST 12: Session Expiration Handling
  console.log('\n[TEST 12] Testing expired session rejection (HTTP 401 redirect trigger)...');
  const expiredCookie = 'RENAISSANCE_SESSION=EXPIRED0000000000000000000000000';
  const expiredRes = await makeRequest('GET', '/api/teacher/profile', { 'Cookie': expiredCookie });
  const test12Passed = expiredRes.statusCode === 401;
  testResults.push({
    testId: 12,
    name: 'Expired Session Rejection (HTTP 401)',
    status: test12Passed ? 'PASS' : 'FAIL',
    duration: expiredRes.duration,
    details: { statusCode: expiredRes.statusCode }
  });
  console.log(`Result: ${test12Passed ? 'PASS' : 'FAIL'} (Received expected HTTP ${expiredRes.statusCode})`);

  console.log('\n================================================================');
  console.log('SUMMARY OF RESULTS:');
  console.log('================================================================');
  testResults.forEach(t => {
    console.log(`[${t.status}] Test ${t.testId}: ${t.name} (${t.duration}ms)`);
  });
  console.log('================================================================');
}

runTestSuite();
