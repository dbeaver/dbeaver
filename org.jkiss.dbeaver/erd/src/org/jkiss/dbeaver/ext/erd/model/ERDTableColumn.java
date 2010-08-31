/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * Column entry in model Table
 * @author Phil Zoio
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
	
}