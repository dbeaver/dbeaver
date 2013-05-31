package org.jkiss.dbeaver.ext.generic.model.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Standard metadata reader, uses column names from JDBC API spec
 */
public class StandardMetadataReader implements GenericMetadataReader {

    static final Log log = LogFactory.getLog(StandardMetadataReader.class);

    @Override
    public String fetchCatalogName(ResultSet dbResult) throws SQLException
    {
        String catalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
        if (CommonUtils.isEmpty(catalog)) {
            // Some drivers uses TABLE_QUALIFIER instead of catalog
            catalog = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_QUALIFIER);
        }
        return catalog;
    }

    @Override
    public String fetchSchemaName(ResultSet dbResult, String catalog) throws SQLException
    {
        String schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
        if (CommonUtils.isEmpty(schemaName)) {
            // some drivers uses TABLE_OWNER column instead of TABLE_SCHEM
            schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_OWNER);
        }
        return schemaName;
    }

    @Override
    public String fetchTableType(ResultSet dbResult) throws SQLException
    {
        return JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
    }
}
