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

/**
 * Document.
 * Document is a set of hierarchically organized nodes - similarly to JSON.
 * Each node can be a map, a list (collection) or a value (anything else).
 * Map key is always a string, value is a node.
 * List item is a value.
 */
public interface DBDDocument extends DBDValue {

    public static final String PROP_ID = "id";
    public static final String PROP_TITLE = "title";
    public static final String PROP_CREATE_TIME = "createTime";

    /**
     * Document property
     * @param name    property name
     * @return property value
     */
    @Nullable
    Object getDocumentProperty(String name);

    /**
     * Root node of document
     * @return root node
     */
    @NotNull
    Object getRootNode();
}