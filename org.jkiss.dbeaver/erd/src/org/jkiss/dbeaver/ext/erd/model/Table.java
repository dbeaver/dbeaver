/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model object representing a relational database Table
 * Also includes the bounds of the table so that the diagram can be 
 * restored following a save, although ideally this should be
 * in a separate diagram specific model hierarchy
 * @author Phil Zoio
 */
public class Table extends PropertyAwareObject
{

	private EntityDiagram entityDiagram;
	private String name;
	private List<Column> columns = new ArrayList<Column>();

	private List<Relationship> primaryKeyRelationships = new ArrayList<Relationship>();
	private List<Relationship> foreignKeyRelationships = new ArrayList<Relationship>();

	public Table()
	{
		super();
	}

	public Table(String name, EntityDiagram entityDiagram)
	{
		super();
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		if (entityDiagram == null)
			throw new NullPointerException("Schema cannot be null");
		this.name = name;
		this.entityDiagram = entityDiagram;
	}

	public void addColumn(Column column)
	{
		if (columns.contains(column))
		{
			throw new IllegalArgumentException("Column already present");
		}
		columns.add(column);
		firePropertyChange(CHILD, null, column);
	}

	public void addColumn(Column column, int index)
	{
		if (columns.contains(column))
		{
			throw new IllegalArgumentException("Column already present");
		}
		columns.add(index, column);
		firePropertyChange(CHILD, null, column);
	}

	public void removeColumn(Column column)
	{
		columns.remove(column);
		firePropertyChange(CHILD, column, null);
	}

	public void switchColumn(Column column, int index)
	{
		columns.remove(column);
		columns.add(index, column);
		firePropertyChange(REORDER, this, column);
	}

	/**
	 * Sets name without firing off any event notifications
	 * 
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @param entityDiagram
	 *            The schema to set.
	 */
	public void setSchema(EntityDiagram entityDiagram)
	{
		this.entityDiagram = entityDiagram;
	}

	/**
	 * Adds relationship where the current object is the foreign key table in a relationship
	 * 
	 * @param table
	 *            the primary key relationship
	 */
	public void addForeignKeyRelationship(Relationship table)
	{
		foreignKeyRelationships.add(table);
		firePropertyChange(OUTPUT, null, table);
	}

	/**
	 * Adds relationship where the current object is the primary key table in a relationship
	 * 
	 * @param table
	 *            the foreign key relationship
	 */
	public void addPrimaryKeyRelationship(Relationship table)
	{
		primaryKeyRelationships.add(table);
		firePropertyChange(INPUT, null, table);
	}

	/**
	 * Removes relationship where the current object is the foreign key table in a relationship
	 * 
	 * @param table
	 *            the primary key relationship
	 */
	public void removeForeignKeyRelationship(Relationship table)
	{
		foreignKeyRelationships.remove(table);
		firePropertyChange(OUTPUT, table, null);
	}

	/**
	 * Removes relationship where the current object is the primary key table in a relationship
	 * 
	 * @param table
	 *            the foreign key relationship
	 */
	public void removePrimaryKeyRelationship(Relationship table)
	{
		primaryKeyRelationships.remove(table);
		firePropertyChange(INPUT, table, null);
	}

	/**
	 * If modified, sets name and fires off event notification
	 * 
	 * @param name
	 *            The name to set.
	 */
	public void modifyName(String name)
	{
		String oldName = this.name;
		if (!name.equals(oldName))
		{
			this.name = name;
			firePropertyChange(NAME, null, name);
		}
	}

	public String getName()
	{
		return name;
	}

	public List getColumns()
	{
		return columns;
	}

	/**
	 * @return Returns the foreignKeyRelationships.
	 */
	public List<Relationship> getForeignKeyRelationships()
	{
		return foreignKeyRelationships;
	}

	/**
	 * @return Returns the primaryKeyRelationships.
	 */
	public List<Relationship> getPrimaryKeyRelationships()
	{
		return primaryKeyRelationships;
	}

	/**
	 * @return Returns the schema.
	 */
	public EntityDiagram getSchema()
	{
		return entityDiagram;
	}

	public String toString()
	{
		return name;
	}

	/**
	 * hashcode implementation for use as key in Map
	 */
	public int hashCode()
	{
		//just an extra line so that this does not fall over when the tool is used incorrectly
		if (entityDiagram == null || name == null)
			return super.hashCode();
		String schemaName = entityDiagram.getName();
		return schemaName.hashCode() + name.hashCode();
	}

	/**
	 * equals implementation for use as key in Map
	 */
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof Table))
			return false;
		Table t = (Table) o;

		if (entityDiagram.getName().equals(t.getSchema().getName()) && name.equals(t.getName()))
		{
			return true;
		}
		else
			return false;
	}
}