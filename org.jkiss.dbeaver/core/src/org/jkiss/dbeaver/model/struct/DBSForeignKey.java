package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSForeignKey
 */
public interface DBSForeignKey<DATASOURCE extends DBPDataSource, TABLE extends DBSTable>
    extends DBSConstraint<DATASOURCE, TABLE>
{
    DBSConstraint getReferencedKey();

    DBSConstraintCascade getDeleteRule();

    DBSConstraintCascade getUpdateRule();

    DBSConstraintDefferability getDefferability();

}
