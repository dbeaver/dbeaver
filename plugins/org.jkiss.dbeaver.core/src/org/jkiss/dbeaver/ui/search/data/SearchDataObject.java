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
package org.jkiss.dbeaver.ui.search.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * Search parameters
 */
public class SearchDataObject implements DBPNamedObject {

    private final DBNNode node;
    private final int foundRows;
    private final DBDDataFilter filter;

    public SearchDataObject(DBNNode node, int foundRows, DBDDataFilter filter) {
        this.node = node;
        this.foundRows = foundRows;
        this.filter = filter;
    }

    @NotNull
    @Property
    @Override
    public String getName() {
        return node.getName();
    }

    @Property(viewable = true)
    public int getFoundRows() {
        return foundRows;
    }

    public DBNNode getNode() {
        return node;
    }

    public DBDDataFilter getFilter() {
        return filter;
    }
}
