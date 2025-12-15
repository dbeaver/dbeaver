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
package org.jkiss.dbeaver.ext.clickhouse;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseDataSource;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.generic.GenericMetaModelRegistry;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ClickhouseDataSourceProvider extends GenericDataSourceProvider {

    public ClickhouseDataSourceProvider() {
    }

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container)
            throws DBException {
        GenericMetaModel metaModel = GenericMetaModelRegistry.getInstance().getMetaModel(container);
        return new ClickhouseDataSource(monitor, container, metaModel);
    }
}
