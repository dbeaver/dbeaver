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
package org.jkiss.dbeaver.runtime.ui.console;

import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConsoleUserInterfaceTest extends DBeaverUnitTest {

    private final ConsoleUserInterface consoleUI = new ConsoleUserInterface();
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void statusMessageIsPrintedAsIs() {
        consoleUI.showError("Connection error", "Can't connect to the database", Status.error("Network is unreachable"));

        assertPrintedInOrder("Connection error: Can't connect to the database", "Network is unreachable");
    }

    @Test
    public void statusExceptionIsPrintedAfterTheMessage() {
        consoleUI.showError("Connection error", null, Status.error("Network is unreachable", new IllegalStateException("No route")));

        assertPrintedInOrder("Connection error", "Network is unreachable", "java.lang.IllegalStateException: No route");
    }

    /**
     * Checks that the console has these lines, in this order.
     * Other lines are allowed between them, because the platform under test may print to the console too.
     */
    private void assertPrintedInOrder(@NotNull String... expectedLines) {
        List<String> lines = output.toString(StandardCharsets.UTF_8).lines().toList();
        int from = 0;
        for (String expectedLine : expectedLines) {
            int index = lines.subList(from, lines.size()).indexOf(expectedLine);
            Assertions.assertTrue(index >= 0, "No line '" + expectedLine + "' in the console output: " + lines);
            from += index + 1;
        }
    }
}
