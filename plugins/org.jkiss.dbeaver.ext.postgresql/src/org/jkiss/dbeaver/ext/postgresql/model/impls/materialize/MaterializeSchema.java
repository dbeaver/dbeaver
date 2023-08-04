package org.jkiss.dbeaver.ext.postgresql.model.impls.materialize;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

public class MaterializeSchema extends PostgreSchema {
    public MaterializeSchema(PostgreDatabase owner, String name, JDBCResultSet resultSet) throws SQLException {
        super(owner, name, resultSet);
    }

    public MaterializeSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    // @NotNull
    // @Override
    // public MaterializeDataSource getDataSource() {
    // return (MaterializeDataSource) super.getDataSource();
    // }

    @Override
    public boolean isSystem() {
        return isCatalogSchema() || PostgreConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(name)
                || name.startsWith(PostgreConstants.SYSTEM_SCHEMA_PREFIX) || name.startsWith("mz_");
    }

}
