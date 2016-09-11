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
package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

/**
 * ValueViewCommandHandler
 */
public class ValueViewCommandHandler extends AbstractHandler {

    public static final String CMD_SAVE_VALUE = "org.jkiss.dbeaver.core.resultset.cell.save";

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ResultSetViewer rsv = (ResultSetViewer) ResultSetCommandHandler.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (rsv == null) {
            return null;
        }
        String actionId = event.getCommand().getId();
        IResultSetPanel visiblePanel = rsv.getVisiblePanel();
        if (visiblePanel instanceof ViewValuePanel) {
            switch (actionId) {
                case ITextEditorActionDefinitionIds.SMART_ENTER:
                case CoreCommands.CMD_EXECUTE_STATEMENT:
                case CMD_SAVE_VALUE:
                    ((ViewValuePanel) visiblePanel).saveValue();
                    break;
            }
        }


        return null;
    }

}