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
- Close cached RPC endpoints and pause the current channel before changing the Desktop identity or signing out.
  Resume only if authorization remains available.
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

## RPC services

The connection service owns ready-to-use, bidirectional RPC endpoints for the independent Account and Storage lanes.
It opens both lanes when the channel becomes ready. Consumers register implementations and create typed proxies directly:

```java
RpcEndpoint account = DDDesktopConnectionService.getInstance().getRpc(DDRpcServices.ACCOUNT);
RpcEndpoint storage = DDDesktopConnectionService.getInstance().getRpc(DDRpcServices.STORAGE);

MessageSubscription registration = account.register("projects", ProjectCallbacks.class, callbacks);
ProjectService projects = account.proxy("projects", ProjectService.class);
StorageService files = storage.proxy("files", StorageService.class);
```

`RpcEndpoint` is from `com.dbeaver.messaging.rpc`, `MessageSubscription` is from
`com.dbeaver.messaging.requestreply`, and `DDRpcServices` is from `com.dbeaver.datadam.external.api.rpc`.
The application supplies the public service interfaces and their implementations; each RPC operation is synchronous
and accepts one argument. Make proxy calls on a worker thread and handle `RpcException` failures.
The service uses the shared Gson codec by default; its codec constructor overload supports custom serialization.

Each service lane has its own bindings, pending requests, and lifecycle. The same binding name can be used independently
in Account and Storage, and either peer can register implementations and invoke the other peer.
The transport topics are `rpc.account` and `rpc.storage`; the registration name selects a contract within that lane.
Additional trusted backend service IDs open lazily through `getRpc(serviceId)`, using `DDRpcServices.topic(serviceId)`.
The local cache allows at most 32 additional service IDs per Desktop identity, with capacity reclaimed from closed endpoints.

Start the connection service during workbench startup. `getRpc` never starts login or waits for a connection:
opening a lane before readiness fails with `DBException`. Repeated access returns the same open endpoint, including
while temporarily disconnected; calls during a disconnect fail rather than queue. Pending calls fail on disconnect,
and registered implementations survive a physical reconnect without re-registration.

The connection service closes the endpoints on identity changes and shutdown. After a new identity becomes ready,
consumers must obtain its fresh endpoints and register implementations again; old proxies stay closed.
Release individual bindings with `registration.close()`. Endpoints are shared and owned by the connection service;
explicitly closing one allows replacement after its subscription cleanup finishes. Access during cleanup fails with
`DBException`. Closing a binding from an old endpoint
cannot remove a replacement endpoint's binding. Endpoint closure cancels calls and interrupts handlers, but application
handlers must cooperate with interruption to stop work already in progress.

## Trusted endpoints

All DataDam endpoints use the shared base host in `DDEndpoints`, with `datadam.cloud` as the source default and
build-time substitution for staging. The External DAM endpoint is `wss://external-dam.<base-host>/ws`, using the
same environment as Account and Storage. Declaring this URL does not provision the service, DNS, TLS, or ingress.

Desktop only supplies a token to the configured External DAM endpoint when the token's saved Account and Storage
origins match the current environment. An arbitrary WebSocket host is not trusted. Remote connections require WSS;
unencrypted WS is allowed only on loopback. Tokens and deployment-specific runtime configuration must not be committed.
