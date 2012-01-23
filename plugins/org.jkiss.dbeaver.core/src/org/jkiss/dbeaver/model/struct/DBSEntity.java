/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSEntity
 */
public interface DBSEntity extends DBSObject
{
    DBSEntityType getEntityType();

    /**
     * Gets this entity attributes
     * @return attribute list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this entity constraints
     * @return association list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this entity associations
     * @return association list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets associations which refers this entity
     * @return reference association list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException;

}
