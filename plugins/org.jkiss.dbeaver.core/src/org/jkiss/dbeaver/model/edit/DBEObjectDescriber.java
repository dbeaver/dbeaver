/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object describer.
 * Provide object's rename functions
 */
public interface DBEObjectDescriber<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    public static final int FEATURE_SET_NAME            = 1;
    public static final int FEATURE_SET_DESCRIPTION    = 2;

    /**
     * Gets features
     * @return ORed features
     */
    int getDescribeFeatures();

    /**
     * Describes object
     *
     * @param monitor progress monitor
     * @param newName new name. If
     * @param newDescription new description
     * @throws DBException on any error
     */
    void describeObject(DBRProgressMonitor monitor, OBJECT_TYPE object, String newName, String newDescription)
        throws DBException;

}