/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSSchema
 */
public interface DBSSchema extends DBSStructureObject, DBSStructureContainer
{
    DBSCatalog getCatalog();
}
