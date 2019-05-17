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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;

class EditVirtualEntityDialog extends BaseDialog {

    private static final Log log = Log.getLog(EditVirtualEntityDialog.class);

    public static final int ID_CREATE_UNIQUE_KEY = 1000;
    public static final int ID_REMOVE_UNIQUE_KEY = 1001;
    public static final int ID_CREATE_FOREIGN_KEY = 2000;
    public static final int ID_REMOVE_FOREIGN_KEY = 2001;
    private static final int ID_CONFIGURE_TRANSFORMS = 3001;
    private static final int ID_CONFIGURE_COLORS = 3002;

    private ResultSetViewer viewer;
    private DBSEntity entity;
    private DBVEntity vEntity;
    private EditDictionaryPage editDictionaryPage;
    private EditConstraintPage editUniqueKeyPage;
    private DBVEntityConstraint uniqueConstraint;

    public EditVirtualEntityDialog(ResultSetViewer viewer, @Nullable DBSEntity entity, @NotNull DBVEntity vEntity) {
        super(viewer.getControl().getShell(), "Edit logical structure / presentation", null);
        this.viewer = viewer;
        this.entity = entity;
        this.vEntity = vEntity;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        TabFolder tabFolder = new TabFolder(composite, SWT.TOP);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumnsPage(tabFolder);
        createUniqueKeysPage(tabFolder);
        createForeignKeysPage(tabFolder);
        createDictionaryPage(tabFolder);

        tabFolder.setSelection(0);

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
            dictItem.setControl(editDictionaryPage.getControl());
        }
    }

    private void createUniqueKeysPage(TabFolder tabFolder) {
        DBDRowIdentifier virtualEntityIdentifier = viewer.getVirtualEntityIdentifier();
        if (virtualEntityIdentifier == null) {
            return;
        }
        TabItem ukItem = new TabItem(tabFolder, SWT.NONE);
        ukItem.setText("Virtual Unique Key");

        uniqueConstraint = (DBVEntityConstraint) virtualEntityIdentifier.getUniqueKey();

        editUniqueKeyPage = new EditConstraintPage(
            "Define unique identifier",
            uniqueConstraint);
        editUniqueKeyPage.createControl(tabFolder);
        ukItem.setControl(editUniqueKeyPage.getControl());
    }

    private void createForeignKeysPage(TabFolder tabFolder) {
        TabItem fkItem = new TabItem(tabFolder, SWT.NONE);
        fkItem.setText("Virtual Foreign Keys");

        Composite panel = new Composite(tabFolder, 1);
        panel.setLayout(new GridLayout(1, false));
        fkItem.setControl(panel);
        Table fkTable = new Table(panel, SWT.FULL_SELECTION | SWT.BORDER);
        fkTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        fkTable.setHeaderVisible(true);
        UIUtils.executeOnResize(fkTable, () -> UIUtils.packColumns(fkTable, true));

        UIUtils.createTableColumn(fkTable, SWT.LEFT, "Ref Table");
        UIUtils.createTableColumn(fkTable, SWT.LEFT, "Columns");

        Composite buttonsPanel = UIUtils.createComposite(panel, 2);
        buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Button btnAdd = createButton(buttonsPanel, ID_CREATE_FOREIGN_KEY, "Add", false);
        btnAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UIUtils.showMessageBox(getShell(), "Not implemented", "Not Implemented Yet", SWT.ICON_ERROR);
            }
        });
        Button btnRemove = createButton(buttonsPanel, ID_REMOVE_FOREIGN_KEY, "Remove", false);
        btnRemove.setEnabled(false);

        fkTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasSelection = fkTable.getSelectionIndex() >= 0;
                getButton(ID_REMOVE_FOREIGN_KEY).setEnabled(hasSelection);
            }
        });
    }

    private void createColumnsPage(TabFolder tabFolder) {
        TabItem colItem = new TabItem(tabFolder, SWT.NONE);
        colItem.setText("Columns view");

        Composite panel = new Composite(tabFolder, 1);
        panel.setLayout(new GridLayout(1, false));
        colItem.setControl(panel);

        Table colTable = new Table(panel, SWT.FULL_SELECTION | SWT.BORDER);
        colTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        colTable.setLayoutData(gd);
        UIUtils.executeOnResize(colTable, () -> UIUtils.packColumns(colTable, true));

        UIUtils.createTableColumn(colTable, SWT.LEFT, "Name");
        UIUtils.createTableColumn(colTable, SWT.LEFT, "Transforms");
        UIUtils.createTableColumn(colTable, SWT.LEFT, "Colors");

        for (DBDAttributeBinding attr : viewer.getModel().getVisibleAttributes()) {
            TableItem attrItem = new TableItem(colTable, SWT.NONE);;
            attrItem.setData(attr);
            attrItem.setText(0, attr.getName());
            attrItem.setImage(0, DBeaverIcons.getImage(DBValueFormatting.getObjectImage(attr, true)));

            updateColumnItem(attrItem);
        }

        Composite buttonsPanel = UIUtils.createComposite(panel, 2);
        buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        Button btnTransforms = createButton(buttonsPanel, ID_CONFIGURE_TRANSFORMS, "Transforms ...", false);
        btnTransforms.setEnabled(false);
        btnTransforms.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem item = colTable.getItem(colTable.getSelectionIndex());
                DBDAttributeBinding attr = (DBDAttributeBinding) item.getData();
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(attr, true);
                assert vAttr != null;
                DBVTransformSettings transformSettings = vAttr.getTransformSettings();
                if (transformSettings == null) {
                    transformSettings = new DBVTransformSettings();
                }
                TransformerSettingsDialog dialog = new TransformerSettingsDialog(viewer, attr, transformSettings, true);
                if (dialog.open() == IDialogConstants.OK_ID) {
                    vAttr.setTransformSettings(transformSettings);
                }
                updateColumnItem(item);
            }
        });

        Button btnColors = createButton(buttonsPanel, ID_CONFIGURE_COLORS, "Colors ...", false);
        btnColors.setEnabled(false);
        btnColors.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem item = colTable.getItem(colTable.getSelectionIndex());
                DBDAttributeBinding attr = (DBDAttributeBinding) item.getData();
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(attr, true);
                ColorSettingsDialog dialog = new ColorSettingsDialog(viewer, attr, null);
                if (dialog.open() == IDialogConstants.OK_ID) {
                    //vEntity.setColorOverride();
                }

                updateColumnItem(item);
            }
        });

        colTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasSelection = colTable.getSelectionIndex() >= 0;
                getButton(ID_CONFIGURE_TRANSFORMS).setEnabled(hasSelection);
                getButton(ID_CONFIGURE_COLORS).setEnabled(hasSelection);
            }
        });
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
                    for (Object value : co.getAttributeValues()) {
                        coStrings.add(CommonUtils.toString(value));
                    }
                }
                colorSettings = String.join(",", coStrings);
            }
        }
        attrItem.setText(2, colorSettings);
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
            uniqueConstraint.setAttributes(uniqueAttrs);
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
        viewer.persistConfig();
        super.okPressed();
    }

}
