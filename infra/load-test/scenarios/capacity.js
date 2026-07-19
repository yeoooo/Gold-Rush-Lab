import exec from 'k6/execution';

import { mine } from '../lib/api.js';
import { checkMine } from '../lib/checks.js';
import { setup as commonSetup } from '../lib/setup.js';
import { sessionFor } from '../lib/utils.js';

export const options = {
    setupTimeout: '5m',
    scenarios: {
        capacity: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 1000,
            stages: [
                { target: 10, duration: '10s' },
                { target: 10, duration: '30s' },
                { target: 30, duration: '10s' },
                { target: 30, duration: '30s' },
                { target: 50, duration: '10s' },
                { target: 50, duration: '30s' },
                { target: 100, duration: '10s' },
                { target: 100, duration: '30s' },
                { target: 200, duration: '10s' },
                { target: 200, duration: '30s' },
                { target: 400, duration: '10s' },
                { target: 400, duration: '30s' },
                { target: 0, duration: '10s' },
            ],
        },
    },
    thresholds: {
        'checks{operation:mine}': ['rate>0.99'],
        'http_req_failed{name:mine}': ['rate<0.01'],
        'http_req_duration{name:mine}': ['p(95)<500', 'p(99)<1000'],
    },
};

export function setup() {
    return commonSetup();
}

export default function (data) {
    const sessionId = sessionFor(data, exec.scenario.iterationInTest);
    checkMine(mine(sessionId));
}
