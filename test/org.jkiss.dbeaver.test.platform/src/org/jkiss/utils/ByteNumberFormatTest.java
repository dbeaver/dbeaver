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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ByteNumberFormatTest extends DBeaverUnitTest {

    private static final long KB = 1024;
    private static final long MB = 1024 * KB;
    private static final long GB = 1024 * MB;
    private static final long TB = 1024 * GB;
    private static final long PB = 1024 * TB;

    @Test
    public void testFormatPlain() {
        final ByteNumberFormat format = new ByteNumberFormat();
        Assertions.assertEquals("0", format.format(0));
        Assertions.assertEquals("512", format.format(KB / 2));
        Assertions.assertEquals("1023", format.format(KB - 1));
        Assertions.assertEquals("1K", format.format(KB));
        Assertions.assertEquals("512K", format.format(MB / 2));
        Assertions.assertEquals("1023K", format.format(MB - 1));
        Assertions.assertEquals("1M", format.format(MB));
        Assertions.assertEquals("512M", format.format(GB / 2));
        Assertions.assertEquals("1023M", format.format(GB - 1));
        Assertions.assertEquals("1G", format.format(GB));
        Assertions.assertEquals("512G", format.format(TB / 2));
        Assertions.assertEquals("1023G", format.format(TB - 1));
        Assertions.assertEquals("1P", format.format(PB));
        Assertions.assertEquals("512P", format.format(PB * 512));
        Assertions.assertEquals("1023P", format.format(PB * 1023));
        Assertions.assertEquals("1024P", format.format(PB * 1024));
        Assertions.assertEquals("2048P", format.format(PB * 2048));
    }

    @Test
    public void testFormatLong() {
        final ByteNumberFormat format = new ByteNumberFormat(ByteNumberFormat.BinaryPrefix.ISO);
        Assertions.assertEquals("0", format.format(0));
        Assertions.assertEquals("512", format.format(KB / 2));
        Assertions.assertEquals("1023", format.format(KB - 1));
        Assertions.assertEquals("1KiB", format.format(KB));
        Assertions.assertEquals("512KiB", format.format(MB / 2));
        Assertions.assertEquals("1023KiB", format.format(MB - 1));
        Assertions.assertEquals("1MiB", format.format(MB));
        Assertions.assertEquals("512MiB", format.format(GB / 2));
        Assertions.assertEquals("1023MiB", format.format(GB - 1));
        Assertions.assertEquals("1GiB", format.format(GB));
        Assertions.assertEquals("512GiB", format.format(TB / 2));
        Assertions.assertEquals("1023GiB", format.format(TB - 1));
        Assertions.assertEquals("1PiB", format.format(PB));
        Assertions.assertEquals("512PiB", format.format(PB * 512));
        Assertions.assertEquals("1023PiB", format.format(PB * 1023));
        Assertions.assertEquals("1024PiB", format.format(PB * 1024));
        Assertions.assertEquals("2048PiB", format.format(PB * 2048));
    }
}
