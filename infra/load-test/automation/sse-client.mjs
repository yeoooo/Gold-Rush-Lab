#!/usr/bin/env node

import { readFile, writeFile } from 'node:fs/promises';

function parseArgs(argv) {
    const args = {};
    for (let index = 0; index < argv.length; index += 2) {
        const key = argv[index];
        const value = argv[index + 1];
        if (!key?.startsWith('--') || value === undefined) {
            throw new Error(`Invalid argument: ${key ?? ''}`);
        }
        args[key.slice(2)] = value;
    }
    return args;
}

function requireValue(args, name) {
    const value = args[name];
    if (!value) {
        throw new Error(`Missing --${name}`);
    }
    return value;
}

function percentile(values, percentileValue) {
    if (values.length === 0) {
        return 0;
    }
    const sorted = [...values].sort((left, right) => left - right);
    const index = Math.max(0, Math.ceil(percentileValue * sorted.length) - 1);
    return sorted[index];
}

function parseEvent(rawEvent) {
    let event = 'message';
    let id = '';
    const data = [];

    for (const line of rawEvent.split(/\r?\n/)) {
        if (line.startsWith(':')) {
            continue;
        }
        const separator = line.indexOf(':');
        const field = separator === -1 ? line : line.slice(0, separator);
        const value = separator === -1 ? '' : line.slice(separator + 1).replace(/^ /, '');
        if (field === 'event') event = value;
        if (field === 'id') id = value;
        if (field === 'data') data.push(value);
    }

    return { event, id, data: data.join('\n') };
}

