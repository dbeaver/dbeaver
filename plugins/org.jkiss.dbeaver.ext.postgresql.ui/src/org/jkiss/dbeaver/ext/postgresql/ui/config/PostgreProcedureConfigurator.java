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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Postgre procedure configurator
 */
public class PostgreProcedureConfigurator implements DBEObjectConfigurator<PostgreProcedure> {

    protected static final Log log = Log.getLog(PostgreProcedureConfigurator.class);

    @Override
    public PostgreProcedure configureObject(DBRProgressMonitor monitor, Object parent, PostgreProcedure newProcedure) {
        return new UITask<PostgreProcedure>() {
            @Override
            protected PostgreProcedure runTask() {
                CreateFunctionPage editPage = new CreateFunctionPage(monitor, newProcedure);
                if (!editPage.edit()) {
                    return null;
                }
                if (editPage.getProcedureType() == DBSProcedureType.FUNCTION) {
                    newProcedure.setKind(PostgreProcedureKind.f);
                    newProcedure.setReturnType(editPage.getReturnType());
                } else {
                    newProcedure.setKind(PostgreProcedureKind.p);
                }
                newProcedure.setName(editPage.getProcedureName());
                newProcedure.setLanguage(editPage.getLanguage());
                newProcedure.setObjectDefinitionText(
                    "CREATE OR REPLACE " + editPage.getProcedureType() + " " + newProcedure.getFullQualifiedSignature() +
                    (newProcedure.getReturnType() == null ? "" : "\n\tRETURNS " + newProcedure.getReturnType().getFullyQualifiedName(DBPEvaluationContext.DDL)) +
                    "\n\tLANGUAGE " + editPage.getLanguage().getName() +
                    "\nAS $" + editPage.getProcedureType().name().toLowerCase() + "$" +
                    "\n\tBEGIN\n" +
                    "\n\tEND;" +
                    "\n$" + editPage.getProcedureType().name().toLowerCase() + "$\n"
                );
                return newProcedure;
            }
        }.execute();
    }

    private static class CreateFunctionPage extends CreateProcedurePage {
        private final PostgreProcedure parent;
        private final DBRProgressMonitor monitor;
        private PostgreLanguage language;
        private PostgreDataType returnType;
        private Combo returnTypeCombo;

        public CreateFunctionPage(DBRProgressMonitor monitor, PostgreProcedure parent) {
            super(parent);
            this.parent = parent;
            this.monitor = monitor;
        }

        @Override
        public DBSProcedureType getPredefinedProcedureType() {
            if (parent.getDataSource().isServerVersionAtLeast(11, 0)) {
                return null;
            }
            return DBSProcedureType.FUNCTION;
        }

        @Override
        public DBSProcedureType getDefaultProcedureType() {
            return DBSProcedureType.FUNCTION;
        }

        @Override
        protected void updateProcedureType(DBSProcedureType type) {
            returnTypeCombo.setEnabled(type.hasReturnValue());
        }

        @Override
        protected void createExtraControls(Composite group) {
            {
                List<PostgreLanguage> languages = new ArrayList<>();
                try {
                    languages.addAll(parent.getDatabase().getLanguages(monitor));
                } catch (DBException e) {
                    log.error(e);
                }
                final Combo languageCombo = UIUtils.createLabelCombo(group, "Language", SWT.DROP_DOWN | SWT.READ_ONLY);
                for (PostgreLanguage lang : languages) {
                    languageCombo.add(lang.getName());
                }

                languageCombo.addModifyListener(e -> {
                    language = languages.get(languageCombo.getSelectionIndex());
                });
                languageCombo.setText("sql");
            }
            {
                List<PostgreDataType> dataTypes = new ArrayList<>(parent.getDatabase().getDataSource().getLocalDataTypes());
                returnTypeCombo = UIUtils.createLabelCombo(group, "Return type", SWT.DROP_DOWN);
                for (PostgreDataType dt : dataTypes) {
                    returnTypeCombo.add(dt.getName());
                }

                returnTypeCombo.addModifyListener(e -> {
                    String dtName = returnTypeCombo.getText();
                    if (!CommonUtils.isEmpty(dtName)) {
                        returnType = parent.getDatabase().getDataSource().getLocalDataType(dtName);
                    } else {
                        returnType = null;
                    }
                });
                returnTypeCombo.setText("int4");
            }

        }

        public PostgreLanguage getLanguage() {
            return language;
        }

        public PostgreDataType getReturnType() {
            return returnType;
        }
    }

}
