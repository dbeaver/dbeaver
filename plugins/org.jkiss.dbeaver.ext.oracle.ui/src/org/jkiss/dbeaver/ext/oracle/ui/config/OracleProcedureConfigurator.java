/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.config;

import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * OracleProcedureConfigurator
 */
public class OracleProcedureConfigurator implements DBEObjectConfigurator<OracleProcedureStandalone> {

    @Override
    public OracleProcedureStandalone configureObject(DBRProgressMonitor monitor, Object container, OracleProcedureStandalone procedure) {
        return new UITask<OracleProcedureStandalone>() {
            @Override
            protected OracleProcedureStandalone runTask() {
                CreateProcedurePage editPage = new CreateProcedurePage(procedure);
                if (!editPage.edit()) {
                    return null;
                }
                DBSProcedureType procedureType = editPage.getProcedureType();
                String procedureName = editPage.getProcedureName();

                procedure.setName(procedureName);
                procedure.setProcedureType(procedureType);

                procedure.setObjectDefinitionText(
                    "CREATE OR REPLACE " + procedureType.name() + " " + procedureName +
                    (procedureType == DBSProcedureType.FUNCTION ? "() RETURN NUMBER" : "") + GeneralUtils.getDefaultLineSeparator() +
                        "IS" + GeneralUtils.getDefaultLineSeparator() +
                        "BEGIN" + GeneralUtils.getDefaultLineSeparator() +
                        (procedureType == DBSProcedureType.FUNCTION ? "\tRETURN 1;" + GeneralUtils.getDefaultLineSeparator() : "") +
                        "END " + procedureName + ";" + GeneralUtils.getDefaultLineSeparator());


                return procedure;
            }
        }.execute();
    }

}
