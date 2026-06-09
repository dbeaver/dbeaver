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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Map;


/**
 * YashanDBPackage
 */
public class YashanDBPackage extends OraclePackage {

    public YashanDBPackage(OracleSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    public YashanDBPackage(OracleSchema schema, String name) {
        super(schema, name);
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options)
            throws DBCException {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = YashanDBUtils.getSource(monitor, this, false, true);
        }
        return sourceDeclaration;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sourceDefinition == null && monitor != null) {
            sourceDefinition = YashanDBUtils.getSource(monitor, this, true, true);
        }
        return sourceDefinition;
    }
}
