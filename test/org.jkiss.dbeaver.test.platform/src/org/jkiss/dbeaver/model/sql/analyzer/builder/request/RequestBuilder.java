/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.analyzer.builder.request;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadataRegistry;
import org.jkiss.dbeaver.model.sql.analyzer.builder.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestBuilder {
    private final DataSource dataSource;
    private final DBSObject object;
    private final List<? extends DBSObject> children;

    private RequestBuilder(@NotNull DataSource dataSource, @NotNull DBSObject object, @NotNull List<? extends DBSObject> children) {
        this.dataSource = dataSource;
        this.object = object;
        this.children = children;
        SQLDialect dialect = new GenericSQLDialect() {
            @Override
            public boolean supportsAliasInSelect() {
                return true;
            }
        };
        when(dataSource.getSQLDialect()).thenReturn(dialect);
    }

    public static RequestBuilder databases(Builder.Consumer<DatabaseContainerBuilder> applier) throws DBException {
        final DataSource dataSource = createDataSource();
        final DatabaseContainerBuilder builder = new DatabaseContainerBuilder(dataSource, "<unnamed>");
        applier.apply(builder);
        return new RequestBuilder(dataSource, builder.build(), builder.getChildren());
    }

    public static RequestBuilder schemas(Builder.Consumer<SchemaContainerBuilder> applier) throws DBException {
        final DataSource dataSource = createDataSource();
        final SchemaContainerBuilder builder = new SchemaContainerBuilder(dataSource, "<unnamed>");
        applier.apply(builder);
        return new RequestBuilder(dataSource, builder.build(), builder.getChildren());
    }

    public static RequestBuilder tables(Builder.Consumer<TableContainerBuilder> applier) throws DBException {
        final DataSource dataSource = createDataSource();
        final TableContainerBuilder builder = new TableContainerBuilder(dataSource, "<unnamed>");
        applier.apply(builder);
        return new RequestBuilder(dataSource, builder.build(), builder.getChildren());
    }

    public static RequestBuilder empty() throws DBException {
        final DataSource dataSource = createDataSource();
        final TableAttributeContainerBuilder builder = new TableAttributeContainerBuilder(dataSource, "<unnamed>");
        return new RequestBuilder(dataSource, builder.build(), builder.getChildren());
    }

    @NotNull
    public RequestResult prepare() throws DBException {
        final DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();

        final DBPDataSourceContainer dataSourceContainer = mock(DBPDataSourceContainer.class);
        when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);

        when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        when(dataSource.getChild(any(), any())).then(x -> DBUtils.findObject(children, x.getArgument(1, String.class)));
        when(dataSource.getChildren(any())).then(x -> children);

        return new RequestResult(dataSource);
    }

    @NotNull
    public DBSObject getObject() {
        return object;
    }

    @NotNull
    private static DataSource createDataSource() {
        DBPDataSourceInfo dsInfo = mock(DBPDataSourceInfo.class);
        when(dsInfo.getSupportedObjectTypes()).then(x -> new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_VIEW,
            RelationalObjectType.TYPE_TABLE_COLUMN,
            RelationalObjectType.TYPE_VIEW_COLUMN,
            RelationalObjectType.TYPE_INDEX,
            RelationalObjectType.TYPE_CONSTRAINT,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_SEQUENCE,
            RelationalObjectType.TYPE_TRIGGER,
            RelationalObjectType.TYPE_DATA_TYPE
        });

        DataSource ds = mock(DataSource.class);
        when(ds.getInfo()).then(x -> dsInfo);
        return ds;
    }

    public interface DataSource extends DBPDataSource, DBSObjectContainer {
    }
}
