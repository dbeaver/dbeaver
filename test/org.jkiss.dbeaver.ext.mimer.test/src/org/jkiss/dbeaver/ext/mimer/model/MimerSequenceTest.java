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

import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * {@link MimerSequence#buildCreateDDL()} - branches between the modern (11.0+) {@code CREATE
 * SEQUENCE} and the older 10.1 {@code CREATE UNIQUE SEQUENCE} grammars depending on {@link
 * MimerDataSource#supportsModernSequenceSyntax()}. Assertions check the
 * DDL's clause content rather than the exact schema-qualified-name prefix, since that part comes
 * from core's own {@code DBUtils}/dialect quoting machinery, not anything Mimer-specific.
 *
 * @author Mimer Information Technology
 */
public class MimerSequenceTest extends DBeaverUnitTest {

    @Mock
    private GenericStructContainer container;

    @Mock
    private MimerDataSource dataSource;

    @BeforeEach
    public void setUp() {
        when(container.getDataSource()).thenReturn(dataSource);
        when(dataSource.getSQLDialect()).thenReturn(new MimerSQLDialect());
    }

    @Test
    public void modernCreateDDLHasNoMinMaxCycleWhenUnsetMeansNoMinMaxCycle() {
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
        MimerSequence seq = new MimerSequence(container, "my_seq");

        String ddl = seq.buildCreateDDL();

        Assertions.assertTrue(ddl.startsWith("CREATE SEQUENCE "), ddl);
        Assertions.assertTrue(ddl.contains("AS INTEGER"), ddl);
        Assertions.assertTrue(ddl.contains("START WITH 1"), ddl); // constructor default
        Assertions.assertTrue(ddl.contains("INCREMENT BY 1"), ddl); // constructor default
        Assertions.assertTrue(ddl.contains("NO MINVALUE"), ddl);
        Assertions.assertTrue(ddl.contains("NO MAXVALUE"), ddl);
        Assertions.assertTrue(ddl.contains("NO CYCLE"), ddl);
        Assertions.assertFalse(ddl.contains("\n    IN "), () -> "no databank set, shouldn't appear: " + ddl);
    }

    @Test
    public void modernCreateDDLIncludesEveryOptionalClauseWhenSet() {
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
        MimerSequence seq = new MimerSequence(container, "my_seq");
        seq.setDataType("BIGINT");
        seq.setLastValue(100L);
        seq.setIncrementBy(5L);
        seq.setMinValue(0L);
        seq.setMaxValue(1000L);
        seq.setCycle(true);
        seq.setDatabank("mydb");

        String ddl = seq.buildCreateDDL();

        Assertions.assertTrue(ddl.contains("AS BIGINT"), ddl);
        Assertions.assertTrue(ddl.contains("START WITH 100"), ddl);
        Assertions.assertTrue(ddl.contains("INCREMENT BY 5"), ddl);
        Assertions.assertTrue(ddl.contains("MINVALUE 0") && !ddl.contains("NO MINVALUE"), ddl);
        Assertions.assertTrue(ddl.contains("MAXVALUE 1000") && !ddl.contains("NO MAXVALUE"), ddl);
        Assertions.assertTrue(ddl.contains("\n    CYCLE") && !ddl.contains("NO CYCLE"), ddl);
        // "mydb" is a plain lowercase identifier, so the Mimer SQL dialect doesn't quote it.
        Assertions.assertTrue(ddl.contains("\n    IN mydb"), ddl);
    }

    /**
     * Mimer SQL 10.1's older grammar has no {@code INCREMENT BY}, {@code CYCLE}, or databank
     * clause at all - not just omitted here, genuinely unrepresentable against that server
     * version.
     */
    @Test
    public void legacyCreateDDLHasNoIncrementCycleOrDatabankClause() {
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(false);
        MimerSequence seq = new MimerSequence(container, "my_seq");
        seq.setDataType("INTEGER");
        seq.setLastValue(10L);
        seq.setMaxValue(100L);
        seq.setMinValue(1L);
        seq.setIncrementBy(5L); // set, but the legacy grammar has no INCREMENT BY at all
        seq.setDatabank("mydb"); // same for databank placement

        String ddl = seq.buildCreateDDL();

        Assertions.assertTrue(ddl.startsWith("CREATE UNIQUE SEQUENCE "), ddl);
        Assertions.assertTrue(ddl.contains("INITIAL_VALUE 10"), ddl);
        Assertions.assertTrue(ddl.contains("MAX_VALUE 100"), ddl);
        Assertions.assertTrue(ddl.contains("MIN_VALUE 1"), ddl);
        Assertions.assertTrue(ddl.contains("AS INTEGER"), ddl);
        Assertions.assertFalse(ddl.contains("INCREMENT"), () -> "legacy grammar has no INCREMENT BY: " + ddl);
        Assertions.assertFalse(ddl.contains("CYCLE"), () -> "legacy grammar has no CYCLE: " + ddl);
        Assertions.assertFalse(ddl.contains(" IN "), () -> "legacy grammar has no databank placement: " + ddl);
    }

    @Test
    public void restartDDLUsesTheGivenValueOverTheCurrentLastValue() {
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
        MimerSequence seq = new MimerSequence(container, "my_seq");
        seq.setLastValue(1L);

        String ddl = seq.buildRestartDDL(42L);

        Assertions.assertTrue(ddl.startsWith("ALTER SEQUENCE "), ddl);
        Assertions.assertTrue(ddl.endsWith("RESTART WITH 42"), ddl);
    }

    @Test
    public void restartDDLFallsBackToTheCurrentLastValueWhenNoneGiven() {
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
        MimerSequence seq = new MimerSequence(container, "my_seq");
        seq.setLastValue(7L);

        Assertions.assertTrue(seq.buildRestartDDL(null).endsWith("RESTART WITH 7"));
    }

    /**
     * A persisted sequence must never populate {@code lastValue} from {@code
     * SEQUENCES.START_VALUE}. Reading it back always ends up blank, which is the correct, honest state for a
     * write-only "type a value here to restart" field.
     */
    @Test
    public void persistedSequenceNeverPopulatesLastValueFromStartValue() throws Exception {
        var dbResult = org.mockito.Mockito.mock(org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet.class);
        when(dbResult.getString("SEQUENCE_NAME")).thenReturn("my_seq");
        when(dbResult.getLong("START_VALUE")).thenReturn(1L);
        when(dbResult.getLong("MINIMUM_VALUE")).thenReturn(1L);
        when(dbResult.getLong("MAXIMUM_VALUE")).thenReturn(2147483647L);
        when(dbResult.getLong("INCREMENT")).thenReturn(1L);

        MimerSequence seq = new MimerSequence(container, dbResult);

        Assertions.assertNull(seq.getLastValue());
    }
}
