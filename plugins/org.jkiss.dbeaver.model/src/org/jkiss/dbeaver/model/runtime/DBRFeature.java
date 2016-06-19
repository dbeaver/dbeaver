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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.qm.QMUtils;

import java.util.Map;

/**
 * DBeaver feature description
 */
public class DBRFeature {

    public static final DBRFeature ROOT = new DBRFeature("Root", "Root Feature");

    private final DBRFeature parentFeature;
    private final String id;
    private final String name;
    private final String description;
    private final String helpURL;
    private final boolean isAbstract;

    private DBRFeature(@NotNull String id, @NotNull String name) {
        this.parentFeature = null;
        this.id = id;
        this.name = name;
        this.description = null;
        this.helpURL = null;
        this.isAbstract = true;
    }

    public DBRFeature(@NotNull DBRFeature parentFeature, @NotNull String id, @NotNull String name, String description, String helpURL, boolean isAbstract) {
        this.parentFeature = parentFeature;
        this.id = id;
        this.name = name;
        this.description = description;
        this.helpURL = helpURL;
        this.isAbstract = isAbstract;
    }

    public DBRFeature(@NotNull DBRFeature parentFeature, @NotNull String id, @NotNull String name) {
        this(parentFeature, id, name, null, null, false);
    }

    public DBRFeature getParentFeature() {
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

    public void use() {
        this.use(null);
    }

    public void use(Map<String, Object> parameters) {
        QMUtils.getDefaultHandler().handleFeatureUsage(this, parameters);
    }

}