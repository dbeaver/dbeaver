/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.eclipse.swt.graphics.Image;

/**
 * Object type
 */
public interface DBSObjectType
{
    String getTypeName();

    String getDescription();

    Image getImage();

    Class<? extends DBSObject> getTypeClass();
}
