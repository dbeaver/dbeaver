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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Column entry in model Table
 * @author Serge Rieder
 */
public class ERDEntityAttribute extends ERDObject<DBSEntityAttribute>
{
    private boolean inPrimaryKey;
    private boolean inForeignKey;

    public ERDEntityAttribute(DBSEntityAttribute attribute, boolean inPrimaryKey) {
        super(attribute);
        this.inPrimaryKey = inPrimaryKey;
    }

	public String getLabelText()
	{
		return object.getName();
	}

    public Image getLabelImage()
    {
        if (object instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)object).getObjectImage();
        } else {
            return DBIcon.TYPE_UNKNOWN.getImage();
        }
    }

    public boolean isInPrimaryKey() {
        return inPrimaryKey;
    }

    public boolean isInForeignKey() {
        return inForeignKey;
    }

    public void setInForeignKey(boolean inForeignKey) {
        this.inForeignKey = inForeignKey;
    }

    @Override
    public String getName()
    {
        return getObject().getName();
    }
}