/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.content.parts;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.imageview.ImageEditor;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorPart;
import org.jkiss.dbeaver.utils.ContentUtils;

import javax.activation.MimeType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * LOB text editor
 */
public class ContentImageEditorPart extends EditorPart implements ContentEditorPart, IResourceChangeListener {

    static final Log log = Log.getLog(ContentImageEditorPart.class);

    private ImageEditor imageViewer;
    private boolean contentValid;

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

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public void dispose()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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
                    InputStream inputStream = new FileInputStream(localFile);
                    try {
                        contentValid = imageViewer.loadImage(inputStream);
                        imageViewer.update();
                    }
                    finally {
                        inputStream.close();
                    }
                }
            }
            catch (Exception e) {
                log.error("Could not load image contents", e);
            }
        }
    }

    @Override
    public void setFocus() {
        imageViewer.setFocus();
    }

    @Override
    public void initPart(IEditorPart contentEditor, MimeType mimeType)
    {
    }

    @Override
    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    @NotNull
    @Override
    public Control getEditorControl()
    {
        return imageViewer;
    }

    @Override
    public String getContentTypeTitle()
    {
        return "Image";
    }

    @Override
    public Image getContentTypeImage()
    {
        return DBIcon.TYPE_IMAGE.getImage();
    }

    @Override
    public String getPreferredMimeType()
    {
        return "image";
    }

    @Override
    public long getMaxContentLength()
    {
        return 20 * 1024 * 1024;
    }

    @Override
    public boolean isPreferredContent()
    {
        return contentValid;
    }

    @Override
    public boolean isOptionalContent()
    {
        return true;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }
        IEditorInput input = getEditorInput();
        IPath localPath = null;
        if (input instanceof IPathEditorInput) {
            localPath = ((IPathEditorInput) input).getPath();
        }
        if (localPath == null) {
            return;
        }
        localPath = ContentUtils.convertPathToWorkspacePath(localPath);
        delta = delta.findMember(localPath);
        if (delta == null) {
            return;
        }
        if (delta.getKind() == IResourceDelta.CHANGED) {
            // Refresh editor
            getSite().getShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    loadImage();
                }
            });
        }
    }

}