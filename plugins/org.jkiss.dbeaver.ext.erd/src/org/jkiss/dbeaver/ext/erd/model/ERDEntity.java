/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeVisibility;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Model object representing a relational database Table
 * Also includes the bounds of the table so that the diagram can be 
 * restored following a save, although ideally this should be
 * in a separate diagram specific model hierarchy
 * @author Serge Rieder
 */
public class ERDEntity extends ERDObject<DBSEntity>
{
	private List<ERDEntityAttribute> columns;

	private List<ERDAssociation> primaryKeyRelationships;
	private List<ERDAssociation> foreignKeyRelationships;
    private List<DBSEntityAssociation> unresolvedKeys;

    private boolean primary = false;

    public ERDEntity(DBSEntity dbsTable) {
        super(dbsTable);
    }

    public void addColumn(ERDEntityAttribute column, boolean reflect)
	{
        if (columns == null) {
            columns = new ArrayList<>();
        }
		if (columns.contains(column))
		{
			throw new IllegalArgumentException("Column already present");
		}
		columns.add(column);
        if (reflect) {
		    firePropertyChange(CHILD, null, column);
        }
	}

	public void removeColumn(ERDEntityAttribute column, boolean reflect)
	{
		columns.remove(column);
        if (reflect) {
		    firePropertyChange(CHILD, column, null);
        }
	}

	public void switchColumn(ERDEntityAttribute column, int index, boolean reflect)
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
        if (foreignKeyRelationships == null) {
            foreignKeyRelationships = new ArrayList<>();
        }
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
        if (primaryKeyRelationships == null) {
            primaryKeyRelationships = new ArrayList<>();
        }
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

	public List<ERDEntityAttribute> getColumns()
	{
		return CommonUtils.safeList(columns);
	}

	/**
	 * @return Returns the foreignKeyRelationships.
	 */
	public List<ERDAssociation> getForeignKeyRelationships()
	{
		return CommonUtils.safeList(foreignKeyRelationships);
	}

	/**
	 * @return Returns the primaryKeyRelationships.
	 */
	public List<ERDAssociation> getPrimaryKeyRelationships()
	{
		return CommonUtils.safeList(primaryKeyRelationships);
	}

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean hasSelfLinks()
    {
        if (foreignKeyRelationships != null) {
            for (ERDAssociation association : foreignKeyRelationships) {
                if (association.getPrimaryKeyEntity() == this) {
                    return true;
                }
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
		if (o == null || !(o instanceof ERDEntity))
			return false;
        return object.equals(((ERDEntity)o).object);
	}

    public static ERDEntity fromObject(DBRProgressMonitor monitor, EntityDiagram diagram, DBSEntity entity)
    {
        ERDEntity erdEntity = new ERDEntity(entity);
        ERDAttributeVisibility attributeVisibility = diagram.getAttributeVisibility();
        if (attributeVisibility != ERDAttributeVisibility.NONE) {
            Set<DBSEntityAttribute> keyColumns = null;
            if (attributeVisibility == ERDAttributeVisibility.KEYS) {
                keyColumns = new HashSet<>();
                try {
                    for (DBSEntityAssociation assoc : CommonUtils.safeCollection(entity.getAssociations(monitor))) {
                        if (assoc instanceof DBSEntityReferrer) {
                            keyColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) assoc));
                        }
                    }
                    for (DBSEntityConstraint constraint : CommonUtils.safeCollection(entity.getConstraints(monitor))) {
                        if (constraint instanceof DBSEntityReferrer) {
                            keyColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) constraint));
                        }
                    }
                } catch (DBException e) {
                    log.warn(e);
                }
            }
            Collection<? extends DBSEntityAttribute> idColumns = null;
            try {
                idColumns = DBUtils.getBestTableIdentifier(monitor, entity);
                if (keyColumns != null) {
                    keyColumns.addAll(idColumns);
                }
            } catch (DBException e) {
                log.error("Error reading table identifier", e);
            }
            try {

                Collection<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
                if (!CommonUtils.isEmpty(attributes)) {
                    for (DBSEntityAttribute attribute : attributes) {
                        if (attribute instanceof DBSEntityAssociation) {
                            // skip attributes which are associations
                            // usual thing in some systems like WMI/CIM model
                            continue;
                        }
                        if (attribute instanceof DBPHiddenObject && ((DBPHiddenObject) attribute).isHidden()) {
                            // Skip hidden attributes
                            continue;
                        }
                        switch (attributeVisibility) {
                            case PRIMARY:
                                if (idColumns == null || !idColumns.contains(attribute)) {
                                    continue;
                                }
                                break;
                            case KEYS:
                                if (keyColumns == null || !keyColumns.contains(attribute)) {
                                    continue;
                                }
                                break;
                            default:
                                break;
                        }
                        ERDEntityAttribute c1 = new ERDEntityAttribute(attribute, idColumns != null && idColumns.contains(attribute));
                        erdEntity.addColumn(c1, false);
                    }
                }
            } catch (DBException e) {
                // just skip this problematic columns
                log.debug("Can't load table '" + entity.getName() + "'columns", e);
            }
        }
        return erdEntity;
    }

    public void addRelations(DBRProgressMonitor monitor, Map<DBSEntity, ERDEntity> tableMap, boolean reflect)
    {
        try {
            Set<DBSEntityAttribute> fkColumns = new HashSet<>();
            // Make associations
            Collection<? extends DBSEntityAssociation> fks = getObject().getAssociations(monitor);
            if (fks != null) {
                for (DBSEntityAssociation fk : fks) {
                    if (fk instanceof DBSEntityReferrer) {
                        fkColumns.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) fk));
                    }
                    ERDEntity entity2 = tableMap.get(fk.getAssociatedEntity());
                    if (entity2 == null) {
                        //log.debug("Table '" + fk.getReferencedKey().getTable().getFullQualifiedName() + "' not found in ERD");
                        if (unresolvedKeys == null) {
                            unresolvedKeys = new ArrayList<>();
                        }
                        unresolvedKeys.add(fk);
                    } else {
                        //if (table1 != entity2) {
                        new ERDAssociation(fk, entity2, this, reflect);
                        //}
                    }
                }
            }

            // Mark column's fk flag
            for (ERDEntityAttribute column : this.getColumns()) {
                if (fkColumns.contains(column.getObject())) {
                    column.setInForeignKey(true);
                }
            }

        } catch (DBException e) {
            log.warn("Can't load table '" + getObject().getName() + "' foreign keys", e);
        }
    }

    public void resolveRelations(Map<DBSEntity, ERDEntity> tableMap, boolean reflect)
    {
        if (CommonUtils.isEmpty(unresolvedKeys)) {
            return;
        }
        for (Iterator<DBSEntityAssociation> iter = unresolvedKeys.iterator(); iter.hasNext(); ) {
            final DBSEntityAssociation fk = iter.next();
            ERDEntity refEntity = tableMap.get(fk.getReferencedConstraint().getParentObject());
            if (refEntity != null) {
                new ERDAssociation(fk, refEntity, this, reflect);
                iter.remove();
            }
        }
    }

    @NotNull
    @Override
    public String getName()
    {
        return getObject().getName();
    }
}