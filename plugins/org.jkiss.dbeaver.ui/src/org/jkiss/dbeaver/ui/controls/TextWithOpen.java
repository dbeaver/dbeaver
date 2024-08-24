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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
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
public class TextWithOpen extends Composite {
    private static final Log log = Log.getLog(TextWithOpen.class);

    private final Text text;
    private final ToolBar toolbar;
    private final boolean multiFS;
    private final boolean binary;

    public TextWithOpen(Composite parent, boolean multiFS) {
        this(parent, multiFS, false);
    }
    public TextWithOpen(Composite parent, boolean multiFS, boolean secured) {
        this(parent, multiFS, secured, false);
    }
    public TextWithOpen(Composite parent, boolean multiFS, boolean secured, boolean binary) {
        super(parent, SWT.NONE);
        this.multiFS = multiFS;
        this.binary = binary;
        final GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        setLayout(gl);

        boolean useTextEditor = isShowFileContentEditor();
        text = new Text(this, SWT.BORDER | ((useTextEditor && !secured) ? SWT.MULTI | SWT.V_SCROLL : SWT.SINGLE));
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

        toolbar = new ToolBar(this, SWT.FLAT);
        if (useTextEditor) {
            final ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
            toolItem.setImage(DBeaverIcons.getImage(UIIcon.TEXTFIELD));
            toolItem.setToolTipText(secured ? UIMessages.text_with_open_dialog_set_text : UIMessages.text_with_open_dialog_edit_text);
            toolItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String newText = EditTextDialog.editText(
                        getShell(),
                        secured ? UIMessages.text_with_open_dialog_set_text : UIMessages.text_with_open_dialog_edit_text,
                        secured ? "" : getText()
                    );
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
                final ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
                toolItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
                toolItem.setToolTipText(UIMessages.text_with_open_dialog_browse);
                toolItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        openBrowser(false);
                    }
                });
            }
            if (isMultiFileSystem()) {
                final ToolItem remoteFsItem = new ToolItem(toolbar, SWT.NONE);
                remoteFsItem.setImage(DBeaverIcons.getImage((getStyle() & SWT.OPEN) != 0 ? UIIcon.OPEN_EXTERNAL : UIIcon.SAVE_EXTERNAL));
                remoteFsItem.setToolTipText(UIMessages.text_with_open_dialog_browse_remote);
                remoteFsItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        openBrowser(true);
                    }
                });
            }
        }

        if (!useTextEditor && !isBinaryContents()) {
            // Open file text in embedded editor
            final ToolItem editItem = new ToolItem(toolbar, SWT.NONE);
            editItem.setImage(DBeaverIcons.getImage(UIIcon.EDIT));
            editItem.setToolTipText(UIMessages.text_with_open_dialog_edit_file);
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

    public ToolBar getToolbar() {
        return toolbar;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        toolbar.setEnabled(enabled);
        text.setEnabled(enabled);
    }

}