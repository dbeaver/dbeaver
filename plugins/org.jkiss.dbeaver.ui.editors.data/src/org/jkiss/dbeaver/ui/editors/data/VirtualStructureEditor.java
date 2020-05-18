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
package org.jkiss.dbeaver.ui.editors.data;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.virtual.EditVirtualColumnsPage;
import org.jkiss.dbeaver.ui.controls.resultset.virtual.EditVirtualEntityDialog;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * VirtualStructureEditor
 */
public class VirtualStructureEditor extends AbstractDatabaseObjectEditor<DBSEntity> implements DBPEventListener {
    private static final Log log = Log.getLog(VirtualStructureEditor.class);

    private boolean activated;
    private Composite parent;
    private DBSEntity entity;
    private DBVEntity vEntity;
    private EditDictionaryPage editDictionaryPage;
    private DBVEntityConstraint uniqueConstraint;
    private Table ukTable;
    private Table fkTable;
    private Table refTable;
    private EditVirtualColumnsPage columnsPage;

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;

        this.entity = getDatabaseObject();
        this.vEntity = DBVUtils.getVirtualEntity(entity, true);
    }

    @Override
    public void setFocus() {
        this.parent.setFocus();
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        new AbstractJob(DataEditorsMessages.virtual_structure_editor_abstract_job_load_entity) {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                for (DBVEntityForeignKey fk : vEntity.getForeignKeys()) {
                    try {
                        fk.getRealReferenceConstraint(monitor);
                        fk.getAssociatedEntity(monitor);
                    } catch (DBException e) {
                        log.debug(e);
                    }
                }
                UIUtils.asyncExec(() -> refreshVisuals());
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void refreshVisuals() {
        columnsPage.refreshAttributes();

        ukTable.removeAll();
        try {
            for (DBVEntityConstraint uk : vEntity.getConstraints()) {
                if (uk.isUseAllColumns() || !CommonUtils.isEmpty(uk.getAttributes())) {
                    createUniqueKeyItem(ukTable, uk);
                }
            }
        } catch (Exception e) {
            log.error("Error loading virtual unique keys", e);
        }
        UIUtils.packColumns(ukTable, true);

        fkTable.removeAll();
        try {
            for (DBVEntityForeignKey fk : vEntity.getForeignKeys()) {
                createForeignKeyItem(fkTable, fk, false);
            }
        } catch (Exception e) {
            log.error("Error loading virtual foreign keys", e);
        }
        UIUtils.packColumns(fkTable, true);

        refTable.removeAll();
        try {
            for (DBVEntityForeignKey fk : DBVUtils.getVirtualReferences(entity)) {
                createForeignKeyItem(refTable, fk, true);
            }
        } catch (Exception e) {
            log.error("Error loading virtual foreign keys", e);
        }
        UIUtils.packColumns(refTable, true);
    }

    @Override
    public void activatePart() {
        if (!activated) {
            createEditorUI();
            refreshPart(this, true);
            activated = true;
        }
    }

    @Override
    public void dispose() {
        if (activated) {
            DBSEntity dbObject = getDatabaseObject();
            if (dbObject != null) {
                DBUtils.getObjectRegistry(dbObject).removeDataSourceListener(this);
            }
        }
        super.dispose();
    }

    private void createEditorUI() {
        Composite composite = UIUtils.createComposite(parent, 1);
        ((GridLayout)composite.getLayout()).makeColumnsEqualWidth = true;

        Composite keysComposite = UIUtils.createComposite(composite, 2);
        ((GridLayout)keysComposite.getLayout()).makeColumnsEqualWidth = true;
        keysComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        createColumnsPage(keysComposite);
        createUniqueKeysPage(keysComposite);
        createForeignKeysPage(keysComposite);
        createReferencesPage(keysComposite);

//        Composite attrsComposite = UIUtils.createComposite(composite, 1);
//        attrsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createInfoLabel(composite, DataEditorsMessages.virtual_structure_editor_info_label_entity_structure, GridData.FILL_HORIZONTAL, 1);
        parent.layout(true, true);

        DBSEntity dbObject = getDatabaseObject();
        if (dbObject != null) {
            DBUtils.getObjectRegistry(dbObject).addDataSourceListener(this);
        }
    }

    private void createDictionaryPage(TabFolder tabFolder) {
        if (entity != null) {
            editDictionaryPage = new EditDictionaryPage(entity);
            editDictionaryPage.createControl(tabFolder);
            TabItem dictItem = new TabItem(tabFolder, SWT.NONE);
            dictItem.setText(DataEditorsMessages.virtual_structure_editor_dictionary_page_text);
            dictItem.setControl(editDictionaryPage.getControl());
            dictItem.setData(EditVirtualEntityDialog.InitPage.DICTIONARY);
        }
    }

    private void createColumnsPage(Composite parent) {
        Group group = UIUtils.createControlGroup(parent, DataEditorsMessages.virtual_structure_editor_columns_group_virtual, 1, GridData.FILL_BOTH, SWT.DEFAULT);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        columnsPage = new EditVirtualColumnsPage(null, vEntity);
        columnsPage.createControl(group);
    }

    private void createUniqueKeysPage(Composite parent) {
        uniqueConstraint = vEntity.getBestIdentifier();
        if (uniqueConstraint == null) {
            return;
        }
        Group group = UIUtils.createControlGroup(parent, DataEditorsMessages.virtual_structure_editor_columns_group_unique_keys, 1, GridData.FILL_BOTH, SWT.DEFAULT);

        ukTable = new Table(group, SWT.FULL_SELECTION | SWT.BORDER);
        ukTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        ukTable.setHeaderVisible(true);

        UIUtils.createTableColumn(ukTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_key_name);
        UIUtils.createTableColumn(ukTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_columns);

        {
            Composite buttonsPanel = UIUtils.createComposite(group, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            Button btnAdd = UIUtils.createDialogButton(buttonsPanel, DataEditorsMessages.virtual_structure_editor_dialog_button_add, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityConstraint newConstraint = new DBVEntityConstraint(vEntity, DBSEntityConstraintType.VIRTUAL_KEY, vEntity.getName() + "_uk");
                    EditConstraintPage editPage = new EditConstraintPage(DataEditorsMessages.virtual_structure_editor_constraint_page_edit_key, newConstraint);
                    if (editPage.edit()) {
                        changeConstraint(newConstraint, editPage);
                        vEntity.addConstraint(newConstraint);
                        createUniqueKeyItem(ukTable, newConstraint);
                        vEntity.persistConfiguration();
                    }
                }
            });

            SelectionAdapter ukEditListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TableItem ukItem = ukTable.getSelection()[0];
                    DBVEntityConstraint virtualUK = (DBVEntityConstraint) ukItem.getData();
                    EditConstraintPage editPage = new EditConstraintPage(DataEditorsMessages.virtual_structure_editor_constraint_page_edit_key, virtualUK);
                    if (editPage.edit()) {
                        changeConstraint(virtualUK, editPage);
                        ukItem.setText(0, DBUtils.getObjectFullName(virtualUK, DBPEvaluationContext.UI));
                        ukItem.setText(1, getConstraintAttrNames(virtualUK));
                        vEntity.persistConfiguration();
                    }
                }
            };
            Button btnEdit = UIUtils.createDialogButton(buttonsPanel, DataEditorsMessages.virtual_structure_editor_dialog_button_edit, ukEditListener);
            btnEdit.setEnabled(false);

            Button btnRemove = UIUtils.createDialogButton(buttonsPanel, DataEditorsMessages.virtual_structure_editor_dialog_button_remove, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityConstraint virtualUK = (DBVEntityConstraint) ukTable.getSelection()[0].getData();
                    if (!UIUtils.confirmAction(parent.getShell(),
                    		DataEditorsMessages.virtual_structure_editor_confirm_action_delete_key,
                        NLS.bind(DataEditorsMessages.virtual_structure_editor_confirm_action_question_delete, virtualUK.getName()))) {
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

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    ukEditListener.widgetSelected(e);
                }
            });
        }
    }

    private void changeConstraint(DBVEntityConstraint constraint, EditConstraintPage editPage) {
        constraint.setName(editPage.getConstraintName());
        constraint.setAttributes(editPage.getSelectedAttributes());
        constraint.setUseAllColumns(editPage.isUseAllColumns());
    }

    private void createUniqueKeyItem(Table ukTable, DBVEntityConstraint uk) {
        TableItem item = new TableItem(ukTable, SWT.NONE);

        item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_UNIQUE_KEY));
        item.setText(0, DBUtils.getObjectFullName(uk, DBPEvaluationContext.UI));
        String ownAttrNames = getConstraintAttrNames(uk);
        item.setText(1, ownAttrNames);

        item.setData(uk);
    }

    private String getConstraintAttrNames(DBVEntityConstraint uk) {
        return uk.isUseAllColumns() ? "*" : uk.getAttributes().stream().map(DBVEntityConstraintColumn::getAttributeName)
            .collect(Collectors.joining(","));
    }

    private void createForeignKeysPage(Composite parent) {
        Group group = UIUtils.createControlGroup(parent, DataEditorsMessages.virtual_structure_editor_control_group_label_foreign_key, 1, GridData.FILL_BOTH, SWT.DEFAULT);

        fkTable = new Table(group, SWT.FULL_SELECTION | SWT.BORDER);
        fkTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        fkTable.setHeaderVisible(true);

        UIUtils.createTableColumn(fkTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_target_table);
        UIUtils.createTableColumn(fkTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_columns);
        UIUtils.createTableColumn(fkTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_datasource);

        {
            Composite buttonsPanel = UIUtils.createComposite(group, 2);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            UIUtils.createDialogButton(buttonsPanel, DataEditorsMessages.virtual_structure_editor_dialog_button_add, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityForeignKey virtualFK = EditForeignKeyPage.createVirtualForeignKey(vEntity);
                    if (virtualFK != null) {
                        createForeignKeyItem(fkTable, virtualFK, true);
                    }
                }
            });

            Button btnRemove = UIUtils.createDialogButton(buttonsPanel, DataEditorsMessages.virtual_structure_editor_dialog_button_remove, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityForeignKey virtualFK = (DBVEntityForeignKey) fkTable.getSelection()[0].getData();
                    if (!UIUtils.confirmAction(parent.getShell(),
                    		DataEditorsMessages.virtual_structure_editor_confirm_action_delete_fk,
                        NLS.bind(DataEditorsMessages.virtual_structure_editor_confirm_action_question_delete_foreign, virtualFK.getName()))) {
                        return;
                    }
                    vEntity.removeForeignKey(virtualFK);
                    fkTable.remove(fkTable.getSelectionIndices());
                    ((Button)e.widget).setEnabled(false);
                }
            });
            btnRemove.setEnabled(false);

            fkTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    btnRemove.setEnabled(fkTable.getSelectionIndex() >= 0);
                }
            });
        }
    }

    private void createReferencesPage(Composite parent) {
        Group group = UIUtils.createControlGroup(parent, DataEditorsMessages.virtual_structure_editor_control_group_references, 1, GridData.FILL_BOTH, SWT.DEFAULT);

        refTable = new Table(group, SWT.FULL_SELECTION | SWT.BORDER);
        refTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        refTable.setHeaderVisible(true);

        UIUtils.createTableColumn(refTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_source_table);
        UIUtils.createTableColumn(refTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_columns);
        UIUtils.createTableColumn(refTable, SWT.LEFT, DataEditorsMessages.virtual_structure_editor_table_column_source_datasource);

        {
            Composite buttonsPanel = UIUtils.createComposite(group, 2);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            UIUtils.createDialogButton(buttonsPanel, DataEditorsMessages.virtual_structure_editor_dialog_button_refresh, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                }
            }).setEnabled(false);
        }
    }

    private void createForeignKeyItem(Table fkTable, DBVEntityForeignKey fk, boolean ref) {
        TableItem item = new TableItem(fkTable, SWT.NONE);
        //item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        DBSEntity refEntity;
        if (ref) {
            refEntity = fk.getEntity();
        } else {
            DBSEntityConstraint refConstraint = fk.getReferencedConstraint();
            refEntity = refConstraint == null ? null : refConstraint.getParentObject();
        }

        item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        item.setText(0, refEntity == null ? "?" : DBUtils.getObjectFullName(refEntity, DBPEvaluationContext.UI));

        String ownAttrNames = fk.getAttributes().stream().map(DBVEntityForeignKeyColumn::getAttributeName)
            .collect(Collectors.joining(","));
        String refAttrNames = fk.getAttributes().stream().map(DBVEntityForeignKeyColumn::getRefAttributeName)
            .collect(Collectors.joining(","));
        item.setText(1, "(" + ownAttrNames + ") -> (" + refAttrNames + ")");
        if (refEntity != null) {
            item.setImage(2, DBeaverIcons.getImage(refEntity.getDataSource().getContainer().getDriver().getIcon()));
            item.setText(2, refEntity.getDataSource().getContainer().getName());
        }

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

    @Override
    public void handleDataSourceEvent(DBPEvent event) {
        if (event.getObject() == vEntity) {
            UIUtils.asyncExec(() -> refreshPart(event, true));
        }
    }
}
