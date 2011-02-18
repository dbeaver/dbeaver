/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBOCommand;
import org.jkiss.dbeaver.model.edit.DBOCommandReflector;
import org.jkiss.dbeaver.model.edit.DBOEditor;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * AbstractDatabaseObjectEditor
 */
public abstract class AbstractDatabaseObjectEditor<OBJECT_TYPE extends DBSObject, OBJECT_MANAGER extends DBOManager<OBJECT_TYPE>>
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
    }

    public void doSaveAs()
    {
    }

    public boolean isDirty()
    {
        return objectManager instanceof DBOEditor<?> && ((DBOEditor<?>) objectManager).isDirty();
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

    /*
    protected void addChangeCommand(DBOCommand<OBJECT_TYPE> command)
    {
        this.objectManager.addCommand(command, null);
        firePropertyChange(PROP_DIRTY);
    }
*/

    private DBOEditor<OBJECT_TYPE> getObjectEditor()
    {
        if (objectManager instanceof DBOEditor<?>) {
            return (DBOEditor<OBJECT_TYPE>)objectManager;
        } else {
            throw new IllegalStateException("Object manager do not provide editor facilities");
        }
    }

    public <COMMAND extends DBOCommand<OBJECT_TYPE>> void addChangeCommand(
        COMMAND command,
        DBOCommandReflector<OBJECT_TYPE, COMMAND> reflector)
    {
        getObjectEditor().addCommand(command, reflector);
        firePropertyChange(PROP_DIRTY);
    }

    public void removeChangeCommand(DBOCommand<OBJECT_TYPE> command)
    {
        getObjectEditor().removeCommand(command);
        firePropertyChange(PROP_DIRTY);
    }

    public void updateChangeCommand(DBOCommand<OBJECT_TYPE> command)
    {
        getObjectEditor().updateCommand(command);
        firePropertyChange(PROP_DIRTY);
    }
}