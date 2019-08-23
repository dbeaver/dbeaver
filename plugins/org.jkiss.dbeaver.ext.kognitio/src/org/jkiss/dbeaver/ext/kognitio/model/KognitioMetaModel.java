package org.jkiss.dbeaver.ext.kognitio.model;

import java.sql.SQLException;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class KognitioMetaModel extends GenericMetaModel {
    private static final String viewDDLSQL =
        "select v.text_seq, ds.name creation_schema " +
        "from sys.ipe_schema s, sys.ipe_table t, " +
        "sys.ipe_view v left outer join sys.ipe_schema ds on v.def_schema = ds.id " +
        "where v.table_id = t.id and t.schema_id = s.id " +
        "and s.name = ? and t.name = ?";

    private static final String tableDDLSQL =
        "select b.text, cast(null as varchar) creation_schema " +
        "from sys.ipe_base b, sys.ipe_table t, sys.ipe_schema s " +
        "where b.table_id = t.id and t.schema_id = s.id " +
        "and s.name = ? and t.name = ?";

    public KognitioMetaModel() {
        super();
    }

    protected String getObjectDDL(DBRProgressMonitor monitor,
            GenericTableBase sourceObject, Map<String, Object> options,
            String sql, String description) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        String schemaName = sourceObject.getContainer().getName();
        String tableName = sourceObject.getName();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, description)) {
            try (JDBCPreparedStatement stmt = session.prepareStatement(sql)) {
                stmt.setString(1, schemaName);
                stmt.setString(2, tableName);

                try (JDBCResultSet resultSet = stmt.executeQuery()) {
                    if (resultSet.nextRow()) {
                        String createText = resultSet.getString(1);
                        String defaultSchemaName = resultSet.getString(2);

                        if (defaultSchemaName != null) {
                            /* If this is a view which was created from a
                             * different schema than the one the view resides
                             * in, note that. */
                            String preamble;
                            if (!defaultSchemaName.equals(schemaName)) {
                                preamble = String.format("-- View %s is in the %s schema, but is created from the %s schema.%n", tableName, schemaName, defaultSchemaName);
                            }
                            else {
                                preamble = "";
                            }

                            /* Include a "set schema" command so you can be in
                             * the right schema to create this view. The set
                             * schema command is commented out because it's
                             * not part of the create text. */
                            return String.format("%s-- set schema %s;%n%s", preamble, defaultSchemaName, createText);
                        }
                        else {
                            return createText;
                        }
                    }
                    else {
                        return "-- Create text not found";
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    public String getViewDDL(DBRProgressMonitor monitor,
            GenericView sourceObject, Map<String, Object> options) throws DBException {
        return getObjectDDL(monitor, sourceObject, options, viewDDLSQL,
                "Read Kognitio view create text");
    }

    public String getTableDDL(DBRProgressMonitor monitor,
            GenericTableBase sourceObject, Map<String, Object> options) throws DBException {
        return getObjectDDL(monitor, sourceObject, options, tableDDLSQL,
                "Read Kognitio base table create text");
    }
}
