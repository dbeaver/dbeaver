/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Model object representing a relational database Table
 * Also includes the bounds of the table so that the diagram can be
 * restored following a serializeDiagram, although ideally this should be
 * in a separate diagram specific model hierarchy
 */
public class ERDEntity extends ERDElement<DBSEntity> {

    static final Log log = Log.getLog(ERDEntity.class);

    private DBPDataSource dataSource;
    private String alias;
    private List<ERDEntityAttribute> attributes;

    private List<DBSEntityAssociation> unresolvedKeys;

    private boolean primary = false;
    private ERDAttributeVisibility attributeVisibility;

    /**
     * Special constructor for creating lazy entities.
     * This entity will be initialized at the moment of creation within diagram.
     */
    public ERDEntity(DBPDataSource dataSource) {
        super();
        this.dataSource = dataSource;
    }

    public ERDEntity(DBSEntity entity) {
        super(entity);
    }

    public DBPDataSource getDataSource() {
        return dataSource != null ? dataSource : getObject().getDataSource();
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ERDAttributeVisibility getAttributeVisibility() {
        return attributeVisibility;
    }

    public void setAttributeVisibility(ERDAttributeVisibility attributeVisibility) {
        this.attributeVisibility = attributeVisibility;
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
            firePropertyChange(PROP_CHILD, null, attribute);
        }
    }

    public void removeAttribute(ERDEntityAttribute attribute, boolean reflect) {
        attributes.remove(attribute);
        if (reflect) {
            firePropertyChange(PROP_CHILD, attribute, null);
        }
    }

    public void sortAttributes(@NotNull Comparator<ERDEntityAttribute> comparator, boolean reflect) {
        if (CommonUtils.isEmpty(attributes)) {
            return;
        }
        attributes.sort(comparator);
        if (reflect) {
            firePropertyChange(PROP_REORDER, this, null);
        }
    }

    public void switchAttribute(ERDEntityAttribute attribute, int index, boolean reflect) {
        attributes.remove(attribute);
        attributes.add(index, attribute);
        if (reflect) {
            firePropertyChange(PROP_REORDER, this, attribute);
        }
    }

    @NotNull
    public List<ERDEntityAttribute> getAttributes() {
        return CommonUtils.safeList(attributes);
    }

    public ERDEntityAttribute getAttribute(DBSEntityAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        if (attributes == null) {
            return null;
        }
        for (ERDEntityAttribute attr : attributes) {
            if (attr.getObject() == attribute) {
                return attr;
            }
        }
        return null;
    }

    public ERDAssociation getAssociation(DBSEntityAssociation association) {
        if (association == null) {
            return null;
        }
        for (ERDAssociation assoc : getAssociations()) {
            if (assoc.getObject() == association) {
                return assoc;
            }
        }
        return null;
    }

    public ERDAssociation getReferenceAssociation(DBSEntityAssociation association) {
        if (association == null) {
            return null;
        }
        for (ERDAssociation assoc : getReferences()) {
            if (assoc.getObject() == association) {
                return assoc;
            }
        }
        return null;
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

    public void reloadAttributes(ERDDiagram diagram) {
        if (!CommonUtils.isEmpty(attributes)) {
            attributes.clear();
        }
        try {
            diagram.getContentProvider().fillEntityFromObject(new VoidProgressMonitor(), diagram, Collections.emptyList(), this);
        } catch (DBCException e) {
            log.debug("Can't reload attributes", e);
        }
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * Resolve and create entity associations.
     * Also caches all unresolved associations (associations with entities which are not present in diagram yet)
     *
     * @param diagram all diagram entities map
     * @param create  if true then creates all found model association. Otherwise only saves unresolved ones.
     * @param reflect reflect UI
     * @throws DBException - exception
     */
    public void addModelRelations(DBRProgressMonitor monitor, ERDContainer diagram, boolean create, boolean reflect) throws DBException {
        Set<DBSEntityAttribute> fkAttrs = new HashSet<>();
        // Make associations
        Collection<? extends DBSEntityAssociation> fks = DBVUtils.getAllAssociations(monitor, getObject());
        if (fks != null) {
            for (DBSEntityAssociation fk : fks) {
                if (fk instanceof DBSEntityReferrer) {
                    fkAttrs.addAll(DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) fk));
                }
                ERDEntity targetEntity = diagram.getEntityMap().get(DBVUtils.getRealEntity(monitor, fk.getAssociatedEntity()));
                if (targetEntity == null) {
                    if (unresolvedKeys == null) {
                        unresolvedKeys = new ArrayList<>();
                    }
                    unresolvedKeys.add(fk);
                } else {
                    if (create) {
                        if (DBUtils.isInheritedObject(fk)) {
                            continue;
                        }
                        diagram.getContentProvider().createAutoAssociation(diagram, fk, this, targetEntity, reflect);
                    }
                }
            }
        }

        // Mark attribute's fk flag
        if (!fkAttrs.isEmpty()) {
            for (ERDEntityAttribute attribute : this.getAttributes()) {
                if (fkAttrs.contains(attribute.getObject())) {
                    attribute.setInForeignKey(true);
                }
            }
        }
    }