async function main() {
    const args = parseArgs(process.argv.slice(2));
    const baseUrl = requireValue(args, 'base-url').replace(/\/$/, '');
    const sessionsFile = requireValue(args, 'sessions-file');
    const readyFile = requireValue(args, 'ready-file');
    const summaryFile = requireValue(args, 'summary-file');
    const expectedFile = requireValue(args, 'expected-file');
    const completeFile = requireValue(args, 'complete-file');
    const timeoutSeconds = Number(requireValue(args, 'timeout-seconds'));

    if (!Number.isInteger(timeoutSeconds) || timeoutSeconds <= 0) {
        throw new Error('--timeout-seconds must be a positive integer');
    }

    const fixture = JSON.parse(await readFile(sessionsFile, 'utf8'));
    if (!Array.isArray(fixture.sessions) || fixture.sessions.length === 0) {
        throw new Error('sessions file does not contain sessions');
    }

    const startedAt = new Date();
    const controllers = fixture.sessions.map(() => new AbortController());
    const readySessions = new Set();
    const receivedEventIds = new Set();
    const lastEventIds = new Map();
    const latencies = [];
    let duplicateCount = 0;
    let invalidCount = 0;
    let reconnectCount = 0;
    let connectionErrorCount = 0;
    let lastConnectionError = null;
    let expected = null;
    let completionSignaled = false;
    let controlRefreshing = false;
    let finished = false;
    let resolveDone;
    let rejectDone;
    const done = new Promise((resolve, reject) => {
        resolveDone = resolve;
        rejectDone = reject;
    });

    const refreshControl = async () => {
        if (controlRefreshing) {
            return;
        }
        controlRefreshing = true;
        try {
            if (expected === null) {
                try {
                    const value = Number((await readFile(expectedFile, 'utf8')).trim());
                    if (!Number.isInteger(value) || value < 0) {
                        throw new Error('--expected-file must contain a non-negative integer');
                    }
                    expected = value;
                } catch (error) {
                    if (error?.code !== 'ENOENT') {
                        throw error;
                    }
                }
            }

            try {
                await readFile(completeFile, 'utf8');
                completionSignaled = true;
            } catch (error) {
                if (error?.code !== 'ENOENT') {
                    throw error;
                }
            }

            if (expected !== null && (
                receivedEventIds.size >= expected || completionSignaled
            )) {
                resolveDone();
            }
        } catch (error) {
            rejectDone(error);
        } finally {
            controlRefreshing = false;
        }
    };

    const markReady = async (sessionId) => {
        readySessions.add(sessionId);
        if (readySessions.size === fixture.sessions.length) {
            await writeFile(readyFile, JSON.stringify({
                readyAt: new Date().toISOString(),
                connections: readySessions.size,
            }));
        }
    };

    const handleEvent = async (sessionId, parsed) => {
        if (parsed.event === 'connected') {
            await markReady(sessionId);
            return;
        }
        if (parsed.event !== 'mining-completed') {
            return;
        }

        try {
            const payload = JSON.parse(parsed.data);
            const eventId = parsed.id || payload.eventId;
            if (!eventId || !payload.requestedAt) {
                invalidCount += 1;
                return;
            }
            if (receivedEventIds.has(eventId)) {
                duplicateCount += 1;
                return;
            }

            receivedEventIds.add(eventId);
            lastEventIds.set(sessionId, eventId);
            const latency = Date.now() - Date.parse(payload.requestedAt);
            if (Number.isFinite(latency) && latency >= 0) {
                latencies.push(latency);
            } else {
                invalidCount += 1;
            }

            if (expected !== null && receivedEventIds.size >= expected) {
                resolveDone();
            }
        } catch (_) {
            invalidCount += 1;
        }
    };

    const connect = async (sessionId, controller) => {
        let attempt = 0;
        while (!finished) {
            try {
                const headers = { Accept: 'text/event-stream' };
                const lastEventId = lastEventIds.get(sessionId);
                if (lastEventId) {
                    headers['Last-Event-ID'] = lastEventId;
                }

                const response = await fetch(
                    `${baseUrl}/events/${encodeURIComponent(sessionId)}`,
                    { headers, signal: controller.signal },
                );
                if (!response.ok || !response.body) {
                    throw new Error(`SSE connection failed: session=${sessionId}, status=${response.status}`);
                }

                attempt = 0;
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';
                while (!finished) {
                    const { value, done: streamDone } = await reader.read();
                    if (streamDone) {
                        throw new Error(`SSE stream closed before completion: session=${sessionId}`);
                    }
                    buffer += decoder.decode(value, { stream: true });
                    const events = buffer.split(/\r?\n\r?\n/);
                    buffer = events.pop() ?? '';
                    for (const rawEvent of events) {
                        await handleEvent(sessionId, parseEvent(rawEvent));
                    }
                }
            } catch (error) {
                if (finished || error?.name === 'AbortError') {
                    return;
                }
                connectionErrorCount += 1;
                reconnectCount += 1;
                lastConnectionError = error?.message ?? String(error);
                attempt += 1;
                const retryDelay = Math.min(1000, 100 * (2 ** Math.min(attempt - 1, 4)));
                await new Promise((resolve) => setTimeout(resolve, retryDelay));
            }
        }
    };

    const connections = fixture.sessions.map((sessionId, index) =>
        connect(sessionId, controllers[index]));
    const controlPoll = setInterval(() => {
        refreshControl().catch(rejectDone);
    }, 100);
    await refreshControl();
    const timeout = setTimeout(() => {
        rejectDone(new Error(
            `SSE completion timeout: received=${receivedEventIds.size}, expected=${expected ?? 'pending'}`,
        ));
    }, timeoutSeconds * 1000);

    let failure = null;
    try {
        await done;
    } catch (error) {
        failure = error;
    } finally {
        finished = true;
        clearInterval(controlPoll);
        clearTimeout(timeout);
        controllers.forEach((controller) => controller.abort());
        await Promise.allSettled(connections);
    }

    const finishedAt = new Date();
    const received = receivedEventIds.size;
    const summary = {
        status: failure
            ? 'failed'
            : (received >= expected ? 'completed' : 'incomplete'),
        error: failure?.message ?? null,
        mineId: fixture.mineId,
        connections: fixture.sessions.length,
        readyConnections: readySessions.size,
        expected,
        received,
        deliveryRate: expected === 0 ? 0 : received / expected,
        duplicateCount,
        invalidCount,
        reconnectCount,
        connectionErrorCount,
        lastConnectionError,
        latency: {
            avg: latencies.length === 0
                ? 0
                : latencies.reduce((sum, value) => sum + value, 0) / latencies.length,
            p95: percentile(latencies, 0.95),
            p99: percentile(latencies, 0.99),
            max: latencies.length === 0 ? 0 : Math.max(...latencies),
        },
        startedAt: startedAt.toISOString(),
        finishedAt: finishedAt.toISOString(),
    };
    await writeFile(summaryFile, JSON.stringify(summary, null, 2));

    if (failure) {
        throw failure;
    }
}

main().catch((error) => {
    process.stderr.write(`${error.stack ?? error.message}\n`);
    process.exitCode = 1;
});
