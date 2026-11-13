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

import org.jkiss.code.NotNull;

import java.util.Map;

/** Standalone regression checks; compile alongside the sanitizer and run without an Eclipse application. */
public class DiagnosticInfoSanitizerTest {
    public static void main(@NotNull String[] args) {
        DiagnosticInfoSanitizer sanitizer = new DiagnosticInfoSanitizer(Map.of("SESSIONNAME", "Console"));
        check(sanitizer, "Start [net.example.Console]", "Start [net.example.Console]");
        check(sanitizer,
            "host=db.example.test\r\n2026-11-10 12:34:56.789 - Start [net.example.Console]\r\n"
                + "Start [net.example.other]\nConnect to db.example.test",
            "host=[REDACTED]\r\n2026-11-10 12:34:56.789 - Start [net.example.Console]\r\n"
                + "Start [net.example.other]\nConnect to [REDACTED]");
        check(sanitizer,
            "!MESSAGE Plug-in org.eclipse.ui.console could not load ConsolePreferencePage or AnsiConsolePreferencePage",
            "!MESSAGE Plug-in org.eclipse.ui.console could not load ConsolePreferencePage or AnsiConsolePreferencePage");
        check(sanitizer, "SESSIONNAME=Console", "SESSIONNAME=[REDACTED]");
        check(sanitizer,
            "Initiate connection to Greenplum database [postgres@db.example.test]",
            "Initiate connection to Greenplum database [postgres@[REDACTED]]");
        check(sanitizer, "Connect to db.example.test", "Connect to [REDACTED]");
        check(sanitizer, "postgres@org.eclipse.example", "postgres@[REDACTED]");
        check(sanitizer, "host=org.eclipse.example", "host=[REDACTED]");
        check(sanitizer, "jdbc:postgresql://db.example.test/database", "[REDACTED]");
        check(sanitizer,
            "dbeaver.distribution.type=community\nds.delayed.keepInstances.default=true\neclipse.application=application",
            "dbeaver.distribution.type=community\nds.delayed.keepInstances.default=true\neclipse.application=application");
        check(sanitizer, "/opt/dbeaver/launcher", "[REDACTED]");
        check(sanitizer,
            "\tat org.eclipse.ui.console.ConsolePreferencePage.create(ConsolePreferencePage.java:42)",
            "\tat org.eclipse.ui.console.ConsolePreferencePage.create(ConsolePreferencePage.java:42)");
        check(new DiagnosticInfoSanitizer(Map.of("API_TOKEN", "secret-value")),
            "prefixsecret-valuesuffix", "prefix[REDACTED]suffix");
        check(new DiagnosticInfoSanitizer(Map.of("USER", "bob")), "bob bobcat", "[REDACTED] bobcat");
    }

    private static void check(@NotNull DiagnosticInfoSanitizer sanitizer, @NotNull String input, @NotNull String expected) {
        String actual = sanitizer.sanitize(input);
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + "\nActual: " + actual);
        }
    }
}
