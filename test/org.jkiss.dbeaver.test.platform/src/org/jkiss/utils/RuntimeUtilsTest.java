/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

public class RuntimeUtilsTest extends DBeaverUnitTest {

    @Test
    public void testSplitCommandLine() {
        Assert.assertEquals(Arrays.asList("/bin/sh", "-c", "echo hello && echo world"), RuntimeUtils.splitCommandLine("/bin/sh -c 'echo hello && echo world'", true));
    }

    @Test
    public void testBackslashPath() {
        Assert.assertEquals(Collections.singletonList("C:\\Windows\\notepad.exe"), RuntimeUtils.splitCommandLine("C:\\Windows\\notepad.exe", false));
        Assert.assertEquals(Collections.singletonList("C:\\Windows\\notepad.exe"), RuntimeUtils.splitCommandLine("C:\\\\Windows\\\\notepad.exe", true));
    }

    @Test
    public void testBackslashEscape() {
        Assert.assertEquals(Arrays.asList("ls", "-l", "/home/folder with spaces"), RuntimeUtils.splitCommandLine("ls -l /home/folder\\ with\\ spaces", true));
        Assert.assertEquals(Arrays.asList("ls", "-l", "/home/\"folder with quotes\""), RuntimeUtils.splitCommandLine("ls -l /home/\\\"folder\\ with\\ quotes\\\"", true));
    }

    @Test
    public void testFormatExecutionTime() {
        Assert.assertEquals("1h 0m 0s", RuntimeUtils.formatExecutionTime(Duration.ofHours(1)));
        Assert.assertEquals("1h 1m 1s", RuntimeUtils.formatExecutionTime(Duration.ofHours(1).plusMinutes(1).plusSeconds(1)));
        Assert.assertEquals("1h 1m 0s", RuntimeUtils.formatExecutionTime(Duration.ofHours(1).plusMinutes(1).plusMillis(500)));
        Assert.assertEquals("11h 41m 9s", RuntimeUtils.formatExecutionTime(Duration.ofSeconds(42069)));
        Assert.assertEquals("1s", RuntimeUtils.formatExecutionTime(Duration.ofSeconds(1)));
        Assert.assertEquals("%.03fs".formatted(0.5), RuntimeUtils.formatExecutionTime(Duration.ofMillis(500)));
        Assert.assertEquals("1m 1s", RuntimeUtils.formatExecutionTime(Duration.ofMinutes(1).plusSeconds(1)));
        Assert.assertEquals("1m 0s", RuntimeUtils.formatExecutionTime(Duration.ofMinutes(1)));
    }
}
