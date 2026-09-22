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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerSQLDialect;
import org.jkiss.dbeaver.ext.mimer.model.MimerSequence;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MimerSequenceManager}'s create-action wiring - delegates straight to {@link
 * MimerSequence#buildCreateDDL()}, whose own modern-vs-legacy branching is covered in full by
 * {@code MimerSequenceTest}.
 * <p>
 * {@code addObjectModifyActions} - the part of this class with the actually interesting logic
 * (only ever reacting to a changed {@code lastValue}, since Mimer SQL's {@code ALTER SEQUENCE}
 * supports nothing else - see the class Javadoc) - isn't covered here: see {@code
 * MimerSchemaManagerTest}'s Javadoc for why ({@code ObjectChangeCommand} can't be mocked across
 * this project's OSGi bundle boundaries, confirmed live).
 *
 * @author Mimer Information Technology
 */
public class MimerSequenceManagerTest extends DBeaverUnitTest {

    @Mock
    private GenericStructContainer container;

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private DBCExecutionContext executionContext;

    private final MimerSequenceManager manager = new MimerSequenceManager();

    @BeforeEach
    public void setUp() {
        when(container.getDataSource()).thenReturn(dataSource);
        when(dataSource.getSQLDialect()).thenReturn(new MimerSQLDialect());
    }

    @Test
    public void createActionDelegatesToTheSequenceSOwnBuildCreateDDL() throws Exception {
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
        MimerSequence sequence = new MimerSequence(container, "my_seq");

        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(sequence);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(sequence.buildCreateDDL(), actions.get(0).getScript());
    }
}
