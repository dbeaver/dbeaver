/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * MySQLTriggerEditor
 */
public class MySQLTriggerEditor extends AbstractDatabaseObjectEditor<MySQLTrigger>
{
    static final Log log = LogFactory.getLog(MySQLTriggerEditor.class);

    private Text ddlText;

    public void createPartControl(Composite parent)
    {
        ddlText = new Text(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.BORDER);
        ddlText.setForeground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        ddlText.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    }

    @Override
    public void setFocus()
    {
        ddlText.setFocus();
    }

    public void activatePart()
    {
        try {
            ddlText.setText(getDatabaseObject().getBody());
        }
        catch (Exception ex) {
            log.error("Can't obtain trigger body", ex);
        }
    }

    public void refreshPart(Object source)
    {
        activatePart();
    }
}