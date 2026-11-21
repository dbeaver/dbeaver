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
package org.jkiss.dbeaver.ext.frostlake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Frostlake's generic meta model.
 *
 * <p>Two jobs. It supplies {@link FrostlakeSchema} so each schema carries the SHOW-backed object
 * kinds, and it sources DDL from {@code GET_DDL}, which Frostlake implements with Snowflake's
 * semantics — so "Generate DDL" produces text that can be run straight back.
 */
public class FrostlakeMetaModel extends GenericMetaModel {

    public FrostlakeMetaModel() {
        super();
    }

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(@NotNull DBRProgressMonitor monitor,
                                                  @NotNull DBPDataSourceContainer container) throws DBException {
        return new FrostlakeDataSource(monitor, container, this);
    }

    @NotNull
    @Override
    public GenericSchema createSchemaImpl(@NotNull GenericDataSource dataSource,
                                          @Nullable GenericCatalog catalog,
                                          @NotNull String schemaName) {
        return new FrostlakeSchema(dataSource, catalog, schemaName);
    }

    @Nullable
    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor,
                              @NotNull GenericTableBase sourceObject,
                              @NotNull java.util.Map<String, Object> options) throws DBException {
        return FrostlakeDDL.readObjectDDL(monitor, sourceObject,
            sourceObject.isView() ? "VIEW" : "TABLE", super.getTableDDL(monitor, sourceObject, options));
    }
}
