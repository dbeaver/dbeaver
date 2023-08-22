package org.jkiss.dbeaver.ext.dm.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.ext.dm.model.DmProcedureStandalone;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DmProcedureConfigurator implements DBEObjectConfigurator<DmProcedureStandalone>{

	@Override
	public DmProcedureStandalone configureObject(DBRProgressMonitor monitor, Object container,
			DmProcedureStandalone procedure,Map<String, Object> options) {
        return new UITask<DmProcedureStandalone>() {
            @Override
            protected DmProcedureStandalone runTask() {
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
