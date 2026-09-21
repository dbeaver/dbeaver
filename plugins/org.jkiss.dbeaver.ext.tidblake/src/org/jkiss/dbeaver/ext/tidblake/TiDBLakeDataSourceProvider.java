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
package org.jkiss.dbeaver.ext.tidblake;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tidblake.model.TiDBLakeDataSource;
import org.jkiss.dbeaver.ext.tidblake.model.TiDBLakeMetaModel;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPInformationProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;


public class TiDBLakeDataSourceProvider extends GenericDataSourceProvider<TiDBLakeDataSource> implements DBPInformationProvider {

    public TiDBLakeDataSourceProvider() {
        super(TiDBLakeDataSource.class);
    }

    @NotNull
    @Override
    public TiDBLakeDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new TiDBLakeDataSource(monitor, container, new TiDBLakeMetaModel());
    }

    @Nullable
    @Override
    public String getObjectInformation(@NotNull DBPObject object, @NotNull String infoType) {
        if (object instanceof DBPDataSourceContainer dsc && infoType.equals(INFO_TARGET_ADDRESS)) {
            return dsc.getConnectionConfiguration().getServerName();
        }
        return null;
    }

}
