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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.mockito.Mockito.when;

/**
 * @author Mimer Information Technology
 */
public class MimerSQLDialectTest extends DBeaverUnitTest {

    @Mock
    private DBPDataSource dataSource;

    @Mock
    private MimerTableColumn column;

    @Test
    public void dualTableIsOneRow() {
        MimerSQLDialect dialect = new MimerSQLDialect();
        Assertions.assertEquals("SYSTEM.ONEROW", dialect.getDualTableName());
    }

    @Test
    public void executeKeywordIsCall() {
        MimerSQLDialect dialect = new MimerSQLDialect();
        Assertions.assertTrue(Arrays.asList(dialect.getExecuteKeywords()).contains("CALL"));
    }

    @Test
    public void hasPsmBlockBounds() {
        MimerSQLDialect dialect = new MimerSQLDialect();
        Assertions.assertNotNull(dialect.getBlockBoundStrings());
        Assertions.assertTrue(dialect.getBlockBoundStrings().length >= 1);
    }

    @Test
    public void supportsTableDropCascade() {
        // Enables the "Cascade" checkbox on the table delete-confirmation dialog - Mimer SQL's
        // DROP TABLE accepts [RESTRICT | CASCADE].
        Assertions.assertTrue(new MimerSQLDialect().supportsTableDropCascade());
    }

    /**
     * A type name that already carries its own {@code COLLATE} clause (as DBeaver's Data
     * Transfer feature can pass back in, see the class Javadoc) must not get a further Length or
     * COLLATE appended - confirmed live as the cause of a malformed {@code "NATIONAL CHARACTER
     * COLLATE UCS_BASIC(10)"} target DDL during a cross-database table export.
     */
    @Test
    public void aTypeNameAlreadyCarryingCollateGetsNoFurtherModifiers() {
        when(column.getMaxLength()).thenReturn(10L);

        String modifiers = new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "NATIONAL CHARACTER COLLATE UCS_BASIC", DBPDataKind.STRING);

        Assertions.assertNull(modifiers);
    }

    /**
     * A not-yet-persisted column's explicitly-typed Length must survive, even for a plain
     * (non-"varying") sized character type - confirmed live: creating a {@code NATIONAL
     * CHARACTER} column with Length 20 silently dropped it, since core's own length computation
     * (deferred to for a persisted column, see the class Javadoc) happened to elide it.
     */
    @Test
    public void pendingColumnKeepsAnExplicitLengthOnAPlainCharacterType() {
        when(column.isPersisted()).thenReturn(false);
        when(column.getMaxLength()).thenReturn(20L);

        String modifiers = new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "NATIONAL CHARACTER", DBPDataKind.STRING);

        Assertions.assertEquals("(20)", modifiers);
    }

    /**
     * Confirmed live: {@code NATIONAL CHARACTER} - unlike {@code CHARACTER}, {@code CHARACTER
     * VARYING} or {@code NATIONAL CHARACTER VARYING} - doesn't reliably resolve to {@link
     * DBPDataKind#STRING} through the normal driver-type-code path, so the length-preserving
     * branch must key off the type name alone, not {@code dataKind}.
     */
    @Test
    public void pendingColumnKeepsLengthEvenWhenDataKindIsNotResolvedToString() {
        when(column.isPersisted()).thenReturn(false);
        when(column.getMaxLength()).thenReturn(20L);

        String modifiers = new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "NATIONAL CHARACTER", DBPDataKind.UNKNOWN);

        Assertions.assertEquals("(20)", modifiers);
    }

    @Test
    public void pendingColumnWithNoExplicitLengthGetsNoModifier() {
        when(column.isPersisted()).thenReturn(false);
        when(column.getMaxLength()).thenReturn(0L);

        String modifiers = new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "CHARACTER", DBPDataKind.STRING);

        Assertions.assertNull(modifiers);
    }

    /**
     * TINYINT never accepts a length. Checked with
     * {@code dataKind = STRING} specifically because that's what a not-yet-persisted Data
     * Transfer target column actually had at the point this method was called.
     */
    @Test
    public void tinyintNeverGetsALengthRegardlessOfDataKind() {
        when(column.getMaxLength()).thenReturn(3L);

        Assertions.assertNull(new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "ODBC.TINYINT", DBPDataKind.STRING));
        Assertions.assertNull(new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "ODBC.TINYINT", DBPDataKind.NUMERIC));
        Assertions.assertNull(new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "TINYINT", DBPDataKind.STRING));
    }

    @Test
    public void collationIsAppendedAfterTheLength() {
        when(column.isPersisted()).thenReturn(false);
        when(column.getCollation()).thenReturn("ISO8BIT");
        when(column.getMaxLength()).thenReturn(50L);

        String modifiers = new MimerSQLDialect().getColumnTypeModifiers(
            dataSource, column, "CHARACTER", DBPDataKind.STRING);

        Assertions.assertEquals("(50) COLLATE ISO8BIT", modifiers);
    }
}
