/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.ui.eula;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class EULAConfirmationDialog extends BaseDialog {
    private final String eula;
    private static final String EULA_ALREADY_CONFIRMED = "eula.confirmed";
    private Text eulaText;

    public EULAConfirmationDialog(Shell parentShell, String eula) {
        super(parentShell, EULAMessages.core_eula_dialog_title, DBIcon.TREE_INFO);
        this.eula = eula;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Font dialogFont = JFaceResources.getDialogFont();
        FontData[] fontData = dialogFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            FontData fd = fontData[i];
            fontData[i] = new FontData(fd.getName(), fd.getHeight() + 1, SWT.NONE);
        }
        Font largeFont = new Font(dialogFont.getDevice(), fontData);

        parent.addDisposeListener(e -> largeFont.dispose());
        return EULAUtils.createEulaText(super.createDialogArea(parent), eula);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.YES_ID, EULAMessages.core_eula_dialog_accept, false);
        createButton(parent, IDialogConstants.NO_ID, IDialogConstants.CANCEL_LABEL, false);
    }


    @Override
    protected boolean canHandleShellCloseEvent() {
        //We don't want user to close this window
        return false;
    }


    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
            case IDialogConstants.NO_ID:
                System.exit(101);
            case IDialogConstants.YES_ID:
                DBWorkbench.getPlatform().getPreferenceStore().setValue(EULA_ALREADY_CONFIRMED, true);
                close();
        }
        super.buttonPressed(buttonId);
    }

}
