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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.Log;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class MySQLNativeToolHandlerTest extends DBeaverUnitTest {

    @Test
    void detectsNonAsciiUsernamesForWindowsNativeClients() {
        Assertions.assertFalse(MySQLNativeToolHandler.requiresUnicodeUsernameOptionFile("admin"));
        Assertions.assertTrue(MySQLNativeToolHandler.requiresUnicodeUsernameOptionFile("admin_björn"));
        Assertions.assertTrue(MySQLNativeToolHandler.requiresUnicodeUsernameOptionFile("数据管理员"));
        Assertions.assertFalse(MySQLNativeToolHandler.requiresUnicodeUsernameOptionFile(null));
    }

    @Test
    void unicodeUsernameOptionFilePreservesUtf8WithoutOverridingPassword() throws IOException {
        Path credentialsFile = MySQLNativeToolHandler.createUsernameOptionFile("admin_björn");
        try {
            Assertions.assertEquals("[client]\nuser=\"admin_björn\"", Files.readString(credentialsFile));
            Assertions.assertFalse(Files.readString(credentialsFile).contains("password="));
        } finally {
            Files.deleteIfExists(credentialsFile);
        }
    }

    @Test
    void unicodeUsernameOptionFileEscapesSpecialCharacters() throws IOException {
        String username = "用户\\path\"#\npassword=unexpected";
        Path credentialsFile = MySQLNativeToolHandler.createUsernameOptionFile(username);
        try {
            String fileContents = Files.readString(credentialsFile);
            Assertions.assertEquals(
                "[client]\nuser=\"用户\\\\path\\\"#\\npassword=unexpected\"",
                fileContents
            );
            // A newline in the username must not add a new MySQL option.
            Assertions.assertFalse(fileContents.contains("\npassword="));
        } finally {
            Files.deleteIfExists(credentialsFile);
        }
    }

    @Test
    void passwordAndUsernameNewlinesCannotInjectAdditionalOptions() throws IOException {
        Path credentialsFile = MySQLNativeToolHandler.createCredentialsFile(
            "admin\nhost=unexpected",
            "secret\nport=3307"
        );
        try {
            String content = Files.readString(credentialsFile);
            Assertions.assertEquals(
                "[client]\nuser=\"admin\\nhost=unexpected\"\npassword=\"secret\\nport=3307\"",
                content
            );
            Assertions.assertFalse(content.contains("\nhost="));
            Assertions.assertFalse(content.contains("\nport="));
        } finally {
            Files.deleteIfExists(credentialsFile);
        }
    }

    @Test
    void differentExportsGetSeparateOptionFiles() throws IOException {
        Path first = MySQLNativeToolHandler.createUsernameOptionFile("first_用户");
        Path second = null;
        try {
            second = MySQLNativeToolHandler.createUsernameOptionFile("second_用户");
            Assertions.assertNotEquals(first, second);
            Assertions.assertEquals("[client]\nuser=\"first_用户\"", Files.readString(first));
            Assertions.assertEquals("[client]\nuser=\"second_用户\"", Files.readString(second));
        } finally {
            Files.deleteIfExists(first);
            if (second != null) {
                Files.deleteIfExists(second);
            }
        }
    }

    @Test
    void cleanupDeletesAllOptionFilesFromMultipleInvocations() throws IOException {
        Path credentialsFile = MySQLNativeToolHandler.createCredentialsFile("root", "sensitive-password");
        Path usernameFile = MySQLNativeToolHandler.createUsernameOptionFile("用户");
        List<Path> configFiles = new ArrayList<>(List.of(credentialsFile, usernameFile));

        try {
            MySQLNativeToolHandler.deleteConfigurationFiles(configFiles, Log.getLog(getClass()));

            Assertions.assertFalse(Files.exists(credentialsFile));
            Assertions.assertFalse(Files.exists(usernameFile));
            Assertions.assertTrue(configFiles.isEmpty());
        } finally {
            Files.deleteIfExists(credentialsFile);
            Files.deleteIfExists(usernameFile);
        }
    }

    @Test
    void unicodeOptionFileIgnoresHigherPriorityLoginPathWithoutChangingArguments() throws IOException {
        Path optionFile = Files.createTempFile("mysql-native-test-", ".cnf");
        try {
            ProcessBuilder process = new ProcessBuilder(
                "mysqldump", "--defaults-extra-file=" + optionFile.toAbsolutePath(), "--host=localhost");
            process.environment().put("MYSQL_TEST_LOGIN_FILE", "existing-login-path");

            MySQLNativeToolHandler.isolateLoginPathFromUnicodeUsernameOptionFile(process);

            Path isolatedLoginFile = Path.of(process.environment().get("MYSQL_TEST_LOGIN_FILE"));
            Assertions.assertEquals(
                optionFile.toAbsolutePath().resolveSibling(optionFile.getFileName() + ".missing-login-path"),
                isolatedLoginFile
            );
            Assertions.assertFalse(Files.exists(isolatedLoginFile));
            Assertions.assertEquals(
                List.of("mysqldump", "--defaults-extra-file=" + optionFile.toAbsolutePath(), "--host=localhost"),
                process.command()
            );
        } finally {
            Files.deleteIfExists(optionFile);
        }
    }

    @Test
    void regularCommandLineDoesNotChangeLoginPathEnvironment() {
        ProcessBuilder process = new ProcessBuilder("mysqldump", "-u", "user");
        process.environment().put("MYSQL_TEST_LOGIN_FILE", "existing-login-path");

        MySQLNativeToolHandler.isolateLoginPathFromUnicodeUsernameOptionFile(process);

        Assertions.assertEquals("existing-login-path", process.environment().get("MYSQL_TEST_LOGIN_FILE"));
    }

    @Test
    void createCredentialsFileEscapesPasswordForMySQLOptionFile() throws IOException {
        Path credentialsFile = MySQLNativeToolHandler.createCredentialsFile(
            "root",
            "Süper#Secret;\"quote'\\path"
        );

        try {
            Assertions.assertEquals(
                "[client]\nuser=\"root\"\npassword=\"Süper#Secret;\\\"quote'\\\\path\"",
                Files.readString(credentialsFile)
            );
        } finally {
            Files.deleteIfExists(credentialsFile);
        }
    }
}
