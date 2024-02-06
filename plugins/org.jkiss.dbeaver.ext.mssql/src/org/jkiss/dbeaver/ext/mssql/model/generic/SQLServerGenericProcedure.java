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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

/**
* SQL Server procedure
*/
public class SQLServerGenericProcedure extends GenericProcedure implements DBSObjectWithScript {

    private String source;

    public SQLServerGenericProcedure(GenericStructContainer container, String procedureName, String specificName, String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

    // Create new object
    public SQLServerGenericProcedure(
        GenericStructContainer container,
        String name
    ) {
        super(container, name, null, DBSProcedureType.PROCEDURE, "", false);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return super.getFullyQualifiedName(context);
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            if (!persisted) {
                source =
                    "CREATE " + getProcedureType().name() + " " + getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        GeneralUtils.getDefaultLineSeparator() +
                        (getProcedureType() == DBSProcedureType.FUNCTION ? "RETURNS INT" + GeneralUtils.getDefaultLineSeparator() : "") +
                        "AS " + GeneralUtils.getDefaultLineSeparator() +
                        "SELECT 1";
            } else {
                source = getDataSource().getMetaModel().getProcedureDDL(monitor, this);
            }
        }
        return source;
    }

    @Override
    @Property(hidden = true)
    public GenericPackage getPackage() {
        return super.getPackage();
    }

    @Override
    @Property(hidden = true)
    public GenericFunctionResultType getFunctionResultType() {
        return super.getFunctionResultType();
    }

    @Override
    public void setObjectDefinitionText(String sourceText) {
        this.source = sourceText;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.source = null;
        return super.refreshObject(monitor);
    }
}
