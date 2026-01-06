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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;

import java.util.List;

/**
 * SelectContentPartDialog
 *
 * @author Serge Rider
 */
class SelectContentPartDialog extends Dialog {

    private List<IEditorPart> dirtyParts;
    private IEditorPart selectedPart;

    private SelectContentPartDialog(Shell parentShell, List<IEditorPart> dirtyParts)
    {
        super(parentShell);
        this.dirtyParts = dirtyParts;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Choose content editor");

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        Label infoLabel = new Label(group, SWT.NONE);
        infoLabel.setText("Content was modified in multiple editors. Choose correct one:");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        infoLabel.setLayoutData(gd);

        final Combo combo = new Combo(group, SWT.READ_ONLY | SWT.DROP_DOWN);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        combo.setLayoutData(gd);
        combo.add("");
        for (IEditorPart part : dirtyParts) {
            combo.add(part.getTitle());
        }
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (combo.getSelectionIndex() >= 1) {
                    selectedPart = dirtyParts.get(combo.getSelectionIndex() - 1);
                } else {
                    selectedPart = null;
                }
                getButton(IDialogConstants.OK_ID).setEnabled(selectedPart != null);
            }
        });

        return group;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        return ctl;
    }

    public IEditorPart getSelectedPart()
    {
        return selectedPart;
    }

    public static IEditorPart selectContentPart(Shell parentShell, List<IEditorPart> dirtyParts)
    {
        SelectContentPartDialog scDialog = new SelectContentPartDialog(parentShell, dirtyParts);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedPart();
        } else {
            return null;
        }
    }
}