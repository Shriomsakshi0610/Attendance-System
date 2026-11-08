const https = require('https');
const fs = require('fs');

const file = fs.createWriteStream('C:/Users/Shrio/Attendance-System/scratch/render_downloaded.apk');

https.get('https://attendance-system-gfr7.onrender.com/api/public/app/download', (res) => {
  console.log('Status:', res.statusCode);
  console.log('Headers:', res.headers);
  res.pipe(file);
  file.on('finish', () => {
    file.close();
    console.log('Finished saving scratch/render_downloaded.apk');
  });
}).on('error', (err) => {
  console.error('Error:', err);
});
