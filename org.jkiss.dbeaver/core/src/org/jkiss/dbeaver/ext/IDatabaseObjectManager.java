/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * IDatabaseObjectManager
 */
public interface IDatabaseObjectManager<OBJECT_TYPE> {

    OBJECT_TYPE getObject();

    void init(DBPObject object) throws DBException;

    boolean supportsEdit();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges(DBRProgressMonitor monitor);

}