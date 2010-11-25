/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object change command
 */
public interface IDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> {

    public enum MergeResult {
        NONE,
        CANCEL_PREVIOUS,
        CANCEL_BOTH
    }

    String getDescription();

    void updateObjectState(OBJECT_TYPE object);

    MergeResult merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand);

    DatabaseObjectChangeAction[] getChangeActions();

}
