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
package org.jkiss.dbeaver.ui.gis.panel;

import org.cts.crs.CoordinateReferenceSystem;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.model.gis.GisTransformUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Database select dialog
 */
public class SelectSRIDDialog extends BaseDialog {

    private static final Log log = Log.getLog(SelectSRIDDialog.class);

    private static final String DIALOG_ID = "DBeaver.SelectSRIDDialog";//$NON-NLS-1$
    private static final int MANAGE_BUTTON_ID = 1000;

    private int selectedSRID;
    private Combo sridCombo;
    private List<Integer> allSupportedCodes;
    private Text crsNameText;
    private Button detailsButton;

    public SelectSRIDDialog(Shell shell, int defCRS) {
        super(shell, GISMessages.panel_select_srid_dialog_title, null);
        selectedSRID = defCRS;
        allSupportedCodes = GisTransformUtils.getSortedEPSGCodes();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return null;//UIUtils.getSettingsSection(UIActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        Group crsGroup = UIUtils.createControlGroup(dialogArea, "CRS", 2, SWT.NONE, 0);
        crsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        sridCombo = UIUtils.createLabelCombo(crsGroup, GISMessages.panel_select_srid_dialog_label_combo_source_srid, GISMessages.panel_select_srid_dialog_label_combo_tooltip_source_crs, SWT.BORDER | SWT.DROP_DOWN);
        sridCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        sridCombo.setItems();
        String[] items = new String[allSupportedCodes.size()];
        for (int i = 0; i < allSupportedCodes.size(); i++) {
            Integer code = allSupportedCodes.get(i);
            String strCode = String.valueOf(code);
            items[i] = strCode;
        }
        sridCombo.setItems(items);
        if (selectedSRID != 0) {
            sridCombo.setText(String.valueOf(selectedSRID));
        }
        sridCombo.addModifyListener(e -> {
            int newSRID = CommonUtils.toInt(sridCombo.getText());
            setSelectedSRID(newSRID);
            updateButtons();
        });

        crsNameText = UIUtils.createLabelText(crsGroup, GISMessages.panel_select_srid_dialog_title_label_text_name, "", SWT.BORDER | SWT.READ_ONLY);
        crsNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createEmptyLabel(crsGroup, 1, 1);
        detailsButton = UIUtils.createPushButton(crsGroup, GISMessages.panel_select_srid_dialog_button_label_details, null);
        detailsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        detailsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ShowSRIDDialog showSRIDDialog = new ShowSRIDDialog(getShell(), getSelectedSRID());
                showSRIDDialog.open();
            }
        });

        setSelectedSRID(selectedSRID);

        return dialogArea;
    }

    private void setSelectedSRID(int newSRID) {
        if (allSupportedCodes.contains(newSRID)) {
            selectedSRID = newSRID;
            try {
                CoordinateReferenceSystem crs = GisTransformUtils.getCRSFactory().getCRS(GisConstants.GIS_REG_EPSG + ":" + selectedSRID);
                crsNameText.setText(CommonUtils.notEmpty(crs.getName()) +  " (" + crs.getCoordinateSystem() + ")");
                detailsButton.setEnabled(true);
            } catch (Throwable e) {
                DBWorkbench.getPlatformUI().showError("Bad CRS", "Error reading CRS info", e);
            }
        } else {
            selectedSRID = 0;
            crsNameText.setText("N/A");
            detailsButton.setEnabled(false);
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        updateButtons();
        return contents;
    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(selectedSRID != 0);
    }

    public int getSelectedSRID() {
        return selectedSRID;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, MANAGE_BUTTON_ID, GISMessages.panel_select_srid_dialog_button_label_manage, false);
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == MANAGE_BUTTON_ID) {
            ManageCRSDialog dialog = new ManageCRSDialog(getShell(), selectedSRID);
            if (dialog.open() == IDialogConstants.OK_ID) {
                int newSRID = dialog.getSelectedSRID();
                if (newSRID != 0) {
                    sridCombo.setText(String.valueOf(newSRID));
                    setSelectedSRID(newSRID);
                }
            }
        } else {
            super.buttonPressed(buttonId);
        }
    }
}