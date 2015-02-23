/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    public static final DBSObjectState UNKNOWN = new DBSObjectState("Unknown", DBIcon.OVER_UNKNOWN);

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