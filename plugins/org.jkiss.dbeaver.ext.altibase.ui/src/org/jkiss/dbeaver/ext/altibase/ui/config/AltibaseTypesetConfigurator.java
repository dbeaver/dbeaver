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
package org.jkiss.dbeaver.ext.altibase.ui.config;

import org.jkiss.dbeaver.ext.altibase.model.AltibaseTypeset;
import org.jkiss.dbeaver.ext.altibase.ui.views.CreateTypesetPage;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

/**
 * AltibaseProcedureConfigurator
 */
public class AltibaseTypesetConfigurator implements DBEObjectConfigurator<AltibaseTypeset> {

    @Override
    public AltibaseTypeset configureObject(DBRProgressMonitor monitor, DBECommandContext commandContext,
            Object container, AltibaseTypeset procedure, Map<String, Object> options) {
        return new UITask<AltibaseTypeset>() {
            @Override
            protected AltibaseTypeset runTask() {
                CreateTypesetPage editPage = new CreateTypesetPage(procedure);
                if (!editPage.edit()) {
                    return null;
                }
                DBSProcedureType procedureType = editPage.getProcedureType();
                String procedureName = editPage.getProcedureName();

                procedure.setName(procedureName);
                procedure.setProcedureType(procedureType);

                procedure.setObjectDefinitionText(
                        "CREATE OR REPLACE TYPESET " + procedureName +
                        (procedureType == DBSProcedureType.FUNCTION ? "() RETURN NUMBER" : "") + GeneralUtils.getDefaultLineSeparator() +
                        "AS" + GeneralUtils.getDefaultLineSeparator() +
                        "\t" + GeneralUtils.getDefaultLineSeparator() + 
                        "END");

                return procedure;
            }
        }.execute();
    }
}
