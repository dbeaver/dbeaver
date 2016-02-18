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
package org.jkiss.dbeaver.ui;

/**
 * Interface defining the application's command IDs.
 * Key bindings can be defined for specific commands.
 * To associate an action with a command, use IAction.setActionDefinitionId(commandId).
 *
 * @see org.eclipse.jface.action.IAction#setActionDefinitionId(String)
 */
public interface ICommandIds
{
    String CMD_COPY_SPECIAL = "org.jkiss.dbeaver.core.edit.copy.special"; //$NON-NLS-1$

    String CMD_OBJECT_OPEN = "org.jkiss.dbeaver.core.object.open"; //$NON-NLS-1$
    String CMD_OBJECT_CREATE = "org.jkiss.dbeaver.core.object.create"; //$NON-NLS-1$
    String CMD_OBJECT_DELETE = "org.jkiss.dbeaver.core.object.delete"; //$NON-NLS-1$

    String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement"; //$NON-NLS-1$
    String CMD_EXECUTE_STATEMENT_NEW = "org.jkiss.dbeaver.ui.editors.sql.run.statementNew"; //$NON-NLS-1$
    String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script"; //$NON-NLS-1$
    String CMD_EXECUTE_SCRIPT_NEW = "org.jkiss.dbeaver.ui.editors.sql.run.scriptNew"; //$NON-NLS-1$

    String CMD_EXPLAIN_PLAN = "org.jkiss.dbeaver.ui.editors.sql.run.explain"; //$NON-NLS-1$
    String CMD_ANALYSE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.analyse"; //$NON-NLS-1$
    String CMD_VALIDATE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.validate"; //$NON-NLS-1$
    String CMD_OPEN_FILE = "org.jkiss.dbeaver.ui.editors.sql.open.file"; //$NON-NLS-1$
    String CMD_SAVE_FILE = "org.jkiss.dbeaver.ui.editors.sql.save.file"; //$NON-NLS-1$
    String CMD_TOGGLE_AUTOCOMMIT = "org.jkiss.dbeaver.core.txn.autocommit"; //$NON-NLS-1$

    String CMD_CONTENT_FORMAT = "org.jkiss.dbeaver.ui.editors.text.content.format"; //$NON-NLS-1$

    String GROUP_TOOLS = "tools";

    String CMD_CONNECT = "org.jkiss.dbeaver.core.connect";
    String CMD_DISCONNECT = "org.jkiss.dbeaver.core.disconnect";
    String CMD_INVALIDATE = "org.jkiss.dbeaver.core.invalidate";
    String CMD_COMMIT = "org.jkiss.dbeaver.core.commit";
    String CMD_ROLLBACK = "org.jkiss.dbeaver.core.rollback";

    String CMD_SQL_EDITOR_OPEN = "org.jkiss.dbeaver.core.sql.editor.open";
    String CMD_LINK_EDITOR = "org.jkiss.dbeaver.core.navigator.linkeditor";
}
