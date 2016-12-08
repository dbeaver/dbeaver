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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerExpression;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;


public class ExecuteHandler extends AbstractHandler
{
    private static final Log log = Log.getLog(ExecuteHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            log.error("No active SQL editor found");
            return null;
        }
        String actionId = event.getCommand().getId();
        switch (actionId) {
            case CoreCommands.CMD_EXECUTE_STATEMENT:
                editor.processSQL(false, false);
                break;
            case CoreCommands.CMD_EXECUTE_STATEMENT_NEW:
                editor.processSQL(true, false);
                break;
            case CoreCommands.CMD_EXECUTE_SCRIPT:
                editor.processSQL(false, true);
                break;
            case CoreCommands.CMD_EXECUTE_SCRIPT_NEW:
                editor.processSQL(true, true);
                break;
            case CoreCommands.CMD_EXECUTE_ROW_COUNT:
                editor.processSQL(false, false, new SQLQueryTransformerCount());
                break;
            case CoreCommands.CMD_EXECUTE_EXPRESSION:
                editor.processSQL(false, false, new SQLQueryTransformerExpression());
                break;
            case CoreCommands.CMD_EXPLAIN_PLAN:
                editor.explainQueryPlan();
                break;
            default:
                log.error("Unsupported SQL editor command: " + actionId);
                break;
        }

        return null;
    }

}