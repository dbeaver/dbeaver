/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.binary.BinaryEditor;

import javax.activation.MimeType;

/**
 * LOB Binary Editor
 */
public class ContentBinaryEditorPart extends BinaryEditor implements IContentEditorPart {

    public ContentBinaryEditorPart()
    {
    }

    public void initPart(IEditorPart contentEditor, MimeType mimeType)
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
        return DBIcon.TYPE_BINARY.getImage();
    }

    public String getPreferredMimeType()
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
    public boolean isPreferredContent()
    {
        return false;
    }

    public boolean isOptionalContent()
    {
        return false;
    }

}
