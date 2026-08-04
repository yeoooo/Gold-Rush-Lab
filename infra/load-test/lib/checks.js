import { check } from 'k6';

import { responseJson } from './utils.js';

export function checkStatus(response, expectedStatus = 200) {
    return check(response, {
        [`status is ${expectedStatus}`]: (res) => res.status === expectedStatus,
    }, { operation: 'status' });
}

export function checkCreateMine(response) {
    const body = responseJson(response);

    return check(response, {
        'create mine response is valid': (res) =>
            res.status === 200 &&
            body !== null &&
            body.success === true &&
            Number.isInteger(body.data?.mineId) &&
            Number.isInteger(body.data?.remainingAmount),
    }, { operation: 'create_mine' });
}

export function checkSignin(response) {
    const body = responseJson(response);

    return check(response, {
        'signin response is valid': (res) =>
            res.status === 200 &&
            body !== null &&
            body.success === true &&
            typeof body.data?.sessionId === 'string' &&
            body.data.sessionId.length > 0,
    }, { operation: 'signin' });
}

export function checkMine(response) {
    const body = responseJson(response);

    return check(response, {
        'mine response is valid': (res) =>
            (res.status === 200 || res.status === 202) &&
            body !== null &&
            body.success === true &&
            ((res.status === 202 && typeof body.data?.eventId === 'string') || (
                body.data?.earned === 1 &&
                Number.isInteger(body.data?.totalGold) &&
                Number.isInteger(body.data?.remained)
            )),
    }, { operation: 'mine' });
}
