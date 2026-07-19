import http from 'k6/http';

import { config } from './config.js';

const headers = Object.freeze({
    Accept: 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded',
});

function post(path, query, name) {
    const queryString = Object.entries(query)
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&');

    return http.post(`${config.baseUrl}${path}?${queryString}`, null, {
        headers,
        timeout: config.timeout,
        tags: { name },
    });
}

export function createMine(amount) {
    return post('/mines', { amount }, 'create_mine');
}

export function signin(mineId) {
    return post('/v01/user/signin', { mineId }, 'signin');
}

export function mine(sessionId) {
    return post('/v01/mine', { sessionId }, 'mine');
}
