/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * IObjectEditor
 */
public interface IObjectManager {

    DBPObject getObject();

    void init(DBPObject object) throws DBException;

    boolean supportsEdit();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges(DBRProgressMonitor monitor);

}