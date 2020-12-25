package org.jkiss.dbeaver.ext.kognitio.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Map;

public class KognitioMetaModel extends GenericMetaModel {
    /* The system table IPE_BASE contains create text for tables, and IPE_VIEW
     * contains create text for views. The create text field in these system
     * tables can only store a certain number of characters, so if an unusually
     * long create text needs more space, extra text is stored in
     * IPE_SQL_FRAGMENTS with the appropriate object ID and type.
     *
     * The result set of the following queries looks like this:
     *
     * TEXT1                 TEXT2                     CREATION_SCHEMA
     * -----------------------------------------------------------------------
     * initial fragment      additional fragment 0     name of creation schema
     * <null>                additional fragment 1     name of creation schema
     * <null>                additional fragment 2     name of creation schema
     * 
     * ... and so on, for as many rows are required for the full text.
     *
     * Most objects have creation text short enough not to need any entries in
     * IPE_SQL_FRAGMENTS, and their results look like this:
     *
     * TEXT1                 TEXT2                     CREATION_SCHEMA
     * -----------------------------------------------------------------------
     * initial fragment      <null>                    name of creation schema
     */
    private static final String viewDDLSQL =
        "select case when f.seq is null or f.seq = 0 then v.text_seq else cast(null as varchar) end text1, f.text text2, ds.name creation_schema " +
        "from sys.ipe_schema s, sys.ipe_table t, " +
        "sys.ipe_view v left outer join sys.ipe_schema ds on v.def_schema = ds.id " +
        "left outer join sys.ipe_sql_fragments f on f.id = v.table_id and f.type = 4 " +
        "where v.table_id = t.id and t.schema_id = s.id " +
        "and s.name = ? and t.name = ? " +
        "order by f.seq";

    private static final String tableDDLSQL =
        "select case when f.seq is null or f.seq = 0 then b.text else cast(null as varchar) end text1, f.text text2, cast(null as varchar) creation_schema " +
        "from sys.ipe_table t, sys.ipe_schema s, sys.ipe_base b " +
        "left outer join sys.ipe_sql_fragments f on f.id = b.table_id and f.type = 3 " +
        "where b.table_id = t.id and t.schema_id = s.id " +
        "and s.name = ? and t.name = ? " +
        "order by f.seq";

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
                    StringBuilder createText = new StringBuilder();
                    String preamble = "";
                    String defaultSchemaQuotedIdentifier = null;
                    while (resultSet.nextRow()) {
                        String createTextFragment1 = resultSet.getString(1);
                        String createTextFragment2 = resultSet.getString(2);
                        String defaultSchemaName = resultSet.getString(3);

                        if (defaultSchemaName != null && defaultSchemaQuotedIdentifier == null) {
                            defaultSchemaQuotedIdentifier = DBUtils.getQuotedIdentifier(dataSource, defaultSchemaName);
                            /* If this is a view which was created from a
                             * different schema than the one the view resides
                             * in, note that. */
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
                            preamble += String.format("-- set schema %s;%n", defaultSchemaQuotedIdentifier, createText);
                        }

                        if (createTextFragment1 != null)
                            createText.append(createTextFragment1);

                        if (createTextFragment2 != null)
                            createText.append(createTextFragment2);
                    }

                    if (createText.length() == 0) {
                        return "-- Create text not found";
                    }
                    else {
                        return preamble + createText.toString();
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
