/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Column entry in model Table
 * @author Serge Rieder
 */
public class ERDTableColumn extends ERDObject<DBSTableColumn>
{

    public ERDTableColumn(DBSTableColumn dbsTableColumn) {
        super(dbsTableColumn);
    }

	public String getLabelText()
	{
		return object.getName() + ":" + object.getTypeName();
	}

    public Image getLabelImage()
    {
        if (object instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)object).getObjectImage();
        } else {
            return DBIcon.TYPE_UNKNOWN.getImage();
        }
    }
}