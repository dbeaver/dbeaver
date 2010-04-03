package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.meta.AbstractProcedure;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GenericProcedure
 */
public class MySQLProcedure extends AbstractProcedure<MySQLDataSource, MySQLCatalog>
{
    static Log log = LogFactory.getLog(MySQLProcedure.class);

    private DBSProcedureType procedureType;
    private String resultType;
    private String bodyType;
    private String body;
    private String charset;
    private List<MySQLProcedureColumn> columns;

    public MySQLProcedure(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
    {
        setName(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_NAME));
        setDescription(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_COMMENT));
        this.procedureType = DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_TYPE).toUpperCase());
        this.resultType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DTD_IDENTIFIER);
        this.bodyType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_BODY);
        this.body = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_DEFINITION);
        this.charset = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHARACTER_SET_CLIENT);
    }

    @Property(name = "Procedure Type", order = 2)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    @Property(name = "Result Type", order = 2)
    public String getResultType()
    {
        return resultType;
    }

    @Property(name = "Body Type", order = 3)
    public String getBodyType()
    {
        return bodyType;
    }

    //@Property(name = "Body", order = 4)
    public String getBody()
    {
        return body;
    }

    @Property(name = "Client Charset", order = 4)
    public String getCharset()
    {
        return charset;
    }

    public List<MySQLProcedureColumn> getColumns()
        throws DBException
    {
        if (columns == null) {
            loadColumns();
        }
        return columns;
    }

    private void loadColumns()
        throws DBException
    {
        List<MySQLProcedureColumn> columnList = new ArrayList<MySQLProcedureColumn>();

        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getContainer().getName();

            // Load procedure columns
            ResultSet dbResult = metaData.getProcedureColumns(
                catalogName,
                null,
                getName(),
                null);
            try {
                while (dbResult.next()) {
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
                    int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    int columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
                    boolean isNullable = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) != DatabaseMetaData.procedureNoNulls;
                    int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
                    int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
                    int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    //DBSDataType dataType = getDataSource().getInfo().getSupportedDataType(typeName);
                    DBSProcedureColumnType columnType;
                    switch (columnTypeNum) {
                        case DatabaseMetaData.procedureColumnIn: columnType = DBSProcedureColumnType.IN; break;
                        case DatabaseMetaData.procedureColumnInOut: columnType = DBSProcedureColumnType.INOUT; break;
                        case DatabaseMetaData.procedureColumnOut: columnType = DBSProcedureColumnType.OUT; break;
                        case DatabaseMetaData.procedureColumnReturn: columnType = DBSProcedureColumnType.RETURN; break;
                        case DatabaseMetaData.procedureColumnResult: columnType = DBSProcedureColumnType.RESULTSET; break;
                        default: columnType = DBSProcedureColumnType.UNKNOWN; break;
                    }
                    if (CommonUtils.isEmpty(columnName) && columnType == DBSProcedureColumnType.RETURN) {
                        columnName = "RETURN";
                    }
                    MySQLProcedureColumn procedureColumn = new MySQLProcedureColumn(
                        this,
                        columnName,
                        typeName,
                        valueType,
                        columnList.size(),
                        columnSize,
                        scale, precision, radix, isNullable,
                        remarks,
                        columnType);
                    columnList.add(procedureColumn);
                }
            }
            finally {
                dbResult.close();
            }
            this.columns = columnList;
        }
        catch (SQLException ex) {
            throw new DBException("SQL Exception (" + ex.getErrorCode() + ")", ex);
        }
    }

}
