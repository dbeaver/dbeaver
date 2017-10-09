/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;
import java.sql.Types;

/**
 * Exasol data types
 *
 * @author Karl Griesser
 */
public class ExasolDataType extends ExasolObject<DBSObject> implements DBSDataType, DBPQualifiedObject {


    private static final Log LOG = Log.getLog(ExasolDataType.class);


    private DBSObject parentNode; // see below

    private ExasolSchema exasolSchema;


    private TypeDesc typeDesc;

    private Integer exasolTypeId;


    private Integer length;
    private Integer scale;


    private String name;


    // -----------------------
    // Constructors
    // -----------------------
    protected ExasolDataType(DBSObject parent, String name, boolean persisted) {
        super(parent, name, persisted);

    }

    public ExasolDataType(DBSObject owner, ResultSet dbResult) throws DBException {
        super(owner, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);

        this.exasolTypeId = JDBCUtils.safeGetInt(dbResult, "TYPE_ID");
        this.length = JDBCUtils.safeGetInt(dbResult, "PRECISION");
        this.scale = JDBCUtils.safeGetInt(dbResult, "MINIMUM_SCALE");

        TypeDesc tempTypeDesc = null;
        String typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        int precision = JDBCUtils.safeGetInt(dbResult, "PRECISION");
        int minimumScale = JDBCUtils.safeGetInt(dbResult, "MINIMUM_SCALE");
        int maximumScale = JDBCUtils.safeGetInt(dbResult, "MAXIMUM_SCALE");

        this.name = typeName;
        switch (name) {
            case "BIGINT":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.BIGINT, precision, minimumScale, maximumScale, typeName);
                break;
            case "INTEGER":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, precision, minimumScale, maximumScale, typeName);
                break;
            case "DECIMAL":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.DECIMAL, precision, minimumScale, maximumScale, typeName);
                break;
            case "DOUBLE PRECISION":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, precision, minimumScale, maximumScale, typeName);
                break;
            case "FLOAT":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.FLOAT, precision, minimumScale, maximumScale, typeName);
                break;
            case "INTERVAL DAY TO SECOND":
                tempTypeDesc = new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, precision, minimumScale, maximumScale, typeName);
                break;
            case "INTERVAL YEAR TO MONTH":
                tempTypeDesc = new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, precision, minimumScale, maximumScale, typeName);
                break;
            case "SMALLINT":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.SMALLINT, precision, minimumScale, maximumScale, typeName);
                break;
            case "TINYINT":
                tempTypeDesc = new TypeDesc(DBPDataKind.NUMERIC, Types.TINYINT, precision, minimumScale, maximumScale, typeName);
                break;
            case "GEOMETRY":
                tempTypeDesc = new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, precision, minimumScale, maximumScale, typeName);
                break;
            case "BOOLEAN":
                tempTypeDesc = new TypeDesc(DBPDataKind.BOOLEAN, Types.BOOLEAN, precision, minimumScale, maximumScale, typeName);
                break;
            case "CHAR":
                tempTypeDesc = new TypeDesc(DBPDataKind.STRING, Types.CHAR, precision, minimumScale, maximumScale, typeName);
                break;
            case "VARCHAR":
                tempTypeDesc = new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, precision, minimumScale, maximumScale, typeName);
                break;
            case "LONG VARCHAR":
                tempTypeDesc = new TypeDesc(DBPDataKind.STRING, Types.LONGNVARCHAR, precision, minimumScale, maximumScale, typeName);
                break;
            case "DATE":
                tempTypeDesc = new TypeDesc(DBPDataKind.DATETIME, Types.DATE, precision, minimumScale, maximumScale, typeName);
                break;
            case "TIMESTAMP":
                tempTypeDesc = new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, precision, minimumScale, maximumScale, typeName);
                break;
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                tempTypeDesc = new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP_WITH_TIMEZONE, precision, minimumScale, maximumScale, typeName);
                break;
            default:
                LOG.error("DataType '" + name + "' is unknown to DBeaver");
        }

        this.typeDesc = tempTypeDesc;
    }

    @Override
    public DBSObject getParentObject() {
        return parentNode;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }


    public int getEquivalentSqlType() {
        return typeDesc.sqlType;
    }

    @Override
    public Integer getPrecision() {
        if (typeDesc.precision != null) {
            return typeDesc.precision;
        } else {
            return 0;
        }
    }


    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public int getMinScale() {
        if (typeDesc.minScale != null) {
            return typeDesc.minScale;
        } else {
            return 0;
        }
    }

    @Override
    public int getMaxScale() {
        if (typeDesc.maxScale != null) {
            return typeDesc.maxScale;
        } else {
            return 0;
        }
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        return DBUtils.getDefaultOperators(this);
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public ExasolSchema getSchema() {
        return exasolSchema;
    }


    @Override
    @Property(viewable = true, editable = false, order = 4)
    public DBPDataKind getDataKind() {
        return typeDesc == null ? DBPDataKind.UNKNOWN : typeDesc.dataKind;
    }


    @Override
    @Property(viewable = true, editable = false, order = 5)
    public long getMaxLength() {
        return length;
    }

    @Override
    @Property(viewable = true, editable = false, order = 6)
    public Integer getScale() {
        return scale;
    }

    @Override
    @Property(viewable = false, editable = false, order = 10)
    public int getTypeID() {
        return typeDesc.sqlType;
    }

    @Property(viewable = false, editable = false, order = 11)
    public Integer getExasolTypeId() {
        return exasolTypeId;
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false)
    public String getDescription() {
        return null;
    }


    // --------------
    // Helper Objects
    // --------------
    private static final class TypeDesc {
        private final DBPDataKind dataKind;
        private final Integer sqlType;
        private final Integer precision;
        private final Integer minScale;
        private final Integer maxScale;
        @SuppressWarnings("unused")
		private final String name;

        private TypeDesc(DBPDataKind dataKind, Integer sqlType, Integer precision, Integer minScale, Integer maxScale, String name) {
            this.name = name;
            this.dataKind = dataKind;
            this.sqlType = sqlType;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }


    @Override
    public boolean isPersisted() {
        return true;
    }


    @Override
    public Object geTypeExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return name;
    }


}
