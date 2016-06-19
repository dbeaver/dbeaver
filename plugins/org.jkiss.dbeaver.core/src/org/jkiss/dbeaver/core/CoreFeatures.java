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

import org.jkiss.dbeaver.model.runtime.DBRFeature;

/**
 * DBeaver project nature
 */
public interface CoreFeatures {

    DBRFeature SQL_EDITOR = new DBRFeature(DBRFeature.ROOT, "SQLEditor", "SQL Editor", "SQL Editor features", null, true);
    DBRFeature SQL_EDITOR_OPEN = new DBRFeature(SQL_EDITOR, "SQLEditor.Open", "Open SQL Editor");
    DBRFeature SQL_EDITOR_EXECUTE_QUERY = new DBRFeature(SQL_EDITOR, "SQLEditor.Query", "Execute SQL query");
    DBRFeature SQL_EDITOR_EXECUTE_SCRIPT = new DBRFeature(SQL_EDITOR, "SQLEditor.Script", "Execute SQL script");
    DBRFeature SQL_EDITOR_EXPLAIN_PLAN = new DBRFeature(SQL_EDITOR, "SQLEditor.Explain", "Explain SQL query plan");
    DBRFeature SQL_EDITOR_QUERY_PARAMS = new DBRFeature(SQL_EDITOR, "SQLEditor.QueryParams", "Use SQL query parameters");

    DBRFeature ENTITY_EDITOR = new DBRFeature(DBRFeature.ROOT, "EntityEditor", "Object Editor", "Object Editor features", null, true);
    DBRFeature ENTITY_EDITOR_MODIFY = new DBRFeature(ENTITY_EDITOR, "EntityEditor.Modify", "Change object properties");
    DBRFeature ENTITY_EDITOR_SAVE = new DBRFeature(ENTITY_EDITOR, "EntityEditor.Save", "Save object properties");
    DBRFeature ENTITY_EDITOR_REJECT = new DBRFeature(ENTITY_EDITOR, "EntityEditor.Reject", "Reject object properties changes");

}