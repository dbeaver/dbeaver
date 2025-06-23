/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.rest.RestServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;

/**
 * DBeaver instance controller.
 */
public abstract class ApplicationInstanceServer<T extends ApplicationInstanceController> implements
    ApplicationInstanceController {

    private static final Log log = Log.getLog(ApplicationInstanceServer.class);
    private final RestServer<T> server;
    private final FileChannel configFileChannel;
    private final Class<T> controllerClass;

    protected ApplicationInstanceServer(Class<T> controllerClass) throws IOException {
        this.controllerClass = controllerClass;
        server = RestServer
            .builder(controllerClass, controllerClass.cast(this))
            .setFilter(address -> address.getAddress().isLoopbackAddress())
            .create();

        configFileChannel = FileChannel.open(
            getConfigPath(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
        );

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Properties props = new Properties();
            props.setProperty("port", String.valueOf(server.getAddress().getPort()));
            props.store(os, "DBeaver instance server properties");
            configFileChannel.write(ByteBuffer.wrap(os.toByteArray()));
        }

        log.debug("Starting instance server at http://localhost:" + server.getAddress().getPort());
    }


    @Override
    public long ping(long payload) {
        return payload;
    }

    @Override
    public String getThreadDump() {
        log.info("Making thread dump");

        StringBuilder td = new StringBuilder();
        for (Map.Entry<Thread, StackTraceElement[]> tde : Thread.getAllStackTraces().entrySet()) {
            td.append(tde.getKey().getId()).append(" ").append(tde.getKey().getName()).append(":\n");
            for (StackTraceElement ste : tde.getValue()) {
                td.append("\t").append(ste.toString()).append("\n");
            }
        }
        return td.toString();
    }

    @Override
    public String getVersion() {
        return GeneralUtils.getProductVersion().toString();
    }


    public void stopInstanceServer() {
        try {
            log.debug("Stop instance server");

            server.stop();

            if (configFileChannel != null) {
                configFileChannel.close();
                Files.delete(getConfigPath());
            }

            log.debug("Instance server has been stopped");
        } catch (Exception e) {
            log.error("Can't stop instance server", e);
        }
    }

    @NotNull
    protected static Path getConfigPath() {
        return getConfigPath(null);
    }

    @NotNull
    protected static Path getConfigPath(@Nullable String workspacePath) {
        if (workspacePath != null) {
            return Path.of(workspacePath).resolve(DBPWorkspace.METADATA_FOLDER).resolve(CONFIG_PROP_FILE);
        } else {
            return GeneralUtils.getMetadataFolder().resolve(CONFIG_PROP_FILE);
        }
    }

}