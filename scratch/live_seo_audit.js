const https = require('https');

function fetch(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)' } }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve({ statusCode: res.statusCode, headers: res.headers, body: data }));
        }).on('error', reject);
    });
}

async function run() {
    console.log('=== LIVE SEO AUDIT ===\n');

    // 1. Audit https://www.renaissanceattendance.site/
    const liveWww = await fetch('https://www.renaissanceattendance.site/');
    console.log('[1] https://www.renaissanceattendance.site/');
    console.log('Status:', liveWww.statusCode);
    console.log('Headers:', {
        'content-type': liveWww.headers['content-type'],
        'cache-control': liveWww.headers['cache-control'],
        'server': liveWww.headers['server'],
        'location': liveWww.headers['location']
    });

    const body = liveWww.body;
    const headOpenIdx = body.indexOf('<head>');
    const headCloseIdx = body.indexOf('</head>');
    const headContent = body.substring(headOpenIdx, headCloseIdx + 7);

    const insIdx = body.indexOf('<ins');
    const canonicalIdx = body.indexOf('rel="canonical"');
    const ogUrlIdx = body.indexOf('property="og:url"');
    const jsonLdIdx = body.indexOf('application/ld+json');

    console.log('\n--- HEAD ELEMENT PLACEMENT ---');
    console.log('<head> starts at:', headOpenIdx);
    console.log('<ins> tag starts at:', insIdx);
    console.log('rel="canonical" starts at:', canonicalIdx);
    console.log('</head> closes at:', headCloseIdx);
    console.log('Is <ins> tag inside <head>?', insIdx > headOpenIdx && insIdx < headCloseIdx);
    console.log('Is <ins> tag placed BEFORE rel="canonical"?', insIdx < canonicalIdx);

    // Extract canonical tag line
    const canonicalLine = body.match(/<link[^>]*rel=["']canonical["'][^>]*>/i);
    console.log('\nCanonical Tag in Source:');
    console.log(canonicalLine ? canonicalLine[0] : 'NOT FOUND');

    // Extract JSON-LD
    const jsonLdMatch = body.match(/<script type=["']application\/ld\+json["']>([\s\S]*?)<\/script>/i);
    console.log('\nJSON-LD in Source:');
    console.log(jsonLdMatch ? jsonLdMatch[1].trim() : 'NOT FOUND');

    // Extract Open Graph URL
    const ogUrlMatch = body.match(/<meta[^>]*property=["']og:url["'][^>]*>/i);
    console.log('\nOpen Graph URL:');
    console.log(ogUrlMatch ? ogUrlMatch[0] : 'NOT FOUND');

    // 2. Audit non-www
    console.log('\n----------------------------------------');
    console.log('[2] https://renaissanceattendance.site/');
    const liveNonWww = await fetch('https://renaissanceattendance.site/');
    console.log('Status:', liveNonWww.statusCode);
    console.log('Location:', liveNonWww.headers['location']);

    // 3. Audit sitemap
    console.log('\n----------------------------------------');
    console.log('[3] https://www.renaissanceattendance.site/sitemap.xml');
    try {
        const sitemap = await fetch('https://www.renaissanceattendance.site/sitemap.xml');
        console.log('Status:', sitemap.statusCode);
        console.log('Content:\n', sitemap.body.trim());
    } catch (e) {
        console.log('Error fetching sitemap:', e.message);
    }

    // 4. Audit robots.txt
    console.log('\n----------------------------------------');
    console.log('[4] https://www.renaissanceattendance.site/robots.txt');
    try {
        const robots = await fetch('https://www.renaissanceattendance.site/robots.txt');
        console.log('Status:', robots.statusCode);
        console.log('Content:\n', robots.body.trim());
    } catch (e) {
        console.log('Error fetching robots.txt:', e.message);
    }
}

run().catch(console.error);
