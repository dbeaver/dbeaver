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

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * DDL format
 */
public enum OracleDDLFormat {

    FULL("Full DDL", true, true, true),
    NO_STORAGE("No storage information", false, true, true),
    COMPACT("Compact form", false, false, false);

    private final String title;
    private final boolean showStorage;
    private final boolean showSegments;
    private final boolean showTablespace;

    private OracleDDLFormat(String title, boolean showStorage, boolean showSegments, boolean showTablespace)
    {
        this.showTablespace = showTablespace;
        this.showSegments = showSegments;
        this.showStorage = showStorage;
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public boolean isShowStorage()
    {
        return showStorage;
    }

    public boolean isShowSegments()
    {
        return showSegments;
    }

    public boolean isShowTablespace()
    {
        return showTablespace;
    }
}
