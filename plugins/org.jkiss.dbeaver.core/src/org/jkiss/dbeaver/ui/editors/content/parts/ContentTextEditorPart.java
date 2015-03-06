/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorPart;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.utils.MimeTypes;

import javax.activation.MimeType;

/**
 * LOB text editor
 */
public class ContentTextEditorPart extends BaseTextEditor implements ContentEditorPart {

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
            return ((IDataSourceProvider)contentEditor).getDataSource().getContainer().getPreferenceStore().getInt(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE);
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
