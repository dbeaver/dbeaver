/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * IDatabaseObjectManager
 */
public interface IDatabaseObjectManager<OBJECT_TYPE extends DBSObject> extends IDataSourceProvider {

    OBJECT_TYPE getObject();

    void init(OBJECT_TYPE object);

    boolean supportsEdit();

    boolean isDirty();

    boolean canUndoCommand();

    boolean canRedoCommand();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges(DBRProgressMonitor monitor) throws DBException;

    Collection<IDatabaseObjectCommand<OBJECT_TYPE>> getCommands();

    <COMMAND extends IDatabaseObjectCommand<OBJECT_TYPE>> void addCommand(
        COMMAND command,
        IDatabaseObjectCommandReflector<COMMAND> reflector);

}