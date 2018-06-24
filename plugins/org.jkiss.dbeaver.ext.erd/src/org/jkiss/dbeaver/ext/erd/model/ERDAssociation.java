/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.draw2d.geometry.Point;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;

import java.util.List;

/**
 * Relates one table to another
 * 
 * @author Serge Rider
 */
public class ERDAssociation extends ERDObject<DBSEntityAssociation>
{
    private static final Log log = Log.getLog(ERDAssociation.class);

    private ERDEntity targetEntity;
	private ERDEntity sourceEntity;
    private List<ERDEntityAttribute> primaryAttributes;
    private List<ERDEntityAttribute> foreignAttributes;

    private List<Point> initBends;
    private Boolean identifying;

    /**
     * Constructor for logical association
     * @param sourceEntity fk table
     * @param targetEntity pk table
     * @param reflect reflect flag
     */
    public ERDAssociation(ERDEntity sourceEntity, ERDEntity targetEntity, boolean reflect)
    {
        super(new ERDLogicalAssociation(
            sourceEntity,
            sourceEntity.getObject().getName() + " -> " + targetEntity.getObject().getName(),
            "",
            new ERDLogicalPrimaryKey(targetEntity, "Primary key", "")));
        this.targetEntity = targetEntity;
        this.sourceEntity = sourceEntity;
        this.targetEntity.addPrimaryKeyRelationship(this, reflect);
        this.sourceEntity.addForeignKeyRelationship(this, reflect);
    }

    /**
     * Constructor for physical association
     * @param object physical FK
     * @param sourceEntity fk table
     * @param targetEntity pk table
     * @param reflect reflect flag
     */
	public ERDAssociation(DBSEntityAssociation object, ERDEntity sourceEntity, ERDEntity targetEntity, boolean reflect)
	{
		super(object);
		this.targetEntity = targetEntity;
		this.sourceEntity = sourceEntity;
        this.targetEntity.addPrimaryKeyRelationship(this, reflect);
        this.sourceEntity.addForeignKeyRelationship(this, reflect);
	}

    public boolean isLogical()
    {
        return getObject() instanceof ERDLogicalAssociation;
    }

	/**
	 * @return Returns the sourceEntity.
	 */
	public ERDEntity getSourceEntity()
	{
		return sourceEntity;
	}

	/**
	 * @return Returns the targetEntity.
	 */
	public ERDEntity getTargetEntity()
	{
		return targetEntity;
	}

	public void setTargetEntity(ERDEntity targetPrimaryKey)
	{
		this.targetEntity = targetPrimaryKey;
	}

	/**
	 * @param sourceForeignKey the foreign key table you are connecting from
	 */
	public void setSourceEntity(ERDEntity sourceForeignKey)
	{
		this.sourceEntity = sourceForeignKey;
	}

    public List<Point> getInitBends()
    {
        return initBends;
    }

    public void setInitBends(List<Point> bends)
    {
        this.initBends = bends;
    }

    public boolean isIdentifying() {
        if (identifying == null) {
            identifying = false;
            try {
                identifying = DBUtils.isIdentifyingAssociation(new VoidProgressMonitor(), getObject());
            } catch (DBException e) {
                log.debug(e);
                identifying = false;
            }
        }
        return identifying;
    }

    @Override
    public String toString()
    {
        return getObject() + " [" + targetEntity + "->" + sourceEntity + "]";
    }

    @NotNull
    @Override
    public String getName()
    {
        return getObject().getName();
    }
}