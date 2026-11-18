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

import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils.MacCandidate;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RuntimeUtilsTest extends DBeaverUnitTest {

    @Test
    public void testHashMacAddressKeepsExistingFormat() throws NoSuchAlgorithmException {
        byte[] macAddress = {2, 1, 2, 3, 4, 5};
        Assertions.assertEquals("1UJA5Y1Q1JG8P", RuntimeUtils.hashMacAddress(macAddress));
    }

    @Test
    public void testSelectMacAddressIgnoresInterfaceOrder() {
        byte[] macAddress = {2, 1, 2, 3, 4, 5};
        var first = new MacCandidate("enp3s0", null, false, macAddress);
        var second = new MacCandidate("wlp2s0", null, false, new byte[] {2, 1, 2, 3, 4, 6});
        var bridge = new MacCandidate("br-4f3f05bcacc1", null, false, new byte[] {2, 1, 2, 3, 4, 7});
        var peers = new MacCandidate("awdl0", null, false, new byte[] {2, 1, 2, 3, 4, 8});
        var candidates = List.of(bridge, second, peers, first);

        Assertions.assertArrayEquals(macAddress, RuntimeUtils.selectMacAddress(candidates));
        Assertions.assertArrayEquals(macAddress, RuntimeUtils.selectMacAddress(candidates.reversed()));
    }

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
}
