/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

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

    public DBPImage getLabelImage()
    {
        if (object instanceof DBPImageProvider) {
            return ((DBPImageProvider)object).getObjectImage();
        } else {
            return DBIcon.TYPE_UNKNOWN;
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

    @NotNull
    @Override
    public String getName()
    {
        return getObject().getName();
    }
}