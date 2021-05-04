/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Model object representing a relational entity
 * Also includes the bounds of the table so that the diagram can be
 * restored following a serializeDiagram, although ideally this should be
 * in a separate diagram specific model hierarchy
 */
public abstract class ERDElement<OBJECT> extends ERDObject<OBJECT> {

    static final Log log = Log.getLog(ERDElement.class);

    private List<ERDAssociation> references;
    private List<ERDAssociation> associations;

    /**
     * Special constructore for creating lazy entities.
     * This entity will be initialized at the moment of creation within diagram.
     */
    public ERDElement() {
        super(null);
    }

    public ERDElement(OBJECT entity) {
        super(entity);
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
            firePropertyChange(PROP_OUTPUT, null, rel);
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
            firePropertyChange(PROP_INPUT, null, table);
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
            firePropertyChange(PROP_OUTPUT, table, null);
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
            firePropertyChange(PROP_INPUT, table, null);
        }
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

    public boolean hasAssociationsWith(ERDElement entity) {
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
