/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.views;

import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectManager;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * MySQLUserEditorAbstract
 */
public abstract class MySQLUserEditorAbstract extends AbstractDatabaseObjectEditor<MySQLUser>
{
    private MySQLUser user;

    public MySQLUser getUser() {
        return user;
    }

    public void initObjectEditor(IDatabaseObjectManager<MySQLUser> manager)
    {
        user = manager.getObject();
    }

}