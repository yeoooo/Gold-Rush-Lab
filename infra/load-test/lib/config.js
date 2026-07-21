function positiveInteger(name, defaultValue) {
    const rawValue = __ENV[name];

    if (rawValue === undefined || rawValue === '') {
        return defaultValue;
    }

    const value = Number(rawValue);
    if (!Number.isInteger(value) || value <= 0) {
        throw new Error(`${name} must be a positive integer: ${rawValue}`);
    }

    return value;
}

function optionalPositiveInteger(name) {
    const rawValue = __ENV[name];

    if (rawValue === undefined || rawValue === '') {
        return null;
    }

    return positiveInteger(name, null);
}

export const config = Object.freeze({
    baseUrl: (__ENV.BASE_URL || 'http://localhost:8080/api').replace(/\/$/, ''),
    mineAmount: positiveInteger('MINE_AMOUNT', 100000),
    userCount: positiveInteger('USER_COUNT', 100),
    iterations: positiveInteger('ITERATIONS', 100),
    timeout: __ENV.TIMEOUT || '5s',
    hotspotMaxDuration: __ENV.HOTSPOT_MAX_DURATION || '1m',
    hotspotMineId: optionalPositiveInteger('HOTSPOT_MINE_ID'),
    stressMaxVu: positiveInteger('STRESS_MAX_VU', 1000),
    soakVus: positiveInteger('SOAK_VUS', 50),
    soakDuration: __ENV.SOAK_DURATION || '2h',
});
