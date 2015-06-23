/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorPart;
import org.jkiss.dbeaver.ui.editors.xml.XMLEditor;
import org.jkiss.dbeaver.utils.MimeTypes;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * XML editor
 */
public class ContentXMLEditorPart extends XMLEditor implements ContentEditorPart {

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
    public DBPImage getContentTypeImage()
    {
        return DBIcon.TYPE_XML;
    }

    @Override
    public String getPreferredMimeType()
    {
        return MimeTypes.TEXT_XML;
    }

    @Override
    public long getMaxContentLength()
    {
        if (contentEditor instanceof DBPContextProvider) {
            DBCExecutionContext context = ((DBPContextProvider) contentEditor).getExecutionContext();
            if (context != null) {
                return context.getDataSource().getContainer().getPreferenceStore().getInt(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE);
            }
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
