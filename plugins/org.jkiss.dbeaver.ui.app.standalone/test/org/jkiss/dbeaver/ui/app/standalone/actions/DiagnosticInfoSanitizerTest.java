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
        check(sanitizer,
            "Command-line arguments: --launcher.library C:\\Users\\bob\\dbeaver\\launcher.dll --launcher.appendVmargs",
            "Command-line arguments: --launcher.library [REDACTED] --launcher.appendVmargs");
        check(sanitizer, "Framework arguments: -eclipse.keyring C:\\Users\\Maria\\secure_storage",
            "Framework arguments: -eclipse.keyring [REDACTED]");
        check(sanitizer,
            "C:\\Users\\Maria\\Downloads\\dbeaver-ue-26.3.0-windows-x86_64 (8)\\dbeaver\\dbeaver.exe",
            "[REDACTED]");
        check(sanitizer, "--launcher.library \"C:\\Program Files\\DBeaver\\launcher.dll\"",
            "--launcher.library \"[REDACTED]\"");
        check(sanitizer,
            "--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens java.sql/java.sql=ALL-UNNAMED "
                + "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-modules=java.sql,java.xml",
            "--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens java.sql/java.sql=ALL-UNNAMED "
                + "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-modules=java.sql,java.xml");
        check(sanitizer,
            "org.osgi.framework.system.packages=java.lang,javax.crypto;version=\"1.0.0\",com.sun.net.httpserver\r\n"
                + "org.osgi.framework.system.capabilities=osgi.ee;osgi.ee=\"JavaSE\";version:List<Version>=\"21.0\"\r\n"
                + "host=db.example.test",
            "org.osgi.framework.system.packages=java.lang,javax.crypto;version=\"1.0.0\",com.sun.net.httpserver\r\n"
                + "org.osgi.framework.system.capabilities=osgi.ee;osgi.ee=\"JavaSE\";version:List<Version>=\"21.0\"\r\n"
                + "host=[REDACTED]");
        check(sanitizer, "  com.google.gson (2.14.0) \"Gson\" [Resolved]",
            "  com.google.gson (2.14.0) \"Gson\" [Resolved]");
        check(sanitizer, "Id: com.google.gson, Version: 2.14.0, Location: reference:file:plugins/com.google.gson_2.14.0.jar",
            "Id: com.google.gson, Version: 2.14.0, Location: reference:file:plugins/com.google.gson_2.14.0.jar");
        check(sanitizer, "Loading bundle:/com.google.gson", "Loading bundle:/com.google.gson");
        check(sanitizer, "Connect to com.google.gson", "Connect to com.google.gson");
        check(sanitizer,
            "org.mongodb.driver.client org.mongodb.driver.cluster org.mongodb.driver.connection",
            "org.mongodb.driver.client org.mongodb.driver.cluster org.mongodb.driver.connection");
        check(sanitizer,
            "meta.com.dbeaver.db.cosmos.nosql.DCosmosContainerProperties.partitionKeyVersion.name",
            "meta.com.dbeaver.db.cosmos.nosql.DCosmosContainerProperties.partitionKeyVersion.name");
        check(sanitizer, "datasourceId=bigquery.ee driverId=cds.calcite",
            "datasourceId=bigquery.ee driverId=cds.calcite");
        check(sanitizer, "License: DB-EXAMPLE-123 (Enterprise)", "License: [REDACTED] (Enterprise)");
        check(sanitizer, "PostgreSQL 11.22 (Debian 11.22-1.pgdg110+1)",
            "PostgreSQL 11.22 (Debian 11.22-1.pgdg110+1)");
        check(sanitizer, "Open [https://db.example.test/path]", "Open [[REDACTED]]");
        check(sanitizer, "server=Microsoft-HTTPAPI/2.0, transfer-encoding=chunked",
            "server=Microsoft-HTTPAPI/2.0, transfer-encoding=chunked");
        check(sanitizer, "content-location=https://example.com/, content-type=application/json",
            "content-location=[REDACTED], content-type=application/json");
        check(sanitizer,
            "serviceEndpoint [https://cosmos-dbeaver-nosql.documents.azure.com:443], connectionPolicy",
            "serviceEndpoint [[REDACTED]], connectionPolicy");
        check(sanitizer, "Open (https://db.example.test/path(a))", "Open ([REDACTED])");
        check(sanitizer, "Open [https://[2001:db8::1]/path]", "Open [[REDACTED]]");
        check(sanitizer, "Microsoft SQL Server 2022 (RC1) - 16.0.950.9 (X64)",
            "Microsoft SQL Server 2022 (RC1) - 16.0.950.9 (X64)");
        String closingDelimiters = ")]}".repeat(70_000);
        check(sanitizer, "https://db.example.test/path" + closingDelimiters, "[REDACTED]" + closingDelimiters);
        check(sanitizer, "Initiate connection pool with management database (H2 Embedded; jdbc:h2:/private/database)",
            "Initiate connection pool with management database (H2 Embedded; [REDACTED])");
        check(sanitizer,
            "Transfer data from \"public.orders@postgres (2)\" into \"archive.orders [Existing]\"",
            "Transfer data from \"[REDACTED]@postgres (2)\" into \"[REDACTED] [Existing]\"");
        check(sanitizer,
            "Transfer data from \"public.orders@db.example.test\" into \"archive.orders [Existing]\" host=db.example.test",
            "Transfer data from \"[REDACTED]@[REDACTED]\" into \"[REDACTED] [Existing]\" host=[REDACTED]");
        check(sanitizer,
            "password=secret token=private host=192.168.1.10 https://user:pass@db.example.test/db /home/bob/workspace",
            "password=[REDACTED] token=[REDACTED] host=[REDACTED] [REDACTED] [REDACTED]");
        check(sanitizer,
            "SQL = CREATE USER DBV_TEST_SYSDG IDENTIFIED BY \"1234\", Original SQL = CREATE USER DBV_TEST_SYSDG IDENTIFIED BY \"1234\"",
            "SQL = CREATE USER DBV_TEST_SYSDG IDENTIFIED BY \"[REDACTED]\", Original SQL = CREATE USER DBV_TEST_SYSDG IDENTIFIED BY \"[REDACTED]\"");
        check(sanitizer, "Open [custom://user:pass@db.example.test/path]", "Open [[REDACTED]]");
        check(sanitizer, "Authorization: Bearer private-token\nConnect to 2001:db8::1",
            "Authorization: [REDACTED]\nConnect to [REDACTED]");
        check(new DiagnosticInfoSanitizer(Map.of("API_TOKEN", "private_token")),
            "org.osgi.framework.system.packages=java.lang,private_token\n"
                + "Transfer data from \"public.private_token@postgres\" into \"archive.orders [Existing]\"",
            "org.osgi.framework.system.packages=java.lang,[REDACTED]\n"
                + "Transfer data from \"public.[REDACTED]@postgres\" into \"[REDACTED] [Existing]\"");
        check(sanitizer,
            "Transfer data from \"SELECT * FROM jobs WHERE endpoint = 'private.customer.example'\" into \"archive.orders\"",
            "Transfer data from \"SELECT * FROM jobs WHERE endpoint = '[REDACTED]'\" into \"[REDACTED]\"");
        check(sanitizer,
            "org.osgi.framework.system.capabilities.extra=com.acme.feature;endpoint=private.customer.example;"
                + "com.acme.key=\"value;private.customer.example,other.customer.example\",com.acme.other",
            "org.osgi.framework.system.capabilities.extra=com.acme.feature;endpoint=[REDACTED];"
                + "com.acme.key=\"value;[REDACTED],[REDACTED]\",com.acme.other");
        check(sanitizer,
            "Transférer les données de « SELECT * FROM jobs WHERE endpoint = 'private.customer.example'@postgres » "
                + "vers « archive.orders [Existing] »",
            "Transférer les données de « SELECT * FROM jobs WHERE endpoint = '[REDACTED]'@postgres » "
                + "vers « [REDACTED] [Existing] »");
        check(sanitizer,
            "Transfer data from \"private.customer.example\" into \"archive.orders [Existing]\"",
            "Transfer data from \"[REDACTED]\" into \"[REDACTED] [Existing]\"");
        check(sanitizer,
            "Transfer data from \"public.orders@postgres\" into \"private.customer.example\"",
            "Transfer data from \"[REDACTED]@postgres\" into \"[REDACTED]\"");
        check(sanitizer,
            "Transfer data from \"private.customer.example@backup\" into \"private.customer.example [Existing]\"",
            "Transfer data from \"[REDACTED]@backup\" into \"[REDACTED] [Existing]\"");
        checkDiagnosticConfiguration();
    }

    private static void checkDiagnosticConfiguration() {
        DiagnosticInfoSanitizer sanitizer = new DiagnosticInfoSanitizer(Map.of(
            "SESSIONNAME", "Console", "HOMEDRIVE", "C:", "USERNAME", "tester", "ProgramFiles", "C:\\Program Files"
        ));
        String install = "C:\\Users\\tester\\Downloads\\dbeaver (9)\\dbeaver";
        check(sanitizer, "java.home=" + install + "\\jre\r\nuser.dir=" + install,
            "java.home=[REDACTED]\r\nuser.dir=[REDACTED]");
        check(sanitizer, "--launcher.library\n" + install + "\\\\plugins/org.eclipse.launcher_1.0.jar\n-startup\n"
            + install + "\\launcher.jar", "--launcher.library\n[REDACTED]\n-startup\n[REDACTED]");
        check(sanitizer, "Framework arguments: -eclipse.keyring " + install + "\\secure storage -os win32",
            "Framework arguments: -eclipse.keyring [REDACTED] -os win32");
        check(sanitizer, "ProgramFiles=C:\\Program Files\nCommonProgramFiles=C:\\Program Files\\Common Files",
            "ProgramFiles=[REDACTED]\nCommonProgramFiles=[REDACTED]");
        check(sanitizer, "Open (C:\\Program Files\\DBeaver (9)\\file.txt)", "Open ([REDACTED])");
        check(sanitizer, "java.library.path=" + install + ";C:\\Program Files\\dotnet\\;C:\\Windows;.",
            "java.library.path=[REDACTED];[REDACTED];[REDACTED];.");
        check(sanitizer, "Instance path: 'file:/C:/Users/tester/AppData/workspace6/'",
            "Instance path: '[REDACTED]'");
        check(sanitizer, "osgi.framework=file:/c:/Users/tester/Downloads/dbeaver (9)/plugins/framework.jar\r\n"
            + "osgi.frameworkClassPath=., file:c:/Users/tester/Downloads/dbeaver (9)/plugins/framework.jar",
            "osgi.framework=[REDACTED]\r\nosgi.frameworkClassPath=., [REDACTED]");
        check(sanitizer, "Path=C:/Users/tester/Downloads/dbeaver (9)/jre/bin;C:\\Program Files\\Java;",
            "Path=[REDACTED];[REDACTED];");
        check(sanitizer, "/bundle_defaults/com.dbeaver.ee.qmdb.core/qm.skipMetadataQueries=false\r\n"
            + "/instance/org.eclipse.ui.editors/background=255,255,255\r\n"
            + "@org.eclipse.core.net=1.5.900.v20260507-0401",
            "/bundle_defaults/com.dbeaver.ee.qmdb.core/qm.skipMetadataQueries=false\r\n"
            + "/instance/org.eclipse.ui.editors/background=255,255,255\r\n"
            + "@org.eclipse.core.net=1.5.900.v20260507-0401");
        check(sanitizer, "/instance/org.jkiss.dbeaver.core/dialog.default.folder=C\\:\\\\Users\\\\tester\\\\Private folder (9)",
            "/instance/org.jkiss.dbeaver.core/dialog.default.folder=[REDACTED]");
        check(sanitizer, "/instance/org.jkiss.dbeaver.core/dialog.default.folder=C\\:\\\\Users\\\\tester\\\\Private \\u0438\\u043D\\u0444",
            "/instance/org.jkiss.dbeaver.core/dialog.default.folder=[REDACTED]");
        check(sanitizer, "/instance/org.jkiss.dbeaver.ui.editors.erd/Palette\\ state=4",
            "/instance/org.jkiss.dbeaver.ui.editors.erd/Palette\\ state=4");
        check(sanitizer, "\\!/=", "\\!/=");
        String commands = "/instance/org.eclipse.ui.workbench/org.eclipse.ui.commands="
            + "<?xml version\\=\"1.0\" encoding\\=\"UTF-8\"?>\\r\\n<org.eclipse.ui.commands>\\r\\n"
            + "<keyBinding keySequence\\=\"CTRL+I\"/>\\r\\n"
            + "<keyBinding commandId\\=\"org.jkiss.dbeaver.ui.ai.showCompletion\"/>\\r\\n</org.eclipse.ui.commands>";
        check(sanitizer, commands, commands);
        check(sanitizer, "/instance/org.jkiss.dbeaver.core/password=\"private value\"\n"
            + "/instance/org.jkiss.dbeaver.core/host=db.example.test",
            "/instance/org.jkiss.dbeaver.core/password=\"[REDACTED]\"\n"
            + "/instance/org.jkiss.dbeaver.core/host=[REDACTED]");
        check(sanitizer, "applicationXMI=org.eclipse.ui.workbench/LegacyIDE.e4xmi\n"
            + "eclipse.p2.data.area=@config.dir/../p2\n"
            + "org.eclipse.equinox.simpleconfigurator.configUrl=file:org.eclipse.equinox.simpleconfigurator/bundles.info",
            "applicationXMI=org.eclipse.ui.workbench/LegacyIDE.e4xmi\n"
            + "eclipse.p2.data.area=@config.dir/../p2\n"
            + "org.eclipse.equinox.simpleconfigurator.configUrl=file:org.eclipse.equinox.simpleconfigurator/bundles.info");
        check(sanitizer, "osgi.bundles=reference:file:org.eclipse.osgi_1.0.v20251022-1724.jar,"
            + "reference:file:org.eclipse.config_1.0.v20251111-1031.jar@1:start",
            "osgi.bundles=reference:file:org.eclipse.osgi_1.0.v20251022-1724.jar,"
            + "reference:file:org.eclipse.config_1.0.v20251111-1031.jar@1:start");
        check(sanitizer, "org.eclipse.ui.console (3.17.100.v20260804-1532) \"Console\" [Active]",
            "org.eclipse.ui.console (3.17.100.v20260804-1532) \"Console\" [Active]");
        check(sanitizer, "Id: org.eclipse.core.commands, Version: 3.13.0.v20260605-1749, "
            + "Location: reference:file:plugins/org.eclipse.core.commands_3.13.0.v20260605-1749.jar",
            "Id: org.eclipse.core.commands, Version: 3.13.0.v20260605-1749, "
            + "Location: reference:file:plugins/org.eclipse.core.commands_3.13.0.v20260605-1749.jar");
        check(sanitizer, "   Service: Signature, Algorithm: SHA256withDSA, Class: sun.security.provider.DSA$SHA256withDSA\n"
            + "    Aliases: 1.3.101.110\n    Aliases: SHA512(256)/RSA\nConnect to 1.3.101.110",
            "   Service: Signature, Algorithm: SHA256withDSA, Class: sun.security.provider.DSA$SHA256withDSA\n"
            + "    Aliases: 1.3.101.110\n    Aliases: SHA512(256)/RSA\nConnect to [REDACTED]");
        check(new DiagnosticInfoSanitizer(Map.of("SESSIONNAME", "SHA")),
            "Class: sun.security.provider.SHA\nSESSIONNAME=SHA",
            "Class: sun.security.provider.SHA\nSESSIONNAME=[REDACTED]");
        check(new DiagnosticInfoSanitizer(Map.of("API_TOKEN", "private_token")),
            "Class: sun.security.private_token\norg.example.plugin (1.0.0) \"private_token\" [Active]",
            "Class: sun.security.[REDACTED]\norg.example.plugin (1.0.0) \"[REDACTED]\" [Active]");
    }

    private static void check(@NotNull DiagnosticInfoSanitizer sanitizer, @NotNull String input, @NotNull String expected) {
        String actual = sanitizer.sanitize(input);
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + "\nActual: " + actual);
        }
    }
}
