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
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.ui.IObjectEditor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.DBException;

import java.lang.reflect.InvocationTargetException;

/**
 * SQLTableData
 */
public class MySQLDDLEditor extends EditorPart implements IObjectEditor
{
    static final Log log = LogFactory.getLog(MySQLDDLEditor.class);

    private Text ddlText;
    private MySQLTable table;

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
        final StringBuilder ddl = new StringBuilder();
        DBeaverCore.getInstance().runAndWait(true, true, new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                try {
                    ddl.append(table.getDDL(monitor));
                }
                catch (DBException e) {
                    log.error("Can't obtain table DDL", e);
                }
            }
        });
        ddlText.setText(ddl.toString());
    }

    public void deactivatePart()
    {
    }

    public DBPObject getObject()
    {
        return table;
    }

    public void setObject(DBPObject object)
    {
        if (!(object instanceof MySQLTable)) {
            throw new IllegalArgumentException("object must be of type " + MySQLTable.class);
        }
        table = (MySQLTable)object;
    }

}