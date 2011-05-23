/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.lang.reflect.InvocationTargetException;

/**
 * MySQLViewEditor
 */
public class MySQLViewEditor extends SQLEditorBase implements IActiveWorkbenchPart
{
    static final Log log = LogFactory.getLog(MySQLViewEditor.class);

    private IDatabaseNodeEditorInput entityEditorInput;
    private StringEditorInput viewDefinitionInput;

    public DBPDataSource getDataSource()
    {
        return entityEditorInput == null ? null : entityEditorInput.getDataSource();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        if (!(input instanceof IDatabaseNodeEditorInput)) {
            throw new PartInitException("Bad editor input");
        }
        entityEditorInput = (IDatabaseNodeEditorInput) input;
        viewDefinitionInput = new StringEditorInput("View", "BLABLAH", false);
        super.init(site, viewDefinitionInput);
    }

    public void activatePart()
    {

    }

    public void deactivatePart()
    {

    }
}