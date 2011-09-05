/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Object state
 */
public class DBSObjectState
{
    public static final DBSObjectState NORMAL = new DBSObjectState(null);
    public static final DBSObjectState INVALID = new DBSObjectState(DBIcon.OVER_ERROR);
    public static final DBSObjectState ACTIVE = new DBSObjectState(DBIcon.OVER_SUCCESS);
    public static final DBSObjectState UNKNOWN = new DBSObjectState(DBIcon.OVER_CONDITION);

    private final DBIcon overlayImage;

    public DBSObjectState(DBIcon overlayImage)
    {
        this.overlayImage = overlayImage;
    }

    public ImageDescriptor getOverlayImage()
    {
        return overlayImage == null ? null : overlayImage.getImageDescriptor();
    }

}