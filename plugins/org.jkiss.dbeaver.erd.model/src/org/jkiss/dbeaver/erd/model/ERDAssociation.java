/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.model;

import org.eclipse.draw2d.geometry.Point;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Relates one table to another
 * 
 * @author Serge Rider
 */
public class ERDAssociation extends ERDObject<DBSEntityAssociation>
{
    private static final Log log = Log.getLog(ERDAssociation.class);

	private ERDElement sourceEntity;
    private ERDElement targetEntity;
    private List<ERDEntityAttribute> sourceAttributes;
    private List<ERDEntityAttribute> targetAttributes;

    private List<Point> initBends;

    /**
     * Constructor for logical association
     * @param sourceEntity fk table
     * @param targetEntity pk table
     * @param reflect reflect flag
     */
    public ERDAssociation(ERDElement sourceEntity, ERDElement targetEntity, boolean reflect)
    {
        super(new ERDLogicalAssociation(
            sourceEntity,
            sourceEntity.getName() + " -> " + targetEntity.getName(),
            "",
            new ERDLogicalPrimaryKey(targetEntity, "Logical primary key", "")));
        this.targetEntity = targetEntity;
        this.sourceEntity = sourceEntity;
        this.targetEntity.addReferenceAssociation(this, reflect);
        this.sourceEntity.addAssociation(this, reflect);
    }

    /**
     * Constructor for physical association
     * @param association physical FK
     * @param sourceEntity fk table
     * @param targetEntity pk table
     * @param reflect reflect flag
     */
	public ERDAssociation(DBSEntityAssociation association, ERDEntity sourceEntity, ERDEntity targetEntity, boolean reflect)
	{
		super(association);
		this.targetEntity = targetEntity;
		this.sourceEntity = sourceEntity;

		// Resolve association attributes
        if (association instanceof DBSEntityReferrer) {
            resolveAttributes((DBSEntityReferrer) association, sourceEntity, targetEntity);
        }

        this.targetEntity.addReferenceAssociation(this, reflect);
        this.sourceEntity.addAssociation(this, reflect);
	}

    /**
     */
    protected void resolveAttributes(DBSEntityReferrer association, ERDEntity sourceEntity, ERDEntity targetEntity) {
        try {
            List<? extends DBSEntityAttributeRef> attrRefs = association.getAttributeReferences(new VoidProgressMonitor());

            if (!CommonUtils.isEmpty(attrRefs)) {
                for (DBSEntityAttributeRef attrRef : attrRefs) {
                    if (attrRef instanceof DBSTableForeignKeyColumn) {
                        DBSEntityAttribute targetAttr = ((DBSTableForeignKeyColumn) attrRef).getReferencedColumn();
                        DBSEntityAttribute sourceAttr = attrRef.getAttribute();
                        if (sourceAttr != null && targetAttr != null) {
                            ERDEntityAttribute erdSourceAttr = ERDUtils.getAttributeByModel(sourceEntity, sourceAttr);
                            ERDEntityAttribute erdTargetAttr = ERDUtils.getAttributeByModel(targetEntity, targetAttr);
                            if (erdSourceAttr != null || erdTargetAttr != null) {
                                addCondition(erdSourceAttr, erdTargetAttr);
                            }
                        }
                    }
                }
            }
        } catch (DBException e) {
            log.error("Error resolving ERD association attributes", e);
        }
    }

    public boolean isLogical()
    {
        return getObject() instanceof ERDLogicalAssociation;
    }

	/**
	 * @return Returns the sourceEntity.
	 */
	public ERDElement getSourceEntity()
	{
		return sourceEntity;
	}

	/**
	 * @return Returns the targetEntity.
	 */
	public ERDElement getTargetEntity()
	{
		return targetEntity;
	}

	public void setTargetEntity(ERDElement targetPrimaryKey)
	{
		this.targetEntity = targetPrimaryKey;
	}

	/**
	 * @param sourceForeignKey the foreign key table you are connecting from
	 */
	public void setSourceEntity(ERDElement sourceForeignKey)
	{
		this.sourceEntity = sourceForeignKey;
	}

	@NotNull
    public List<ERDEntityAttribute> getSourceAttributes() {
        return sourceAttributes == null ? Collections.emptyList() : sourceAttributes;
    }

    @NotNull
    public List<ERDEntityAttribute> getTargetAttributes() {
        return targetAttributes == null ? Collections.emptyList() : targetAttributes;
    }

    public void addCondition(@Nullable ERDEntityAttribute sourceAttribute, @Nullable ERDEntityAttribute targetAttribute) {
	    if (sourceAttribute != null) {
            if (sourceAttributes == null) {
                sourceAttributes = new ArrayList<>();
            }
            sourceAttributes.add(sourceAttribute);
        }

        if (targetAttribute != null) {
            if (targetAttributes == null) {
                targetAttributes = new ArrayList<>();
            }
            targetAttributes.add(targetAttribute);
        }
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
        return getObject() + " [" + sourceEntity + "->" + targetEntity + "]";
    }

    @NotNull
    @Override
    public String getName()
    {
        return getObject().getName();
    }

}