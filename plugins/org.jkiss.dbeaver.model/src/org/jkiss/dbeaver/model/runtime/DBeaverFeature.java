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

package org.jkiss.dbeaver.model.runtime;

/**
 * DBeaver feature description
 */
public class DBeaverFeature {

    private final DBeaverFeature parentFeature;
    private final String id;
    private final String name;
    private final String description;
    private final String helpURL;
    private final boolean isAbstract;

    public DBeaverFeature(DBeaverFeature parentFeature, String id, String name, String description, String helpURL, boolean isAbstract) {
        this.parentFeature = parentFeature;
        this.id = id;
        this.name = name;
        this.description = description;
        this.helpURL = helpURL;
        this.isAbstract = isAbstract;
    }

    public DBeaverFeature getParentFeature() {
        return parentFeature;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getHelpURL() {
        return helpURL;
    }

    public boolean isAbstract() {
        return isAbstract;
    }
}