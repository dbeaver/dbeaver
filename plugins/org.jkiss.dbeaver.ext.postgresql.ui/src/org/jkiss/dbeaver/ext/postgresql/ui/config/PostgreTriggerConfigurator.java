/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

/**
 * Postgre sequence configurator
 */
public class PostgreTriggerConfigurator implements DBEObjectConfigurator<PostgreTrigger> {
    
    protected static final Log log = Log.getLog(PostgreTriggerConfigurator.class);

    @Override
    public PostgreTrigger configureObject(DBRProgressMonitor monitor, Object parent, PostgreTrigger trigger) {
        return new UITask<PostgreTrigger>() {

            @Override
            protected PostgreTrigger runTask() {
                TriggerEditPage editPage = new TriggerEditPage(trigger);
                if (!editPage.edit()) {
                    return null;
                }
                try {
                    trigger.setName(editPage.getEntityName());
                    trigger.setFunction(editPage.selectedFunction);
                    String procName = "X";
                    PostgreProcedure function = trigger.getFunction(monitor);
                    if (function != null) {
                        procName = function.getFullQualifiedSignature();
                    }
                    trigger.setObjectDefinitionText("CREATE TRIGGER " + DBUtils.getQuotedIdentifier(trigger) + "\n"
                            + "BEFORE UPDATE" + " " + "\n" + "ON " + trigger.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
                            + " FOR EACH ROW" + "\n" + "EXECUTE PROCEDURE " + (function == null ? procName : function.getFullyQualifiedName(DBPEvaluationContext.DDL))+ "()\n");
                } catch (DBException e) {
                    log.error(e);
                }
                return trigger;
            }
        }.execute();
    }

    public class TriggerEditPage extends EntityEditPage {

        PostgreTrigger trigger;
        CSmartSelector functionCombo;
        PostgreProcedure selectedFunction;
        Text processIdText;
        
        public TriggerEditPage editPage;

        public TriggerEditPage(PostgreTrigger trigger) {
            super(trigger.getDataSource(), DBSEntityType.TRIGGER);
            this.trigger = trigger;
        }
        
        public TriggerEditPage getEditPage() {
            return editPage;
        }

        @Override
        protected Control createPageContents(Composite parent) {
            Composite pageContents = (Composite) super.createPageContents(parent);
            UIUtils.createControlLabel(pageContents, "Trigger function");
            functionCombo = new PostgreProcedureSelector(pageContents, parent);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = UIUtils.getFontHeight(functionCombo) * 30;
            functionCombo.setLayoutData(gd);
            return pageContents;
        }

        private class PostgreProcedureSelector extends CSmartSelector<PostgreProcedure> {
            private final Composite parent;

            public PostgreProcedureSelector(Composite pageContents, Composite parent) {
                super(pageContents, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new LabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return DBeaverIcons.getImage(DBIcon.TREE_PROCEDURE);
                    }

                    @Override
                    public String getText(Object element) {
                        if (element == null) {
                            return "N/A";
                        }
                        return ((PostgreProcedure) element).getFullQualifiedSignature();
                    }
                });
                this.parent = parent;
            }

            @Override
            protected void dropDown(boolean drop) {
                if (drop) {
                    DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                    DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(trigger.getDatabase());
                    if (dsNode != null) {
                        DBNNode curNode = selectedFunction == null ? null
                                : navigatorModel.getNodeByObject(selectedFunction);
                        DBNNode node = DBWorkbench.getPlatformUI().selectObject(parent.getShell(),
                                "Select function for ", dsNode, curNode,
                                new Class[] { DBSInstance.class, DBSObjectContainer.class, PostgreProcedure.class },
                                new Class[] { PostgreProcedure.class }, null);
                        if (node instanceof DBNDatabaseNode
                                && ((DBNDatabaseNode) node).getObject() instanceof PostgreProcedure) {
                            functionCombo.removeAll();
                            selectedFunction = (PostgreProcedure) ((DBNDatabaseNode) node).getObject();
                            functionCombo.addItem(selectedFunction);
                            functionCombo.select(selectedFunction);
                        }
                        
                    }
                }

            }
            
        }
    }

}
