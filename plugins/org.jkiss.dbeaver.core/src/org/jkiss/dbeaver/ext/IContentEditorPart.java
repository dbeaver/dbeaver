/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;

import javax.activation.MimeType;

/**
 * Database content editor
 */
public interface IContentEditorPart extends IEditorPart {

    void initPart(IEditorPart contentEditor, MimeType mimeType);

    IEditorActionBarContributor getActionBarContributor();

    String getContentTypeTitle();

    Image getContentTypeImage();

    String getPreferredMimeType();

    /**
     * Maximum part length. If content length is more than this value then this part will be committed.
     * @return max length
     */
    long getMaxContentLength();

    /**
     * Preferred content part will be set as default part in content editor.
     * @return true or false
     */
    boolean isPreferredContent();

    boolean isOptionalContent();
}
