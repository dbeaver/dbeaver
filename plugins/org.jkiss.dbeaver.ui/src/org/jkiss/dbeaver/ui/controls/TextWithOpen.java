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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ide.IDE;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TextWithOpen
 */
public class TextWithOpen {
    private static final Log log = Log.getLog(TextWithOpen.class);

    protected final Composite panel;
    private final Text text;
    private final Composite toolbar;
    private final boolean multiFS;
    private final boolean binary;

    public TextWithOpen(Composite parent, boolean multiFS) {
        this(parent, multiFS, false);
    }
    public TextWithOpen(Composite parent, boolean multiFS, boolean secured) {
        this(parent, multiFS, secured, false);
    }
    public TextWithOpen(Composite parent, boolean multiFS, boolean secured, boolean binary) {
        panel = new Composite(parent, SWT.NONE);
        this.multiFS = multiFS;
        this.binary = binary;
        final GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        panel.setLayout(gl);

        boolean useTextEditor = isShowFileContentEditor();
        text = new Text(panel, SWT.BORDER | ((useTextEditor && !secured) ? SWT.MULTI | SWT.V_SCROLL : SWT.SINGLE));
        if (secured) {
            text.setEchoChar('*');
        }
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
        if (useTextEditor) {
            gd.heightHint = text.getLineHeight() * (secured ? 1 : 2);
            gd.widthHint = 300;
        } else {
            // We use width hint to avoid infinite text control expansion if predefined text is too long
            gd.widthHint = 200;
        }
        text.setLayoutData(gd);

        toolbar = new Composite(panel, SWT.NONE);
        RowLayout layout = new RowLayout();
        layout.marginTop = 0;
        layout.marginBottom = 0;
        toolbar.setLayout(layout);
        if (useTextEditor) {
            UIUtils.createPushButton(
                toolbar,
                null,
                secured ? UIMessages.text_with_open_dialog_set_text : UIMessages.text_with_open_dialog_edit_text,
                UIIcon.TEXTFIELD,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String newText = getNewTextFromUser(secured);
                    if (newText != null) {
                        setText(newText);
                    }
                }
            });
        }
        {
            {
                // Local FS works only on local machine. Will not work for TE remote tasks.
                // Do we need to do anything about it in UI?
                UIUtils.createPushButton(
                    toolbar,
                    null,
                    UIMessages.text_with_open_dialog_browse,
                    UIIcon.OPEN,
                    new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        openBrowser(false);
                    }
                });
            }
            if (isMultiFileSystem()) {
                UIUtils.createPushButton(
                    toolbar,
                    null,
                    UIMessages.text_with_open_dialog_browse_remote,
                    (getPanelStyle() & SWT.OPEN) != 0 ? UIIcon.OPEN_EXTERNAL : UIIcon.SAVE_EXTERNAL,
                    new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        openBrowser(true);
                    }
                });
            }
        }

        if (!useTextEditor && !isBinaryContents() && !isFolderContents()) {
            // Open file text in embedded editor
            Button editItem = UIUtils.createPushButton(
                toolbar,
                null,
                UIMessages.text_with_open_dialog_edit_file,
                UIIcon.EDIT,
                new SelectionAdapter() {
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
                }
            );
            TextWithOpen.this.text.addModifyListener(e -> {
                String fileName = TextWithOpen.this.text.getText().trim();
                Path targetFile;
                try {
                    if (!IOUtils.isLocalFile(fileName)) {
                        editItem.setEnabled(false);
                    } else {
                        targetFile = Path.of(fileName);
                        editItem.setEnabled(Files.exists(targetFile) && !Files.isDirectory(targetFile));
                    }
                } catch (Exception ex) {
                    log.debug("Error getting file info: " + ex.getMessage());
                    editItem.setEnabled(false);
                }
            });
            editItem.setEnabled(false);
        }

        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_CENTER);
        toolbar.setLayoutData(gd);
    }

    protected int getPanelStyle() {
        return SWT.OPEN;
    }

    public Composite getPanel() {
        return panel;
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

    protected boolean isFolderContents() {
        return false;
    }

    protected boolean isBinaryContents() {
        return binary;
    }

    public boolean isMultiFileSystem() {
        return multiFS;
    }

    public DBPProject getProject() {
        return null;
    }

    protected void openBrowser(boolean remoteFS) {

    }

    public Text getTextControl() {
        return text;
    }

    public Composite getToolbar() {
        return toolbar;
    }

    public void setEnabled(boolean enabled) {
        panel.setEnabled(enabled);
        toolbar.setEnabled(enabled);
        text.setEnabled(enabled);
    }

    /**
     * Sets panel layout data
     */
    public void setLayoutData(Object data) {
        panel.setLayoutData(data);
    }

    public Shell getShell() {
        return panel.getShell();
    }

    /**
     * Sets tooltip for text and panel
     */
    public void setToolTipText(String toolTip) {
        this.panel.setToolTipText(toolTip);
        this.text.setToolTipText(toolTip);
    }

    public Composite getParent() {
        return panel.getParent();
    }

    @Nullable
    protected String getNewTextFromUser(boolean secured) {
        return EditTextDialog.editText(
            panel.getShell(),
            secured ? UIMessages.text_with_open_dialog_set_text : UIMessages.text_with_open_dialog_edit_text,
            secured ? "" : getText()
        );
    }
}