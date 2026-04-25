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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Regression tests for dbeaver/dbeaver#38044 — resetting a driver property
 * to null in the Driver Properties tab caused a connection failure
 * because {@code JDBCDataSource.fillConnectionProperties} forwarded the
 * cleared value to the JDBC driver as an empty string. SQLite (and other
 * drivers that interpret an empty PRAGMA literally) responded with
 * {@code SQLITE_ERROR: SQL error or missing database (incomplete input)}.
 */
public class JDBCDataSourcePropertiesTest extends DBeaverUnitTest {

    @Test
    public void copyNonEmptyValuePropagatesToJdbcProperties() {
        Properties target = new Properties();

        JDBCDataSource.copyConnectionProperty(target, "currentSchema", "myschema");

        assertEquals("myschema", target.getProperty("currentSchema"));
    }

    @Test
    public void copyNullValueIsSkipped() {
        Properties target = new Properties();
        // Bug #38044: a property cleared by the user is stored as a null value;
        // forwarding it as `setProperty(key, "")` made SQLite reject the connection
        // with `(incomplete input)` while applying an empty PRAGMA.
        JDBCDataSource.copyConnectionProperty(target, "encoding", null);

        assertFalse(
            "null value must not produce an empty-string entry in the JDBC Properties bag",
            target.containsKey("encoding"));
        assertNull(target.getProperty("encoding"));
    }

    @Test
    public void copyEmptyStringValueIsSkipped() {
        Properties target = new Properties();
        // Same shape as the null case: an empty-string text widget should mean
        // "do not send this property", not "send empty".
        JDBCDataSource.copyConnectionProperty(target, "encoding", "");

        assertFalse(target.containsKey("encoding"));
    }

    @Test
    public void copyNullKeyIsSkipped() {
        Properties target = new Properties();
        JDBCDataSource.copyConnectionProperty(target, null, "anything");

        assertEquals("the target Properties must remain empty", 0, target.size());
    }

    @Test
    public void copyEmptyKeyIsSkipped() {
        Properties target = new Properties();
        JDBCDataSource.copyConnectionProperty(target, "", "anything");

        assertEquals(0, target.size());
    }

    @Test
    public void copyNumericValueIsStringified() {
        Properties target = new Properties();
        JDBCDataSource.copyConnectionProperty(target, "loginTimeout", 30);

        assertEquals("30", target.getProperty("loginTimeout"));
    }

    @Test
    public void copyBooleanValueIsStringified() {
        Properties target = new Properties();
        JDBCDataSource.copyConnectionProperty(target, "ssl", Boolean.TRUE);

        assertEquals("true", target.getProperty("ssl"));
    }

    @Test
    public void copyOverridesExistingPropertyWhenValuePresent() {
        Properties target = new Properties();
        target.setProperty("encoding", "UTF-8");

        JDBCDataSource.copyConnectionProperty(target, "encoding", "UTF-16");

        assertEquals("UTF-16", target.getProperty("encoding"));
    }

    @Test
    public void copyDoesNotRemoveExistingPropertyWhenNullArrives() {
        // The helper is additive only — when the loop encounters a null/cleared
        // entry it must leave the target untouched, so a previously-copied
        // property from internal datasource defaults survives.
        Properties target = new Properties();
        target.setProperty("encoding", "UTF-8");

        JDBCDataSource.copyConnectionProperty(target, "encoding", null);

        assertEquals(
            "previously-set properties must survive a subsequent null entry",
            "UTF-8",
            target.getProperty("encoding"));
    }
}
