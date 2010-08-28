/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

/**
 * Relates one table to another
 * 
 * @author Phil Zoio
 */
public class Relationship extends PropertyAwareObject
{

	private Table primaryKeyTable;
	private Table foreignKeyTable;
	private Column foreignKeyColumn;

	/**
	 * @param foreignTable
	 * @param primaryKeyTable
	 * @param foreignKeyColumn
	 */
	public Relationship(Table foreignTable, Table primaryTable)
	{
		super();
		this.primaryKeyTable = primaryTable;
		this.foreignKeyTable = foreignTable;
		this.primaryKeyTable.addPrimaryKeyRelationship(this);
		this.foreignKeyTable.addForeignKeyRelationship(this);
	}

	/**
	 * @return Returns the foreignKeyColumn.
	 */
	public Column getForeignKeyColumn()
	{
		return foreignKeyColumn;
	}

	/**
	 * @return Returns the foreignKeyTable.
	 */
	public Table getForeignKeyTable()
	{
		return foreignKeyTable;
	}

	/**
	 * @return Returns the primaryKeyTable.
	 */
	public Table getPrimaryKeyTable()
	{
		return primaryKeyTable;
	}

	/**
	 * @param sourceForeignKey the primary key table you are connecting to
	 */
	public void setPrimaryKeyTable(Table targetPrimaryKey)
	{
		this.primaryKeyTable = targetPrimaryKey;
	}	
	
	/**
	 * @param sourceForeignKey the foreign key table you are connecting from
	 */
	public void setForeignKeyTable(Table sourceForeignKey)
	{
		this.foreignKeyTable = sourceForeignKey;
	}
}