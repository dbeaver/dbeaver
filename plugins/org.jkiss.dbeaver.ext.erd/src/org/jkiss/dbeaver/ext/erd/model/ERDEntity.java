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
import org.jkiss.dbeaver.model.DBPDataSource;
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

    private DBPDataSource dataSource;
    private String alias;
    private List<ERDEntityAttribute> attributes;

    private List<ERDAssociation> references;
    private List<ERDAssociation> associations;
    private List<DBSEntityAssociation> unresolvedKeys;

    private boolean primary = false;

    /**
     * Special constructore for creating lazy entities.
     * This entity will be initialized at the moment of creation within diagram.
     */
    public ERDEntity(DBPDataSource dataSource) {
        super(null);
        this.dataSource = dataSource;
    }

    public ERDEntity(DBSEntity entity) {
        super(entity);
    }

    public DBPDataSource getDataSource() {
        return dataSource;
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
    public void addAssociation(ERDAssociation rel, boolean reflect) {
        if (associations == null) {
            associations = new ArrayList<>();
        }
        associations.add(rel);
        if (reflect) {
            firePropertyChange(OUTPUT, null, rel);
        }
    }

    /**
     * Adds relationship where the current object is the primary key table in a relationship
     *
     * @param table the foreign key relationship
     */
    public void addReferenceAssociation(ERDAssociation table, boolean reflect) {
        if (references == null) {
            references = new ArrayList<>();
        }
        references.add(table);
        if (reflect) {
            firePropertyChange(INPUT, null, table);
        }
    }

    /**
     * Removes relationship where the current object is the foreign key table in a relationship
     *
     * @param table the primary key relationship
     */
    public void removeAssociation(ERDAssociation table, boolean reflect) {
        associations.remove(table);
        if (reflect) {
            firePropertyChange(OUTPUT, table, null);
        }
    }

    /**
     * Removes relationship where the current object is the primary key table in a relationship
     *
     * @param table the foreign key relationship
     */
    public void removeReferenceAssociation(ERDAssociation table, boolean reflect) {
        references.remove(table);
        if (reflect) {
            firePropertyChange(INPUT, table, null);
        }
    }

    @NotNull
    public List<ERDEntityAttribute> getAttributes() {
        return CommonUtils.safeList(attributes);
    }

    @NotNull
    public List<ERDEntityAttribute> getCheckedAttributes() {
        List<ERDEntityAttribute> result = new ArrayList<>();
        if (attributes != null) {
            for (ERDEntityAttribute attr : attributes) {
                if (attr.isChecked()) {
                    result.add(attr);
                }
            }
        }
        return result;
    }

    /**
     * @return Returns the associations.
     */
    @NotNull
    public List<ERDAssociation> getAssociations() {
        return CommonUtils.safeList(associations);
    }

    /**
     * @return Returns the references.
     */
    @NotNull
    public List<ERDAssociation> getReferences() {
        return CommonUtils.safeList(references);
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean hasSelfLinks() {
        if (associations != null) {
            for (ERDAssociation association : associations) {
                if (association.getTargetEntity() == this) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolve and create entity associations.
     * Also caches all unresolved associations (associations with entities which are not present in diagram yet)
     * @param diagram all diagram entities map
     * @param create    if true then creates all found model association. Otherwise only saves unresolved ones.
     * @param reflect   reflect UI
     */
    public void addModelRelations(DBRProgressMonitor monitor, ERDContainer diagram, boolean create, boolean reflect) {
        try {
            Set<DBSEntityAttribute> fkAttrs = new HashSet<>();
            // Make associations
            Collection<? extends DBSEntityAssociation> fks = getObject().getAssociations(monitor);
            if (fks != null) {
                for (DBSEntityAssociation fk : fks) {
                    if (fk instanceof DBSEntityReferrer) {
                        fkAttrs.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) fk));
                    }
                    ERDEntity entity2 = diagram.getEntityMap().get(fk.getAssociatedEntity());
                    if (entity2 == null) {
                        //log.debug("Table '" + fk.getReferencedKey().getTable().getFullyQualifiedName() + "' not found in ERD");
                        if (unresolvedKeys == null) {
                            unresolvedKeys = new ArrayList<>();
                        }
                        unresolvedKeys.add(fk);
                    } else {
                        if (create) {
                            if (DBUtils.isInheritedObject(fk)) {
                                continue;
                            }
                            diagram.getDecorator().createAutoAssociation(diagram, fk, this, entity2, reflect);
                        }
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

    public void resolveRelations(ERDContainer diagram, boolean reflect) {
        if (CommonUtils.isEmpty(unresolvedKeys)) {
            return;
        }
        for (Iterator<DBSEntityAssociation> iter = unresolvedKeys.iterator(); iter.hasNext(); ) {
            final DBSEntityAssociation fk = iter.next();
            if (fk.getReferencedConstraint() != null) {
                ERDEntity refEntity = diagram.getEntityMap().get(fk.getReferencedConstraint().getParentObject());
                if (refEntity != null) {
                    ERDAssociation erdAssociation = diagram.getDecorator().createAutoAssociation(diagram, fk, this, refEntity, reflect);
                    if (erdAssociation != null) {
                        iter.remove();
                    }
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
        return getName() + (CommonUtils.isEmpty(alias) ? "" : " " + alias);
    }

    @Override
    public int hashCode() {
        int aliasHC = alias == null ? 0 : alias.hashCode();
        return (object == null ? 0 : object.hashCode()) + aliasHC;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ERDEntity) {
            return CommonUtils.equalObjects(object, ((ERDEntity) o).object) &&
                CommonUtils.equalObjects(alias, ((ERDEntity) o).alias);
        }
        return false;
    }

    public boolean hasAssociationsWith(ERDEntity entity) {
        if (associations != null) {
            for (ERDAssociation assoc : associations) {
                if (assoc.getTargetEntity() == entity) {
                    return true;
                }
            }
        }
        return false;
    }
}
