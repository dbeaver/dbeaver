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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Postgre procedure configurator
 */
public class PostgreProcedureConfigurator implements DBEObjectConfigurator<PostgreProcedure> {

    protected static final Log log = Log.getLog(PostgreProcedureConfigurator.class);

    @Override
    public PostgreProcedure configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object parent, @NotNull PostgreProcedure newProcedure, @NotNull Map<String, Object> options) {
        return new UITask<PostgreProcedure>() {
            @Override
            protected PostgreProcedure runTask() {
            	ClassLoader loader = Thread.currentThread().getContextClassLoader();
            	InputStream stream = loader.getResourceAsStream("/org/jkiss/dbeaver/ext/postgresql/PostgreTemplates.properties");
            	Properties prop = new Properties();
            	try {
					prop.load(stream);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	String owner = "";
            	try (JDBCSession session = DBUtils.openMetaSession(monitor, newProcedure, "Read owner for new procedure")) {
            		owner = JDBCUtils.queryString(
            				session,
            				"select roleid::regrole as role from pg_auth_members as m where roleid > 16384 and member::regrole ::text = current_user"
            			);
            		if (owner == null || owner.equals("")) {
            			owner = JDBCUtils.queryString(session,  "select current_user");
            		}
            	} catch (SQLException e) {
                    log.error("Error reading owner for new procedure");
                    log.error(e);
                } catch (DBException e) {
                    log.error("Error reading owner for new procedure");
                    log.error(e);
                }
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
                PostgreLanguage language = editPage.getLanguage();
                if (language != null) {
                    newProcedure.setLanguage(language);
                }
                String template;
                if (newProcedure.getKind() == PostgreProcedureKind.f) {
                	template = prop.getProperty("function");
                } else {
                	template = prop.getProperty("procedure");
                }
                template = template.replace("{{procedureName}}", editPage.getProcedureType().toString().toLowerCase());
                template = template.replace("{{signature}}", newProcedure.getFullQualifiedSignature());
                template = template.replace("{{language}}", (language == null ? "" : "language " + language.getName()));
                template = template.replace("{{owner}}", owner);
                template = template.replace(
                			"{{returnType}}",
                			(newProcedure.getReturnType() == null ? "" : "returns " + newProcedure.getReturnType().getFullyQualifiedName(DBPEvaluationContext.DDL))
                		);
                newProcedure.setObjectDefinitionText(template);
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
                List<PostgreDataType> dataTypes = new ArrayList<>(parent.getDatabase().getLocalDataTypes());
                dataTypes.sort(Comparator.comparing(PostgreDataType::getName));
                returnTypeCombo = UIUtils.createLabelCombo(group, "Return type", SWT.DROP_DOWN);
                for (PostgreDataType dt : dataTypes) {
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
                returnTypeCombo.setText("integer");
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
