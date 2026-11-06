// Run after docker compose -p atleta-beta-check -f docker-compose.beta-check.yml up --build -d.
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const container = 'atleta-beta-check-backend-1';
const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', timeout: 15000 }).trim();
const inspect = () => JSON.parse(docker('inspect', container))[0];
const config = inspect().HostConfig;
assert.equal(config.Memory, 512 * 1024 * 1024, 'Expected a 512 MiB RAM limit');
assert.equal(config.MemorySwap, config.Memory, 'Swap must be disabled');
assert.equal(config.NanoCpus, 100000000, 'Expected a 0.1 CPU quota');

const deadline = Date.now() + 600000;
let ready = false;
let lastReport = 0;
while (Date.now() < deadline) {
  const state = inspect().State;
  assert.equal(state.Running, true, `Backend stopped: ${state.Error || state.ExitCode}`);
  try {
    const response = await fetch('http://127.0.0.1:18080/actuator/health/readiness', {
      signal: AbortSignal.timeout(2000),
    });
    ready = response.ok && (await response.json()).status === 'UP';
  } catch { /* Cold startup under 0.1 CPU is expected to take several minutes. */ }
  if (ready) break;
  if (Date.now() - lastReport >= 30000) {
    console.log('Waiting for beta readiness (512 MiB, 0.1 CPU)...');
    lastReport = Date.now();
  }
  await new Promise(resolve => setTimeout(resolve, 3000));
}
assert.ok(ready, 'Backend failed to become ready in ten minutes');
const smoke = JSON.parse(execFileSync(process.execPath, [fileURLToPath(new URL('./smoke-beta.mjs', import.meta.url))], {
  encoding: 'utf8', timeout: 300000,
  env: { ...process.env, BETA_TEST_URL: 'http://127.0.0.1:18080' },
}).trim());
const readCgroup = name => docker('exec', container, 'cat', `/sys/fs/cgroup/${name}`);
const events = Object.fromEntries(readCgroup('memory.events').split('\n').map(line => {
  const [key, value] = line.split(/\s+/); return [key, Number(value)];
}));
const result = {
  result: 'PASS', memoryLimitMiB: config.Memory / 1048576, cpuQuota: config.NanoCpus / 1e9,
  startupSeconds: Number(docker('logs', container).match(/Started ServerAtletaApplication in ([\d.]+) seconds/)?.[1]) || null,
  memoryCurrentMiB: Math.round(Number(readCgroup('memory.current')) / 104857.6) / 10,
  memoryPeakMiB: Math.round(Number(readCgroup('memory.peak')) / 104857.6) / 10,
  memoryEvents: events, restarts: inspect().RestartCount, smoke,
};
assert.equal(events.oom, 0, 'OOM events detected');
assert.equal(events.oom_kill, 0, 'OOM kills detected');
assert.equal(result.restarts, 0, 'Backend restarted during verification');
assert.equal(inspect().State.OOMKilled, false);
console.log(JSON.stringify(result, null, 2));
