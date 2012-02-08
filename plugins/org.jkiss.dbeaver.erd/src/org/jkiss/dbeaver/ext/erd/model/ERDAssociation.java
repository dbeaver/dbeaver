/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

	private ERDEntity primaryKeyEntity;
	private ERDEntity foreignKeyEntity;
    private List<Point> initBends;

    /**
     * Constructor for logical association
     * @param foreignEntity fk table
     * @param primaryEntity pk table
     * @param reflect reflect flag
     */
    public ERDAssociation(ERDEntity foreignEntity, ERDEntity primaryEntity, boolean reflect)
    {
        super(new ERDLogicalForeignKey(
            foreignEntity,
            foreignEntity.getObject().getName() + " -> " + primaryEntity.getObject().getName(),
            "",
            new ERDLogicalPrimaryKey(primaryEntity, "Primary key", "")));
        this.primaryKeyEntity = primaryEntity;
        this.foreignKeyEntity = foreignEntity;
        this.primaryKeyEntity.addPrimaryKeyRelationship(this, reflect);
        this.foreignKeyEntity.addForeignKeyRelationship(this, reflect);
    }

    /**
     * Constructor for physical association
     * @param object physical FK
     * @param foreignEntity fk table
     * @param primaryEntity pk table
     * @param reflect reflect flag
     */
	public ERDAssociation(DBSEntityAssociation object, ERDEntity foreignEntity, ERDEntity primaryEntity, boolean reflect)
	{
		super(object);
		this.primaryKeyEntity = primaryEntity;
		this.foreignKeyEntity = foreignEntity;
        this.primaryKeyEntity.addPrimaryKeyRelationship(this, reflect);
        this.foreignKeyEntity.addForeignKeyRelationship(this, reflect);
	}

    public boolean isLogical()
    {
        return getObject() instanceof ERDLogicalForeignKey;
    }

	/**
	 * @return Returns the foreignKeyEntity.
	 */
	public ERDEntity getForeignKeyEntity()
	{
		return foreignKeyEntity;
	}

	/**
	 * @return Returns the primaryKeyEntity.
	 */
	public ERDEntity getPrimaryKeyEntity()
	{
		return primaryKeyEntity;
	}

	public void setPrimaryKeyEntity(ERDEntity targetPrimaryKey)
	{
		this.primaryKeyEntity = targetPrimaryKey;
	}

	/**
	 * @param sourceForeignKey the foreign key table you are connecting from
	 */
	public void setForeignKeyEntity(ERDEntity sourceForeignKey)
	{
		this.foreignKeyEntity = sourceForeignKey;
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
        return getObject() + " [" + primaryKeyEntity + "->" + foreignKeyEntity + "]";
    }

    public String getName()
    {
        return getObject().getName();
    }
}