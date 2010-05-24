/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.ui.controls.imageview.ImageViewControl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

/**
 * LOB text editor
 */
public class LOBImageEditor extends EditorPart {

    static Log log = LogFactory.getLog(LOBImageEditor.class);

    private ImageViewControl imageViewer;

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
                        imageViewer.loadImage(inputStream);
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

}