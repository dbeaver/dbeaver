package org.jkiss.dbeaver.ext.postgresql.model.impls.materialize;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

public class MaterializeSchemaCache extends PostgreDatabase.SchemaCache {
    @Override
    protected MaterializeSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner,
            @NotNull JDBCResultSet resultSet) throws SQLException {
        String name = JDBCUtils.safeGetString(resultSet, "nspname");
        if (name == null) {
            return null;
        }
        if (MaterializeSchema.isUtilitySchema(name)
                && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects()) {
            return null;
        }
        return new MaterializeSchema(owner, name, resultSet);
    }
}
