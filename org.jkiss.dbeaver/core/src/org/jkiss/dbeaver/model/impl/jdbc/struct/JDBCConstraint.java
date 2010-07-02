package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.impl.meta.AbstractConstraint;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * JDBC abstract constraint
 */
public abstract class JDBCConstraint<DATASOURCE extends DBPDataSource, TABLE extends DBSTable> extends AbstractConstraint<DATASOURCE, TABLE> {

    protected JDBCConstraint(TABLE table, String name, String description) {
        super(table, name, description);
    }
}
