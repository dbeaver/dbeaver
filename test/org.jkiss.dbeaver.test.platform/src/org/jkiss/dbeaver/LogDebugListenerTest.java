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

import org.jkiss.code.Nullable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class LogDebugListenerTest extends DBeaverUnitTest {

    @Test
    public void identifiesMessagesForUIUntilRemoved() {
        Log log = Log.getLog(LogDebugListenerTest.class);
        List<Object> messages = new ArrayList<>();
        List<Boolean> notifyUIFlags = new ArrayList<>();
        Log.Listener listener = new Log.Listener() {
            @Override
            public void loggedMessage(@Nullable Object message, @Nullable Throwable exception) {
                loggedMessage(message, exception, true);
            }

            @Override
            public void loggedMessage(@Nullable Object message, @Nullable Throwable exception, boolean notifyUI) {
                messages.add(message);
                notifyUIFlags.add(notifyUI);
            }
        };

        Log.addListener(listener);
        try {
            log.debug("Starting");
            log.info("Informational message");
            Assertions.assertEquals(List.of("Starting", "Informational message"), messages);
            Assertions.assertEquals(List.of(true, false), notifyUIFlags);
        } finally {
            Log.removeListener(listener);
        }

        log.debug("Finished");
        Assertions.assertEquals(List.of("Starting", "Informational message"), messages);
    }

    @Test
    public void legacyListenerReceivesMessages() {
        Log log = Log.getLog(LogDebugListenerTest.class);
        List<Object> messages = new ArrayList<>();
        Log.Listener listener = (message, exception) -> messages.add(message);

        Log.addListener(listener);
        try {
            log.debug("Starting");
            log.info("Informational message");
            Assertions.assertEquals(List.of("Starting", "Informational message"), messages);
        } finally {
            Log.removeListener(listener);
        }
    }
}
