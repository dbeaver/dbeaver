/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GenericProcedure
 */
public class GenericProcedure extends AbstractProcedure<GenericDataSource, GenericEntityContainer>
{
    static final Log log = LogFactory.getLog(GenericProcedure.class);

    private DBSProcedureType procedureType;
    private List<GenericProcedureColumn> columns;

    public GenericProcedure(
        GenericEntityContainer container,
        String procedureName,
        String description,
        DBSProcedureType procedureType)
    {
        super(container, procedureName, description);
        this.procedureType = procedureType;
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

    @Property(name = "Package", viewable = true, order = 5)
    private GenericPackage getPackage()
    {
        return getContainer() instanceof GenericPackage ? (GenericPackage) getContainer() : null;
    }

    @Property(name = "Type", viewable = true, order = 6)
    public DBSProcedureType getProcedureType()
    {
        return procedureType;
    }

    public List<GenericProcedureColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            loadChildren(monitor);
        }
        return columns;
    }

    private void loadChildren(DBRProgressMonitor monitor) throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load procedure columns");
        try {
            final JDBCResultSet dbResult = context.getMetaData().getProcedureColumns(
                getCatalog() == null ? this.getPackage() == null || !this.getPackage().isNameFromCatalog() ? null : this.getPackage().getName() : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
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
                    int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                    //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
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
                    GenericProcedureColumn column = new GenericProcedureColumn(
                        this,
                        columnName,
                        typeName,
                        valueType,
                        position,
                        columnSize,
                        scale, precision, radix, isNullable,
                        remarks,
                        columnType);

                    addColumn(column);
                }
            }
            finally {
                dbResult.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            context.close();
        }

    }

    private void addColumn(GenericProcedureColumn column)
    {
        if (this.columns == null) {
            this.columns = new ArrayList<GenericProcedureColumn>();
        }
        this.columns.add(column);
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getCatalog() == null ? null : getCatalog().getName(),
            getSchema() == null ? null : getSchema().getName(),
            getName());
    }
}
