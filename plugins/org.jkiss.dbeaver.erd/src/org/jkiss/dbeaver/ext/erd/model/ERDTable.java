/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.*;

/**
 * Model object representing a relational database Table
 * Also includes the bounds of the table so that the diagram can be 
 * restored following a save, although ideally this should be
 * in a separate diagram specific model hierarchy
 * @author Serge Rieder
 */
public class ERDTable extends ERDObject<DBSTable>
{

	private List<ERDTableColumn> columns = new ArrayList<ERDTableColumn>();

	private List<ERDAssociation> primaryKeyRelationships = new ArrayList<ERDAssociation>();
	private List<ERDAssociation> foreignKeyRelationships = new ArrayList<ERDAssociation>();
    private List<DBSForeignKey> unresolvedKeys;

    private boolean primary = false;
    private ERDLogicalPrimaryKey logicalPK;

    public ERDTable(DBSTable dbsTable) {
        super(dbsTable);
    }

    public void addColumn(ERDTableColumn column, boolean reflect)
	{
		if (columns.contains(column))
		{
			throw new IllegalArgumentException("Column already present");
		}
		columns.add(column);
        if (reflect) {
		    firePropertyChange(CHILD, null, column);
        }
	}

	public void removeColumn(ERDTableColumn column, boolean reflect)
	{
		columns.remove(column);
        if (reflect) {
		    firePropertyChange(CHILD, column, null);
        }
	}

	public void switchColumn(ERDTableColumn column, int index, boolean reflect)
	{
		columns.remove(column);
		columns.add(index, column);
        if (reflect) {
		    firePropertyChange(REORDER, this, column);
        }
	}

	/**
	 * Sets name without firing off any event notifications
	 * 
	 * @param name
	 *            The name to set.
	 *
	public void setName(String name)
	{
		this.name = name;
	}*/

	/**
	 * Adds relationship where the current object is the foreign key table in a relationship
	 * 
	 * @param rel
	 *            the primary key relationship
	 */
	public void addForeignKeyRelationship(ERDAssociation rel, boolean reflect)
	{
		foreignKeyRelationships.add(rel);
        if (reflect) {
		    firePropertyChange(OUTPUT, null, rel);
        }
	}

	/**
	 * Adds relationship where the current object is the primary key table in a relationship
	 * 
	 * @param table
	 *            the foreign key relationship
	 */
	public void addPrimaryKeyRelationship(ERDAssociation table, boolean reflect)
	{
		primaryKeyRelationships.add(table);
        if (reflect) {
		    firePropertyChange(INPUT, null, table);
        }
	}

	/**
	 * Removes relationship where the current object is the foreign key table in a relationship
	 * 
	 * @param table
	 *            the primary key relationship
	 */
	public void removeForeignKeyRelationship(ERDAssociation table, boolean reflect)
	{
		foreignKeyRelationships.remove(table);
        if (reflect) {
		    firePropertyChange(OUTPUT, table, null);
        }
	}

	/**
	 * Removes relationship where the current object is the primary key table in a relationship
	 * 
	 * @param table
	 *            the foreign key relationship
	 */
	public void removePrimaryKeyRelationship(ERDAssociation table, boolean reflect)
	{
		primaryKeyRelationships.remove(table);
        if (reflect) {
		    firePropertyChange(INPUT, table, null);
        }
	}

	public List<ERDTableColumn> getColumns()
	{
		return columns;
	}

	/**
	 * @return Returns the foreignKeyRelationships.
	 */
	public List<ERDAssociation> getForeignKeyRelationships()
	{
		return foreignKeyRelationships;
	}

	/**
	 * @return Returns the primaryKeyRelationships.
	 */
	public List<ERDAssociation> getPrimaryKeyRelationships()
	{
		return primaryKeyRelationships;
	}

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean hasSelfLinks()
    {
        for (ERDAssociation association : foreignKeyRelationships) {
            if (association.getPrimaryKeyTable() == this) {
                return true;
            }
        }
        return false;
    }

	public String toString()
	{
		return object.getName();
	}

	/**
	 * hashcode implementation for use as key in Map
	 */
	public int hashCode()
	{
		return object.hashCode();
	}

	/**
	 * equals implementation for use as key in Map
	 */
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof ERDTable))
			return false;
        return object.equals(((ERDTable)o).object);
	}

    public static ERDTable fromObject(DBRProgressMonitor monitor, DBSEntity entity)
    {
        if (!(entity instanceof DBSTable)) {
            return null;
        }
        DBSTable dbTable = (DBSTable)entity;
        ERDTable erdTable = new ERDTable(dbTable);

        try {
            Collection<DBSTableColumn> idColumns = DBUtils.getBestTableIdentifier(monitor, dbTable);

            Collection<? extends DBSTableColumn> columns = dbTable.getColumns(monitor);
            if (!CommonUtils.isEmpty(columns)) {
                for (DBSTableColumn column : columns) {
                    ERDTableColumn c1 = new ERDTableColumn(column, idColumns.contains(column));
                    erdTable.addColumn(c1, false);
                }
            }
        } catch (DBException e) {
            // just skip this problematic columns
            log.debug("Could not load table '" + dbTable.getName() + "'columns", e);
        }
        return erdTable;
    }

    public void addRelations(DBRProgressMonitor monitor, Map<DBSEntity, ERDTable> tableMap, boolean reflect)
    {
        try {
            Set<DBSTableColumn> fkColumns = new HashSet<DBSTableColumn>();
            // Make associations
            Collection<? extends DBSForeignKey> fks = getObject().getAssociations(monitor);
            if (fks != null) {
                for (DBSForeignKey fk : fks) {
                    fkColumns.addAll(DBUtils.getTableColumns(monitor, fk));
                    ERDTable table2 = tableMap.get(fk.getAssociatedEntity());
                    if (table2 == null) {
                        //log.debug("Table '" + fk.getReferencedKey().getTable().getFullQualifiedName() + "' not found in ERD");
                        if (unresolvedKeys == null) {
                            unresolvedKeys = new ArrayList<DBSForeignKey>();
                        }
                        unresolvedKeys.add(fk);
                    } else {
                        //if (table1 != table2) {
                        new ERDAssociation(fk, table2, this, reflect);
                        //}
                    }
                }
            }

            // Mark column's fk flag
            for (ERDTableColumn column : this.getColumns()) {
                if (fkColumns.contains(column.getObject())) {
                    column.setInForeignKey(true);
                }
            }

        } catch (DBException e) {
            log.warn("Could not load table '" + getObject().getName() + "' foreign keys", e);
        }
    }

    public void resolveRelations(Map<DBSEntity, ERDTable> tableMap, boolean reflect)
    {
        if (CommonUtils.isEmpty(unresolvedKeys)) {
            return;
        }
        for (Iterator<DBSForeignKey> iter = unresolvedKeys.iterator(); iter.hasNext(); ) {
            final DBSForeignKey fk = iter.next();
            ERDTable refTable = tableMap.get(fk.getReferencedKey().getTable());
            if (refTable != null) {
                new ERDAssociation(fk, refTable, this, reflect);
                iter.remove();
            }
        }
    }

    public ERDLogicalPrimaryKey getLogicalPrimaryKey()
    {
        if (logicalPK == null) {
            logicalPK = new ERDLogicalPrimaryKey(this, "Primary key", "");
        }
        return logicalPK;
    }

    public String getName()
    {
        return getObject().getName();
    }
}