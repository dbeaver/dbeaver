/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.imageview.ImageViewControl;

import java.io.InputStream;

/**
 * LOB text editor
 */
public class ContentImageEditorPart extends EditorPart implements IContentEditorPart {

    static Log log = LogFactory.getLog(ContentImageEditorPart.class);

    private ImageViewControl imageViewer;
    private boolean contentValid;

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void createPartControl(Composite parent) {
        imageViewer = new ImageViewControl(parent, SWT.NONE);

        IEditorInput input = getEditorInput();
        if (input instanceof IStorageEditorInput) {
            try {
                InputStream inputStream = ((IStorageEditorInput) input).getStorage().getContents();
                if (inputStream != null) {
                    try {
                        contentValid = imageViewer.loadImage(inputStream);
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

    public void setFocus() {
        imageViewer.setFocus();
    }

    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    public String getContentTypeTitle()
    {
        return "Image";
    }

    public Image getContentTypeImage()
    {
        return DBIcon.IMAGE.getImage();
    }

    public String getPreferedMimeType()
    {
        return "image";
    }

    public long getMaxContentLength()
    {
        return 10 * 1024 * 1024;
    }

    public boolean isContentValid()
    {
        return contentValid;
    }
}