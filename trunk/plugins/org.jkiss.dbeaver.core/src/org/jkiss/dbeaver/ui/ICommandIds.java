/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    public static final String CMD_COPY_SPECIAL = "org.jkiss.dbeaver.core.edit.copy.special"; //$NON-NLS-1$

    public static final String CMD_OBJECT_OPEN = "org.jkiss.dbeaver.core.object.open"; //$NON-NLS-1$
    public static final String CMD_OBJECT_CREATE = "org.jkiss.dbeaver.core.object.create"; //$NON-NLS-1$
    public static final String CMD_OBJECT_DELETE = "org.jkiss.dbeaver.core.object.delete"; //$NON-NLS-1$

    public static final String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement"; //$NON-NLS-1$
    public static final String CMD_EXECUTE_STATEMENT_NEW = "org.jkiss.dbeaver.ui.editors.sql.run.statementNew"; //$NON-NLS-1$
    public static final String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script"; //$NON-NLS-1$
    public static final String CMD_EXECUTE_SCRIPT_NEW = "org.jkiss.dbeaver.ui.editors.sql.run.scriptNew"; //$NON-NLS-1$

    public static final String CMD_EXPLAIN_PLAN = "org.jkiss.dbeaver.ui.editors.sql.run.explain"; //$NON-NLS-1$
    public static final String CMD_ANALYSE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.analyse"; //$NON-NLS-1$
    public static final String CMD_VALIDATE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.validate"; //$NON-NLS-1$
    public static final String CMD_OPEN_FILE = "org.jkiss.dbeaver.ui.editors.sql.open.file"; //$NON-NLS-1$
    public static final String CMD_SAVE_FILE = "org.jkiss.dbeaver.ui.editors.sql.save.file"; //$NON-NLS-1$
    public static final String CMD_TOGGLE_AUTOCOMMIT = "org.jkiss.dbeaver.core.txn.autocommit"; //$NON-NLS-1$

    public static final String CMD_CONTENT_FORMAT = "org.jkiss.dbeaver.ui.editors.text.content.format"; //$NON-NLS-1$

    public static final String GROUP_TOOLS = "tools";

    public static final String CMD_COMMIT = "org.jkiss.dbeaver.core.commit";
    public static final String CMD_ROLLBACK = "org.jkiss.dbeaver.core.rollback";
}
