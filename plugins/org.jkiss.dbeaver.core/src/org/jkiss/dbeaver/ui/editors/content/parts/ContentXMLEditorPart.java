/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.xml.XMLEditor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * XML editor
 */
public class ContentXMLEditorPart extends XMLEditor implements IContentEditorPart {

    private IEditorPart contentEditor;
    private MimeType mimeType;

    public ContentXMLEditorPart() {
    }

    @Override
    public void initPart(IEditorPart contentEditor, MimeType mimeType)
    {
        this.contentEditor = contentEditor;
        this.mimeType = mimeType;
    }

    @Override
    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    @Override
    public String getContentTypeTitle()
    {
        return "XML";
    }

    @Override
    public Image getContentTypeImage()
    {
        return DBIcon.TYPE_XML.getImage();
    }

    @Override
    public String getPreferredMimeType()
    {
        return MimeTypes.TEXT_XML;
    }

    @Override
    public long getMaxContentLength()
    {
        if (contentEditor instanceof IDataSourceProvider) {
            return ((IDataSourceProvider)contentEditor).getDataSource().getContainer().getPreferenceStore().getInt(PrefConstants.RS_EDIT_MAX_TEXT_SIZE);
        }
        return 10 * 1024 * 1024;
    }

    @Override
    public boolean isPreferredContent()
    {
        try {
            return mimeType.match(MimeTypes.TEXT_XML);
        } catch (MimeTypeParseException e) {
            return false;
        }
    }

    @Override
    public boolean isOptionalContent()
    {
        return true;
    }
}
