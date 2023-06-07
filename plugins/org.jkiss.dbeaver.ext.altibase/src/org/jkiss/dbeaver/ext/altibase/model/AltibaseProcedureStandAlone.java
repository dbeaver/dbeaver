/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.util.Map;

public class AltibaseProcedureStandAlone extends AltibaseProcedureBase {

    public AltibaseProcedureStandAlone(GenericStructContainer container, String procedureName, String specificName,
            String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            source = getDataSource().getMetaModel().getProcedureDDL(monitor, this);
        }
        return source;
    }

    public String getProcedureTypeName() {
        DBSProcedureType procedureType = getProcedureType();
        if (procedureType == DBSProcedureType.UNKNOWN) {
            return "TYPESET";
        } else {
            return procedureType.name();
        }
    }
}
