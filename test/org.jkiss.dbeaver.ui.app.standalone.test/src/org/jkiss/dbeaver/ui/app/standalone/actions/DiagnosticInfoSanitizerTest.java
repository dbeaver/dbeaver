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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticInfoSanitizerTest {
    @Test
    void removesSensitiveValuesWithoutDroppingDiagnosticContext() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of("TOKEN", "very-private-value", "REGION", "xy"));
        String result = sanitizer.sanitize(
            "Connect jdbc:postgresql://db.example.com:5432/store?user=admin&password=secret "
                + "host=db.example.com ip=192.168.1.20 file=/Users/alice/work/log.txt "
                + "token=very-private-value region=xy status=failed"
        );

        assertTrue(result.contains("status=failed"));
        for (String secret : new String[]{"jdbc:", "db.example.com", "192.168.1.20", "alice", "very-private-value", "region=xy"}) {
            assertFalse(result.contains(secret), secret);
        }
    }

    @Test
    void leavesOrdinaryLogLinesIntact() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        assertEquals("Connection failed in java.lang.String with code 42",
            sanitizer.sanitize("Connection failed in java.lang.String with code 42"));
    }

    @Test
    void removesQuotedPathsIpv6AndBareHostnames() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        String result = sanitizer.sanitize(
            "From db.internal via 2001:db8::1 at \"C:\\Users\\Alice Smith\\logs\\error.log\""
        );
        assertTrue(result.startsWith("From "));
        assertFalse(result.contains("db.internal"));
        assertFalse(result.contains("2001:db8::1"));
        assertFalse(result.contains("Alice Smith"));
    }
}
