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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostgreDumpVersion {
    public static final int MIN_ROWS_PER_INSERT_VERSION = 12;

    private static final Pattern VERSION_PATTERN = Pattern.compile("pg_dump\\s+\\([^)]*\\)\\s+(\\d+)");

    private PostgreDumpVersion() {
    }

    public static int getMajorVersion(@NotNull DBPNativeClientLocation clientHome) throws IOException {
        File binary = RuntimeUtils.getNativeClientBinary(clientHome, PostgreConstants.BIN_FOLDER, "pg_dump");
        Process process = new ProcessBuilder(binary.getAbsolutePath(), "--version")
            .redirectErrorStream(true)
            .start();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                throw new IOException("Timed out reading pg_dump version from " + binary);
            }
            String output;
            try (InputStream stdout = process.getInputStream()) {
                output = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
            }
            Matcher matcher = VERSION_PATTERN.matcher(output);
            if (process.exitValue() != 0 || !matcher.find()) {
                throw new IOException("Could not determine pg_dump version from " + binary + ": " + output.trim());
            }
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid pg_dump version from " + binary, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading pg_dump version from " + binary, e);
        } finally {
            process.destroyForcibly();
        }
    }
}
