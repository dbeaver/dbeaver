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
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

/**
 * Doris View Base - abstract base class for views and materialized views.
 * Extends GenericView and implements catalog-aware fully qualified names.
 */
public abstract class DorisViewBase extends GenericView {

    public DorisViewBase(
        @NotNull GenericStructContainer container,
        @Nullable String viewName,
        @Nullable String viewType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, viewName, viewType, dbResult);
    }

    @NotNull
    @Override
    public DorisDataSource getDataSource() {
        return (DorisDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public GenericCatalog getCatalog() {
        return DorisObjectNameUtils.getCatalog(getContainer());
    }

    @Nullable
    @Override
    public GenericSchema getSchema() {
        return DorisObjectNameUtils.getSchema(getContainer());
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DorisObjectNameUtils.getFullyQualifiedName(getDataSource(), getCatalog(), getSchema(), this);
    }
}
