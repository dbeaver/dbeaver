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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ide.IDE;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TextWithOpen
 */
public class TextWithOpen extends Composite {
    private final Text text;
    private final ToolBar toolbar;

    public TextWithOpen(Composite parent) {
        super(parent, SWT.NONE);
        final GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        setLayout(gl);

        boolean useTextEditor = isShowFileContentEditor();
        text = new Text(this, SWT.BORDER | (useTextEditor ? SWT.MULTI | SWT.V_SCROLL : SWT.SINGLE));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
        if (useTextEditor) {
            gd.heightHint = text.getLineHeight();
        }
        text.setLayoutData(gd);

        toolbar = new ToolBar(this, SWT.FLAT);
        if (useTextEditor) {
            final ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
            toolItem.setImage(DBeaverIcons.getImage(UIIcon.TEXTFIELD));
            toolItem.setToolTipText("Edit text");
            toolItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String newText = EditTextDialog.editText(getShell(), "Edit text", getText());
                    if (newText != null) {
                        setText(newText);
                    }
                }
            });
        }
        {
            final ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
            toolItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
            toolItem.setToolTipText("Browse");
            toolItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openBrowser();
                }
            });
        }

        if (!useTextEditor && !isBinaryContents()) {
            // Open file text in embedded editor
            final ToolItem editItem = new ToolItem(toolbar, SWT.NONE);
            editItem.setImage(DBeaverIcons.getImage(UIIcon.EDIT));
            editItem.setToolTipText("Edit file");
            editItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String filePath = TextWithOpen.this.text.getText();

                    IFileStore store = EFS.getLocalFileSystem().getStore(Path.of(filePath).toUri());
                    try {
                        IDE.openEditorOnFileStore(UIUtils.getActiveWorkbenchWindow().getActivePage(), store);
                    } catch (Exception ex) {
                        DBWorkbench.getPlatformUI().showError("File open error", null, ex);
                    }
                }
            });
            TextWithOpen.this.text.addModifyListener(e -> {
                Path targetFile = Path.of(TextWithOpen.this.text.getText().trim()).toAbsolutePath();
                editItem.setEnabled(Files.exists(targetFile) && !Files.isDirectory(targetFile));
            });
            editItem.setEnabled(false);
        }

        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_CENTER);
        toolbar.setLayoutData(gd);
    }

    public String getText() {
        return text.getText();
    }

    public void setText(String str) {
        text.setText(str);
    }

    protected boolean isShowFileContentEditor() {
        return false;
    }

    protected boolean isBinaryContents() {
        return false;
    }

    protected void openBrowser() {

    }

    public Text getTextControl() {
        return text;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        toolbar.setEnabled(enabled);
        text.setEnabled(enabled);
    }

}