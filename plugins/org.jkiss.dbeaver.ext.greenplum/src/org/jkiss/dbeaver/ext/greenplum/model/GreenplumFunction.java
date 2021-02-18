/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

public class GreenplumFunction extends PostgreProcedure {
    private FunctionExecLocation executionLocation;

    public GreenplumFunction(PostgreSchema schema) {
        super(schema);
        this.executionLocation = getDataSource().isServerVersionAtLeast(9,4) ? FunctionExecLocation.a : null;
    }

    public GreenplumFunction(DBRProgressMonitor monitor, PostgreSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        this.executionLocation = readExecutionLocationIfSupported(dbResult);
    }

    public FunctionExecLocation getExecutionLocation() {
        return executionLocation;
    }

    @Override
    protected String generateFunctionDeclaration(PostgreLanguage language, String returnTypeName, String functionBody) {
        String functionDeclaration = super.generateFunctionDeclaration(language, returnTypeName, functionBody);

        if(this.executionLocation != null) {
            StringBuilder def = new StringBuilder(functionDeclaration);
            return def.append("EXECUTE ON ").append(this.executionLocation.getValue()).toString();
        }

        return functionDeclaration;
    }

    private FunctionExecLocation readExecutionLocationIfSupported(ResultSet dbResult) {
        return getDataSource().isServerVersionAtLeast(9, 4) ?
                CommonUtils.valueOf(FunctionExecLocation.class, JDBCUtils.safeGetString(dbResult, "proexeclocation"))
                : null;
    }

    public enum FunctionExecLocation {
        a("ANY"),
        m("MASTER"),
        s("ALL SEGMENTS");

        private String execLocation;

        FunctionExecLocation(String execLocationString) {
            this.execLocation = execLocationString;
        }

        public String getValue() {
            return execLocation;
        }
    }
}
