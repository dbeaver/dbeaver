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
    public static final DBSObjectState NORMAL = new DBSObjectState("Normal", null);
    public static final DBSObjectState INVALID = new DBSObjectState("Invalid", DBIcon.OVER_ERROR);
    public static final DBSObjectState ACTIVE = new DBSObjectState("Active", DBIcon.OVER_SUCCESS);
    public static final DBSObjectState UNKNOWN = new DBSObjectState("Unknown", DBIcon.OVER_CONDITION);

    private final String title;
    private final DBIcon overlayImage;

    public DBSObjectState(String title, DBIcon overlayImage)
    {
        this.title = title;
        this.overlayImage = overlayImage;
    }

    public String getTitle()
    {
        return title;
    }

    public ImageDescriptor getOverlayImage()
    {
        return overlayImage == null ? null : overlayImage.getImageDescriptor();
    }

}