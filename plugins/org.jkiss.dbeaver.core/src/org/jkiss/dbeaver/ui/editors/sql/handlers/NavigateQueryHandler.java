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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class NavigateQueryHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (!(activeEditor instanceof SQLEditorBase)) {
            return null;
        }
        SQLEditorBase editor = (SQLEditorBase)activeEditor;

        String actionId = event.getCommand().getId();

        SQLQuery nextQuery;
        switch (actionId) {
            case CoreCommands.CMD_SQL_QUERY_NEXT:
                nextQuery = editor.extractNextQuery(true);
                break;
            case CoreCommands.CMD_SQL_QUERY_PREV:
                nextQuery = editor.extractNextQuery(false);
                break;
            default:
                nextQuery = null;
                break;
        }
        if (nextQuery != null) {
            editor.selectAndReveal(nextQuery.getOffset(), nextQuery.getLength());
        }
        return null;
    }

}
