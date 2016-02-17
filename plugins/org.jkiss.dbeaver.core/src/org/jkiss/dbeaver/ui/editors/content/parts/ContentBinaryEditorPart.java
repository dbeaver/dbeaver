/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.editors.binary.BinaryEditor;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorPart;

import javax.activation.MimeType;

/**
 * CONTENT Binary Editor
 */
public class ContentBinaryEditorPart extends BinaryEditor implements ContentEditorPart {

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

    @Nullable
    @Override
    public Control getEditorControl()
    {
        return getManager().getControl();
    }

    @Override
    public String getContentTypeTitle()
    {
        return "Binary";
    }

    @Override
    public DBPImage getContentTypeImage()
    {
        return DBIcon.TYPE_BINARY;
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
