/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericProcedure
 */
public class GenericProcedure extends AbstractProcedure<GenericDataSource, GenericStructContainer> implements GenericStoredCode
{
    private static final Pattern PATTERN_COL_NAME_NUMERIC = Pattern.compile("\\$?([0-9]+)");

    private String plainName;
    private DBSProcedureType procedureType;
    private List<GenericProcedureColumn> columns;

    public GenericProcedure(
        GenericStructContainer container,
        String procedureName,
        String specificName,
        String description,
        DBSProcedureType procedureType)
    {
        super(container, CommonUtils.isEmpty(specificName) ? procedureName : specificName, description);
        this.procedureType = procedureType;
        this.plainName = procedureName;
    }

/*
    @Property(name = "Plain Name", viewable = true, order = 2)
    public String getPlainName()
    {
        return plainName;
    }
*/

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
    public GenericPackage getPackage()
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
            loadColumns(monitor);
        }
        return columns;
    }

    private void loadColumns(DBRProgressMonitor monitor) throws DBException
    {
        Collection<GenericProcedure> procedures = getContainer().getProcedures(monitor, getName());
        if (procedures == null || !procedures.contains(this)) {
            throw new DBException("Internal error - cannot read columns for procedure '" + getName() + "' because its not found in container");
        }
        Iterator<GenericProcedure> procIter = procedures.iterator();
        GenericProcedure procedure = null;

        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load procedure columns");
        try {
            final JDBCResultSet dbResult = context.getMetaData().getProcedureColumns(
                getCatalog() == null ? this.getPackage() == null || !this.getPackage().isNameFromCatalog() ? null : this.getPackage().getName() : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                getName(),
                null);
            try {
                int previousPosition = -1;
                while (dbResult.next()) {
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
                    int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    int columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
                    boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
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
                    if (position == 0) {
                        // Some drivers do not return ordinal position (PostgreSQL) but
                        // position is contained in column name
                        Matcher numberMatcher = PATTERN_COL_NAME_NUMERIC.matcher(columnName);
                        if (numberMatcher.matches()) {
                            position = Integer.parseInt(numberMatcher.group(1));
                        }
                    }

                    if (procedure == null || (previousPosition >= 0 && position <= previousPosition && procIter.hasNext())) {
                        procedure = procIter.next();
                    }
                    GenericProcedureColumn column = new GenericProcedureColumn(
                        procedure,
                        columnName,
                        typeName,
                        valueType,
                        position,
                        columnSize,
                        scale, precision, radix, notNull,
                        remarks,
                        columnType);

                    procedure.addColumn(column);

                    previousPosition = position;
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
            getCatalog(),
            getSchema(),
            this);
    }
}
