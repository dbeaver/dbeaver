/*
 * DBeaver - Universal Database Manager
 */

package org.jkiss.dbeaver.ext.gaussdb.ui.config;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBFunction;
import org.jkiss.dbeaver.ext.gaussdb.ui.views.CreateFunctionOrProcedurePage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * GaussDB Function configurator
 */
public class GaussDBFunctionConfigurator implements DBEObjectConfigurator<GaussDBFunction> {

    protected static final Log log        = Log.getLog(GaussDBFunctionConfigurator.class);

    public static boolean      isFunction = true;

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
                String function = "CREATE [OR REPLACE] FUNCTION " + newProcedure.getFullQualifiedSignature()
                            + " ([ parameter [IN|OUT|INOUT] datatype[,parameter [IN|OUT|INOUT] datatype] ])\r\n" + " RETURNS "
                            + newProcedure.getReturnType().getDefaultValue() + "\r\n" + " LANGUAGE " + language.getName() + "\r\n"
                            + "\r\n" + "AS\r\n" + "\r\n" + " '/*iso file path and name*/',$$/*function name*/$$";
                newProcedure.setObjectDefinitionText(function);
                return newProcedure;
            }
        }.execute();
    }

}