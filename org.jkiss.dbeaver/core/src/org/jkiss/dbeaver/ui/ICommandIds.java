/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

/**
 * Interface defining the application's command IDs.
 * Key bindings can be defined for specific commands.
 * To associate an action with a command, use IAction.setActionDefinitionId(commandId).
 *
 * @see org.eclipse.jface.action.IAction#setActionDefinitionId(String)
 */
public final class ICommandIds
{
    public static final String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement";
    public static final String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script";
    public static final String CMD_OPEN_FILE = "org.jkiss.dbeaver.ui.editors.sql.open.file";

}
