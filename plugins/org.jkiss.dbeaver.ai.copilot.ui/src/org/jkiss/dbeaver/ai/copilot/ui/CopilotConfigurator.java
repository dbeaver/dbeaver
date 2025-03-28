/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ai.copilot.ui;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.copilot.CopilotClient;
import org.jkiss.dbeaver.model.ai.copilot.CopilotConstants;
import org.jkiss.dbeaver.model.ai.openai.OpenAIModel;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.browser.BrowserPopup;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.sql.ai.openai.OpenAiConfigurator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class CopilotConfigurator extends OpenAiConfigurator {

    private Text accessTokenText;
    private String accessToken;

    @Override
    public void createControl(
        @NotNull Composite parent,
        DAICompletionEngine object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite authorizeComposite = UIUtils.createComposite(parent, 3);
        authorizeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(authorizeComposite);
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createModelParameters(composite);

        createAdditionalSettings(composite);
        UIUtils.syncExec(this::applySettings);
    }

    @Override
    public void saveSettings(@NotNull AIEngineSettings aiSettings) {
        aiSettings.getProperties().put(CopilotConstants.COPILOT_ACCESS_TOKEN, accessToken);

        super.saveSettings(aiSettings);
    }

    @Override
    public void loadSettings(@NotNull AIEngineSettings aiSettings) {
        accessToken = CommonUtils.toString(aiSettings.getProperties().get(CopilotConstants.COPILOT_ACCESS_TOKEN), "");
        accessTokenText.setText(accessToken);
        super.loadSettings(aiSettings);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @NotNull
    @Override
    protected OpenAIModel[] getSupportedGPTModels() {
        return new OpenAIModel[] {
            OpenAIModel.GPT_4,
            OpenAIModel.GPT_TURBO
        };
    }

    @Override
    protected String getDefaultModel() {
        return OpenAIModel.GPT_4.getName();
    }

    @Override
    protected void createConnectionParameters(@NotNull Composite parent) {

        accessTokenText = UIUtils.createLabelText(
            parent,
            CopilotMessages.copilot_access_token,
            "",
            SWT.BORDER | SWT.PASSWORD
        );
        accessTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        accessTokenText.addModifyListener((e -> accessToken = accessTokenText.getText()));
        accessTokenText.setMessage(CopilotMessages.copilot_preference_page_token_info);
        UIUtils.createDialogButton(parent, CopilotMessages.copilot_access_token_authorize, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new AbstractJob("Requesting authorization") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        try (var copilotClient = new CopilotClient()) {
                            CopilotClient.ResponseData responseData = copilotClient.requestAuth(monitor);
                            AtomicReference<BrowserPopup> popupOauth = new AtomicReference<>();
                            UIUtils.asyncExec(() -> {
                                CopyDeviceDialog copyYourCode = new CopyDeviceDialog(
                                    responseData.user_code(),
                                    UIUtils.getActiveShell(),
                                    CopilotMessages.oauth_user_dialog_code_title,

                                    DBIcon.STATUS_INFO
                                );
                                copyYourCode.open();
                                if (ModelPreferences.getPreferences().getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH)) {
                                    try {
                                        popupOauth.set(BrowserPopup.openBrowser("OAuth", new URL(responseData.verification_uri())));
                                    } catch (MalformedURLException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                } else {
                                    ShellUtils.launchProgram(responseData.verification_uri());
                                }
                            });
                            monitor.subTask("Requesting access token");
                            String token = copilotClient.requestAccessToken(responseData.device_code(), monitor);
                            if (token != null) {
                                CopilotConfigurator.this.accessToken = token;
                            }
                            UIUtils.syncExec(() -> {
                                UIUtils.showMessageBox(
                                    UIUtils.getActiveShell(),
                                    CopilotMessages.oauth_success_title,
                                    CopilotMessages.oauth_success_message,
                                    SWT.ICON_INFORMATION
                                );
                                if (popupOauth.get() != null) {
                                    popupOauth.get().close();
                                }
                            });

                            UIUtils.syncExec(() -> {
                                if (accessTokenText != null && !accessTokenText.isDisposed()) {
                                    accessTokenText.setText(accessToken);
                                }
                            });
                        } catch (TimeoutException | DBException ex) {
                            return GeneralUtils.makeErrorStatus("Error during authorization", ex);
                        } catch (InterruptedException ex) {
                            return Status.OK_STATUS;
                        }

                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        });
        createURLInfoLink(parent);
    }

    @Override
    protected void createURLInfoLink(@NotNull Composite parent) {
        Link link = UIUtils.createLink(
            parent,
            NLS.bind(CopilotMessages.copilot_preference_page_token_info, getApiKeyURL()),
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.openWebBrowser(getApiKeyURL());
                }
            }
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        link.setLayoutData(gd);
    }

    private static class CopyDeviceDialog extends BaseDialog {

        private final String userCode;

        public CopyDeviceDialog(
            String userCode,
            Shell parentShell,
            String title,
            @Nullable DBIcon icon
        ) {
            super(parentShell, title, icon);
            this.userCode = userCode;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            Text text = new Text(dialogArea, SWT.NONE);
            text.setText(NLS.bind(CopilotMessages.oauth_code_request_message, userCode));
            return dialogArea;
        }

        @Override
        protected void createButtonsForButtonBar(@NotNull Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, CopilotMessages.gpt_preference_page_advanced_copilot_copy_button, true);
        }

        @Override
        protected void okPressed() {
            UIUtils.setClipboardContents(UIUtils.getDisplay(), TextTransfer.getInstance(), userCode);
            close();
        }
    }
}
