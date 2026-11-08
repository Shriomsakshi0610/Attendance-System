const https = require('https');
const fs = require('fs');

async function testDownload() {
  console.log('Testing GET https://attendance-system-gfr7.onrender.com/api/public/app/download ...');
  const start = Date.now();

  const req = https.request({
    hostname: 'attendance-system-gfr7.onrender.com',
    port: 443,
    path: '/api/public/app/download',
    method: 'GET',
    headers: {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 14) Renaissance-Teacher-Android',
      'Accept': '*/*'
    }
  }, (res) => {
    console.log('Status Code:', res.statusCode);
    console.log('Headers:', res.headers);

    let bytesReceived = 0;
    const chunks = [];

    res.on('data', (chunk) => {
      bytesReceived += chunk.length;
      if (bytesReceived <= 1024 * 100) {
        // save initial chunks for inspection
        chunks.push(chunk);
      }
      if (bytesReceived % (1024 * 1024) < chunk.length) {
        console.log(`Received: ${(bytesReceived / (1024 * 1024)).toFixed(2)} MB`);
      }
    });

    res.on('end', () => {
      const duration = Date.now() - start;
      console.log(`Download finished in ${duration}ms! Total bytes: ${bytesReceived}`);
      const initial = Buffer.concat(chunks).slice(0, 500);
      console.log('First 50 bytes (hex):', initial.slice(0, 50).toString('hex'));
      console.log('First 200 bytes (text):', initial.slice(0, 200).toString('utf8'));
    });
  });

  req.on('error', (err) => {
    console.error('Request error:', err);
  });

  req.end();
}

testDownload();
