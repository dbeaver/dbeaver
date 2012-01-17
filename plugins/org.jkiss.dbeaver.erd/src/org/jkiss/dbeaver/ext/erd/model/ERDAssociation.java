/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.draw2d.geometry.Point;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;

import java.util.List;

/**
 * Relates one table to another
 * 
 * @author Serge Rieder
 */
public class ERDAssociation extends ERDObject<DBSEntityAssociation>
{

	private ERDTable primaryKeyTable;
	private ERDTable foreignKeyTable;
    private List<Point> initBends;

    /**
     * Constructor for logical association
     * @param foreignTable fk table
     * @param primaryTable pk table
     * @param reflect reflect flag
     */
    public ERDAssociation(ERDTable foreignTable, ERDTable primaryTable, boolean reflect)
    {
        super(new ERDLogicalForeignKey(
            foreignTable,
            foreignTable.getObject().getName() + " -> " + primaryTable.getObject().getName(),
            "",
            primaryTable.getLogicalPrimaryKey()));
        this.primaryKeyTable = primaryTable;
        this.foreignKeyTable = foreignTable;
        this.primaryKeyTable.addPrimaryKeyRelationship(this, reflect);
        this.foreignKeyTable.addForeignKeyRelationship(this, reflect);
    }

    /**
     * Constructor for physical association
     * @param object physical FK
     * @param foreignTable fk table
     * @param primaryTable pk table
     * @param reflect reflect flag
     */
	public ERDAssociation(DBSEntityAssociation object, ERDTable foreignTable, ERDTable primaryTable, boolean reflect)
	{
		super(object);
		this.primaryKeyTable = primaryTable;
		this.foreignKeyTable = foreignTable;
        this.primaryKeyTable.addPrimaryKeyRelationship(this, reflect);
        this.foreignKeyTable.addForeignKeyRelationship(this, reflect);
	}

    public boolean isLogical()
    {
        return getObject() instanceof ERDLogicalForeignKey;
    }

	/**
	 * @return Returns the foreignKeyTable.
	 */
	public ERDTable getForeignKeyTable()
	{
		return foreignKeyTable;
	}

	/**
	 * @return Returns the primaryKeyTable.
	 */
	public ERDTable getPrimaryKeyTable()
	{
		return primaryKeyTable;
	}

	public void setPrimaryKeyTable(ERDTable targetPrimaryKey)
	{
		this.primaryKeyTable = targetPrimaryKey;
	}

	/**
	 * @param sourceForeignKey the foreign key table you are connecting from
	 */
	public void setForeignKeyTable(ERDTable sourceForeignKey)
	{
		this.foreignKeyTable = sourceForeignKey;
	}

    public List<Point> getInitBends()
    {
        return initBends;
    }

    public void setInitBends(List<Point> bends)
    {
        this.initBends = bends;
    }

    @Override
    public String toString()
    {
        return getObject() + " [" + primaryKeyTable + "->" + foreignKeyTable + "]";
    }

    public String getName()
    {
        return getObject().getName();
    }
}