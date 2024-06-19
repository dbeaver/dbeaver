package org.jkiss.dbeaver.ext.gaussdb.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBFunction;
import org.jkiss.dbeaver.ext.gaussdb.ui.views.CreateFunctionOrProcedurePage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GaussDBFunctionConfigurator implements DBEObjectConfigurator<GaussDBFunction> {

    @Override
    public GaussDBFunction configureObject(DBRProgressMonitor monitor,
                                           DBECommandContext commandContext,
                                           Object container,
                                           GaussDBFunction newProcedure,
                                           Map<String, Object> options) {
        return new UITask<GaussDBFunction>() {
            @Override
            protected GaussDBFunction runTask() {
                CreateFunctionOrProcedurePage editPage = new CreateFunctionOrProcedurePage(monitor, newProcedure, isFunction);
                if (!editPage.edit()) {
                    return null;
                }

                newProcedure.setKind(PostgreProcedureKind.f);
                newProcedure.setReturnType(editPage.getReturnType());
                newProcedure.setName(editPage.getProcedureName());
                PostgreLanguage language = editPage.getLanguage();
                if (language != null) {
                    newProcedure.setLanguage(language);
                }
                newProcedure.setObjectDefinitionText("CREATE OR REPLACE " + editPage.getProcedureType() + " "
                            + newProcedure.getFullQualifiedSignature()
                            + (newProcedure.getReturnType() == null ? ""
                                        : "\n\tRETURNS "
                                                    + newProcedure.getReturnType().getFullyQualifiedName(DBPEvaluationContext.DDL))
                            + (language == null ? "" : "\n\tLANGUAGE " + language.getName()) + "\nAS $"
                            + editPage.getProcedureType().name().toLowerCase() + "$" + "\n\tBEGIN\n" + "\n\tEND;" + "\n$"
                            + editPage.getProcedureType().name().toLowerCase() + "$\n");
                return newProcedure;
            }
        }.execute();
    }

}