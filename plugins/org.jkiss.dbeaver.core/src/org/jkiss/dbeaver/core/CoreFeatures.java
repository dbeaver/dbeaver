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
package org.jkiss.dbeaver.core;

import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

/**
 * DBeaver project nature
 */
public interface CoreFeatures {

    DBRFeature SQL_EDITOR = new DBRFeature(DBRFeature.ROOT, "SQL Editor", "SQL Editor features", null, true, null);
    DBRFeature SQL_EDITOR_OPEN = new DBRFeature(SQL_EDITOR, "Open SQL Editor");
    DBRFeature SQL_EDITOR_EXECUTE_QUERY = new DBRFeature(SQL_EDITOR, "Execute SQL query");
    DBRFeature SQL_EDITOR_EXECUTE_SCRIPT = new DBRFeature(SQL_EDITOR, "Execute SQL script");
    DBRFeature SQL_EDITOR_EXPLAIN_PLAN = new DBRFeature(SQL_EDITOR, "Explain SQL query plan");
    DBRFeature SQL_EDITOR_QUERY_PARAMS = new DBRFeature(SQL_EDITOR, "Use SQL query parameters");

    DBRFeature ENTITY_EDITOR = new DBRFeature(DBRFeature.ROOT, "Object Editor", "Object Editor features", null, true, null);
    DBRFeature ENTITY_EDITOR_MODIFY = new DBRFeature(ENTITY_EDITOR, "Change object properties");
    DBRFeature ENTITY_EDITOR_SAVE = new DBRFeature(ENTITY_EDITOR, "Save object properties");
    DBRFeature ENTITY_EDITOR_REJECT = new DBRFeature(ENTITY_EDITOR, "Reject object properties changes");

}