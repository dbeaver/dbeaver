/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers.image;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.imageview.ImageEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * CONTENT text editor
 */
public class ImageEditorPart extends EditorPart implements IRefreshablePart {

    private static final Log log = Log.getLog(ImageEditorPart.class);

    private ImageEditor imageViewer;

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);

        //ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public void dispose()
    {
        //ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        imageViewer = new ImageEditor(parent, SWT.NONE);

        loadImage();
    }

    private void loadImage() {
        if (imageViewer == null || imageViewer.isDisposed()) {
            return;
        }
        if (getEditorInput() instanceof IPathEditorInput) {
            try {
                final IPath absolutePath = ((IPathEditorInput)getEditorInput()).getPath();
                File localFile = absolutePath.toFile();
                if (localFile.exists()) {
                    try (InputStream inputStream = new FileInputStream(localFile)) {
                        imageViewer.loadImage(inputStream);
                        imageViewer.update();
                    }
                }
            }
            catch (Exception e) {
                log.error("Can't load image contents", e);
            }
        }
    }

    @Override
    public void setFocus() {
        imageViewer.setFocus();
    }

    @Override
    public String getTitle()
    {
        return "Image";
    }

    @Override
    public Image getTitleImage()
    {
        return DBeaverIcons.getImage(DBIcon.TYPE_IMAGE);
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        refreshImage();
    }

    private void refreshImage() {
        // Refresh editor
        UIUtils.asyncExec(new Runnable() {
            @Override
            public void run() {
                loadImage();
            }
        });
    }

}