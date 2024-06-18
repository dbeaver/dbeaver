package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

public class GaussDBSchemaCache extends PostgreDatabase.SchemaCache {

    @Override
    protected PostgreSchema fetchObject(@NotNull JDBCSession session,
                                        @NotNull PostgreDatabase owner,
                                        @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
        String name = JDBCUtils.safeGetString(resultSet, "nspname");
        if (name == null) {
            return null;
        }
        if (GaussDBSchema.isUtilitySchema(name)
                    && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects()) {
            return null;
        }
        return new GaussDBSchema(owner, name, resultSet);
    }

}