    public void resolveRelations(ERDContainer diagram, boolean reflect) {
        if (CommonUtils.isEmpty(unresolvedKeys)) {
            return;
        }
        for (Iterator<DBSEntityAssociation> iter = unresolvedKeys.iterator(); iter.hasNext(); ) {
            final DBSEntityAssociation fk = iter.next();
            if (fk.getReferencedConstraint() != null) {
                ERDEntity refEntity = diagram.getEntityMap().get(
                    DBVUtils.tryGetRealEntity(fk.getReferencedConstraint().getParentObject()));
                if (refEntity != null) {
                    ERDAssociation erdAssociation = diagram.getContentProvider().createAutoAssociation(diagram, fk, this, refEntity, reflect);
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
    public void fromMap(@NotNull ERDContext context, Map<String, Object> map) {
        alias = JSONUtils.getString(map, "alias");

        try {
            for (Map<String, Object> attrMap : JSONUtils.getObjectList(map, "attributes")) {
                String name = JSONUtils.getString(attrMap, "name");
                if (CommonUtils.isEmpty(name)) {
                    continue;
                }
                DBSEntityAttribute attribute = getObject().getAttribute(context.getMonitor(), name);
                if (attribute == null) {
                    log.error("Attribute '" + name + "' not found in entity " + getName());
                    continue;
                }
                ERDEntityAttribute attr = new ERDEntityAttribute(attribute, false);
                attr.fromMap(context, attrMap);
                addAttribute(attr, false);
            }
        } catch (DBException e) {
            log.error("Error reading entity attributes", e);
        }
    }

    @Override
    public Map<String, Object> toMap(@NotNull ERDContext context, boolean fullInfo) {
        DBSEntity dbsEntity = getObject();

        Map<String, Object> entityMap = new LinkedHashMap<>();
        entityMap.put("id", context.addElementInfo(this));
        DBNDatabaseNode node = context.getNavigatorModel().getNodeByObject(context.getMonitor(), dbsEntity, true);
        if (node != null) {
            //TODO: use only node URI after finish migration to the new node path format
            entityMap.put("nodeId", node.getNodeItemPath());
            entityMap.put("nodeUri", node.getNodeUri());
        }
        entityMap.put("name", this.getName());
        if (!CommonUtils.isEmpty(this.getAlias())) {
            entityMap.put("alias", this.getAlias());
        }
        if (dbsEntity != null && fullInfo) {
            if (dbsEntity instanceof DBPQualifiedObject) {
                entityMap.put("fqn", ((DBPQualifiedObject) dbsEntity).getFullyQualifiedName(DBPEvaluationContext.UI));
            }
        }

        if (fullInfo) {
            entityMap.put("iconIndex", context.getIconIndex(DBValueFormatting.getObjectImage(dbsEntity)));
        }

        entityMap.put("attributes", this.getAttributes().stream().map(a -> a.toMap(context, fullInfo)).collect(Collectors.toList()));

        return entityMap;
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

}
