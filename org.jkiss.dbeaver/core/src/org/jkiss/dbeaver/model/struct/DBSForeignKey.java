package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSForeignKey
 */
public interface DBSForeignKey<DATASOURCE extends DBPDataSource, TABLE extends DBSTable>
    extends DBSConstraint<DATASOURCE, TABLE>
{
    DBSUniqueKey getReferencedKey();

    DBSConstraintCascade getDeleteRule();

    DBSConstraintCascade getUpdateRule();

    DBSConstraintDefferability getDefferability();

    Collection<? extends DBSForeignKeyColumn<DATASOURCE>> getColumns();
}
