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
package org.jkiss.dbeaver.ext.h2;

import org.jkiss.dbeaver.ext.h2.util.H2Utils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class H2UtilsTest extends DBeaverUnitTest {
    private final String originalAllowedClasses = System.getProperty("h2.allowedClasses");

    @AfterEach
    public void restoreAllowedClasses() {
        if (originalAllowedClasses != null) {
            System.setProperty("h2.allowedClasses", originalAllowedClasses);
        } else {
            System.clearProperty("h2.allowedClasses");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "CREATE ALIAS EXEC AS 'void exec() {}'",
        "create force alias exec as $$ void exec() {} $$",
        "CREATE OR REPLACE ALIAS EXEC DETERMINISTIC AS 'void exec() {}'",
        "-- comment\nCREATE ALIAS EXEC AS 'void exec() {}'",
        "// comment\nCREATE ALIAS EXEC AS 'void exec() {}'",
        "/* comment */ CREATE ALIAS EXEC AS 'void exec() {}'",
        "CREATE/**/ALIAS EXEC/**/AS 'void exec() {}'",
        "; CREATE ALIAS EXEC AS 'void exec() {}'",
        "SELECT 1; CREATE ALIAS EXEC AS 'void exec() {}'",
        "CREATE TRIGGER EXEC BEFORE INSERT ON TEST AS 'org.h2.api.Trigger create() { return null; }'"
    })
    public void detectsJavaSourceDefinitions(String query) {
        Assertions.assertTrue(H2Utils.isJavaSourceDefinition(query));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "CREATE ALIAS SIN FOR 'java.lang.Math.sin'",
        "CREATE FORCE ALIAS SPATIAL FOR 'org.h2gis.functions.factory.H2GISFunctions.load'",
        "CREATE ALIAS \"X AS Y\" FOR 'java.lang.Math.sin'",
        "CREATE ALIAS [X AS Y] FOR 'java.lang.Math.sin'",
        "CREATE TRIGGER AUDIT BEFORE INSERT ON TEST CALL 'com.example.AuditTrigger'",
        "SELECT 'CREATE ALIAS EXEC AS malicious'",
        "CREATE TABLE ALIASES (SOURCE VARCHAR)"
    })
    public void permitsOtherStatements(String query) {
        Assertions.assertFalse(H2Utils.isJavaSourceDefinition(query));
    }

    @Test
    public void detectsRestrictedClassLoading() {
        System.setProperty("h2.allowedClasses", "java.lang.Math,org.h2.mvstore.db.MVTableEngine");
        Assertions.assertTrue(H2Utils.isClassLoadingRestricted());

        System.setProperty("h2.allowedClasses", "*");
        Assertions.assertFalse(H2Utils.isClassLoadingRestricted());
    }
}
