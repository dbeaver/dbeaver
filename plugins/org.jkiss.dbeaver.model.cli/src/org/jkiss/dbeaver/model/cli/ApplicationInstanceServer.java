/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.cli.rest.BearerRequestHandler;
import org.jkiss.dbeaver.utils.FileMutex;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;
import org.jkiss.utils.rest.RestServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;

/**
 * DBeaver instance controller.
 */
public abstract class ApplicationInstanceServer<T extends ApplicationInstanceController>
    implements ApplicationInstanceController {

    private static final Log log = Log.getLog(ApplicationInstanceServer.class);

    private static final Duration REGISTRY_LOCK_TIMEOUT = Duration.ofSeconds(5);

    private final RestServer<T> server;

    protected ApplicationInstanceServer(Class<T> controllerClass) throws IOException {
        String password = SecurityUtils.generatePassword(32);
        server = RestServer
            .builder(controllerClass, controllerClass.cast(this))
            .setFilter(address -> address.getAddress().isLoopbackAddress())
            .setLandingPage(GeneralUtils.getProductTitle())
            .setHandlerFactory(
                (cls, object, gson, filter, landingPage)
                    -> new BearerRequestHandler<>(
                        cls, object, gson, filter, landingPage, password
                )
            )
            .create();

        long startedAt = ProcessHandle.current()
            .info()
            .startInstant()
            .map(java.time.Instant::toEpochMilli)
            .orElse(System.currentTimeMillis());

        InstanceServerProperties serverProperties = new InstanceServerProperties(
            server.getAddress().getPort(),
            password,
            startedAt
        );

        try {
            registerCurrentProcess(getConfigPath(), serverProperties);
        } catch (IOException e) {
            try {
                server.stop();
            } catch (Exception stopError) {
                e.addSuppressed(stopError);
            }
            throw e;
        }

        log.debug("Starting instance server at http://localhost:" + serverProperties.port());
    }

    @Override
    public long ping(long payload) {
        return payload;
    }

    @NotNull
    @Override
    public String getThreadDump() {
        log.info("Making thread dump");

        StringBuilder td = new StringBuilder();
        for (Map.Entry<Thread, StackTraceElement[]> tde : Thread.getAllStackTraces().entrySet()) {
            td.append(tde.getKey().threadId()).append(" ").append(tde.getKey().getName()).append(":\n");
            for (StackTraceElement ste : tde.getValue()) {
                td.append("\t").append(ste).append("\n");
            }
        }
        return td.toString();
    }

    @NotNull
    @Override
    public String getVersion() {
        return GeneralUtils.getProductVersion().toString();
    }


    public void stopInstanceServer() {
        try {
            log.debug("Stop instance server");
            server.stop();
            log.debug("Instance server has been stopped");
        } catch (Exception e) {
            log.error("Can't stop instance server", e);
        }

        try {
            unregisterCurrentProcess(getConfigPath());
        } catch (Exception unregisterError) {
            log.debug("Cannot unregister instance server", unregisterError);
        }

    }

    @NotNull
    protected static Path getConfigPath() {
        return getConfigPath(null);
    }

    @NotNull
    protected static Path getConfigPath(@Nullable Path workspacePath) {
        if (workspacePath != null) {
            return workspacePath.resolve(DBPWorkspace.METADATA_FOLDER).resolve(CONFIG_PROP_FILE);
        } else {
            return GeneralUtils.getMetadataFolder().resolve(CONFIG_PROP_FILE);
        }
    }

    private void registerCurrentProcess(
        @NotNull Path configPath,
        @NotNull InstanceServerProperties serverProperties
    ) throws IOException {
        long pid = ProcessHandle.current().pid();
        withRegistryLock(configPath, () -> {
            Map<Long, InstanceServerProperties> registry = loadRegistry(configPath);
            gcByPid(registry);

            registry.put(pid, serverProperties);

            storeRegistry(configPath, registry);
        });
    }

    private static void unregisterCurrentProcess(@NotNull Path configPath) throws IOException {
        long pid = ProcessHandle.current().pid();
        withRegistryLock(configPath, () -> {
            Map<Long, InstanceServerProperties> registry = loadRegistry(configPath);
            gcByPid(registry);

            registry.remove(pid);

            storeRegistry(configPath, registry);
        });
    }

    @NotNull
    private static Map<Long, InstanceServerProperties> loadRegistry(@NotNull Path configPath) throws IOException {
        Properties props = new Properties();
        if (Files.notExists(configPath)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            props.load(reader);
        }
        return propertiesToMap(props);
    }

    private static void storeRegistry(
        @NotNull Path configPath,
        @NotNull Map<Long, InstanceServerProperties> registry
    ) throws IOException {
        if (registry.isEmpty()) {
            Files.deleteIfExists(configPath);
            return;
        }

        Path parent = configPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmp = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        boolean moved = false;
        try {
            Properties props = toProperties(registry);
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                props.store(writer, "");
            }

            try {
                Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING);
            }

            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tmp);
            }
        }
    }

    private static void gcByPid(@NotNull Map<Long, InstanceServerProperties> registry) {
        registry.entrySet().removeIf(entry -> isStaleProcessEntry(entry.getKey(), entry.getValue()));
    }

    private static Map<Long, InstanceServerProperties> propertiesToMap(@NotNull Properties props) {
        Set<String> keys = new HashSet<>(props.stringPropertyNames());
        String prefix = InstanceServerProperties.PROPERTY_INSTANCE + ".";
        Map<Long, InstanceServerProperties> registry = new LinkedHashMap<>();

        for (String key : keys) {
            Long pid = extractPid(key, prefix);
            if (pid == null || registry.containsKey(pid)) {
                continue;
            }
            InstanceServerProperties serverProperties = InstanceServerProperties.readFrom(props, pid);
            if (serverProperties != null) {
                registry.put(pid, serverProperties);
            }
        }

        return registry;
    }

    @NotNull
    private static Properties toProperties(@NotNull Map<Long, InstanceServerProperties> registry) {
        Properties props = new Properties();
        for (Map.Entry<Long, InstanceServerProperties> entry : registry.entrySet()) {
            entry.getValue().writeTo(props, entry.getKey());
        }
        return props;
    }

    @Nullable
    private static Long extractPid(@NotNull String key, @NotNull String prefix) {
        if (!key.startsWith(prefix)) {
            return null;
        }
        int dot = key.indexOf('.', prefix.length());
        if (dot < 0) {
            return null;
        }
        String pidPart = key.substring(prefix.length(), dot);
        try {
            return Long.parseLong(pidPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isStaleProcessEntry(long pid, @NotNull InstanceServerProperties serverProperties) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return true;
        }

        long actualStartedAt = handle.get()
            .info()
            .startInstant()
            .map(java.time.Instant::toEpochMilli)
            .orElse(-1L);

        return actualStartedAt < 0 || actualStartedAt != serverProperties.startedAt();
    }

    private static void withRegistryLock(@NotNull Path configPath, @NotNull IOAction action) throws IOException {
        Path lockPath = configPath.resolveSibling(configPath.getFileName() + ".lock");
        Path parent = lockPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (FileMutex ignored = FileMutex.tryLock(lockPath, REGISTRY_LOCK_TIMEOUT)) {
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for registry lock: " + lockPath, e);
        }
    }


    public static class InstanceConnectionParameters implements GeneralUtils.IParameterHandler {
        boolean makeConnect = true;
        boolean openConsole = false;
        boolean createNewConnection = true;

        @Override
        public boolean setParameter(@NotNull String name, @NotNull String value) {
            return switch (name) {
                case "connect" -> {
                    makeConnect = CommonUtils.toBoolean(value);
                    yield true;
                }
                case "openConsole" -> {
                    openConsole = CommonUtils.toBoolean(value);
                    yield true;
                }
                case "create" -> {
                    createNewConnection = CommonUtils.toBoolean(value);
                    yield true;
                }
                default -> false;
            };
        }

        public boolean isCreateNewConnection() {
            return createNewConnection;
        }

        public boolean isMakeConnect() {
            return makeConnect;
        }

        public boolean isOpenConsole() {
            return openConsole;
        }
    }

    @FunctionalInterface
    private interface IOAction {
        void run() throws IOException;
    }
}
