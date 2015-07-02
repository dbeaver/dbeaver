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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.w3c.dom.Document;

/**
 * XML document
 */
public class DBDDocumentXML implements DBDDocument {

    private Document document;

    @Nullable
    @Override
    public Object getDocumentProperty(String name) {
        if (PROP_ID.equals(name)) {
            return document.getDocumentURI();
        }
        return null;
    }

    @NotNull
    @Override
    public Object getRootNode() {
        return null;//new DocumentNode(document.getDocumentElement());
    }

    @Override
    public Object getRawValue() {
        return document;
    }

    @Override
    public boolean isNull() {
        return document == null;
    }

    @Override
    public void release() {
        document = null;
    }
}
