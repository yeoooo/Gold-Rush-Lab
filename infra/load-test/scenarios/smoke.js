import { mine } from '../lib/api.js';
import { checkMine } from '../lib/checks.js';
import { setup as commonSetup } from '../lib/setup.js';

export const options = {
    vus: 1,
    duration: '10s',
    setupTimeout: '2m',
    thresholds: {
        checks: ['rate==1'],
        http_req_failed: ['rate==0'],
    },
};

export function setup() {
    return commonSetup({ userCount: 1 });
}

export default function (data) {
    checkMine(mine(data.sessions[0]));
}
