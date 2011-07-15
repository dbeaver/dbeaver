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
    public static final String CMD_COPY_SPECIAL = "org.jkiss.dbeaver.core.edit.copy.special"; //$NON-NLS-1$

    public static final String CMD_OBJECT_OPEN = "org.jkiss.dbeaver.core.object.open"; //$NON-NLS-1$
    public static final String CMD_OBJECT_CREATE = "org.jkiss.dbeaver.core.object.create"; //$NON-NLS-1$
    public static final String CMD_OBJECT_DELETE = "org.jkiss.dbeaver.core.object.delete"; //$NON-NLS-1$
    //public static final String CMD_OBJECT_RENAME = "org.jkiss.dbeaver.core.object.rename"; //$NON-NLS-1$
    public static final String CMD_OBJECT_FIND = "org.jkiss.dbeaver.core.object.find"; //$NON-NLS-1$

    public static final String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement"; //$NON-NLS-1$
    public static final String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script"; //$NON-NLS-1$
    public static final String CMD_EXPLAIN_PLAN = "org.jkiss.dbeaver.ui.editors.sql.run.explain"; //$NON-NLS-1$
    public static final String CMD_ANALYSE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.analyse"; //$NON-NLS-1$
    public static final String CMD_VALIDATE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.validate"; //$NON-NLS-1$
    public static final String CMD_OPEN_FILE = "org.jkiss.dbeaver.ui.editors.sql.open.file"; //$NON-NLS-1$
    public static final String CMD_SAVE_FILE = "org.jkiss.dbeaver.ui.editors.sql.save.file"; //$NON-NLS-1$

    public static final String CMD_CONTENT_FORMAT = "org.jkiss.dbeaver.ui.editors.text.content.format"; //$NON-NLS-1$
}
