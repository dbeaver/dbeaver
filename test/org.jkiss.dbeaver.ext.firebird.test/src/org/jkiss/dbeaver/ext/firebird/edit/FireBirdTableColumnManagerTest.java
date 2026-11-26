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
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataType;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdFieldType;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdSQLDialect;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTable;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FireBirdTableColumnManagerTest extends DBeaverUnitTest {

    private static final String DOMAIN_NAME = "DOM_UUID";
    private static final String IMPLICIT_DOMAIN_NAME = "RDB$100";

    private FireBirdDataSource dataSource;
    private FireBirdTable table;
    private DBRProgressMonitor progressMonitor;

    @BeforeEach
    public void setUp() {
        dataSource = Mockito.mock(FireBirdDataSource.class);
        FireBirdSQLDialect dialect = Mockito.mock(FireBirdSQLDialect.class);
        Mockito.when(dialect.getQuotedIdentifier(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(dialect.getColumnTypeModifiers(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenCallRealMethod();
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        FireBirdDataType charType = new FireBirdDataType(dataSource, FireBirdFieldType.CHAR);
        Mockito.when(dataSource.getLocalDataType(DOMAIN_NAME)).thenReturn(domainType(DOMAIN_NAME));
        Mockito.when(dataSource.getLocalDataType(IMPLICIT_DOMAIN_NAME)).thenReturn(domainType(IMPLICIT_DOMAIN_NAME));
        Mockito.when(dataSource.getLocalDataType(charType.getTypeName())).thenReturn(charType);

        table = Mockito.mock(FireBirdTable.class);
        Mockito.when(table.getDataSource()).thenReturn(dataSource);
        Mockito.when(table.getFullyQualifiedName(Mockito.any())).thenReturn("TEST_TABLE");
        progressMonitor = Mockito.mock(DBRProgressMonitor.class);
    }

    @Test
    public void columnDeclarationUsesUserDomainInsteadOfUnderlyingType() throws DBException {
        FireBirdTableColumn column = loadColumn(DOMAIN_NAME);

        Assertions.assertEquals(DOMAIN_NAME, column.getDomainName());
        Assertions.assertEquals("TEST_NAME DOM_UUID NOT NULL", getDeclaration(column));
    }

    @Test
    public void columnDeclarationUsesUnderlyingTypeForImplicitDomain() throws DBException {
        FireBirdTableColumn column = loadColumn(IMPLICIT_DOMAIN_NAME);

        Assertions.assertNull(column.getDomainName());
        String declaration = getDeclaration(column);
        Assertions.assertTrue(declaration.startsWith("TEST_NAME CHAR"), declaration);
        Assertions.assertFalse(declaration.contains(IMPLICIT_DOMAIN_NAME), declaration);
    }

    @Test
    public void changingTypeNameUpdatesDomain() throws DBException {
        FireBirdTableColumn column = loadColumn(DOMAIN_NAME);

        column.setTypeName(FireBirdFieldType.CHAR.getName());
        Assertions.assertNull(column.getDomainName());

        column.setTypeName(DOMAIN_NAME);
        Assertions.assertEquals(DOMAIN_NAME, column.getDomainName());
    }

    @Test
    public void changingDataTypeUpdatesDomain() throws DBException {
        FireBirdTableColumn column = loadColumn(DOMAIN_NAME);

        column.setDataType(new FireBirdDataType(dataSource, FireBirdFieldType.CHAR));
        Assertions.assertNull(column.getDomainName());

        column.setDataType(domainType(DOMAIN_NAME));
        Assertions.assertEquals(DOMAIN_NAME, column.getDomainName());
    }

    @Test
    public void alterTypeKeepsDomainWithoutSizeChange() throws DBException {
        FireBirdTableColumn column = loadColumn(DOMAIN_NAME);

        Assertions.assertEquals(
            List.of("ALTER TABLE TEST_TABLE ALTER COLUMN TEST_NAME TYPE DOM_UUID"),
            getModifyScripts(column, Map.of(DBConstants.PROP_ID_TYPE_NAME, DOMAIN_NAME)));
    }

    @Test
    public void alterSizeOfDomainColumnUsesUnderlyingType() throws DBException {
        FireBirdTableColumn column = loadColumn(DOMAIN_NAME);
        column.setMaxLength(20);

        Assertions.assertEquals(
            List.of("ALTER TABLE TEST_TABLE ALTER COLUMN TEST_NAME TYPE CHAR(20)"),
            getModifyScripts(column, Map.of("maxLength", 20L)));
    }

    @Test
    public void alterSizeAfterSwitchingToDomainSwitchesDomainFirst() throws DBException {
        FireBirdTableColumn column = loadColumn(IMPLICIT_DOMAIN_NAME);
        // The type name is the domain name now, which must not get a size modifier.
        // Switching to the domain first takes over its character set and collation, the resize keeps them.
        column.setTypeName(DOMAIN_NAME);
        column.setMaxLength(20);

        Assertions.assertEquals(
            List.of(
                "ALTER TABLE TEST_TABLE ALTER COLUMN TEST_NAME TYPE DOM_UUID",
                "ALTER TABLE TEST_TABLE ALTER COLUMN TEST_NAME TYPE CHAR(20)"),
            getModifyScripts(column, Map.of(DBConstants.PROP_ID_TYPE_NAME, DOMAIN_NAME, "maxLength", 20L)));
    }

    @Test
    public void alterSizeKeepsSizeEqualToDomain() throws DBException {
        FireBirdTableColumn column = loadColumn(DOMAIN_NAME);

        // The domain has length 16 as well, but the size must not be dropped once the domain is no longer used
        Assertions.assertEquals(
            List.of("ALTER TABLE TEST_TABLE ALTER COLUMN TEST_NAME TYPE CHAR(16)"),
            getModifyScripts(column, Map.of("maxLength", 16L)));
    }

    @Test
    public void newDomainColumnWithoutSizeChangeUsesDomain() {
        FireBirdTableColumn column = newColumn(DOMAIN_NAME);

        Assertions.assertEquals("TEST_NAME DOM_UUID", getCreateDeclaration(column, Map.of(DBConstants.PROP_ID_TYPE_NAME, DOMAIN_NAME)));
    }

    @Test
    public void newDomainColumnWithSizeChangeUsesUnderlyingTypeAndCharset() {
        FireBirdTableColumn column = newColumn(DOMAIN_NAME);
        column.setMaxLength(20);

        Assertions.assertEquals(
            "TEST_NAME CHAR(20) CHARACTER SET OCTETS",
            getCreateDeclaration(column, Map.of(DBConstants.PROP_ID_TYPE_NAME, DOMAIN_NAME, DBConstants.PROP_ID_MAX_LENGTH, 20L)));
    }

    @Test
    public void newDomainColumnWithSizeChangeAppendsCollationLast() {
        FireBirdDataType domain = domainType(DOMAIN_NAME);
        domain.setCollationName("UNICODE_CI");
        FireBirdTableColumn column = newColumn(DOMAIN_NAME);
        column.setDataType(domain);
        column.setMaxLength(20);
        column.setDefaultValue("'x'");
        column.setRequired(true);

        // Firebird accepts COLLATE only at the end of the column definition
        Assertions.assertEquals(
            "TEST_NAME CHAR(20) CHARACTER SET OCTETS DEFAULT 'x' NOT NULL COLLATE UNICODE_CI",
            getCreateDeclaration(column, Map.of(DBConstants.PROP_ID_TYPE_NAME, DOMAIN_NAME, DBConstants.PROP_ID_MAX_LENGTH, 20L)));
        // Without a size change the domain brings its own collation
        Assertions.assertEquals(
            "TEST_NAME DOM_UUID DEFAULT 'x' NOT NULL",
            getCreateDeclaration(column, Map.of(DBConstants.PROP_ID_TYPE_NAME, DOMAIN_NAME)));
    }

    @NotNull
    private List<String> getModifyScripts(@NotNull FireBirdTableColumn column, @NotNull Map<String, Object> properties)
        throws DBException {
        return new TestColumnManager().getModifyActions(progressMonitor, column, properties).stream()
            .map(DBEPersistAction::getScript)
            .toList();
    }

    @NotNull
    private FireBirdTableColumn loadColumn(@NotNull String fieldSource) throws DBException {
        Mockito.when(table.getColumnDomainType(Mockito.any(), Mockito.any())).thenReturn(fieldSource);
        return new FireBirdTableColumn(
            progressMonitor, null, table, "TEST_NAME", FireBirdFieldType.CHAR.getName(), Types.CHAR, Types.CHAR,
            1, 16, 16, null, null, 10, true, null, null, false, false);
    }

    @NotNull
    private FireBirdTableColumn newColumn(@NotNull String domainName) {
        FireBirdTableColumn column = new FireBirdTableColumn(table);
        column.setName("TEST_NAME");
        column.setDataType(domainType(domainName));
        column.setMaxLength(16);
        return column;
    }

    @NotNull
    private FireBirdDataType domainType(@NotNull String name) {
        FireBirdDataType domain = new FireBirdDataType(dataSource, FireBirdFieldType.CHAR, 0, name, null, 0, 0);
        domain.setFieldLength(16);
        domain.setCharLength(16);
        domain.setCharsetName("OCTETS");
        return domain;
    }

    @NotNull
    private String getCreateDeclaration(@NotNull FireBirdTableColumn column, @NotNull Map<String, Object> properties) {
        return new TestColumnManager().getCreateDeclaration(progressMonitor, column, properties);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private String getDeclaration(@NotNull FireBirdTableColumn column) {
        DBECommandAbstract<GenericTableColumn> command = Mockito.mock(DBECommandAbstract.class);
        Mockito.when(command.getObject()).thenReturn(column);
        return new TestColumnManager().getDeclaration(progressMonitor, column, command);
    }

    /**
     * Exposes the protected column declaration builder of the manager.
     */
    private static class TestColumnManager extends FireBirdTableColumnManager {
        @NotNull
        String getDeclaration(
            @NotNull DBRProgressMonitor monitor,
            @NotNull FireBirdTableColumn column,
            @NotNull DBECommandAbstract<GenericTableColumn> command
        ) {
            return getNestedDeclaration(monitor, column.getTable(), command, Map.of()).toString();
        }

        @NotNull
        String getCreateDeclaration(
            @NotNull DBRProgressMonitor monitor,
            @NotNull FireBirdTableColumn column,
            @NotNull Map<String, Object> properties
        ) {
            ObjectCreateCommand command = makeCreateCommand(column, Map.of());
            command.getProperties().putAll(properties);
            return getNestedDeclaration(monitor, column.getTable(), command, Map.of()).toString();
        }

        @NotNull
        List<DBEPersistAction> getModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull FireBirdTableColumn column,
            @NotNull Map<String, Object> properties
        ) throws DBException {
            ObjectChangeCommand command = new ObjectChangeCommand(column);
            command.getProperties().putAll(properties);
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(monitor, Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }
    }
}
