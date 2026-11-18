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
package org.jkiss.utils;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RuntimeUtilsTest extends DBeaverUnitTest {

    @Test
    public void testMakePlatformResourceUrl() {
        Assertions.assertEquals(
            "platform:/plugin/org.jkiss.dbeaver.ui/icons/misc/search.svg",
            RuntimeUtils.makePlatformResourceUrl("org.jkiss.dbeaver.ui", "icons/misc/search.svg")
        );
    }

    @Test
    public void testSplitCommandLine() {
        Assertions.assertEquals(
            Arrays.asList("/bin/sh", "-c", "echo hello && echo world"),
            RuntimeUtils.splitCommandLine("/bin/sh -c 'echo hello && echo world'", true)
        );
    }

    @Test
    public void testBackslashPath() {
        Assertions.assertEquals(Collections.singletonList("C:\\Windows\\notepad.exe"), RuntimeUtils.splitCommandLine("C:\\Windows\\notepad.exe", false));
        Assertions.assertEquals(Collections.singletonList("C:\\Windows\\notepad.exe"), RuntimeUtils.splitCommandLine("C:\\\\Windows\\\\notepad.exe", true));
    }

    @Test
    public void testBackslashEscape() {
        Assertions.assertEquals(Arrays.asList("ls", "-l", "/home/folder with spaces"), RuntimeUtils.splitCommandLine("ls -l /home/folder\\ with\\ spaces", true));
        Assertions.assertEquals(Arrays.asList("ls", "-l", "/home/\"folder with quotes\""), RuntimeUtils.splitCommandLine("ls -l /home/\\\"folder\\ with\\ quotes\\\"", true));
    }

    @Test
    public void testWaitForFutureReturnsResult() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        Assertions.assertEquals(
            "result",
            RuntimeUtils.waitForFuture(future, new NullProgressMonitor(), 1, TimeUnit.SECONDS)
        );
    }

    @Test
    public void testWaitForFutureReturnsNullOnTimeout() throws ExecutionException, InterruptedException {
        // A never-completing future must not block the caller forever: the caller needs
        // the timeout to re-check whether the parser is still running.
        Assertions.assertNull(
            RuntimeUtils.waitForFuture(new CompletableFuture<>(), new NullProgressMonitor(), 10, TimeUnit.MILLISECONDS)
        );
    }

    @Test
    public void testWaitForFutureCancelsOnMonitor() {
        NullProgressMonitor monitor = new NullProgressMonitor();
        monitor.setCanceled(true);

        Assertions.assertThrows(
            CancellationException.class,
            () -> RuntimeUtils.waitForFuture(new CompletableFuture<>(), monitor, 1, TimeUnit.SECONDS)
        );
    }

    @Test
    public void testWaitForFuturePropagatesFailure() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("failure"));

        Assertions.assertThrows(
            ExecutionException.class,
            () -> RuntimeUtils.waitForFuture(future, new NullProgressMonitor(), 1, TimeUnit.SECONDS)
        );
    }
}
