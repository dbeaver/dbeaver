/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;

/**
 * MySQLUserEditorGeneral
 */
public class MySQLUserEditorPrivileges extends MySQLUserEditorAbstract
{
    static final Log log = LogFactory.getLog(MySQLUserEditorPrivileges.class);

    public void createPartControl(Composite parent)
    {
    }

    public void activatePart()
    {
        try {
            //
            getUser().getHost();
        }
        catch (Exception ex) {
            log.error("Can't obtain trigger body", ex);
        }
    }

}