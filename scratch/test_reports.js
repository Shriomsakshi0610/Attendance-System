const https = require('https');

function request(path, cookie) {
  return new Promise((resolve) => {
    const start = Date.now();
    const req = https.request({
      hostname: 'attendance-system-gfr7.onrender.com',
      port: 443,
      path: path,
      method: 'GET',
      headers: {
        'Cookie': cookie,
        'User-Agent': 'Renaissance-Teacher-Android'
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        resolve({
          path,
          status: res.statusCode,
          duration: Date.now() - start,
          dataLength: data.length,
          preview: data.substring(0, 150)
        });
      });
    });
    req.on('error', err => resolve({ path, error: err.message }));
    req.end();
  });
}

async function run() {
  const cookie = 'RENAISSANCE_SESSION=3C940DEE9D949E9D9ADEA955A613085B';
  const endpoints = [
    '/api/reports/attendance-summary',
    '/api/reports/datewise',
    '/api/reports/export/summary-csv'
  ];

  for (const ep of endpoints) {
    const res = await request(ep, cookie);
    console.log(res);
  }
}

run();
