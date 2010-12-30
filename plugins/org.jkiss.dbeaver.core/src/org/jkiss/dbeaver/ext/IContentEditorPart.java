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

    /**
     * Maximum part length. If content length is more than this value then this part will be committed.
     * @return
     */
    long getMaxContentLength();

    /**
     * Prefered content part will be set as default part in content editor.
     * @return
     */
    boolean isPreferedContent();

    boolean isOptionalContent();
}
