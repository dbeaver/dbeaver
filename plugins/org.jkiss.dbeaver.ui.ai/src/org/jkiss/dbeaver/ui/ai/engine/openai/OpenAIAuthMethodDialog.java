/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.engine.openai;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;

class OpenAIAuthMethodDialog extends Dialog {
    enum Method {
        BROWSER,
        DEVICE_CODE
    }

    private Method selectedMethod = Method.BROWSER;

    OpenAIAuthMethodDialog(@NotNull Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        getShell().setText(AIUIMessages.openai_configurator_sign_in_dialog_title);

        Label promptLabel = new Label(composite, SWT.WRAP);
        promptLabel.setText(AIUIMessages.openai_configurator_sign_in_dialog_prompt);
        promptLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(460, SWT.DEFAULT).create());

        Composite optionsComposite = new Composite(composite, SWT.NO_RADIO_GROUP);
        optionsComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        GridLayoutFactory.fillDefaults().spacing(0, 6).applyTo(optionsComposite);

        Button browserButton = new Button(optionsComposite, SWT.RADIO);
        browserButton.setText(AIUIMessages.openai_configurator_sign_in_browser);
        browserButton.setSelection(true);

        Label browserDescriptionLabel = new Label(optionsComposite, SWT.WRAP);
        browserDescriptionLabel.setText(AIUIMessages.openai_configurator_sign_in_browser_description);
        browserDescriptionLabel.setLayoutData(
            GridDataFactory.fillDefaults().grab(true, false).indent(22, 0).hint(438, SWT.DEFAULT).create()
        );

        Button deviceCodeButton = new Button(optionsComposite, SWT.RADIO);
        deviceCodeButton.setText(AIUIMessages.openai_configurator_sign_in_device_code);

        Label deviceCodeDescriptionLabel = new Label(optionsComposite, SWT.WRAP);
        deviceCodeDescriptionLabel.setText(AIUIMessages.openai_configurator_sign_in_device_code_description);
        deviceCodeDescriptionLabel.setLayoutData(
            GridDataFactory.fillDefaults().grab(true, false).indent(22, 0).hint(438, SWT.DEFAULT).create()
        );

        browserButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            if (browserButton.getSelection()) {
                deviceCodeButton.setSelection(false);
                selectedMethod = Method.BROWSER;
            }
        }));
        deviceCodeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            if (deviceCodeButton.getSelection()) {
                browserButton.setSelection(false);
                selectedMethod = Method.DEVICE_CODE;
            }
        }));
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(
            parent,
            IDialogConstants.OK_ID,
            AIUIMessages.openai_configurator_sign_in_continue,
            true
        );
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @NotNull
    Method getSelectedMethod() {
        return selectedMethod;
    }
}
