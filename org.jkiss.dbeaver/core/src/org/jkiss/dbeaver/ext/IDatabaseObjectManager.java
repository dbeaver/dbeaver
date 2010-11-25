/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * IDatabaseObjectManager
 */
public interface IDatabaseObjectManager<OBJECT_TYPE extends DBSObject> extends IDataSourceProvider {

    OBJECT_TYPE getObject();

    void init(OBJECT_TYPE object);

    boolean supportsEdit();

    boolean isDirty();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges(DBRProgressMonitor monitor);

    void addCommand(IDatabaseObjectCommand<OBJECT_TYPE> command);
}