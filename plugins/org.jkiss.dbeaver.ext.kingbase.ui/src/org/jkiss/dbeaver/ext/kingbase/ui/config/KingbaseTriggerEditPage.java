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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedure;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTriggerBase;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class KingbaseTriggerEditPage extends EntityEditPage {

    private final KingbaseTriggerBase trigger;
    private CSmartSelector<KingbaseProcedure> functionCombo;
    KingbaseProcedure selectedFunction;

    KingbaseTriggerEditPage(KingbaseTriggerBase trigger) {
        super(trigger.getDataSource(), DBSEntityType.TRIGGER);
        this.trigger = trigger;
    }

    @Override
    public DBSObject getObject() {
        return trigger;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite pageContents = (Composite) super.createPageContents(parent);
        addExtraCombo(pageContents);
        UIUtils.createControlLabel(pageContents, KingbaseMessages.dialog_trigger_edit_page_label_trigger_function);
        functionCombo = new KingbaseProcedureSelector(pageContents, parent);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = UIUtils.getFontHeight(functionCombo) * 30;
        functionCombo.setLayoutData(gd);

        UIUtils.asyncExec(functionCombo::layout);

        return pageContents;
    }

    public void addExtraCombo(Composite parent) {
    }

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete() && selectedFunction != null;
    }

    private class KingbaseProcedureSelector extends CSmartSelector<KingbaseProcedure> {
        private final Composite parent;

        KingbaseProcedureSelector(Composite pageContents, Composite parent) {
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
                    return ((KingbaseProcedure) element).getFullQualifiedSignature();
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
                    	KingbaseMessages.dialog_trigger_edit_page_select_function_title, dsNode, curNode,
                        new Class[]{ DBSInstance.class, DBSObjectContainer.class, KingbaseProcedure.class },
                        new Class[]{ KingbaseProcedure.class }, null);
                    if (node instanceof DBNDatabaseNode
                        && ((DBNDatabaseNode) node).getObject() instanceof KingbaseProcedure) {
                        functionCombo.removeAll();
                        selectedFunction = (KingbaseProcedure) ((DBNDatabaseNode) node).getObject();
                        functionCombo.addItem(selectedFunction);
                        functionCombo.select(selectedFunction);
                        updatePageState();
                    }
                }
            }
        }
    }
}
