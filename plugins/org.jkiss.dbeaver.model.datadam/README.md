# Desktop DataDam integration

The Desktop remote DAM session service opens an authenticated WebSocket to Remote DAM at `/ws`.
It uses the existing Desktop SSO access token and never starts a login, opens a browser, or requests credentials.
Imported project encryption keys are not prerequisites for the remote DAM session.

## Remote DAM session lifecycle

- Initialize the session service through the model plugin's `org.jkiss.dbeaver.pluginService` contribution.
- Open the channel only while the existing authorization service reports `SIGNED_IN`, after login or session restoration.
  Initialization while signed out only registers observers; it does not open a socket or restore credentials.
- Request the current access token before every socket attempt.
- Send `hello` and wait for `ready` before accepting application messages or reporting a session.
- Keep `appDeploymentId` and `appInstanceId` across reconnects; use the new server-assigned `damSessionId` for each socket.
- Retry transport failures with bounded backoff. Pause when authorization is unavailable or the upgrade is rejected with HTTP 401/403.
- Close cached RPC endpoints and pause the current channel before changing the Desktop identity or signing out.
  Resume only if authorization remains available.
- The access service schedules token expiry using the same 30-second safety margin as its status checks. Expiry
  invalidates cached RPC endpoints and pauses the channel without starting login or clearing saved tokens and keys.
  A new access operation replaces the scheduled task, and stale tasks cannot invalidate a newer session.
- Release the session and subscriptions when the platform deactivates plugin services.

`DDRemoteDamDesktopSessionPluginService` owns activation and shutdown without Workbench, SWT or UI listeners. Repeated
activation and deactivation are harmless, and shutdown never creates a new session service. `DDDesktopInitializer`
restores saved authorization and initializes project synchronization; the session lifecycle contribution does not load tokens.

Messages use the shared contracts from the `com.dbeaver.datadam.dam.remote.api` bundle. Topic subscriptions survive reconnects,
but outgoing messages are rejected while disconnected rather than buffered for later delivery.
Service consumers obtain the channel from `DDRemoteDamDesktopSessionService` and release their subscriptions when no longer needed.
The shared WebSocket client's 90-second idle timeout detects missing server traffic; Remote DAM must send its
heartbeat more frequently than this timeout (the server default is 30 seconds). Desktop does not add another heartbeat protocol.

## Channel state

`channel.getStatus()` returns the current immutable state: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, or `CLOSED`.
The connected snapshot contains its `DDRemoteDamSessionInfo` through `status.session()`; `status.isReady()` reports readiness
for application messages. Session metadata exposes `appDeploymentId()`, `appInstanceId()`, and `damSessionId()`.
Readiness is a local observation, so every send must still handle failure.

`channel.observeState(observer)` delivers the current state and then subsequent changes in order, without a
check-then-subscribe gap. Its returned `DDRemoteDamSubscription` unregisters only that observer. Existing
`addSessionListener` callbacks remain transition-only and do not replay an earlier session.
State observers may run on the subscribing or state-changing thread; keep them short and dispatch UI work to the UI thread.
Session-listener callbacks and incoming messages share one channel-wide delivery job, outside the channel lock and
WebSocket I/O callbacks. An old session's disconnect callbacks finish before a replacement session is announced,
including across pause/resume. Keep listeners short: a blocked listener delays subsequent delivery rather than allowing
callbacks from a replacement session to overtake it. Incoming queues remain bounded during that delay.

## RPC services

The session service owns one bidirectional RPC endpoint for the current Desktop identity. Consumers choose
their topics and can use the default Gson codec or supply their own codec per proxy or implementation:

```java
MessagingRpcEndpoint endpoint = DDRemoteDamDesktopSessionService.getInstance().getRpc();
StorageService ordinaryFiles = endpoint.proxy("files", StorageService.class);
DDRemoteDamGsonRpcCodec codec = new DDRemoteDamGsonRpcCodec(
    new GsonBuilder().registerTypeAdapter(FileInfo.class, fileInfoAdapter).create());

MessagingSubscription registration = endpoint.register("projects.changed", ProjectCallbacks.class, callbacks, codec);
StorageService files = endpoint.proxy("storage.files", StorageService.class, codec);
```

`MessagingRpcEndpoint` is from `com.dbeaver.messaging.rpc`, `MessagingSubscription` is from
`com.dbeaver.messaging.requestreply`, and `DDRemoteDamGsonRpcCodec` is from `com.dbeaver.datadam.dam.remote.api.rpc`.
The application supplies the public service interfaces and their implementations; each RPC operation is synchronous
and accepts zero, one, or multiple arguments, including varargs. Make proxy calls on a worker thread and handle
`MessagingRpcException` failures.
The overloads without a codec use the session owner's default `DDRemoteDamGsonRpcCodec` with an uncustomized Gson instance.
An explicitly supplied codec belongs only to that proxy or registration; one plugin's type adapters do not change
another plugin's serialization.
Both peers must use compatible payload representations. DataDam codecs produce and consume `JsonElement` values.

The supplied topic is the actual transport topic. There are no reserved Account or Storage lanes, automatic prefixes,
or a second RPC service name. Naming conventions such as `storage.files` belong to the application.
Either peer can register an implementation and invoke the other peer on a topic.

The model plugin lifecycle starts the session service. `getRpc` never starts login or waits for a session:
obtaining an endpoint before readiness fails with `DBException`. Repeated access returns the same open endpoint, including
while temporarily disconnected; calls during a disconnect fail rather than queue. Pending calls fail on disconnect,
and registered implementations survive a physical reconnect without re-registration.

The session service closes the endpoint on identity changes and shutdown. After a new identity becomes ready,
consumers must obtain its fresh endpoint and register implementations again; old proxies stay closed.
Release individual bindings with `registration.close()`. The endpoint is shared and owned by the session service;
explicitly closing it allows replacement after its subscription cleanup finishes. Access during cleanup fails with
`DBException`. Closing a binding from an old endpoint
cannot remove a replacement endpoint's binding. Endpoint closure cancels calls and interrupts handlers, but application
handlers must cooperate with interruption to stop work already in progress.

## Trusted endpoints

By default, DataDam endpoints use the shared base host in `DDEndpoints`, with `datadam.cloud` as the source default and
build-time substitution for staging. The default Remote DAM endpoint is `wss://dam.<base-host>/ws`.
Declaring this URL does not provision the service, DNS, TLS, or ingress.

For local development, add these JVM options to the DBeaver Desktop run configuration:

```text
-Ddbeaver.datadam.account.url=http://localhost:9001
-Ddbeaver.datadam.storage.url=http://localhost:9000
-Ddbeaver.datadam.remote.url=ws://localhost:9006/ws
```

Each property overrides its endpoint independently; omitted properties retain their default URLs. The Remote DAM
property is the full WebSocket URL, including `/ws`. SSO is discovered through the configured Account endpoint.
Properties are read once when `DDEndpoints` is initialized, so changing them requires a Desktop restart.

Desktop only supplies a token to the configured Remote DAM endpoint when the token's saved Account and Storage
origins match the current environment. An arbitrary WebSocket host is not trusted. Remote DAM sessions require WSS;
unencrypted WS is allowed only on loopback. Tokens and deployment-specific runtime configuration must not be committed.
