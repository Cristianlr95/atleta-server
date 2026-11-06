// Creates one test account: use a disposable local database, never real user data.
import assert from 'node:assert/strict';
const base = process.env.BETA_TEST_URL || 'http://127.0.0.1:18080';
assert(['127.0.0.1', 'localhost'].includes(new URL(base).hostname), 'Local verification only');
const invite = process.env.BETA_INVITE_CODE || '';
assert.ok(invite.length >= 20, 'BETA_INVITE_CODE is required for beta registration');
const timings = [];
async function request(path, { method = 'GET', body, token, expected = 200, headers = {} } = {}) {
  const start = performance.now();
  const response = await fetch(`${base}${path}`, {
    method, signal: AbortSignal.timeout(30000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: body ? JSON.stringify(body) : undefined,
  });
  assert.equal(response.status, expected, `${method} ${path}: ${await response.clone().text()}`);
  timings.push(performance.now() - start);
  return response;
}
const email = `beta-${Date.now()}@example.test`;
const password = 'LocalBetaTest!2026';
await request('/actuator/health');
await request('/api/v1/athletes/register', {
  method: 'POST', expected: 201, headers: { 'X-Beta-Invite': invite },
  body: { email, password, nombre: 'Beta Verification', genero: 'MASCULINO' },
});
const auth = await (await request('/api/v1/athletes/login', { method: 'POST', body: { email, password } })).json();
const token = auth.accessToken;
await request('/api/v1/player-profiles', {
  method: 'POST', expected: 201, token,
  body: { atletaUuid: auth.atletaUuid, alias: `beta-${Date.now()}`, genero: 'MASCULINO' },
});
const path = `/api/v1/player-profiles/${auth.atletaUuid}`;
const team = await (await request('/api/v1/teams', {
  method: 'POST', expected: 201, token,
  body: { nombre: `Beta Team ${Date.now()}`, creadorUuid: auth.atletaUuid },
})).json();
assert.ok(team.id, 'Team ID missing');
await request(`/api/v1/teams/${team.id}`, { token });
await request(`/api/v1/teams/${team.id}/members/active`, { token });
await request(`/api/v1/teams/by-player/${auth.atletaUuid}`, { token });
const match = await (await request('/api/v1/matches', {
  method: 'POST', expected: 201, token,
  body: {
    modalidad: 'CINCO_VS_CINCO', creadorUuid: auth.atletaUuid,
    fechaHoraProgramada: new Date(Date.now() + 86400000).toISOString().slice(0, 19),
    latitud: -33.4489, longitud: -70.6693, cuota: 0,
  },
})).json();
assert.ok(match.id, 'Match ID missing');
await request(`/api/v1/matches/${match.id}`, { token });
await request(`/api/v1/matches/by-player-or-creator/${auth.atletaUuid}`, { token });
// Four concurrent workers, 100 authenticated profile reads each.
await Promise.all(Array.from({ length: 4 }, async () => {
  for (let i = 0; i < 100; i++) await request(path, { token });
}));
await request('/actuator/health/readiness');
await request('/api/v1/teams/logo', { method: 'POST', token, expected: 503 });
await request(`${path}/picture`, { method: 'POST', token, expected: 503 });
await request('/actuator/metrics', { expected: 401 });
const cors = await request('/api/v1/athletes/login', {
  method: 'OPTIONS', headers: { Origin: 'https://atleta-beta.pages.dev', 'Access-Control-Request-Method': 'POST' },
});
assert.equal(cors.headers.get('access-control-allow-origin'), 'https://atleta-beta.pages.dev');
await request('/api/v1/athletes/login', {
  method: 'OPTIONS', expected: 403,
  headers: { Origin: 'https://untrusted.example', 'Access-Control-Request-Method': 'POST' },
});
const refreshed = await (await request('/api/v1/athletes/auth/refresh', {
  method: 'POST', body: { refreshToken: auth.refreshToken },
})).json();
assert.ok(refreshed.accessToken && refreshed.refreshToken, 'Session rotation failed');
await request('/api/v1/athletes/auth/logout', {
  method: 'POST', expected: 204, body: { refreshToken: refreshed.refreshToken },
});
await request(path, { token: refreshed.accessToken, expected: 401 });
timings.sort((a, b) => a - b);
console.log(JSON.stringify({ requests: timings.length, concurrency: 4, p95Ms: Math.round(timings[Math.floor(timings.length * 0.95)]), maxMs: Math.round(timings.at(-1)), result: 'PASS' }));
