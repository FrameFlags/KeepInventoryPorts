// Minimal bot used by the 1.12.2 Forge behavior cell. It only keeps a real
// player connection open while the server-side DeathProbe runs its scenario.
const mineflayer = require('mineflayer');

const bot = mineflayer.createBot({
  host: '127.0.0.1',
  port: 25565,
  username: 'KIPProbeBot',
  version: '1.12.2',
});

const timeout = setTimeout(() => {
  console.log('KIP_BOT_TIMEOUT');
  bot.quit();
  process.exit(0);
}, 120000);

bot.on('spawn', () => console.log('KIP_BOT_SPAWNED'));
bot.on('error', (err) => {
  console.error('KIP_BOT_ERROR', err && err.message ? err.message : err);
  clearTimeout(timeout);
  process.exit(1);
});
bot.on('end', () => {
  clearTimeout(timeout);
  process.exit(0);
});
