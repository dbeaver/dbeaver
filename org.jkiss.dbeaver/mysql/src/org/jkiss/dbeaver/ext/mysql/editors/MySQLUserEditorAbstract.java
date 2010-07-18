/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.runtime.MySQLUserManager;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * MySQLUserEditorAbstract
 */
public abstract class MySQLUserEditorAbstract extends AbstractDatabaseObjectEditor<MySQLUserManager>
{
    private MySQLUserManager manager;

    public MySQLUser getUser() {
        return manager.getObject();
    }

    public void initObjectEditor(MySQLUserManager manager)
    {
        this.manager = manager;
    }

}