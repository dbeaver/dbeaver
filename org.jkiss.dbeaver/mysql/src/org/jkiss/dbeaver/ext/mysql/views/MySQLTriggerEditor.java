/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.ext.ui.IObjectEditor;
import org.jkiss.dbeaver.model.DBPObject;

/**
 * SQLTableData
 */
public class MySQLTriggerEditor extends EditorPart implements IObjectEditor
{
    static final Log log = LogFactory.getLog(MySQLTriggerEditor.class);

    private Text ddlText;
    private MySQLTrigger trigger;

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        setSite(site);
        setInput(input);
    }

    public void dispose()
    {
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
        ddlText = new Text(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.BORDER);
        ddlText.setForeground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        ddlText.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    }

    public void setFocus()
    {
    }

    public void activatePart()
    {
        try {
            ddlText.setText(trigger.getBody());
        }
        catch (Exception ex) {
            log.error("Can't obtain trigger body", ex);
        }
    }

    public void deactivatePart()
    {
    }

    public DBPObject getObject()
    {
        return trigger;
    }

    public void setObject(DBPObject object)
    {
        if (!(object instanceof MySQLTrigger)) {
            throw new IllegalArgumentException("object must be of type " + MySQLTrigger.class);
        }
        trigger = (MySQLTrigger) object;
    }

}