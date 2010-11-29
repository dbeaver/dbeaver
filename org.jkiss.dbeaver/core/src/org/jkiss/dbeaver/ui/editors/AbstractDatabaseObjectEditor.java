/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommandReflector;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * AbstractDatabaseObjectEditor
 */
public abstract class AbstractDatabaseObjectEditor<OBJECT_TYPE extends DBSObject, OBJECT_MANAGER extends IDatabaseObjectManager<OBJECT_TYPE>>
    extends EditorPart implements IDatabaseObjectEditor<OBJECT_MANAGER>
{
    private OBJECT_MANAGER objectManager;

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.setSite(site);
        super.setInput(input);
    }

    public void dispose()
    {
        super.dispose();
    }

    public void doSave(IProgressMonitor monitor)
    {
        if (objectManager.isDirty()) {
            Throwable error = null;
            try {
                objectManager.saveChanges(new DefaultProgressMonitor(monitor));
            } catch (DBException e) {
                error = e;
            }
            finally {
                final Throwable saveError = error;
                Display.getDefault().asyncExec(new Runnable() {
                    public void run()
                    {
                        firePropertyChange(PROP_DIRTY);
                        if (saveError != null) {
                            UIUtils.showErrorDialog(
                                getSite().getShell(),
                                "Could not save '" + objectManager.getObject().getName() + "'",
                                saveError.getMessage());
                        }
                    }
                });
            }
        }
    }

    public void doSaveAs()
    {
    }

    public boolean isDirty()
    {
        return objectManager.isDirty();
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void setFocus() {

    }

    public void deactivatePart() {
        // do nothing by default
    }

    public DBPDataSource getDataSource() {
        return getObjectManager().getDataSource();
    }

    public OBJECT_MANAGER getObjectManager() {
        return objectManager;
    }

    public OBJECT_TYPE getDatabaseObject() {
        return objectManager.getObject();
    }

    public void initObjectEditor(OBJECT_MANAGER objectManager) {
        this.objectManager = objectManager;
    }

    public void resetObjectChanges()
    {

    }

/*
    protected void addChangeCommand(IDatabaseObjectCommand<OBJECT_TYPE> command)
    {
        this.objectManager.addCommand(command, null);
        firePropertyChange(PROP_DIRTY);
    }
*/

    protected <COMMAND extends IDatabaseObjectCommand<OBJECT_TYPE>> void addChangeCommand(
        COMMAND command,
        IDatabaseObjectCommandReflector<COMMAND> reflector)
    {
        this.objectManager.addCommand(command, reflector);
        firePropertyChange(PROP_DIRTY);
    }

}