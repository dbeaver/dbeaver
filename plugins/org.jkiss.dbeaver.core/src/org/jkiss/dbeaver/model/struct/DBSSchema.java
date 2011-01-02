/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSSchema
 */
public interface DBSSchema extends DBSEntity, DBSEntityContainer
{
    DBSCatalog getCatalog();
}
