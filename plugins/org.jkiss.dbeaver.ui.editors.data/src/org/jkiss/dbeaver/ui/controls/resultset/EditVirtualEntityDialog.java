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
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

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

    public EditVirtualEntityDialog(ResultSetViewer viewer, @Nullable DBSEntity entity, @NotNull DBVEntity vEntity) {
        super(viewer.getControl().getShell(), "Edit logical structure", null);
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

        DBVEntityConstraint constraint = (DBVEntityConstraint) virtualEntityIdentifier.getUniqueKey();

        editUniqueKeyPage = new EditConstraintPage(
            "Define unique identifier",
            constraint);
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
        createButton(buttonsPanel, ID_CREATE_FOREIGN_KEY, "Add", false);
        createButton(buttonsPanel, ID_REMOVE_FOREIGN_KEY, "Remove", false).setEnabled(false);

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

            String transformSettings = "N/A";
            DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(attr, false);
            if (vAttr != null) {
                if (!CommonUtils.isEmpty(vAttr.getTransformSettings().getIncludedTransformers())) {
                    transformSettings = String.join(",", vAttr.getTransformSettings().getIncludedTransformers());
                }
            }
            attrItem.setText(1, transformSettings);

            String colorSettings = "N/A";
            {
                java.util.List<DBVColorOverride> coList = vEntity.getColorOverrides(attr.getName());
                if (!coList.isEmpty()) {
                    StringBuilder coString = new StringBuilder();
                    for (DBVColorOverride co : coList) {
                        if (coString.length() > 0) coString.append(",");
                        coString.append(co.getOperator().getStringValue()).append(Arrays.toString(co.getAttributeValues()));
                    }
                    colorSettings = coString.toString();
                }
            }
            attrItem.setText(2, colorSettings);
        }

        Composite buttonsPanel = UIUtils.createComposite(panel, 2);
        //buttonsPanel.setLayout(new GridLayout(2, false));
        buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        createButton(buttonsPanel, ID_CONFIGURE_TRANSFORMS, "Transforms ...", false).setEnabled(false);
        createButton(buttonsPanel, ID_CONFIGURE_COLORS, "Colors ...", false).setEnabled(false);

        colTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasSelection = colTable.getSelectionIndex() >= 0;
                getButton(ID_CONFIGURE_TRANSFORMS).setEnabled(hasSelection);
                getButton(ID_CONFIGURE_COLORS).setEnabled(hasSelection);
            }
        });
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
            try {
                editUniqueKeyPage.performFinish();
            } catch (DBException e) {
                log.error(e);
            }
        }
        if (editDictionaryPage != null) {
            editDictionaryPage.performFinish();
        }
        super.okPressed();
    }

}
