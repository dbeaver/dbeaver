/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractDatabaseObjectManager
 */
public abstract class AbstractDatabaseObjectManager<OBJECT_TYPE extends DBSObject> implements IDatabaseObjectManager<OBJECT_TYPE> {

    private OBJECT_TYPE object;
    private List<IDatabaseObjectCommand<OBJECT_TYPE>> commands = new ArrayList<IDatabaseObjectCommand<OBJECT_TYPE>>();

    public DBPDataSource getDataSource() {
        return object.getDataSource();
    }

    public OBJECT_TYPE getObject() {
        return object;
    }

    @SuppressWarnings("unchecked")
    public void init(OBJECT_TYPE object) {
        if (object == null) {
            throw new IllegalArgumentException("Object can't be NULL");
        }
        this.object = object;
    }

    public boolean supportsEdit() {
        return false;
    }

    public boolean isDirty()
    {
        return !commands.isEmpty();
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        // TODO: implement object save
        commands.clear();
    }

    public void resetChanges(DBRProgressMonitor monitor) {
        commands.clear();
    }

    public void addCommand(IDatabaseObjectCommand<OBJECT_TYPE> command)
    {
        commands.add(command);
    }
}
