package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBProcedureConfigurator implements DBEObjectConfigurator<YashanDBProcedureStandalone> {

    @Override
    public YashanDBProcedureStandalone configureObject(DBRProgressMonitor monitor, Object container, YashanDBProcedureStandalone procedure, Map<String, Object> options) {
        return new UITask<YashanDBProcedureStandalone>() {
            @Override
            protected YashanDBProcedureStandalone runTask() {
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
                                "END " + procedureName + ";" + GeneralUtils.getDefaultLineSeparator()
                );


                return procedure;
            }
        }.execute();
    }
}
