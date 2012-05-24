/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.MimeTypes;

import javax.activation.MimeType;

/**
 * LOB text editor
 */
public class ContentTextEditorPart extends BaseTextEditor implements IContentEditorPart {

    private IEditorPart contentEditor;

    public ContentTextEditorPart() {
        setDocumentProvider(new FileRefDocumentProvider());
    }

    @Override
    public void initPart(IEditorPart contentEditor, MimeType mimeType)
    {
        this.contentEditor = contentEditor;
    }

    @Override
    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    @Override
    public String getContentTypeTitle()
    {
        return "Text";
    }

    @Override
    public Image getContentTypeImage()
    {
        return DBIcon.TYPE_TEXT.getImage();
    }

    @Override
    public String getPreferredMimeType()
    {
        return MimeTypes.TEXT;
    }

    @Override
    public long getMaxContentLength()
    {
        if (contentEditor instanceof IDataSourceProvider) {
            return ((IDataSourceProvider)contentEditor).getDataSource().getContainer().getPreferenceStore().getInt(PrefConstants.RS_EDIT_MAX_TEXT_SIZE);
        }
        return 10 * 1024 * 1024;
    }

    /**
     * Always return false cos' text editor can load any binary content
     * @return false
     */
    @Override
    public boolean isPreferredContent()
    {
        return false;
    }

    @Override
    public boolean isOptionalContent()
    {
        return true;
    }
}
