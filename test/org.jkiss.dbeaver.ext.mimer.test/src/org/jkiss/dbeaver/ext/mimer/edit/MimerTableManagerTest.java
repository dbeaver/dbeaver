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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.mimer.model.MimerSQLDialect;
import org.jkiss.dbeaver.ext.mimer.model.MimerTable;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableColumn;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndex;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MimerTableManager#getChildTypes()} - declares the real Mimer SQL subclasses
 * ({@link MimerTableColumn}/{@link MimerTableIndex}) as child types instead of the inherited
 * generic ones, so the "DDL" tab's preview generation resolves the correct Mimer-specific
 * {@code SQLObjectEditor} for a still-pending column/index (Clustered/Ignore Nulls/auto-increment
 * sequence wiring).
 *
 * @author Mimer Information Technology
 */
public class MimerTableManagerTest extends DBeaverUnitTest {

    private final MimerTableManager manager = new MimerTableManager();

    @Test
    public void declaresTheMimerSpecificColumnAndIndexSubclasses() {
        List<Class<? extends DBSObject>> childTypes = Arrays.asList(manager.getChildTypes());

        Assertions.assertTrue(childTypes.contains(MimerTableColumn.class), childTypes::toString);
        Assertions.assertTrue(childTypes.contains(MimerTableIndex.class), childTypes::toString);
    }

    @Test
    public void doesNotDeclareThePlainGenericColumnOrIndexClasses() {
        List<Class<? extends DBSObject>> childTypes = Arrays.asList(manager.getChildTypes());

        // The whole point of overriding getChildTypes() - left at the inherited generic classes,
        // SQLTableManager#getTableDDL would resolve the plain GenericIndexManager instead of
        // MimerIndexManager for a pending index, silently dropping CLUSTERED/IGNORE NULLS from
        // the DDL preview even though the real Save path was always correct.
        Assertions.assertFalse(childTypes.contains(GenericTableColumn.class), childTypes::toString);
        Assertions.assertFalse(childTypes.contains(GenericTableIndex.class), childTypes::toString);
    }

    @Test
    public void stillDeclaresTheUnchangedForeignKeyAndUniqueKeyTypes() {
        // Constraints/foreign keys have no Mimer-specific manager - left as the plain generic
        // classes deliberately, not omitted by accident.
        List<Class<? extends DBSObject>> childTypes = Arrays.asList(manager.getChildTypes());

        Assertions.assertTrue(childTypes.contains(GenericUniqueKey.class), childTypes::toString);
        Assertions.assertTrue(childTypes.contains(GenericTableForeignKey.class), childTypes::toString);
    }

    // --- CREATE TABLE ... IN <databank> (appendTableModifiers) ---

    /** Exposes the protected {@code appendTableModifiers} for direct testing. */
    private static final class TestableManager extends MimerTableManager {
        String modifiersFor(MimerTable table, boolean alter) throws DBException {
            StringBuilder ddl = new StringBuilder("CREATE TABLE \"s\".\"t\" (\n\tid INTEGER\n)");
            appendTableModifiers(new VoidProgressMonitor(), table, null, ddl, alter, new HashMap<>());
            return ddl.toString();
        }
    }

    private MimerTable tableWithDatabank(String databank) throws DBException {
        GenericDataSource ds = mock(GenericDataSource.class);
        when(ds.getSQLDialect()).thenReturn(new MimerSQLDialect());
        MimerTable table = mock(MimerTable.class);
        when(table.getDataSource()).thenReturn(ds);
        when(table.getDatabank(any())).thenReturn(databank);
        return table;
    }

    @Test
    public void createAppendsInDatabankUnquotedForAPlainName() throws Exception {
        String ddl = new TestableManager().modifiersFor(tableWithDatabank("salesdb"), false);
        Assertions.assertTrue(ddl.endsWith(" IN salesdb"), ddl);
    }

    @Test
    public void createQuotesTheDatabankNameOnlyWhenTheDialectNeedsTo() throws Exception {
        // A name with a space must be quoted; a plain one (above) must not.
        String ddl = new TestableManager().modifiersFor(tableWithDatabank("my bank"), false);
        Assertions.assertTrue(ddl.endsWith(" IN \"my bank\""), ddl);
    }

    @Test
    public void createOmitsInDatabankWhenBlankOrNull() throws Exception {
        Assertions.assertFalse(new TestableManager().modifiersFor(tableWithDatabank("   "), false).contains(" IN "));
        Assertions.assertFalse(new TestableManager().modifiersFor(tableWithDatabank(null), false).contains(" IN "));
    }

    @Test
    public void alterNeverAppendsInDatabank() throws Exception {
        // Mimer SQL has no ALTER TABLE ... SET DATABANK.
        Assertions.assertFalse(new TestableManager().modifiersFor(tableWithDatabank("salesdb"), true).contains(" IN "));
    }
}
