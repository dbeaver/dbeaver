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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.client.GPTAPIClient;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.Optional;
import java.util.Stack;

public class GPTSuggestionPopup extends AbstractPopupPanel {
    private static final Log log = Log.getLog(GPTSuggestionPopup.class);

    private final SQLEditor editorContext;
    private Text resultText;
    private boolean loading = false;

    public GPTSuggestionPopup(@NotNull Shell parentShell, @NotNull String title, @NotNull SQLEditor editor) {
        super(parentShell, title);
        editorContext = editor;
        setModeless(true);

    }

    private boolean isLoading() {
        return loading;
    }



    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Text inputField = new Text(placeholder, SWT.NONE);
        inputField.addModifyListener(new ModifyListener() {
            // We want only the latest modify event
            private Job activeJob;
            @Override
            public void modifyText(ModifyEvent e) {
                if (activeJob != null) {
                    activeJob.cancel();
                }
                activeJob = new Job("Processing gpt request") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        // We want to have a delayed modify response to avoid sending too many requests
                        try {
                            DBCExecutionContext executionContext = editorContext.getExecutionContext();
                            DBSObject object;
                            if (executionContext == null
                                || executionContext.getContextDefaults() == null
                                || executionContext.getContextDefaults().getDefaultSchema() != null) {
                                object = null;
                            } else {
                                object = executionContext.getContextDefaults().getDefaultSchema();
                            }
                            Optional<String> completion = GPTAPIClient.requestCompletion(inputField.getText(), monitor,
                                object);
                            if (completion.isPresent()) {
                                resultText.setText(completion.get());
                            } else {
                                resultText.setText("");
                            }
                            return Status.OK_STATUS;
                        } catch (DBException ex) {
                            log.error(ex);
                            return Status.error(ex.getMessage());
                        }
                    }
                };
                activeJob.schedule();
            }
        });
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
            }
        });
        resultText = new Text(placeholder, SWT.READ_ONLY | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(placeholder.getFont()) * 12;
        gd.widthHint = UIUtils.getFontHeight(placeholder.getFont()) * 12;
        resultText.setLayoutData(gd);

        return super.createDialogArea(parent);
    }



    @Override
    protected Control createButtonBar(Composite parent) {
        return super.createButtonBar(parent);
    }

    @Override
    protected boolean isShowTitle() {
        return false;
    }


    @Override
    protected boolean isModeless() {
        return true;
    }
}
