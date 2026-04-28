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
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

public record InstanceServerProperties(int port, @NotNull String password, long startedAt) {
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_STARTED_AT = "startedAt";
    public static final String PROPERTY_INSTANCE = "instance";

    @NotNull
    public static String baseKey(long pid) {
        return PROPERTY_INSTANCE + "." + pid;
    }

    @NotNull
    public static String portKey(long pid) {
        return baseKey(pid) + "." + PROPERTY_PORT;
    }

    @NotNull
    public static String passwordKey(long pid) {
        return baseKey(pid) + "." + PROPERTY_PASSWORD;
    }

    @NotNull
    public static String startedAtKey(long pid) {
        return baseKey(pid) + "." + PROPERTY_STARTED_AT;
    }

    public void writeTo(@NotNull Properties properties, long pid) {
        properties.setProperty(portKey(pid), String.valueOf(port));
        properties.setProperty(passwordKey(pid), password);
        properties.setProperty(startedAtKey(pid), String.valueOf(startedAt));
    }

    public static void removeFrom(@NotNull Properties properties, long pid) {
        properties.remove(portKey(pid));
        properties.remove(passwordKey(pid));
        properties.remove(startedAtKey(pid));
    }

    @Nullable
    public static InstanceServerProperties readFrom(@NotNull Properties properties, long pid) {
        String portValue = properties.getProperty(portKey(pid));
        String passwordValue = properties.getProperty(passwordKey(pid));
        String startedAtValue = properties.getProperty(startedAtKey(pid), "0");

        if (CommonUtils.isEmptyTrimmed(portValue) || CommonUtils.isEmptyTrimmed(passwordValue)) {
            return null;
        }

        try {
            int port = Integer.parseInt(portValue);
            long startedAt = Long.parseLong(startedAtValue);
            return new InstanceServerProperties(port, passwordValue, startedAt);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}