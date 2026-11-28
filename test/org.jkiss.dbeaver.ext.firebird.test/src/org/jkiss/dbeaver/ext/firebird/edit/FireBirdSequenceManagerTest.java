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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataSource;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdMetaModel;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FireBirdSequenceManagerTest extends DBeaverUnitTest {

    private FireBirdDataSource dataSource;
    private GenericStructContainer container;
    private DBRProgressMonitor monitor;

    @BeforeEach
    public void setUp() {
        dataSource = Mockito.mock(FireBirdDataSource.class);
        container = Mockito.mock(GenericStructContainer.class);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        monitor = Mockito.mock(DBRProgressMonitor.class);
        setServerVersion(4);
    }

    @Test
    public void createsSequenceWithDefaultsWithoutOptions() throws Exception {
        Assertions.assertEquals(List.of("CREATE SEQUENCE SEQ_ID"), getCreateScripts(newSequence()));
    }

    @Test
    public void createsSequenceWithStartValueAndIncrement() throws Exception {
        FireBirdSequence sequence = newSequence();
        sequence.setStartValue(100L);
        sequence.setIncrementBy(-5L);

        Assertions.assertEquals(
            List.of("CREATE SEQUENCE SEQ_ID START WITH 100 INCREMENT BY -5"),
            getCreateScripts(sequence));
    }

    @Test
    public void firebird3StartsWithCurrentValue() throws Exception {
        setServerVersion(3);
        Assertions.assertEquals(List.of("CREATE SEQUENCE SEQ_ID"), getCreateScripts(newSequence()));

        // Firebird 3 returns START WITH plus the increment first
        FireBirdSequence sequence = newSequence();
        sequence.setStartValue(100L);
        sequence.setIncrementBy(5L);
        Assertions.assertEquals(
            List.of("CREATE SEQUENCE SEQ_ID START WITH 95 INCREMENT BY 5"),
            getCreateScripts(sequence));

        sequence.setStartValue(1L);
        Assertions.assertEquals(
            List.of("CREATE SEQUENCE SEQ_ID START WITH -4 INCREMENT BY 5"),
            getCreateScripts(sequence));
    }

    @Test
    public void firebird25RejectsOptions() throws Exception {
        setServerVersion(2);
        FireBirdSequence sequence = newSequence();
        Assertions.assertFalse(sequence.isOptionsSupported());
        Assertions.assertEquals(List.of("CREATE SEQUENCE SEQ_ID"), getCreateScripts(sequence));

        sequence.setStartValue(100L);
        Assertions.assertThrows(DBException.class, () -> getCreateScripts(sequence));

        sequence.setStartValue(1L);
        sequence.setIncrementBy(5L);
        Assertions.assertThrows(DBException.class, () -> getCreateScripts(sequence));
        Assertions.assertThrows(DBException.class, () -> getModifyScripts(sequence, Map.of("incrementBy", 5L)));
    }

    @Test
    public void rejectsEmptyOrZeroIncrement() {
        FireBirdSequence sequence = newSequence();
        sequence.setIncrementBy(null);
        Assertions.assertThrows(DBException.class, () -> getModifyScripts(sequence, Map.of("incrementBy", "")));

        sequence.setIncrementBy(0L);
        Assertions.assertThrows(DBException.class, () -> getModifyScripts(sequence, Map.of("incrementBy", 0L)));
        Assertions.assertThrows(DBException.class, () -> getCreateScripts(sequence));
    }

    @Test
    public void newSequenceDoesNotReadCurrentValue() throws Exception {
        // The sequence does not exist in the database yet, so its current value must not be queried
        Assertions.assertNull(newSequence().getLastValue(monitor));
    }

    @Test
    public void changesIncrement() throws Exception {
        FireBirdSequence sequence = newSequence();
        sequence.setIncrementBy(10L);

        Assertions.assertEquals(
            List.of("ALTER SEQUENCE SEQ_ID INCREMENT BY 10"),
            getModifyScripts(sequence, Map.of("incrementBy", 10L)));
        Assertions.assertEquals(List.of(), getModifyScripts(sequence, Map.of("description", "Ids")));
    }

    @Test
    public void readsStartValueAndIncrement() throws Exception {
        JDBCResultSet resultSet = mockSequenceResultSet();

        FireBirdSequence sequence = (FireBirdSequence) new FireBirdMetaModel()
            .createSequenceImpl(Mockito.mock(JDBCSession.class), container, resultSet);

        Assertions.assertNotNull(sequence);
        Assertions.assertEquals("SEQ_ID", sequence.getName());
        Assertions.assertEquals(10L, sequence.getStartValue());
        Assertions.assertEquals(5L, sequence.getIncrementBy());
        Assertions.assertEquals("Ids", sequence.getDescription());
    }

    @Test
    public void firebird3ReadsStartValueAsFirstValue() throws Exception {
        setServerVersion(3);
        JDBCResultSet resultSet = mockSequenceResultSet();

        FireBirdSequence sequence = (FireBirdSequence) new FireBirdMetaModel()
            .createSequenceImpl(Mockito.mock(JDBCSession.class), container, resultSet);

        // RDB$INITIAL_VALUE is the current value on Firebird 3, the first NEXT VALUE FOR adds the increment
        Assertions.assertNotNull(sequence);
        Assertions.assertEquals(15L, sequence.getStartValue());
        Assertions.assertEquals(5L, sequence.getIncrementBy());
    }

    @Test
    public void oldServersHaveNoStartValue() throws Exception {
        setServerVersion(2);
        JDBCResultSet resultSet = mockSequenceResultSet();

        FireBirdSequence sequence = (FireBirdSequence) new FireBirdMetaModel()
            .createSequenceImpl(Mockito.mock(JDBCSession.class), container, resultSet);

        Assertions.assertNotNull(sequence);
        Assertions.assertNull(sequence.getStartValue());
        Assertions.assertEquals(1L, sequence.getIncrementBy());
    }

    private void setServerVersion(int majorVersion) {
        Mockito.when(dataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt()))
            .thenAnswer(invocation -> majorVersion >= invocation.<Integer>getArgument(0));
    }

    @NotNull
    private JDBCResultSet mockSequenceResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$GENERATOR_NAME")).thenReturn("SEQ_ID                         ");
        Mockito.when(resultSet.getString("RDB$DESCRIPTION")).thenReturn("Ids");
        Mockito.when(resultSet.getLong("RDB$INITIAL_VALUE")).thenReturn(10L);
        Mockito.when(resultSet.getInt("RDB$GENERATOR_INCREMENT")).thenReturn(5);
        return resultSet;
    }

    @NotNull
    private FireBirdSequence newSequence() {
        return new FireBirdSequence(container, "SEQ_ID") {
            @NotNull
            @Override
            public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
                return getName();
            }
        };
    }

    @NotNull
    private List<String> getCreateScripts(@NotNull FireBirdSequence sequence) throws DBException {
        return new TestSequenceManager().getCreateActions(monitor, sequence).stream().map(DBEPersistAction::getScript).toList();
    }

    @NotNull
    private List<String> getModifyScripts(
        @NotNull FireBirdSequence sequence,
        @NotNull Map<String, Object> properties
    ) throws DBException {
        return new TestSequenceManager().getModifyActions(monitor, sequence, properties).stream()
            .map(DBEPersistAction::getScript)
            .toList();
    }

    /**
     * Exposes the protected action builders of the manager.
     */
    private static class TestSequenceManager extends FireBirdSequenceManager {

        @NotNull
        List<DBEPersistAction> getCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull FireBirdSequence sequence
        ) throws DBException {
            // The constructor is protected, an anonymous subclass may call it
            ObjectCreateCommand command = new ObjectCreateCommand(sequence, "Create", Map.of()) { };
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectCreateActions(monitor, Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }

        @NotNull
        List<DBEPersistAction> getModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull FireBirdSequence sequence,
            @NotNull Map<String, Object> properties
        ) throws DBException {
            ObjectChangeCommand command = new ObjectChangeCommand(sequence);
            command.getProperties().putAll(properties);
            // The command context validates a command before it builds the persist actions
            validateObjectProperties(monitor, command, Map.of());
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(monitor, Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }
    }
}
