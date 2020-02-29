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
package org.jkiss.dbeaver.ui.controls.resultset.virtual;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class EditVirtualEntityDialog extends BaseDialog {

    private static final Log log = Log.getLog(EditVirtualEntityDialog.class);

    private static final String DIALOG_ID = "DBeaver.EditVirtualEntityDialog";//$NON-NLS-1$

    public static final int ID_CREATE_UNIQUE_KEY = 1000;
    public static final int ID_REMOVE_UNIQUE_KEY = 1001;
    public static final int ID_CREATE_FOREIGN_KEY = 2000;
    public static final int ID_REMOVE_FOREIGN_KEY = 2001;

    private ResultSetViewer viewer;
    private DBSEntity entity;
    private DBVEntity vEntity;
    private EditDictionaryPage editDictionaryPage;
    private EditConstraintPage editUniqueKeyPage;
    private DBVEntityConstraint uniqueConstraint;
    private InitPage initPage = InitPage.UNIQUE_KEY;

    private EditVirtualColumnsPage columnsPage;

    private boolean structChanged = false;

    public enum InitPage {
        ATTRIBUTES,
        UNIQUE_KEY,
        FOREIGN_KEYS,
        DICTIONARY,
    }

    public EditVirtualEntityDialog(ResultSetViewer viewer, @Nullable DBSEntity entity, @NotNull DBVEntity vEntity) {
        super(viewer.getControl().getShell(), "Edit logical structure / presentation", null);
        this.viewer = viewer;
        this.entity = entity;
        this.vEntity = vEntity;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    public InitPage getInitPage() {
        return initPage;
    }

    public void setInitPage(InitPage initPage) {
        this.initPage = initPage;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        try {
            UIUtils.runInProgressService(monitor -> {
                for (DBVEntityForeignKey fk : vEntity.getForeignKeys()) {
                    try {
                        fk.getRealReferenceConstraint(monitor);
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
        Composite composite = super.createDialogArea(parent);

        TabFolder tabFolder = new TabFolder(composite, SWT.TOP);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumnsPage(tabFolder);
        createUniqueKeysPage(tabFolder);
        createForeignKeysPage(tabFolder);
        createDictionaryPage(tabFolder);

        for (TabItem item : tabFolder.getItems()) {
            if (item.getData() == initPage) {
                tabFolder.setSelection(item);
                break;
            }
        }

        UIUtils.createInfoLabel(composite, "Entity logical structure is defined on client-side.\nYou can define virtual unique/foreign keys even if physical database\n" +
            "doesn't have or doesn't support them. Also you can define how to view column values.");

        return parent;
    }

    private void createDictionaryPage(TabFolder tabFolder) {
        if (entity != null) {
            editDictionaryPage = new EditDictionaryPage(entity);
            editDictionaryPage.createControl(tabFolder);
            TabItem dictItem = new TabItem(tabFolder, SWT.NONE);
            dictItem.setText("Dictionary");
            dictItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_PACKAGE));
            dictItem.setControl(editDictionaryPage.getControl());
            dictItem.setData(InitPage.DICTIONARY);
        }
    }

    private void createColumnsPage(TabFolder tabFolder) {
        TabItem attrsItem = new TabItem(tabFolder, SWT.NONE);
        attrsItem.setText("Virtual Columns");
        attrsItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_COLUMN));
        attrsItem.setData(InitPage.ATTRIBUTES);

        Composite panel = createColumnsEditPanel(tabFolder);

        attrsItem.setControl(panel);
    }

    @NotNull
    private Composite createColumnsEditPanel(TabFolder tabFolder) {
        columnsPage = new EditVirtualColumnsPage(viewer, vEntity);
        return columnsPage.createPageContents(tabFolder);
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

    private void createUniqueKeysPage(TabFolder tabFolder) {
        uniqueConstraint = vEntity.getBestIdentifier();
        if (uniqueConstraint == null) {
            return;
        }
        TabItem ukItem = new TabItem(tabFolder, SWT.NONE);
        ukItem.setText("Virtual Unique Key");
        ukItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_UNIQUE_KEY));
        ukItem.setData(InitPage.UNIQUE_KEY);

        editUniqueKeyPage = new EditConstraintPage(
            "Define unique identifier",
            uniqueConstraint);
        editUniqueKeyPage.createControl(tabFolder);
        ukItem.setControl(editUniqueKeyPage.getControl());
    }

    private void createForeignKeysPage(TabFolder tabFolder) {
        TabItem fkItem = new TabItem(tabFolder, SWT.NONE);
        fkItem.setText("Virtual Foreign Keys");
        fkItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        fkItem.setData(InitPage.FOREIGN_KEYS);

        Composite panel = new Composite(tabFolder, 1);
        panel.setLayout(new GridLayout(1, false));
        fkItem.setControl(panel);
        Table fkTable = new Table(panel, SWT.FULL_SELECTION | SWT.BORDER);
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
            Composite buttonsPanel = UIUtils.createComposite(panel, 2);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            Button btnAdd = createButton(buttonsPanel, ID_CREATE_FOREIGN_KEY, "Add", false);
            btnAdd.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityForeignKey virtualFK = EditForeignKeyPage.createVirtualForeignKey(vEntity);
                    if (virtualFK != null) {
                        createForeignKeyItem(fkTable, virtualFK);
                        structChanged = true;
                    }
                }
            });

            Button btnRemove = createButton(buttonsPanel, ID_REMOVE_FOREIGN_KEY, "Remove", false);
            btnRemove.setEnabled(false);
            btnRemove.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityForeignKey virtualFK = (DBVEntityForeignKey) fkTable.getSelection()[0].getData();
                    if (!UIUtils.confirmAction(getShell(),
                        "Delete virtual FK",
                        "Are you sure you want to delete virtual foreign key '" + virtualFK.getName() + "'?")) {
                        return;
                    }
                    vEntity.removeForeignKey(virtualFK);
                    fkTable.remove(fkTable.getSelectionIndices());
                    structChanged = true;
                }
            });
        }

        fkTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasSelection = fkTable.getSelectionIndex() >= 0;
                getButton(ID_REMOVE_FOREIGN_KEY).setEnabled(hasSelection);
            }
        });
    }

    private void createForeignKeyItem(Table fkTable, DBVEntityForeignKey fk) {
        DBSEntityConstraint referencedConstraint = fk.getReferencedConstraint();
        if (referencedConstraint == null) {
            log.debug("No reference constraint for FK " + fk.getName());
            return;
        }

        TableItem item = new TableItem(fkTable, SWT.NONE);
        //item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        DBSEntity refEntity = referencedConstraint.getParentObject();

        item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_FOREIGN_KEY));
        if (referencedConstraint != null) {
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

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed()
    {
        if (editUniqueKeyPage != null) {
            Collection<DBSEntityAttribute> uniqueAttrs = editUniqueKeyPage.getSelectedAttributes();
            uniqueConstraint.setName(editUniqueKeyPage.getConstraintName());
            uniqueConstraint.setUseAllColumns(editUniqueKeyPage.isUseAllColumns());
            uniqueConstraint.setAttributes(uniqueConstraint.isUseAllColumns() ? Collections.emptyList() : uniqueAttrs);
            DBDRowIdentifier virtualEntityIdentifier = viewer.getVirtualEntityIdentifier();
            if (virtualEntityIdentifier != null) {
                try {
                    virtualEntityIdentifier.reloadAttributes(new VoidProgressMonitor(), viewer.getModel().getAttributes());
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
        if (editDictionaryPage != null) {
            editDictionaryPage.saveDictionarySettings();
        }
        vEntity.persistConfiguration();
        if (structChanged || columnsPage.isStructChanged()) {
            viewer.refreshData(null);
        }
        super.okPressed();
    }

}
