/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

    @Override
    public void initPart(IEditorPart contentEditor, MimeType mimeType)
    {
    }

    @Override
    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    @Override
    public String getContentTypeTitle()
    {
        return "Binary";
    }

    @Override
    public Image getContentTypeImage()
    {
        return DBIcon.TYPE_BINARY.getImage();
    }

    @Override
    public String getPreferredMimeType()
    {
        return "application";
    }

    @Override
    public long getMaxContentLength()
    {
        return Long.MAX_VALUE;
    }

    /**
     * Any content is valid for binary editor so always returns true
     * @return
     */
    @Override
    public boolean isPreferredContent()
    {
        return false;
    }

    @Override
    public boolean isOptionalContent()
    {
        return false;
    }

}
