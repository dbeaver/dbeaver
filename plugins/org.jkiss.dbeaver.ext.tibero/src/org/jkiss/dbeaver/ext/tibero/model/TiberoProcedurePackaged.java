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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

public class TiberoProcedurePackaged extends AbstractProcedure<TiberoDataSource, TiberoPackage> {

    private final DBSProcedureType procedureType;
    private List<TiberoProcedureParameter> arguments;

    public TiberoProcedurePackaged(
        @NotNull TiberoPackage container,
        @NotNull String name,
        @NotNull DBSProcedureType procedureType
    ) {
        super(container, true, name, null);
        this.procedureType = procedureType;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public TiberoSchema getSchema() {
        return getContainer().getSchema();
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public TiberoPackage getPackage() {
        return getContainer();
    }

    @Override
    @Property(viewable = true, order = 5)
    public DBSProcedureType getProcedureType() {
        return procedureType;
    }

    @Nullable
    @Override
    public synchronized Collection<TiberoProcedureParameter> getParameters(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (arguments == null) {
            arguments = getContainer().getDataSource().loadPackageProcedureParameters(monitor, this);
        }
        return arguments;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getContainer(), this);
    }

    @NotNull
    @Override
    public String getName() {
        return CommonUtils.notEmpty(super.getName());
    }
}
