/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object change command
 */
public interface IDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> {

    public static final long FLAG_NONE = 0;
    public static final long FLAG_PERMANENT = 1;

    public enum MergeResult {
        NONE,
        CANCEL_PREVIOUS,
        CANCEL_BOTH,
        ABSORBED
    }

    String getTitle();

    long getFlags();

    void doLocal(OBJECT_TYPE object);

    void undoLocal(OBJECT_TYPE object);

    MergeResult doMerge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand);

    void undoMerge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand);

    IDatabasePersistAction[] getPersistActions();

}
