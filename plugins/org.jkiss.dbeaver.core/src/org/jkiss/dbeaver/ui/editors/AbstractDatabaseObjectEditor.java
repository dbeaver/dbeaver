/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * AbstractDatabaseObjectEditor
 */
public abstract class AbstractDatabaseObjectEditor<OBJECT_TYPE extends DBSObject>
    extends EditorPart implements IDatabaseNodeEditor, IActiveWorkbenchPart
{

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
    }

    public void doSaveAs()
    {
    }

    public boolean isDirty()
    {
        return getEditorInput().getObjectCommander().isDirty();
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void setFocus() {

    }

    public void activatePart()
    {
        // do nothing by default
    }

    public void deactivatePart() {
        // do nothing by default
    }

    public DBPDataSource getDataSource() {
        OBJECT_TYPE object = getDatabaseObject();
        return object == null ? null : object.getDataSource();
    }

    public OBJECT_TYPE getDatabaseObject() {
        return (OBJECT_TYPE) getEditorInput().getDatabaseObject();
    }

    @Override
    public IDatabaseNodeEditorInput getEditorInput()
    {
        return (IDatabaseNodeEditorInput)super.getEditorInput();
    }

    @Override
    protected void setInput(IEditorInput input)
    {
        super.setInput(input);
    }

    /*
    protected void addChangeCommand(DBECommand<OBJECT_TYPE> command)
    {
        this.objectManager.addCommand(command, null);
        firePropertyChange(PROP_DIRTY);
    }
*/

    private DBEObjectCommander getObjectCommander()
    {
        return getEditorInput().getObjectCommander();
    }

    public void addChangeCommand(
        DBECommand<OBJECT_TYPE> command,
        DBECommandReflector<OBJECT_TYPE, ? extends DBECommand<OBJECT_TYPE>> reflector)
    {
        getObjectCommander().addCommand(command, (DBECommandReflector) reflector);
    }

    public void removeChangeCommand(DBECommand<OBJECT_TYPE> command)
    {
        getObjectCommander().removeCommand(command);
    }

    public void updateChangeCommand(DBECommand<OBJECT_TYPE> command)
    {
        getObjectCommander().updateCommand(command);
    }
}