import exec from 'k6/execution';

import { mine } from '../lib/api.js';
import { checkMine } from '../lib/checks.js';
import { config } from '../lib/config.js';
import { setup as commonSetup } from '../lib/setup.js';
import { sessionFor } from '../lib/utils.js';

export const options = {
    setupTimeout: '5m',
    scenarios: {
        soak: {
            executor: 'constant-vus',
            vus: config.soakVus,
            duration: config.soakDuration,
            gracefulStop: '30s',
        },
    },
    thresholds: {
        'checks{operation:mine}': ['rate>0.99'],
        'http_req_failed{name:mine}': ['rate<0.01'],
        'http_req_duration{name:mine}': ['p(95)<1000'],
    },
};

export function setup() {
    return commonSetup({ userCount: Math.max(config.userCount, config.soakVus) });
}

export default function (data) {
    const sessionId = sessionFor(data, exec.vu.idInTest - 1);
    checkMine(mine(sessionId));
}
