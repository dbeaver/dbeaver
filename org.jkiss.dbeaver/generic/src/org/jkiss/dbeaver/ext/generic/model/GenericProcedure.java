package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.meta.AbstractProcedure;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GenericProcedure
 */
public class GenericProcedure extends AbstractProcedure<GenericDataSource, GenericStructureContainer>
{
    static Log log = LogFactory.getLog(GenericProcedure.class);

    private DBSProcedureType procedureType;
    private List<GenericProcedureColumn> columns;

    public GenericProcedure(
        GenericStructureContainer container,
        String procedureName,
        String description,
        DBSProcedureType procedureType
    )
    {
        super(container, procedureName, description);
        this.procedureType = procedureType;
    }

    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    @Property(name = "Catalog", viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }

    @Property(name = "Schema", viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return getContainer().getSchema();
    }

    public List<GenericProcedureColumn> getColumns()
        throws DBException
    {
        if (columns == null) {
            loadColumns();
        }
        return columns;
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }
    private void loadColumns()
        throws DBException
    {
        List<GenericProcedureColumn> columnList = new ArrayList<GenericProcedureColumn>();

        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Load procedure columns
            ResultSet dbResult = metaData.getProcedureColumns(
                catalogName,
                schemaName,
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
                    GenericProcedureColumn procedureColumn = new GenericProcedureColumn(
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
