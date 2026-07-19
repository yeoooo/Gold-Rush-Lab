import exec from 'k6/execution';

import { mine } from '../lib/api.js';
import { checkMine } from '../lib/checks.js';
import { config } from '../lib/config.js';
import { setup as commonSetup } from '../lib/setup.js';

export const options = {
    setupTimeout: '5m',
    scenarios: {
        hotspot: {
            executor: 'per-vu-iterations',
            vus: config.userCount,
            iterations: 1,
            maxDuration: '1m',
        },
    },
    thresholds: {
        checks: ['rate==1'],
        http_req_failed: ['rate==0'],
    },
};

export function setup() {
    return commonSetup({
        userCount: config.userCount,
        mineId: config.hotspotMineId,
    });
}

export default function (data) {
    const sessionId = data.sessions[exec.vu.idInTest - 1];
    checkMine(mine(sessionId));
}
