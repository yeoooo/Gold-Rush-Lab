import exec from 'k6/execution';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

import { mine } from '../../lib/api.js';
import { checkMine } from '../../lib/checks.js';
import { config } from '../../lib/config.js';
import { setup as commonSetup } from '../../lib/setup.js';

const requests = new Counter('benchmark_requests');
const errors = new Rate('benchmark_errors');
const latency = new Trend('benchmark_latency', true);
const mineId = new Gauge('benchmark_mine_id');
const startedAt = new Gauge('benchmark_started_at_ms');
const finishedAt = new Gauge('benchmark_finished_at_ms');

let vuStarted = false;

export const options = {
    setupTimeout: '10m',
    scenarios: {
        hotspot: {
            executor: 'per-vu-iterations',
            vus: config.userCount,
            iterations: config.iterations,
            maxDuration: config.hotspotMaxDuration,
        },
    },
    summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'min', 'max'],
};

export function setup() {
    const data = commonSetup({ userCount: config.userCount });
    mineId.add(data.mineId);
    return data;
}

export default function (data) {
    if (!vuStarted) {
        startedAt.add(Date.now());
        vuStarted = true;
    }

    const sessionId = data.sessions[exec.vu.idInTest - 1];
    const response = mine(sessionId);
    const valid = checkMine(response);

    requests.add(1);
    errors.add(!valid);
    latency.add(response.timings.duration);
    finishedAt.add(Date.now());
}
