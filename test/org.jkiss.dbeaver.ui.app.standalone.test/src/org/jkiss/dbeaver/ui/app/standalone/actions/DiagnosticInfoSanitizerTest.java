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

    @Test
    void removesQuotedCredentialsIncludingSpacesAndEscapes() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        assertEquals("{\"password\":\"[REDACTED]\",'token':'[REDACTED]'}",
            sanitizer.sanitize("{\"password\":\"my secret\",'token':'a\\'b c'}"));
        assertEquals("password=\"[REDACTED]\" user='[REDACTED]' status=failed",
            sanitizer.sanitize("password=\"my secret\" user='Jane Doe' status=failed"));
    }

    @Test
    void removesHostnamesWithUnlistedSuffixes() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        assertEquals("From [REDACTED] and [REDACTED] to [REDACTED]",
            sanitizer.sanitize("From db.customer.de and db.example.co.uk to DB.EXAMPLE.DE"));
    }

    @Test
    void preservesTimestampAndPluginNameInLogHeader() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        String lines = "2026-11-04 12:56:04.687 - Start [org.eclipse.osgi]\n"
            + "2026-11-04 12:56:04.696 - Start [org.jkiss.dbeaver.core]\n"
            + "2026-11-04 12:56:04.701 - Start [org.jkiss.dbeaver.ui.app.standalone]";
        assertEquals(lines, sanitizer.sanitize(lines));
        assertEquals("Found [[REDACTED]]", sanitizer.sanitize("Found [db.customer.de]"));
    }

    @Test
    void preservesProductVersionAndJavaStackFrames() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        String lines = "2026-11-04 14:18:33.184 - DBeaver Ultimate 25.3.0.100 is starting\n"
            + "\tat java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:94)\n"
            + "\tat java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:108)\n"
            + "\tat java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:114)";
        assertEquals(lines, sanitizer.sanitize(lines));
        assertEquals("\tat [REDACTED]", sanitizer.sanitize("\tat /Users/alice/private.log"));
    }

    @Test
    void preservesSupportVersionsDespiteNumericEnvironmentValues() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of(
            "SHLVL", "0", "CPU_BITS", "64", "JAVA_VERSION", "25.0.4", "CLANG_BUILD", "1700", "PIN_TOKEN", "1234"
        ));
        String lines = "2026-11-04 14:24:47.084 - DBeaver Ultimate 25.3.0.100 is starting\n"
            + "2026-11-04 14:24:47.084 - Java version: 25.0.4 by JetBrains s.r.o. (64bit)\n"
            + "PostgreSQL 15.13 (Homebrew) on aarch64-apple-darwin24.4.0.100, compiled by Apple clang version 17.0.0.100 "
            + "(clang-1700.0.13.3), 64-bit', driverName='PostgreSQL JDBC Driver', driverVersion='42.7.13'.";
        assertEquals(lines, sanitizer.sanitize(lines));
        assertEquals("token=[REDACTED] ip=[REDACTED]",
            sanitizer.sanitize("token=1234 ip=192.168.0.1"));
        assertEquals("2026-11-04 14:30:46.756 - DBeaver Ultimate 26.2.5.qualifier is starting",
            sanitizer.sanitize("2026-11-04 14:30:46.756 - DBeaver Ultimate 26.2.5.qualifier is starting"));
        assertEquals("2026-11-04 14:30:46.756 - DBeaver Ultimate 26.2.5.123123123 is starting",
            sanitizer.sanitize("2026-11-04 14:30:46.756 - DBeaver Ultimate 26.2.5.123123123 is starting"));
        var versionEnvironment = new DiagnosticInfoSanitizer(Map.of("DBEAVER_PRODUCT_VERSION", "26.2.5.qualifier"));
        assertEquals("2026-11-04 14:30:46.756 - DBeaver Ultimate 26.2.5.qualifier is starting",
            versionEnvironment.sanitize("2026-11-04 14:30:46.756 - DBeaver Ultimate 26.2.5.qualifier is starting"));
    }

    @Test
    void preservesEclipseLogEntryBundleIdentifiers() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        String lines = "!ENTRY org.jkiss.dbeaver.ui 2 0 2026-11-04 14:05:13.124\n"
            + "!MESSAGE Cannot connect DataDam desktop channel\n"
            + "!SUBENTRY 1 org.eclipse.core.runtime 2 0 2026-11-04 14:05:13.125\n"
            + "!MESSAGE java.net.http.HttpConnectTimeoutException: HTTP connect timed out\n"
            + "!SUBENTRY 2 org.eclipse.core.runtime 2 0 2026-11-04 14:05:13.126";
        assertEquals(lines, sanitizer.sanitize(lines));
        assertEquals("!MESSAGE Connection to [REDACTED] failed",
            sanitizer.sanitize("!MESSAGE Connection to db.customer.de failed"));
    }

    @Test
    void removesAuthorizationHeadersAndUriUserinfo() {
        var sanitizer = new DiagnosticInfoSanitizer(Map.of());
        assertEquals("Authorization: [REDACTED]\nProxy-Authorization: [REDACTED]\nstatus=failed",
            sanitizer.sanitize("Authorization: Bearer ghp_abcDEF123\nProxy-Authorization: Basic YWRtaW46c2VjcmV0\nstatus=failed"));
        assertEquals("Cannot connect [REDACTED] status=failed",
            sanitizer.sanitize("Cannot connect redis://admin:s3cret@cache.customer.de:6379 status=failed"));
        assertEquals("{\"authorization\":\"[REDACTED]\"}",
            sanitizer.sanitize("{\"authorization\":\"Bearer ghp_abcDEF123\"}"));
    }
}
