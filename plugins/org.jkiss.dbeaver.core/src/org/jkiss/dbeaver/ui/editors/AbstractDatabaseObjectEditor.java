/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.IActiveWorkbenchPart;

/**
 * AbstractDatabaseObjectEditor
 */
public abstract class AbstractDatabaseObjectEditor<OBJECT_TYPE extends DBSObject>
    extends EditorPart implements IDatabaseEditor, IActiveWorkbenchPart
{

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.setSite(site);
        super.setInput(input);
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isDirty()
    {
        return getEditorInput().getCommandContext().isDirty();
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public void activatePart()
    {
        // do nothing by default
    }

    @Override
    public void deactivatePart() {
        // do nothing by default
    }

    @Override
    public void recreateEditorControl() {
        // Not supported
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return getEditorInput().getExecutionContext();
    }

    public OBJECT_TYPE getDatabaseObject() {
        return (OBJECT_TYPE) getEditorInput().getDatabaseObject();
    }

    @Override
    public IDatabaseEditorInput getEditorInput()
    {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    /*
    protected void addChangeCommand(DBECommand<OBJECT_TYPE> command)
    {
        this.objectManager.addCommand(command, null);
        firePropertyChange(PROP_DIRTY);
    }
*/

    private DBECommandContext getObjectCommander()
    {
        return getEditorInput().getCommandContext();
    }

    public void addChangeCommand(
        DBECommand<OBJECT_TYPE> command,
        DBECommandReflector<OBJECT_TYPE, ? extends DBECommand<OBJECT_TYPE>> reflector)
    {
        getObjectCommander().addCommand(command, reflector);
    }

    public void removeChangeCommand(DBECommand<OBJECT_TYPE> command)
    {
        getObjectCommander().removeCommand(command);
    }

    public void updateChangeCommand(DBECommand<OBJECT_TYPE> command,
        DBECommandReflector<OBJECT_TYPE, ? extends DBECommand<OBJECT_TYPE>> reflector)
    {
        getObjectCommander().updateCommand(command, reflector);
    }

    @Override
    public boolean isActiveTask() {
        return false;
    }
}