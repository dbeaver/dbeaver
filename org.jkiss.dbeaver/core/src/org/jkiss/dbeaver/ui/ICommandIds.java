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
    public static final String CMD_EDIT_DRIVERS = "org.jkiss.dbeaver.core.editDrivers";
    public static final String CMD_NEW_CONNECTION = "org.jkiss.dbeaver.core.newConnection";
    public static final String CMD_EDIT_CONNECTION = "org.jkiss.dbeaver.core.editConnection";
    public static final String CMD_DELETE_CONNECTION = "org.jkiss.dbeaver.core.deleteConnection";
    public static final String CMD_CONNECT = "org.jkiss.dbeaver.core.connect";
    public static final String CMD_DISCONNECT = "org.jkiss.dbeaver.core.disconnect";
    public static final String CMD_REFRESH_TREE = "org.jkiss.dbeaver.core.refreshTree";
    public static final String CMD_OPEN_MESSAGE = "org.jkiss.dbeaver.core.openMessage";
    public static final String CMD_OPEN_SQLEDITOR = "org.jkiss.dbeaver.core.openSQLEditor";
    public static final String CMD_OPEN_FOLDEREDITOR = "org.jkiss.dbeaver.core.openFolderEditor";
    public static final String CMD_OPEN_ENTITYEDITOR = "org.jkiss.dbeaver.core.openEntityEditor";

    public static final String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement";
    public static final String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script";
    public static final String CMD_COMMIT = "org.jkiss.dbeaver.ui.editors.sql.commit";
    public static final String CMD_ROLLBACK = "org.jkiss.dbeaver.ui.editors.sql.rollback";

}
