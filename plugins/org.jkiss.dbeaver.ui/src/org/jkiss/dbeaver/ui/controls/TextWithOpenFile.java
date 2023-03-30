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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;

/**
 * TextWithOpen
 */
public class TextWithOpenFile extends TextWithOpen
{
    private final String title;
    private final String[] filterExt;
    private final int style;
    private final boolean binary;
    private boolean openFolder = false;

    public TextWithOpenFile(Composite parent, String title, String[] filterExt, int style, boolean binary) {
        super(parent);
        this.title = title;
        this.filterExt = filterExt;
        this.style = style;
        this.binary = binary;
    }

    public TextWithOpenFile(Composite parent, String title, String[] filterExt) {
        this(parent, title, filterExt, SWT.SINGLE | SWT.OPEN, false);
    }

    public TextWithOpenFile(Composite parent, String title, String[] filterExt, boolean binary) {
        this(parent, title, filterExt, SWT.SINGLE | SWT.OPEN, binary);
    }

    public void setOpenFolder(boolean openFolder) {
        this.openFolder = openFolder;
    }

    @Override
    protected boolean isBinaryContents() {
        return binary;
    }

    protected void openBrowser() {
        String directory = getDialogDirectory();
        String selected;
        if (openFolder) {
            DirectoryDialog fd = new DirectoryDialog(getShell(), style);
            if (directory != null) {
                fd.setFilterPath(directory);
            }
            if (title != null) {
                fd.setText(title);
            }
            selected = fd.open();
        } else {
            FileDialog fd = new FileDialog(getShell(), style);
            fd.setText(title);
            fd.setFilterExtensions(filterExt);
            if (directory != null) {
                DialogUtils.setCurDialogFolder(directory);
            }
            selected = DialogUtils.openFileDialog(fd);

            if (selected != null && isShowFileContentEditor()) {
                Path filePath = Path.of(selected);
                try {
                    if (binary) {
                        byte[] bytes = Files.readAllBytes(filePath);
                        selected = Base64.getEncoder().encodeToString(bytes);
                    } else {
                        selected = Files.readString(filePath);
                    }
                } catch (IOException e) {
                    DBWorkbench.getPlatformUI().showError("File read error", "Can't read file '" + filePath + "' contents", e);
                }
            }
        }
        if (selected != null) {
            setText(selected);
        }
    }

    private String getDialogDirectory() {
        final String text = getText();
        if (CommonUtils.isEmptyTrimmed(text)) {
            return null;
        }
        try {
            String dirPath = IOUtils.getDirectoryPath(text);
            if (CommonUtils.isNotEmpty(dirPath)) {
                return dirPath;
            }
        } catch (InvalidPathException ignored) {
        }
        return null;
    }

}