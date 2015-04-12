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
