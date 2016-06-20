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
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;

/**
 * DBeaver project nature
 */
public interface CoreFeatures {

    DBRFeature SQL_EDITOR = DBRFeature.createCategory("SQL Editor", "SQL Editor features");
    DBRFeature SQL_EDITOR_OPEN = DBRFeature.createFeature(SQL_EDITOR, "Open SQL Editor");
    DBRFeature SQL_EDITOR_EXECUTE_QUERY = DBRFeature.createCommandFeature(SQL_EDITOR, CoreCommands.CMD_EXECUTE_STATEMENT);
    DBRFeature SQL_EDITOR_EXECUTE_QUERY_NEW = DBRFeature.createCommandFeature(SQL_EDITOR, CoreCommands.CMD_EXECUTE_STATEMENT_NEW);
    DBRFeature SQL_EDITOR_EXECUTE_SCRIPT = DBRFeature.createFeature(SQL_EDITOR, "Execute SQL script");
    DBRFeature SQL_EDITOR_EXPLAIN_PLAN = DBRFeature.createCommandFeature(SQL_EDITOR, CoreCommands.CMD_EXPLAIN_PLAN);
    DBRFeature SQL_EDITOR_QUERY_PARAMS = DBRFeature.createFeature(SQL_EDITOR, "Use SQL query parameters");

    DBRFeature ENTITY_EDITOR = DBRFeature.createCategory("Object Editor", "Object Editor features");
    DBRFeature ENTITY_EDITOR_MODIFY = DBRFeature.createFeature(ENTITY_EDITOR, "Change object properties");
    DBRFeature ENTITY_EDITOR_SAVE = DBRFeature.createFeature(ENTITY_EDITOR, "Save object properties");
    DBRFeature ENTITY_EDITOR_REJECT = DBRFeature.createFeature(ENTITY_EDITOR, "Reject object properties changes");

    DBRFeature RESULT_SET = DBRFeature.createCategory("Result Set", "ResultSet operation");
    DBRFeature RESULT_SET_APPLY_CHANGES = DBRFeature.createCommandFeature(RESULT_SET, ResultSetCommandHandler.CMD_APPLY_CHANGES);
}