/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object editor.
 * Provide object's rename and change description functions
 */
public interface DBEObjectEditor<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    public static final long FEATURE_RENAME         = 1;
    public static final long FEATURE_DESCRIBE       = 2;

    /**
     * Gets supported editor features
     * @return ORed features
     */
    public long getEditorFeatures();

    /**
     * Renames object
     * @param monitor progress monitor
     * @param newName new name
     * @throws DBException on any error
     */
    void renameObject(DBRProgressMonitor monitor, String newName)
        throws DBException;

    /**
     * Updates object description
     * @param monitor progress monitor
     * @param newDescription new description
     * @throws DBException on any error
     */
    void setObjectDescription(DBRProgressMonitor monitor, String newDescription)
        throws DBException;

}