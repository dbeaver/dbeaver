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

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeVisibility;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.*;

/**
 * Represents a Schema in the model. Note that this class also includes
 * diagram specific information (layoutManualDesired and layoutManualAllowed fields)
 * although ideally these should be in a separate model hierarchy
 * @author Serge Rieder
 */
public class EntityDiagram extends ERDObject<DBSObject>
{
	private String name;
	private List<ERDEntity> entities = new ArrayList<ERDEntity>();
	private boolean layoutManualDesired = true;
	private boolean layoutManualAllowed = false;
    private Map<DBSEntity, ERDEntity> tableMap = new IdentityHashMap<DBSEntity, ERDEntity>();
    private Map<ERDObject, Rectangle> initBounds = new IdentityHashMap<ERDObject, Rectangle>();
    private List<ERDNote> notes = new ArrayList<ERDNote>();
    private boolean needsAutoLayout;
    private ERDAttributeVisibility attributeVisibility = ERDAttributeVisibility.PRIMARY;

    private List<String> errorMessages = new ArrayList<String>();

    public EntityDiagram(DBSObject container, String name)
	{
		super(container);
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		this.name = name;
        this.attributeVisibility = ERDAttributeVisibility.getDefaultVisibility(Activator.getDefault().getPreferenceStore());
	}


    public ERDAttributeVisibility getAttributeVisibility()
    {
        return attributeVisibility;
    }

    public void setAttributeVisibility(ERDAttributeVisibility attributeVisibility)
    {
        this.attributeVisibility = attributeVisibility;
        ERDAttributeVisibility.setDefaultVisibility(Activator.getDefault().getPreferenceStore(), attributeVisibility);
    }

	public synchronized void addTable(ERDEntity entity, boolean reflect)
	{
        addTable(entity, -1, reflect);
	}

	public synchronized void addTable(ERDEntity entity, int i, boolean reflect)
	{
        if (i < 0) {
            entities.add(entity);
        } else {
		    entities.add(i, entity);
        }
        tableMap.put(entity.getObject(), entity);

        if (reflect) {
		    firePropertyChange(CHILD, null, entity);
/*
            for (ERDAssociation rel : entity.getPrimaryKeyRelationships()) {
                entity.firePropertyChange(INPUT, null, rel);
            }
            for (ERDAssociation rel : entity.getForeignKeyRelationships()) {
                entity.firePropertyChange(OUTPUT, null, rel);
            }
*/
        }

        resolveRelations(reflect);

        if (reflect) {
            for (ERDAssociation rel : entity.getPrimaryKeyRelationships()) {
                rel.getForeignKeyEntity().firePropertyChange(OUTPUT, null, rel);
            }
        }
	}

    private void resolveRelations(boolean reflect)
    {
        // Resolve incomplete relations
        for (ERDEntity erdEntity : getEntities()) {
            erdEntity.resolveRelations(tableMap, reflect);
        }
    }

	public synchronized void removeTable(ERDEntity entity, boolean reflect)
	{
        tableMap.remove(entity.getObject());
		entities.remove(entity);
        if (reflect) {
		    firePropertyChange(CHILD, entity, null);
        }
	}

    /**
	 * @return the Tables for the current schema
	 */
	public synchronized List<ERDEntity> getEntities()
	{
		return entities;
	}

    public synchronized List<ERDNote> getNotes()
    {
        return notes;
    }

    public synchronized void addNote(ERDNote note, boolean reflect)
    {
        notes.add(note);

        if (reflect) {
            firePropertyChange(CHILD, null, note);
        }
    }

    public synchronized void removeNote(ERDNote note, boolean reflect)
    {
        notes.remove(note);

        if (reflect) {
            firePropertyChange(CHILD, note, null);
        }
    }

    /**
	 * @return the name of the schema
	 */
	@Override
    public String getName()
	{
		return name;
	}

    public void setName(String name)
    {
        this.name = name;
    }

