/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 13, 2004
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

    private ERDEntity primaryKeyEntity;
	private ERDEntity foreignKeyEntity;
    private List<Point> initBends;
    private Boolean identifying;

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

    public boolean isIdentifying() {
        if (identifying == null) {
            identifying = false;
            try {
                identifying = DBUtils.isIdentifyingAssociation(new VoidProgressMonitor(), getObject());
            } catch (DBException e) {
                log.debug(e);
            }
        }
        return identifying;
    }

    @Override
    public String toString()
    {
        return getObject() + " [" + primaryKeyEntity + "->" + foreignKeyEntity + "]";
    }

    @NotNull
    @Override
    public String getName()
    {
        return getObject().getName();
    }
}