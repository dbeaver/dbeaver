# Desktop DataDam integration

The Desktop connection service opens an authenticated WebSocket to External DAM at `/ws`.
It uses the existing Desktop SSO access token and never starts a login, opens a browser, or requests credentials.
Imported project encryption keys and the tracking session are not prerequisites for the connection.

## Connection lifecycle

- Restore the existing Desktop login during workbench startup.
- Request the current access token before every connection attempt.
- Send `hello` and wait for `ready` before accepting application messages or reporting a connection.
- Keep the Deployment ID and application run ID across reconnects; use the new server-assigned connection ID for each socket.
- Retry transport failures with bounded backoff. Pause when authorization is unavailable or the upgrade is rejected with HTTP 401/403.
- Close the current channel before changing the Desktop identity or signing out. Resume only if authorization remains available.
- Release the connection and subscriptions when the workbench shuts down.

Messages use the shared `com.dbeaver.datadam.external.api` contracts. Topic subscriptions survive reconnects,
but outgoing messages are rejected while disconnected rather than buffered for later delivery.
Service consumers obtain the channel from `DDDesktopConnectionService` and release their subscriptions when no longer needed.
The shared WebSocket client's 90-second idle timeout detects missing server traffic; External DAM must send its
heartbeat more frequently than this timeout (the server default is 30 seconds). Desktop does not add another heartbeat protocol.

## Channel state

`channel.getStatus()` returns the current immutable state: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, or `CLOSED`.
The connected snapshot contains its `DDConnectionInfo`; `status.isReady()` reports readiness for application messages.
Readiness is a local observation, so every send must still handle failure.

`channel.observeState(observer)` delivers the current state and then subsequent changes in order, without a
check-then-subscribe gap. Its returned `DDSubscription` unregisters only that observer. Existing
`addConnectionListener` callbacks remain transition-only and do not replay an earlier connection.
State observers may run on the subscribing or state-changing thread; keep them short and dispatch UI work to the UI thread.

## Trusted endpoints

All DataDam endpoints use the shared base host in `DDEndpoints`, with `datadam.cloud` as the source default and
build-time substitution for staging. The External DAM endpoint is `wss://external-dam.<base-host>/ws`, using the
same environment as Account and Storage. Declaring this URL does not provision the service, DNS, TLS, or ingress.

Desktop only supplies a token to the configured External DAM endpoint when the token's saved Account and Storage
origins match the current environment. An arbitrary WebSocket host is not trusted. Remote connections require WSS;
unencrypted WS is allowed only on loopback. Tokens and deployment-specific runtime configuration must not be committed.