	/**
	 * @param layoutManualAllowed
	 *            The layoutManualAllowed to set.
	 */
	public void setLayoutManualAllowed(boolean layoutManualAllowed)
	{
		this.layoutManualAllowed = layoutManualAllowed;
	}

	/**
	 * @return Returns the layoutManualDesired.
	 */
	public boolean isLayoutManualDesired()
	{
		return layoutManualDesired;
	}

	/**
	 * @param layoutManualDesired
	 *            The layoutManualDesired to set.
	 */
	public void setLayoutManualDesired(boolean layoutManualDesired)
	{
		this.layoutManualDesired = layoutManualDesired;
	}

	/**
	 * @return Returns whether we can lay out individual entities manually using the XYLayout
	 */
	public boolean isLayoutManualAllowed()
	{
		return layoutManualAllowed;
	}

    public int getEntityCount() {
        return entities.size();
    }

    public EntityDiagram copy()
    {
        EntityDiagram copy = new EntityDiagram(getObject(), getName());
        copy.entities.addAll(this.entities);
        copy.tableMap.putAll(this.tableMap);
        copy.layoutManualDesired = this.layoutManualDesired;
        copy.layoutManualAllowed = this.layoutManualAllowed;
        copy.initBounds = initBounds;
        return copy;
    }

    public void fillTables(DBRProgressMonitor monitor, Collection<DBSEntity> tables, DBSObject dbObject)
    {
        // Load entities
        monitor.beginTask("Load entities metadata", tables.size());
        for (DBSEntity table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask("Load " + table.getName());
            ERDEntity erdEntity = ERDEntity.fromObject(monitor, this, table);
            erdEntity.setPrimary(table == dbObject);

            addTable(erdEntity, false);
            tableMap.put(table, erdEntity);

            monitor.worked(1);
        }

        monitor.done();

        // Load relations
        monitor.beginTask("Load entities' relations", tables.size());
        for (DBSEntity table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask("Load " + table.getName());
            final ERDEntity erdEntity = tableMap.get(table);
            if (erdEntity != null) {
                erdEntity.addRelations(monitor, tableMap, false);
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    public boolean containsTable(DBSEntity table)
    {
        for (ERDEntity erdEntity : entities) {
            if (erdEntity.getObject() == table) {
                return true;
            }
        }
        return false;
    }

    public Map<DBSEntity,ERDEntity> getTableMap()
    {
        return tableMap;
    }

    public ERDEntity getERDTable(DBSEntity table)
    {
        return tableMap.get(table);
    }

    public void clear()
    {
        this.entities.clear();
        this.tableMap.clear();
        this.initBounds.clear();
    }

    public Rectangle getInitBounds(ERDObject erdObject)
    {
        return initBounds.get(erdObject);
    }

    public void addInitBounds(ERDObject erdTable, Rectangle bounds)
    {
        initBounds.put(erdTable, bounds);
    }

    public boolean isNeedsAutoLayout()
    {
        return needsAutoLayout;
    }

    public void setNeedsAutoLayout(boolean needsAutoLayout)
    {
        this.needsAutoLayout = needsAutoLayout;
    }

    public void addInitRelationBends(ERDEntity sourceEntity, ERDEntity targetEntity, String relName, List<Point> bends)
    {
        for (ERDAssociation rel : sourceEntity.getPrimaryKeyRelationships()) {
            if (rel.getForeignKeyEntity() == targetEntity && relName.equals(rel.getObject().getName())) {
                rel.setInitBends(bends);
            }
        }
    }

    public List<ERDObject> getContents()
    {
        List<ERDObject> children = new ArrayList<ERDObject>(entities.size() + notes.size());
        children.addAll(entities);
        children.addAll(notes);
        return children;
    }

    public List<String> getErrorMessages()
    {
        return errorMessages;
    }

    public void addErrorMessage(String message)
    {
        errorMessages.add(message);
    }
}