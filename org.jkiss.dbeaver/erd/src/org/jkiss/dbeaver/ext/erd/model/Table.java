/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Model object representing a relational database Table
 * Also includes the bounds of the table so that the diagram can be 
 * restored following a save, although ideally this should be
 * in a separate diagram specific model hierarchy
 * @author Phil Zoio
 */
public class Table extends PropertyAwareObject
{

	private Schema schema;
	private String name;
	private ArrayList columns = new ArrayList();
	private Rectangle bounds;

	private ArrayList primaryKeyRelationships = new ArrayList();
	private ArrayList foreignKeyRelationships = new ArrayList();

	public Table()
	{
		super();
	}

	/**
	 * @param name
	 */
	public Table(String name, Schema schema)
	{
		super();
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		if (schema == null)
			throw new NullPointerException("Schema cannot be null");
		this.name = name;
		this.schema = schema;
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
	 * @param schema
	 *            The schema to set.
	 */
	public void setSchema(Schema schema)
	{
		this.schema = schema;
	}

	/**
	 * Sets bounds without firing off any event notifications
	 * 
	 * @param bounds
	 *            The bounds to set.
	 */
	public void setBounds(Rectangle bounds)
	{
		this.bounds = bounds;
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

	/**
	 * If modified, sets bounds and fires off event notification
	 * 
	 * @param bounds
	 *            The bounds to set.
	 */
	public void modifyBounds(Rectangle bounds)
	{
		Rectangle oldBounds = this.bounds;
		if (!bounds.equals(oldBounds))
		{
			this.bounds = bounds;
			firePropertyChange(BOUNDS, null, bounds);
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
	public List getForeignKeyRelationships()
	{
		return foreignKeyRelationships;
	}

	/**
	 * @return Returns the primaryKeyRelationships.
	 */
	public List getPrimaryKeyRelationships()
	{
		return primaryKeyRelationships;
	}

	/**
	 * @return Returns the schema.
	 */
	public Schema getSchema()
	{
		return schema;
	}

	/**
	 * @return Returns the bounds.
	 */
	public Rectangle getBounds()
	{
		return bounds;
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
		if (schema == null || name == null)
			return super.hashCode();
		String schemaName = schema.getName();
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

		if (schema.getName().equals(t.getSchema().getName()) && name.equals(t.getName()))
		{
			return true;
		}
		else
			return false;
	}
}