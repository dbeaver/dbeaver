/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object change command
 */
public interface IDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> {

    public enum MergeResult {
        NONE,
        CANCEL_PREVIOUS,
        CANCEL_BOTH,
        ABSORBED
    }

    String getTitle();

    Image getIcon();

    void updateModel(OBJECT_TYPE object);

    MergeResult merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand);

    IDatabasePersistAction[] getPersistActions();

}
