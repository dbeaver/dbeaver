/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.h2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class H2RoutineAlias extends GenericProcedure {

    private String javaClass;
    private String javaMethod;
    private String source;

    public H2RoutineAlias(@NotNull GenericStructContainer container, @NotNull String procedureName, String description, DBSProcedureType procedureType,
                          @Nullable GenericFunctionResultType functionResultType, @NotNull JDBCResultSet dbResult) {
        super(container, procedureName, procedureName, description, procedureType, functionResultType);
        this.javaClass = JDBCUtils.safeGetString(dbResult, "JAVA_CLASS");
        this.javaMethod = JDBCUtils.safeGetString(dbResult, "JAVA_METHOD");
        this.source = JDBCUtils.safeGetString(dbResult, "SOURCE");
    }

    @Property(viewable = true, order = 7)
    public String getJavaClass() {
        return javaClass;
    }

    @Property(viewable = true, order = 8)
    public String getJavaMethod() {
        return javaMethod;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return super.getDescription();
    }

    // Not supported
    @Override
    public GenericPackage getPackage() {
        return super.getPackage();
    }

    // Not supported
    @Override
    public GenericCatalog getCatalog() {
        return super.getCatalog();
    }

    // Not supported
    @Override
    public GenericFunctionResultType getFunctionResultType() {
        return super.getFunctionResultType();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) {
        StringBuilder sourceDDL = new StringBuilder(128);
        sourceDDL.append("CREATE ALIAS ").append(getFullyQualifiedName(DBPEvaluationContext.DDL));
        if (CommonUtils.isNotEmpty(source)) {
            sourceDDL.append(" AS $$\n").append(source).append("$$");
        } else if (CommonUtils.isNotEmpty(javaClass) && CommonUtils.isNotEmpty(javaMethod)) {
            sourceDDL.append(" FOR \"").append(javaClass).append(".").append(javaMethod).append("\";");
        }
        return sourceDDL.toString();
    }
}
