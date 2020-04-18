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

import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.gis.GisTransformUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;

/**
 * SRID details dialog
 */
public class ShowSRIDDialog extends BaseDialog {

    private static final Log log = Log.getLog(ShowSRIDDialog.class);

    private static final String DIALOG_ID = "DBeaver.ShowSRIDDialog";//$NON-NLS-1$

    private int selectedSRID;

    public ShowSRIDDialog(Shell shell, int defCRS) {
        super(shell, GISMessages.panel_show_srid_dialog_title_select, null);
        selectedSRID = defCRS;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return null;//UIUtils.getSettingsSection(UIActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);
        try {
            CoordinateReferenceSystem crs = GisTransformUtils.getCRSFactory().getCRS("EPSG:" + selectedSRID);

            Group crsGroup = UIUtils.createControlGroup(dialogArea, GISMessages.panel_show_srid_dialog_control_group_label_details, 2, SWT.NONE, 0);
            crsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createLabelText(crsGroup, "SRID", String.valueOf(selectedSRID), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(crsGroup, GISMessages.panel_show_srid_dialog_label_text_name, crs.getName(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(crsGroup, GISMessages.panel_show_srid_dialog_label_text_coordinate, crs.getCoordinateSystem().toString(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(crsGroup, GISMessages.panel_show_srid_dialog_label_text_projection, crs.getProjection() == null ? "N/A" : crs.getProjection().toString(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(crsGroup, GISMessages.panel_show_srid_dialog_label_text_type, crs.getType() == null ? "N/A" : crs.getType().toString(), SWT.BORDER | SWT.READ_ONLY);

            Text wktText = UIUtils.createLabelText(crsGroup, "WKT", crs.toWKT(), SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = UIUtils.getFontHeight(wktText) * 15;
            gd.widthHint = UIUtils.getFontHeight(wktText) * 40;
            wktText.setLayoutData(gd);
        } catch (CRSException e) {
            DBWorkbench.getPlatformUI().showError("CRS error", "Error reading SRID " + selectedSRID + " details", e);
        }

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

}