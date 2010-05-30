/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;

/**
 * Database content editor
 */
public interface IContentEditorPart extends IEditorPart {

    void initPart(IEditorPart contentEditor);

    IEditorActionBarContributor getActionBarContributor();

    String getContentTypeTitle();

    Image getContentTypeImage();

    String getPreferedMimeType();

    long getMaxContentLength();

    boolean isContentValid();

}
