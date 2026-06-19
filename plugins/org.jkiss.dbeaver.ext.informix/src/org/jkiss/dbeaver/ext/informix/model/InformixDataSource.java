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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

public class InformixDataSource extends GenericDataSource {

    public InformixDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    @NotNull
    @Override
    public InformixDataSource getDataSource() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            DBSStructureAssistant<GenericExecutionContext> genericAssistant =
                (DBSStructureAssistant<GenericExecutionContext>) super.getAdapter(DBSStructureAssistant.class);
            return adapter.cast(new InformixStructureAssistant(this, genericAssistant));
        }
        return super.getAdapter(adapter);
    }
}