import exec from 'k6/execution';

import { mine } from '../lib/api.js';
import { checkMine } from '../lib/checks.js';
import { config } from '../lib/config.js';
import { setup as commonSetup } from '../lib/setup.js';
import { sessionFor } from '../lib/utils.js';

const maxVus = config.stressMaxVu;
const stageVus = (ratio) => Math.max(1, Math.round(maxVus * ratio));

export const options = {
    setupTimeout: '5m',
    scenarios: {
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            gracefulRampDown: '10s',
            stages: [
                { target: stageVus(0.1), duration: '1m' },
                { target: stageVus(0.3), duration: '1m' },
                { target: stageVus(0.5), duration: '1m' },
                { target: maxVus, duration: '1m' },
                { target: maxVus, duration: '2m' },
                { target: 0, duration: '1m' },
            ],
        },
    },
    thresholds: {
        'checks{operation:mine}': ['rate>0.95'],
        'http_req_failed{name:mine}': ['rate<0.05'],
        'http_req_duration{name:mine}': ['p(95)<2000'],
    },
};

export function setup() {
    return commonSetup({ userCount: Math.max(config.userCount, maxVus) });
}

export default function (data) {
    const sessionId = sessionFor(data, exec.vu.idInTest - 1);
    checkMine(mine(sessionId));
}
