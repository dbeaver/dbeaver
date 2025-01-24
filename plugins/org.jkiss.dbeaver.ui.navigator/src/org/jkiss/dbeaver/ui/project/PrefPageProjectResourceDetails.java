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
package org.jkiss.dbeaver.ui.project;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.project.FileSystemExplorerView;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectResourceDetails extends AbstractPrefPage implements IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.resourceDetails"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectResourceDetails.class);

    private IAdaptable element;
    private Path resourcePath;

    public PrefPageProjectResourceDetails() {
        setDescription("Resource information details");
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (resourcePath == null) {
            return composite;
        }
        UIUtils.createLabelText(composite, "URI", resourcePath.toUri().toString(), SWT.BORDER | SWT.READ_ONLY);
        FileAttributeView fileAttributeView = Files.getFileAttributeView(resourcePath, PosixFileAttributeView.class);
        if (fileAttributeView == null) {
            fileAttributeView = Files.getFileAttributeView(resourcePath, FileOwnerAttributeView.class);
        }
        if (fileAttributeView == null) {
            fileAttributeView = Files.getFileAttributeView(resourcePath, BasicFileAttributeView.class);
        }

        if (fileAttributeView instanceof BasicFileAttributeView basicAttrsView) {
            try {
                BasicFileAttributes attributes = basicAttrsView.readAttributes();

                if (attributes.isDirectory()) {
                    UIUtils.createCheckbox(composite, "Directory", null, attributes.isDirectory(), 2).setEnabled(false);
                } else {
                    UIUtils.createLabelText(composite, "File size", FileSystemExplorerView.FILE_SIZE_FORMAT.format(attributes.size()), SWT.BORDER | SWT.READ_ONLY);
                }
                UIUtils.createLabelText(composite, "Creation time",
                    FileSystemExplorerView.FILE_TIMESTAMP_FORMAT.format(attributes.creationTime().toMillis()), SWT.BORDER | SWT.READ_ONLY);
                UIUtils.createLabelText(composite, "Last modified time",
                    FileSystemExplorerView.FILE_TIMESTAMP_FORMAT.format(attributes.lastModifiedTime().toMillis()), SWT.BORDER | SWT.READ_ONLY);
                Object fileKey = attributes.fileKey();
                if (fileKey != null) {
                    UIUtils.createLabelText(composite, "File key", CommonUtils.toString(fileKey), SWT.BORDER | SWT.READ_ONLY);
                }

                if (attributes instanceof PosixFileAttributes posixAttributes) {
                    UserPrincipal owner = posixAttributes.owner();
                    if (owner != null) {
                        UIUtils.createLabelText(composite, "File owner", CommonUtils.toString(owner.getName()), SWT.BORDER | SWT.READ_ONLY);
                    }
                    GroupPrincipal group = posixAttributes.group();
                    if (group != null) {
                        UIUtils.createLabelText(composite, "File group", CommonUtils.toString(group.getName()), SWT.BORDER | SWT.READ_ONLY);
                    }

                    Set<PosixFilePermission> permissions = posixAttributes.permissions();
                    if (!CommonUtils.isEmpty(permissions)) {
                        UIUtils.createLabelText(composite, "Permissions",
                            permissions.stream().map(Enum::name).collect(Collectors.joining(",")), SWT.BORDER | SWT.READ_ONLY);
                    }
                }

            } catch (IOException e) {
                DBWorkbench.getPlatformUI().showError("Error reading file attributes", null, e);
            }
        }

        return composite;
    }

    @Override
    public IAdaptable getElement() {
        return element;
    }

    @Override
    public void setElement(IAdaptable element) {
        this.element = element;
        this.resourcePath = DBUtils.getAdapter(Path.class, element);
    }

}
