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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Document.
 * Document is a set of hierarchically organized nodes - similarly to JSON.
 * Each node can be a map, a list (collection) or a value (anything else).
 * Map key is always a string, value is a node.
 * List item is a value.
 */
public interface DBDDocument extends DBDValue {

    String PROP_ID = "id";
    String PROP_TITLE = "title";
    String PROP_CREATE_TIME = "createTime";

    /**
     * Document property
     * @param name    property name
     * @return property value
     */
    @Nullable
    Object getDocumentProperty(String name);

    /**
     * Document content type (mime type).
     * @return content type
     */
    @NotNull
    String getDocumentContentType();

    /**
     * Root node of document
     * @return root node
     */
    @NotNull
    Object getRootNode();

    /**
     * Serializes document into stream
     *
     * @param monitor   progress monitor
     * @param stream    stream
     * @param encoding  stream encoding
     * @throws DBException
     */
    void serializeDocument(@NotNull DBRProgressMonitor monitor, @NotNull OutputStream stream, @Nullable String encoding)
        throws DBException;

    /**
     * Updates document from stream
     *
     * @param monitor   progress monitor
     * @param stream    stream
     * @param encoding  stream encoding
     * @throws DBException
     */
    void updateDocument(@NotNull DBRProgressMonitor monitor, @NotNull InputStream stream, @Nullable String encoding)
        throws DBException;

}