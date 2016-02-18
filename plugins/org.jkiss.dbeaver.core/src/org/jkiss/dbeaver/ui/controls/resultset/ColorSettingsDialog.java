/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;

import java.util.Arrays;
import java.util.List;

class ColorSettingsDialog extends HelpEnabledDialog {

    private static final String DIALOG_ID = "DBeaver.ColorSettingsDialog";//$NON-NLS-1$

    private final ResultSetViewer resultSetViewer;
    @Nullable
    private final List<DBDAttributeBinding> attributes;
    @Nullable
    private final ResultSetRow row;

    private CheckboxTableViewer colorsViewer;

    public ColorSettingsDialog(
        ResultSetViewer resultSetViewer,
        @Nullable final DBDAttributeBinding attr,
        @Nullable final ResultSetRow row)
    {
        super(resultSetViewer.getControl().getShell(), IHelpContextIds.CTX_ROW_COLORS);
        this.resultSetViewer = resultSetViewer;
        this.attributes = Arrays.asList(resultSetViewer.getModel().getAttributes());
        this.row = row;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Customize row coloring");

        Composite composite = (Composite) super.createDialogArea(parent);
        Composite mainGroup = new Composite(composite, SWT.NONE);
        mainGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
        mainGroup.setLayout(new GridLayout(2, false));

        {
            Group colorsGroup = new Group(mainGroup, SWT.NONE);
            colorsGroup.setText("Colors");
            colorsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
            colorsGroup.setLayout(new GridLayout(1, false));

            colorsViewer = CheckboxTableViewer.newCheckList(colorsGroup, SWT.SINGLE | SWT.BORDER);
            colorsViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

            ToolBar toolbar = new ToolBar(colorsGroup, SWT.FLAT | SWT.HORIZONTAL);
            final ToolItem newButton = new ToolItem(toolbar, SWT.NONE);
            newButton.setText("Add");
            newButton.setImage(DBeaverIcons.getImage(UIIcon.ROW_ADD));
            final ToolItem deleteButton = new ToolItem(toolbar, SWT.NONE);
            deleteButton.setText("Delete");
            deleteButton.setImage(DBeaverIcons.getImage(UIIcon.ROW_DELETE));
        }

        {
            Group settingsGroup = new Group(mainGroup, SWT.NONE);
            settingsGroup.setText("Settings");
            settingsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
            settingsGroup.setLayout(new GridLayout(2, false));
            UIUtils.createLabelText(settingsGroup, "Title", "");
            UIUtils.createLabelCombo(settingsGroup, "Attribute", SWT.READ_ONLY | SWT.DROP_DOWN);
            UIUtils.createLabelCombo(settingsGroup, "Criteria", SWT.READ_ONLY | SWT.DROP_DOWN);
            UIUtils.createLabelText(settingsGroup, "Value", "");
            UIUtils.createControlLabel(settingsGroup, "Foreground");
            new ColorSelector(settingsGroup);
            UIUtils.createControlLabel(settingsGroup, "Background");
            new ColorSelector(settingsGroup);
        }


        return parent;
    }

    private void refreshData() {
        colorsViewer.refresh();
    }

    @Override
    public int open()
    {
        return super.open();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.ABORT_ID, CoreMessages.controls_resultset_filter_button_reset, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        super.buttonPressed(buttonId);
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

}
