export function responseJson(response) {
    try {
        return response.json();
    } catch (_) {
        return null;
    }
}

export function sessionFor(data, index) {
    if (!data || !Array.isArray(data.sessions) || data.sessions.length === 0) {
        throw new Error('No session is available. Check the setup result.');
    }

    return data.sessions[index % data.sessions.length];
}
