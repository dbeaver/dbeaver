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
     * Return source raw document (e.g. {@link org.w3c.dom.Document})
     * @return raw document
     */
    @NotNull
    Object getRawDocument();

    /**
     * document property
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