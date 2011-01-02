/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    public static final String CMD_COPY_SPECIAL = "org.jkiss.dbeaver.core.edit.copy.special";

    public static final String CMD_OBJECT_OPEN = "org.jkiss.dbeaver.core.object.open";
    public static final String CMD_OBJECT_CREATE = "org.jkiss.dbeaver.core.object.create";
    public static final String CMD_OBJECT_DELETE = "org.jkiss.dbeaver.core.object.delete";
    public static final String CMD_OBJECT_FIND = "org.jkiss.dbeaver.core.object.find";

    public static final String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement";
    public static final String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script";
    public static final String CMD_OPEN_FILE = "org.jkiss.dbeaver.ui.editors.sql.open.file";
    public static final String CMD_SAVE_FILE = "org.jkiss.dbeaver.ui.editors.sql.save.file";

    public static final String CMD_CONTENT_FORMAT = "org.jkiss.dbeaver.ui.editors.text.content.format";
}
