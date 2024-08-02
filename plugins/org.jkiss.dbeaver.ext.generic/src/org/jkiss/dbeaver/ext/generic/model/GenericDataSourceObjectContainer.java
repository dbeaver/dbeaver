/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class GenericDataSourceObjectContainer extends GenericObjectContainer {
    private GenericDataSource dataSource;

    public GenericDataSourceObjectContainer(GenericDataSource dataSource) {
        super(dataSource);
        this.dataSource = dataSource;
    }

    @Override
    public GenericCatalog getCatalog() {
        return null;
    }

    @Override
    public GenericSchema getSchema() {
        return null;
    }

    @Override
    public GenericStructContainer getObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return GenericTable.class;
    }

    @NotNull
    @Override
    public String getName() {
        return dataSource.getName();
    }

    @Nullable
    @Override
    public String getDescription() {
        return dataSource.getDescription();
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getParentObject();
    }
}
