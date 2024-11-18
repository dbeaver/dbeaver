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

package org.jkiss.dbeaver.ext.kingbase.ui.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseLanguage;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedure;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedureKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

/**
 * Kingbase procedure configurator
 */
public class KingbaseProcedureConfigurator implements DBEObjectConfigurator<KingbaseProcedure> {

    protected static final Log log = Log.getLog(KingbaseProcedureConfigurator.class);

    @Override
    public KingbaseProcedure configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object parent, @NotNull KingbaseProcedure newProcedure, @NotNull Map<String, Object> options) {
        return new UITask<KingbaseProcedure>() {
            @Override
            protected KingbaseProcedure runTask() {
                CreateFunctionPage editPage = new CreateFunctionPage(monitor, newProcedure);
                if (!editPage.edit()) {
                    return null;
                }
                if (editPage.getProcedureType() == DBSProcedureType.FUNCTION) {
                    newProcedure.setKind(KingbaseProcedureKind.f);
                    newProcedure.setReturnType(editPage.getReturnType());
                } else {
                    newProcedure.setKind(KingbaseProcedureKind.p);
                }
                newProcedure.setName(editPage.getProcedureName());
                KingbaseLanguage language = editPage.getLanguage();
                if (language != null) {
                    newProcedure.setLanguage(language);
                }
                newProcedure.setObjectDefinitionText(
                    "CREATE OR REPLACE " + editPage.getProcedureType() + " " + newProcedure.getFullQualifiedSignature() +
                    (newProcedure.getReturnType() == null ? "" : "\n\tRETURNS " + newProcedure.getReturnType().getFullyQualifiedName(DBPEvaluationContext.DDL)) +
                    (language == null ? "" : "\n\tLANGUAGE " + language.getName()) +
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
        private final KingbaseProcedure parent;
        private final DBRProgressMonitor monitor;
        private KingbaseLanguage language;
        private KingbaseDataType returnType;
        private Combo returnTypeCombo;

        public CreateFunctionPage(DBRProgressMonitor monitor, KingbaseProcedure parent) {
            super(parent);
            this.parent = parent;
            this.monitor = monitor;
        }

        @Override
        public DBSProcedureType getPredefinedProcedureType() {
            
            return null;
            
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
                List<KingbaseLanguage> languages = new ArrayList<>();
                try {
                    languages.addAll(parent.getDatabase().getLanguages(monitor));
                } catch (DBException e) {
                    log.error(e);
                }
                final Combo languageCombo = UIUtils.createLabelCombo(group, "Language", SWT.DROP_DOWN | SWT.READ_ONLY);
                for (KingbaseLanguage lang : languages) {
                    languageCombo.add(lang.getName());
                }

                languageCombo.addModifyListener(e -> {
                    language = languages.get(languageCombo.getSelectionIndex());
                });
                languageCombo.setText("sql");
            }
            {
                List<KingbaseDataType> dataTypes = new ArrayList<>(parent.getDatabase().getLocalDataTypes());
                dataTypes.sort(Comparator.comparing(KingbaseDataType::getName));
                returnTypeCombo = UIUtils.createLabelCombo(group, "Return type", SWT.DROP_DOWN);
                for (KingbaseDataType dt : dataTypes) {
                    returnTypeCombo.add(dt.getName());
                }

                returnTypeCombo.addModifyListener(e -> {
                    String dtName = returnTypeCombo.getText();
                    if (!CommonUtils.isEmpty(dtName)) {
                        returnType = parent.getDatabase().getLocalDataType(dtName);
                    } else {
                        returnType = null;
                    }
                });
                returnTypeCombo.setText("int4");
            }

        }

        public KingbaseLanguage getLanguage() {
            return language;
        }

        public KingbaseDataType getReturnType() {
            return returnType;
        }
    }

}
