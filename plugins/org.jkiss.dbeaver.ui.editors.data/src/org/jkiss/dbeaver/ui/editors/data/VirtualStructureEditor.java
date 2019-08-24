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
package org.jkiss.dbeaver.ui.editors.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.EditVirtualEntityDialog;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * VirtualStructureEditor
 */
public class VirtualStructureEditor extends AbstractDatabaseObjectEditor<DBSEntity>
{
    private static final Log log = Log.getLog(VirtualStructureEditor.class);

    private boolean activated;
    private Composite parent;
    private DBSEntity entity;
    private DBVEntity vEntity;
    private EditDictionaryPage editDictionaryPage;
    private EditConstraintPage editUniqueKeyPage;
    private DBVEntityConstraint uniqueConstraint;
    private boolean fkChanged = false;

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;

        this.entity = getDatabaseObject();
        this.vEntity = DBVUtils.getVirtualEntity(entity, true);
    }

    @Override
    public void setFocus() {

    }

    @Override
    public void refreshPart(Object source, boolean force) {

    }

    @Override
    public void activatePart() {
        if (!activated) {
            createEditorUI();
            activated = true;
        }
    }

    private void createEditorUI() {
        try {
            UIUtils.runInProgressService(monitor -> {
                for (DBVEntityForeignKey fk : vEntity.getForeignKeys()) {
                    try {
                        fk.getRealReferenceConatraint(monitor);
                        fk.getAssociatedEntity(monitor);
                    } catch (DBException e) {
                        log.debug(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }

        Composite composite = UIUtils.createComposite(parent, 1);
        ((GridLayout)composite.getLayout()).makeColumnsEqualWidth = true;

        //createColumnsPage(tabFolder);
        createUniqueKeysPage(composite);
        createForeignKeysPage(composite);
        //createDictionaryPage(composite);

        UIUtils.createInfoLabel(composite, "Entity logical structure is defined on client-side." +
            "\nYou can define virtual unique/foreign keys even if physical database " +
            "doesn't have or doesn't support them.", GridData.FILL_HORIZONTAL, 2);
        parent.layout(true, true);
    }

    private void createDictionaryPage(TabFolder tabFolder) {
        if (entity != null) {
            editDictionaryPage = new EditDictionaryPage(entity);
            editDictionaryPage.createControl(tabFolder);
            TabItem dictItem = new TabItem(tabFolder, SWT.NONE);
            dictItem.setText("Dictionary");
            dictItem.setControl(editDictionaryPage.getControl());
            dictItem.setData(EditVirtualEntityDialog.InitPage.DICTIONARY);
        }
    }

    private void createUniqueKeysPage(Composite parent) {
        uniqueConstraint = vEntity.getBestIdentifier();
        if (uniqueConstraint == null) {
            return;
        }
        Group group = UIUtils.createControlGroup(parent, "Virtual Unique Keys", 1, GridData.FILL_BOTH, SWT.DEFAULT);

        Table ukTable = new Table(group, SWT.FULL_SELECTION | SWT.BORDER);
        ukTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        ukTable.setHeaderVisible(true);
        UIUtils.executeOnResize(ukTable, () -> UIUtils.packColumns(ukTable, true));

        UIUtils.createTableColumn(ukTable, SWT.LEFT, "Ref Table");
        UIUtils.createTableColumn(ukTable, SWT.LEFT, "Columns");

        for (DBVEntityConstraint uk : vEntity.getConstraints()) {
            if (!CommonUtils.isEmpty(uk.getAttributes())) {
                createUniqueKeyItem(ukTable, uk);
            }
        }

        {
            Composite buttonsPanel = UIUtils.createComposite(group, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            Button btnAdd = UIUtils.createDialogButton(buttonsPanel, "Add", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityConstraint newConstraint = new DBVEntityConstraint(vEntity, DBSEntityConstraintType.VIRTUAL_KEY, vEntity.getName() + "_uk");
                    EditConstraintPage editPage = new EditConstraintPage("Edit unique key", newConstraint);
                    if (editPage.edit()) {
                        changeConstraint(newConstraint, editPage);
                        vEntity.addConstraint(newConstraint);
                        createUniqueKeyItem(ukTable, newConstraint);
                        vEntity.persistConfiguration();
                    }
                }
            });

            Button btnEdit = UIUtils.createDialogButton(buttonsPanel, "Edit", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TableItem ukItem = ukTable.getSelection()[0];
                    DBVEntityConstraint virtualUK = (DBVEntityConstraint) ukItem.getData();
                    EditConstraintPage editPage = new EditConstraintPage("Edit unique key", virtualUK);
                    if (editPage.edit()) {
                        changeConstraint(virtualUK, editPage);
                        ukItem.setText(0, DBUtils.getObjectFullName(virtualUK, DBPEvaluationContext.UI));
                        ukItem.setText(1, virtualUK.getAttributes().stream().map(DBVEntityConstraintColumn::getAttributeName).collect(Collectors.joining(",")));
                        vEntity.persistConfiguration();
                    }
                }
            });
            btnEdit.setEnabled(false);

            Button btnRemove = UIUtils.createDialogButton(buttonsPanel, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityConstraint virtualUK = (DBVEntityConstraint) ukTable.getSelection()[0].getData();
                    if (!UIUtils.confirmAction(parent.getShell(),
                        "Delete virtual unique key",
                        "Are you sure you want to delete virtual unique key '" + virtualUK.getName() + "'?")) {
                        return;
                    }
                    vEntity.removeConstraint(virtualUK);
                    ukTable.remove(ukTable.getSelectionIndices());
                    vEntity.persistConfiguration();
                }
            });
            btnRemove.setEnabled(false);

            ukTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    btnRemove.setEnabled(ukTable.getSelectionIndex() >= 0);
                    btnEdit.setEnabled(ukTable.getSelectionIndex() >= 0);
                }
            });
        }

