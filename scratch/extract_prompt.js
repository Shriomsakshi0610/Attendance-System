const fs = require('fs');
const readline = require('readline');

const rl = readline.createInterface({
  input: fs.createReadStream('C:/Users/Shrio/.gemini/antigravity/brain/3b35cddd-eaad-4ea0-972a-0c9bd2be3f67/.system_generated/logs/transcript_full.jsonl')
});

rl.on('line', (line) => {
  if (line.includes('"step_index":5531')) {
    const obj = JSON.parse(line);
    fs.writeFileSync('C:/Users/Shrio/Attendance-System/scratch/user_prompt_10.txt', obj.content, 'utf8');
    console.log('Saved user prompt 10, length:', obj.content.length);
    process.exit(0);
  }
});
