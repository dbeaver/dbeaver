/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.ai.popup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;

public class GPTSuggestionPopup extends AbstractPopupPanel {
    private Text inputField;
    private String inputText;

    public GPTSuggestionPopup(@NotNull Shell parentShell, @NotNull String title) {
        super(parentShell, title);
        setModeless(true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite placeholder = super.createDialogArea(parent);
        inputField = new Text(placeholder, SWT.BORDER | SWT.MULTI);
        //inputField.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(placeholder.getFont()) * 10;
        gd.widthHint = UIUtils.getFontHeight(placeholder.getFont()) * 40;
        inputField.setLayoutData(gd);

        inputField.addModifyListener(e -> inputText = inputField.getText());
        inputField.addKeyListener(KeyListener.keyReleasedAdapter(keyEvent -> {
            if (keyEvent.keyCode == SWT.CR) {
                okPressed();
            }
        }));

        return placeholder;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        // No buttons
    }

    @Override
    protected boolean isModeless() {
        return true;
    }

    public String getInputText() {
        return inputText;
    }
}