/*
        editUniqueKeyPage = new EditConstraintPage(
            "Define unique identifier",
            uniqueConstraint);
        editUniqueKeyPage.createControl(group);
*/
    }

    private void changeConstraint(DBVEntityConstraint constraint, EditConstraintPage editPage) {
        constraint.setName(editPage.getConstraintName());
        constraint.setAttributes(editPage.getSelectedAttributes());
    }

    private void createUniqueKeyItem(Table ukTable, DBVEntityConstraint uk) {
        TableItem item = new TableItem(ukTable, SWT.NONE);

        item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_UNIQUE_KEY));
        item.setText(0, DBUtils.getObjectFullName(uk, DBPEvaluationContext.UI));
        String ownAttrNames = uk.getAttributes().stream().map(DBVEntityConstraintColumn::getAttributeName)
            .collect(Collectors.joining(","));
        item.setText(1, ownAttrNames);

        item.setData(uk);
    }

    private void createForeignKeysPage(Composite parent) {
        Group group = UIUtils.createControlGroup(parent, "Virtual Foreign Keys", 1, GridData.FILL_BOTH, SWT.DEFAULT);

        Table fkTable = new Table(group, SWT.FULL_SELECTION | SWT.BORDER);
        fkTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        fkTable.setHeaderVisible(true);
        UIUtils.executeOnResize(fkTable, () -> UIUtils.packColumns(fkTable, true));

        UIUtils.createTableColumn(fkTable, SWT.LEFT, "Ref Table");
        UIUtils.createTableColumn(fkTable, SWT.LEFT, "Columns");
        UIUtils.createTableColumn(fkTable, SWT.LEFT, "Ref Datasource");

        for (DBVEntityForeignKey fk : vEntity.getForeignKeys()) {
            createForeignKeyItem(fkTable, fk);
        }

        {
            Composite buttonsPanel = UIUtils.createComposite(group, 2);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            Button btnAdd = UIUtils.createDialogButton(buttonsPanel, "Add", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityForeignKey virtualFK = EditForeignKeyPage.createVirtualForeignKey(vEntity);
                    if (virtualFK != null) {
                        createForeignKeyItem(fkTable, virtualFK);
                        fkChanged = true;
                    }
                }
            });

            Button btnRemove = UIUtils.createDialogButton(buttonsPanel, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityForeignKey virtualFK = (DBVEntityForeignKey) fkTable.getSelection()[0].getData();
                    if (!UIUtils.confirmAction(parent.getShell(),
                        "Delete virtual FK",
                        "Are you sure you want to delete virtual foreign key '" + virtualFK.getName() + "'?")) {
                        return;
                    }
                    vEntity.removeForeignKey(virtualFK);
                    fkTable.remove(fkTable.getSelectionIndices());
                    fkChanged = true;
                }
            });
            btnRemove.setEnabled(false);

            fkTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean hasSelection = fkTable.getSelectionIndex() >= 0;
                    btnRemove.setEnabled(hasSelection);
                }
            });
        }
    }

    private void createForeignKeyItem(Table fkTable, DBVEntityForeignKey fk) {
        TableItem item = new TableItem(fkTable, SWT.NONE);
        //item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        DBSEntity refEntity = fk.getReferencedConstraint().getParentObject();

        item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        if (fk.getReferencedConstraint() != null) {
            item.setText(0, DBUtils.getObjectFullName(refEntity, DBPEvaluationContext.UI));
        }
        String ownAttrNames = fk.getAttributes().stream().map(DBVEntityForeignKeyColumn::getAttributeName)
            .collect(Collectors.joining(","));
        String refAttrNames = fk.getAttributes().stream().map(DBVEntityForeignKeyColumn::getRefAttributeName)
            .collect(Collectors.joining(","));
        item.setText(1, "(" + ownAttrNames + ") -> (" + refAttrNames + ")");

        item.setImage(2, DBeaverIcons.getImage(refEntity.getDataSource().getContainer().getDriver().getIcon()));
        item.setText(2, refEntity.getDataSource().getContainer().getName());

        item.setData(fk);
    }

    private void updateColumnItem(TableItem attrItem) {
        DBDAttributeBinding attr = (DBDAttributeBinding) attrItem.getData();
        String transformStr = "";
        DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(attr, false);
        if (vAttr != null) {
            DBVTransformSettings transformSettings = vAttr.getTransformSettings();
            if (transformSettings != null) {
                if (!CommonUtils.isEmpty(transformSettings.getIncludedTransformers())) {
                    transformStr = String.join(",", transformSettings.getIncludedTransformers());
                } else if (!CommonUtils.isEmpty(transformSettings.getCustomTransformer())) {
                    DBDAttributeTransformerDescriptor td =
                        DBWorkbench.getPlatform().getValueHandlerRegistry().getTransformer(transformSettings.getCustomTransformer());
                    if (td != null) {
                        transformStr = td.getName();
                    }
                }
            }
        }
        attrItem.setText(1, transformStr);

        String colorSettings = "";
        {
            java.util.List<DBVColorOverride> coList = vEntity.getColorOverrides(attr.getName());
            if (!coList.isEmpty()) {
                java.util.List<String> coStrings = new ArrayList<>();
                for (DBVColorOverride co : coList) {
                    if (co.getAttributeValues() != null) {
                        for (Object value : co.getAttributeValues()) {
                            coStrings.add(CommonUtils.toString(value));
                        }
                    }
                }
                colorSettings = String.join(",", coStrings);
            }
        }
        attrItem.setText(2, colorSettings);
    }

}
