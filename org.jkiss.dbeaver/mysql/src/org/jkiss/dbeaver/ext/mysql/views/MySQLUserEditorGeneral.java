/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.ui.editors.AbstractObjectEditor;

/**
 * MySQLUserEditorGeneral
 */
public class MySQLUserEditorGeneral extends AbstractObjectEditor
{
    static final Log log = LogFactory.getLog(MySQLUserEditorGeneral.class);

    private MySQLUser user;

    public void createPartControl(Composite parent)
    {
    }

    public void activatePart()
    {
        try {
            //
            user.getHost();
        }
        catch (Exception ex) {
            log.error("Can't obtain trigger body", ex);
        }
    }

    public DBPObject getObject()
    {
        return user;
    }

    public void setObject(DBPObject object)
    {
        if (!(object instanceof MySQLUser)) {
            throw new IllegalArgumentException("Object must be of type " + MySQLUser.class);
        }
        user = (MySQLUser) object;
    }

}