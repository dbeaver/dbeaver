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
package org.jkiss.dbeaver;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogOutputStreamTest extends DBeaverUnitTest {

    @Test
    public void testWriteAfterFailedRotation() throws IOException {
        Path tempDirectory = Files.createTempDirectory("dbeaver-log-output");
        Path logPath = tempDirectory.resolve("dbeaver.log");
        DBPPreferenceStore preferenceStore = ModelPreferences.getPreferences();
        long originalMaxLogSize = preferenceStore.getLong(LogOutputStream.LOGS_MAX_FILE_SIZE);

        try {
            NonRenamableFile logFile = new NonRenamableFile(logPath);
            try (LogOutputStream output = new LogOutputStream(logFile)) {
                output.write(new byte[]{1, 2});
                preferenceStore.setValue(LogOutputStream.LOGS_MAX_FILE_SIZE, 1L);
                output.write(new byte[]{3});
            }

            assertEquals(1, logFile.renameAttempts);
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(logPath));
        } finally {
            preferenceStore.setValue(LogOutputStream.LOGS_MAX_FILE_SIZE, originalMaxLogSize);
            Files.deleteIfExists(logPath);
            Files.deleteIfExists(tempDirectory);
        }
    }

    private static final class NonRenamableFile extends File {
        @Serial
        private static final long serialVersionUID = 1L;

        private int renameAttempts;

        private NonRenamableFile(@NotNull Path path) {
            super(path.toString());
        }

        @Override
        public boolean renameTo(@NotNull File destination) {
            renameAttempts++;
            return false;
        }
    }
}
