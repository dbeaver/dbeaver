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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
 */
public class ERDEntity extends ERDObject<DBSEntity> {

    static final Log log = Log.getLog(ERDEntity.class);

    private String alias;
    private List<ERDEntityAttribute> attributes;

    private List<ERDAssociation> primaryKeyRelationships;
    private List<ERDAssociation> foreignKeyRelationships;
    private List<DBSEntityAssociation> unresolvedKeys;

    private boolean primary = false;

    public ERDEntity(DBSEntity entity) {
        super(entity);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void addAttribute(ERDEntityAttribute attribute, boolean reflect) {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        if (attributes.contains(attribute)) {
            throw new IllegalArgumentException("Attribute already present");
        }
        attributes.add(attribute);
        if (reflect) {
            firePropertyChange(CHILD, null, attribute);
        }
    }

    public void removeAttribute(ERDEntityAttribute attribute, boolean reflect) {
        attributes.remove(attribute);
        if (reflect) {
            firePropertyChange(CHILD, attribute, null);
        }
    }

    public void switchAttribute(ERDEntityAttribute attribute, int index, boolean reflect) {
        attributes.remove(attribute);
        attributes.add(index, attribute);
        if (reflect) {
            firePropertyChange(REORDER, this, attribute);
        }
    }

    /**
     * Adds relationship where the current object is the foreign key table in a relationship
     *
     * @param rel the primary key relationship
     */
    public void addForeignKeyRelationship(ERDAssociation rel, boolean reflect) {
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
     * @param table the foreign key relationship
     */
    public void addPrimaryKeyRelationship(ERDAssociation table, boolean reflect) {
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
     * @param table the primary key relationship
     */
    public void removeForeignKeyRelationship(ERDAssociation table, boolean reflect) {
        foreignKeyRelationships.remove(table);
        if (reflect) {
            firePropertyChange(OUTPUT, table, null);
        }
    }

    /**
     * Removes relationship where the current object is the primary key table in a relationship
     *
     * @param table the foreign key relationship
     */
    public void removePrimaryKeyRelationship(ERDAssociation table, boolean reflect) {
        primaryKeyRelationships.remove(table);
        if (reflect) {
            firePropertyChange(INPUT, table, null);
        }
    }

    public List<ERDEntityAttribute> getAttributes() {
        return CommonUtils.safeList(attributes);
    }

    /**
     * @return Returns the foreignKeyRelationships.
     */
    public List<ERDAssociation> getForeignKeyRelationships() {
        return CommonUtils.safeList(foreignKeyRelationships);
    }

    /**
     * @return Returns the primaryKeyRelationships.
     */
    public List<ERDAssociation> getPrimaryKeyRelationships() {
        return CommonUtils.safeList(primaryKeyRelationships);
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean hasSelfLinks() {
        if (foreignKeyRelationships != null) {
            for (ERDAssociation association : foreignKeyRelationships) {
                if (association.getTargetEntity() == this) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addRelations(DBRProgressMonitor monitor, Map<DBSEntity, ERDEntity> tableMap, boolean reflect) {
        try {
            Set<DBSEntityAttribute> fkAttrs = new HashSet<>();
            // Make associations
            Collection<? extends DBSEntityAssociation> fks = getObject().getAssociations(monitor);
            if (fks != null) {
                for (DBSEntityAssociation fk : fks) {
                    if (fk instanceof DBSEntityReferrer) {
                        fkAttrs.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) fk));
                    }
                    ERDEntity entity2 = tableMap.get(fk.getAssociatedEntity());
                    if (entity2 == null) {
                        //log.debug("Table '" + fk.getReferencedKey().getTable().getFullyQualifiedName() + "' not found in ERD");
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

            // Mark attribute's fk flag
            for (ERDEntityAttribute attribute : this.getAttributes()) {
                if (fkAttrs.contains(attribute.getObject())) {
                    attribute.setInForeignKey(true);
                }
            }

        } catch (DBException e) {
            log.warn("Can't load table '" + getObject().getName() + "' foreign keys", e);
        }
    }

    public void resolveRelations(Map<DBSEntity, ERDEntity> tableMap, boolean reflect) {
        if (CommonUtils.isEmpty(unresolvedKeys)) {
            return;
        }
        for (Iterator<DBSEntityAssociation> iter = unresolvedKeys.iterator(); iter.hasNext(); ) {
            final DBSEntityAssociation fk = iter.next();
            if (fk.getReferencedConstraint() != null) {
                ERDEntity refEntity = tableMap.get(fk.getReferencedConstraint().getParentObject());
                if (refEntity != null) {
                    new ERDAssociation(fk, refEntity, this, reflect);
                    iter.remove();
                }
            }
        }
    }

    @NotNull
    @Override
    public String getName() {
        return getObject().getName();
    }

    @Override
    public String toString() {
        return object.getName();
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof ERDEntity &&
            object.equals(((ERDEntity) o).object);
    }

}
