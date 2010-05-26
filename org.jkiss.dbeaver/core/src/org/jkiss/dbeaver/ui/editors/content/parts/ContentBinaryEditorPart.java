/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.jkiss.dbeaver.ui.editors.binary.BinaryEditor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.swt.graphics.Image;

/**
 * LOB Binary Editor
 */
public class ContentBinaryEditorPart extends BinaryEditor implements IContentEditorPart {

    public ContentBinaryEditorPart()
    {
    }

    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    public String getContentTypeTitle()
    {
        return "Binary";
    }

    public Image getContentTypeImage()
    {
        return DBIcon.HEX.getImage();
    }

    public String getPreferedMimeType()
    {
        return "application";
    }

    public long getMaxContentLength()
    {
        return Long.MAX_VALUE;
    }

    /**
     * Any content is valid for binary editor so always returns true
     * @return
     */
    public boolean isContentValid()
    {
        return true;
    }

}
