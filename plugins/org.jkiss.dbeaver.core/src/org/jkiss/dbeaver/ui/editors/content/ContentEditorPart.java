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

package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.ISingleControlEditor;

import javax.activation.MimeType;

/**
 * Database content editor
 */
public interface ContentEditorPart extends IEditorPart, ISingleControlEditor {

    void initPart(IEditorPart contentEditor, @Nullable MimeType mimeType);

    IEditorActionBarContributor getActionBarContributor();

    String getContentTypeTitle();

    DBPImage getContentTypeImage();

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
