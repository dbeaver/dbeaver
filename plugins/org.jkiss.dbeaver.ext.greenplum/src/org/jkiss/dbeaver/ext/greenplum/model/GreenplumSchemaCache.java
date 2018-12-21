package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.SQLException;

public class GreenplumSchemaCache extends PostgreDatabase.SchemaCache {

    @Override
    protected GreenplumSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
        String name = JDBCUtils.safeGetString(resultSet, "nspname");
        if (name == null) {
            return null;
        }
        if (GreenplumSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().isShowUtilityObjects()) {
            return null;
        }
        return new GreenplumSchema(owner, name, resultSet);
    }
}
