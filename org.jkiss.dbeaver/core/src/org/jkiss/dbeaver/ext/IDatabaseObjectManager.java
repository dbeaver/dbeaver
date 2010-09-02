/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * IDatabaseObjectManager
 */
public interface IDatabaseObjectManager<OBJECT_TYPE extends DBPObject> {

    DBPDataSource getDataSource();

    OBJECT_TYPE getObject();

    void init(DBPDataSource dataSource, OBJECT_TYPE object) throws DBException;

    boolean supportsEdit();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges(DBRProgressMonitor monitor);

}