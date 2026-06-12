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
package org.jkiss.dbeaver.ext.doris.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.List;

/**
 * Doris Database - represents a database/schema within a Doris catalog.
 */
public class DorisDatabase extends GenericSchema {

    public DorisDatabase(
        @NotNull DorisDataSource dataSource,
        @Nullable DorisCatalog catalog,
        @NotNull String schemaName
    ) {
        super(dataSource, catalog, schemaName);
    }

    @NotNull
    @Override
    public DorisDataSource getDataSource() {
        return (DorisDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public DorisCatalog getCatalog() {
        return (DorisCatalog) super.getCatalog();
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return GenericTableBase.class;
    }

    @NotNull
    public List<DorisTable> getTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTableCache().getTypedObjects(monitor, this, DorisTable.class);
    }

    @NotNull
    public List<DorisView> getViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTableCache().getTypedObjects(monitor, this, DorisView.class);
    }
}
