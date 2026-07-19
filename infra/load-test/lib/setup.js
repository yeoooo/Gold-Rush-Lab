import { fail } from 'k6';

import { createMine, signin } from './api.js';
import { checkCreateMine, checkSignin } from './checks.js';
import { config } from './config.js';
import { responseJson } from './utils.js';

export function setup(options = {}) {
    const userCount = options.userCount || config.userCount;
    const mineId = options.mineId || createTestMine();
    const sessions = [];

    for (let index = 0; index < userCount; index += 1) {
        const response = signin(mineId);
        if (!checkSignin(response)) {
            fail(`Failed to create user ${index + 1}/${userCount}: status=${response.status}`);
        }

        sessions.push(responseJson(response).data.sessionId);
    }

    return { mineId, sessions };
}

function createTestMine() {
    const response = createMine(config.mineAmount);
    if (!checkCreateMine(response)) {
        fail(`Failed to create mine: status=${response.status}`);
    }

    return responseJson(response).data.mineId;
}
