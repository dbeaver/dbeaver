/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Object type
 */
public interface DBSObjectType
{
    String getTypeName();

    String getDescription();

    ImageDescriptor getImage();

    Class<? extends DBSObject> getTypeClass();
}
