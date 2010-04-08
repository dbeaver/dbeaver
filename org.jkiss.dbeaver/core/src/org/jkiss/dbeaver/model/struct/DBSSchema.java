package org.jkiss.dbeaver.model.struct;

/**
 * DBSSchema
 */
public interface DBSSchema extends DBSStructureObject, DBSStructureContainer
{
    DBSCatalog getCatalog();
}
