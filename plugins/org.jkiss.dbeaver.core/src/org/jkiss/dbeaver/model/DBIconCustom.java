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

package org.jkiss.dbeaver.model;

/**
 * Image with overlays
 */
public class DBIconCustom implements DBPImage
{
    private final DBPImage main;
    private final boolean disabled;
    private final DBPImage topLeft, topRight, bottomLeft, bottomRight;

    public DBIconCustom(DBPImage main, boolean disabled, DBPImage topLeft, DBPImage topRight, DBPImage bottomLeft, DBPImage bottomRight) {
        this.main = main;
        this.disabled = disabled;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public DBPImage getTopLeft() {
        return topLeft;
    }

    public DBPImage getTopRight() {
        return topRight;
    }

    public DBPImage getBottomLeft() {
        return bottomLeft;
    }

    public DBPImage getBottomRight() {
        return bottomRight;
    }

    @Override
    public String getLocation() {
        return main.getLocation();
    }

}
