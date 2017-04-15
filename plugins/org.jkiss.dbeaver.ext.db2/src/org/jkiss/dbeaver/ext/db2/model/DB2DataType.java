/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2DataTypeMetaType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * DB2 data types
 * 
 * @author Denis Forveille
 */
public class DB2DataType extends DB2Object<DBSObject> implements DBSDataType, DBPQualifiedObject {

    private static final Log                   LOG              = Log.getLog(DB2DataType.class);

    private static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<>(32);            // See init below

    private DBSObject                          parentNode;                                      // see below

    private DB2Schema                          db2Schema;

    private String                             fullyQualifiedName;

    private TypeDesc                           typeDesc;

    private Integer                            db2TypeId;

    private String                             ownerCol;
    private DB2OwnerType                       ownerType;

    private String                             sourceSchemaName;
    private String                             sourceModuleName;
    private String                             sourceName;

    private DB2DataTypeMetaType                metaType;

    private Integer                            length;
    private Integer                            scale;

    private Timestamp                          createTime;
    private Timestamp                          alterTime;
    private Timestamp                          lastRegenTime;
    private String                             constraintText;
    private String                             remarks;

    private DB2Module                          db2Module;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2DataType(DBSObject owner, ResultSet dbResult) throws DBException
    {
        super(owner, JDBCUtils.safeGetStringTrimmed(dbResult, "TYPENAME"), true);

        DB2DataSource db2DataSource = (DB2DataSource) owner.getDataSource();

        this.db2TypeId = JDBCUtils.safeGetInteger(dbResult, "TYPEID");

        this.ownerCol = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.sourceSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCESCHEMA");
        this.sourceName = JDBCUtils.safeGetString(dbResult, "SOURCENAME");
        this.metaType = CommonUtils.valueOf(DB2DataTypeMetaType.class, JDBCUtils.safeGetString(dbResult, "METATYPE"));
        this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
        this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
        if (db2DataSource.isAtLeastV9_7()) {
            this.sourceModuleName = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCEMODULENAME");
        }
        if (db2DataSource.isAtLeastV10_5()) {
            this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
            this.constraintText = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TEXT");
        }

        // Store associated DB2Schema
        // DB2DataType can have 3 owners:
        // - DataSource (= "System" DataTypes)
        // - DB2Schema (=UDT)
        // - DB2Module
        if (owner instanceof DB2Schema) {
            this.db2Schema = (DB2Schema) owner;
        } else {
            if (owner instanceof DB2Module) {
                this.db2Schema = ((DB2Module) owner).getSchema();
                String typeModuleName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPEMODULENAME");
                if (typeModuleName != null) {
                    this.db2Module = DB2Utils.findModuleBySchemaNameAndName(new VoidProgressMonitor(), db2DataSource,
                        db2Schema.getName(), typeModuleName);
                }

            } else {
                // System datatypes
                String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPESCHEMA");
                try {
                    this.db2Schema = db2DataSource.getSchema(new VoidProgressMonitor(), schemaName);
                } catch (DBException e) {
                    LOG.error("Impossible! Schema '" + schemaName + "' for dataType '" + name + "' not found??", e);
                    // In this case, 'this.db2Schema' will be null...
                }
            }
        }

        if ((db2Schema != null) && (db2Schema.getName().equals(DB2Constants.SYSTEM_DATATYPE_SCHEMA))) {
            // DF: not sure of that. Maybe for system DataTypes, we should set db2Schema to null instead..
            fullyQualifiedName = name;
        } else {
            fullyQualifiedName = db2Schema.getName() + "." + name;
        }

        // Determine DBSKind and javax.sql.Types
        TypeDesc tempTypeDesc = null;

        // If the dataType is a SYSIBM dataType, get it
        if (db2Schema.getName().equals(DB2Constants.SYSTEM_DATATYPE_SCHEMA)) {
            tempTypeDesc = PREDEFINED_TYPES.get(name);
            // NLS_STRING_UNITS_TYPE is a SYSIBM type, but not a predefined one...
            // so tempTypeDesc may be null at this time even if the schema is SYSIBM
        }

        if (tempTypeDesc == null) {
            // This is a UDT

            // Check for Structured or Array like DataTypes
            switch (metaType) {
            case R:
                tempTypeDesc = new TypeDesc(DBPDataKind.STRUCT, Types.STRUCT, null, null, null);
                break;
            case A:
            case L:
                tempTypeDesc = new TypeDesc(DBPDataKind.ARRAY, Types.ARRAY, null, null, null);
                break;
            default:
                // If the UDT is based on a SYSIBM dataType, get it
                if ((sourceSchemaName != null) && (sourceSchemaName.equals(DB2Constants.SYSTEM_DATATYPE_SCHEMA))) {
                    LOG.debug(name + " is a User Defined Type base on a System Data Type.");
                    tempTypeDesc = PREDEFINED_TYPES.get(sourceName);
                } else {
                    // This UDT is based on another UDT, set it's TypeDesc to unkknown as looking for the base type recursively
                    // could lead to infinite loops:
                    // load corresponding module ->module load its own UDTs ->come back here to instanciate the UDT -> look for type
                    // in
                    // module etc.
                    // It would have to be done recursively with a direct SQL. No real benefit here..
                    LOG.debug(name + " is a User Defined Type base on another UDT. Set its DBPDataKind to UNKNOWN/OTHER");
                    tempTypeDesc = new TypeDesc(DBPDataKind.UNKNOWN, Types.OTHER, null, null, null);
                }
                break;
            }
        }
        this.typeDesc = tempTypeDesc;

        // if the getParentObject() return the "real" parent ie DB2Schema or DB2DataSource,
        // when someone, as a first action, opens the table/column tab and then clicks on the datatype link,
        // nothing is displayed and the following message appears in the logs :
        // !MESSAGE Can't find tree node for object <database name> (org.jkiss.dbeaver.ext.db2.model.DB2DataSource)
        // With this code (copied from OracleDataType), it works.
        if ((parent instanceof DB2Schema) || (parent instanceof DB2Module)) {
            parentNode = parent;
        } else {
            if (parent instanceof DB2DataSource) {
                parentNode = ((DB2DataSource) parent).getContainer();
            }
        }

    }

    @Override
    public DBSObject getParentObject()
    {
        return parentNode;
    }

    @Override
    public String getTypeName()
    {
        return name;
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return fullyQualifiedName;
    }

    public int getEquivalentSqlType()
    {
        return typeDesc.sqlType;
    }

    @Override
    public int getPrecision()
    {
        if (typeDesc.precision != null) {
            return typeDesc.precision;
        } else {
            return 0;
        }
    }

    @Nullable
    @Override
    public Object geTypeExtension()
    {
        return metaType;
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        return null;
    }

    @Override
    public int getMinScale()
    {
        if (typeDesc.minScale != null) {
            return typeDesc.minScale;
        } else {
            return 0;
        }
    }

    @Override
    public int getMaxScale()
    {
        if (typeDesc.maxScale != null) {
            return typeDesc.maxScale;
        } else {
            return 0;
        }
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute)
    {
        return DBUtils.getDefaultOperators(this);
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public DB2Schema getSchema()
    {
        return db2Schema;
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2Module getModule()
    {
        return db2Module;
    }

    @Override
    @Property(viewable = true, editable = false, order = 4)
    public DBPDataKind getDataKind()
    {
        return typeDesc == null ? DBPDataKind.UNKNOWN : typeDesc.dataKind;
    }

    @Property(viewable = false, editable = false, order = 5)
    public DB2DataTypeMetaType getMetaType()
    {
        return metaType;
    }

    @Override
    @Property(viewable = true, editable = false, order = 5)
    public long getMaxLength()
    {
        return length;
    }

    @Override
    @Property(viewable = true, editable = false, order = 6)
    public int getScale()
    {
        return scale;
    }

    @Override
    @Property(viewable = false, editable = false, order = 10)
    public int getTypeID()
    {
        return typeDesc.sqlType;
    }

    @Property(viewable = false, editable = false, order = 11)
    public Integer getDb2TypeId()
    {
        return db2TypeId;
    }

    @Property(viewable = false, editable = false)
    public String getConstraintText()
    {
        return constraintText;
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false)
    public String getDescription()
    {
        return remarks;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_SOURCE, order = 20)
    public String getSourceSchemaName()
    {
        return sourceSchemaName;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_SOURCE, order = 21)
    public String getSourceModuleName()
    {
        return sourceModuleName;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_SOURCE, order = 22)
    public String getSourceName()
    {
        return sourceName;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return ownerCol;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastRegenTime()
    {
        return lastRegenTime;
    }

    // --------------
    // Helper Objects
    // --------------
    private static final class TypeDesc {
        private final DBPDataKind dataKind;
        private final Integer     sqlType;
        private final Integer     precision;
        private final Integer     minScale;
        private final Integer     maxScale;

        private TypeDesc(DBPDataKind dataKind, Integer sqlType, Integer precision, Integer minScale, Integer maxScale)
        {
            this.dataKind = dataKind;
            this.sqlType = sqlType;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }

    static {
        PREDEFINED_TYPES.put("ARRAY", new TypeDesc(DBPDataKind.ARRAY, Types.ARRAY, null, null, null));
        PREDEFINED_TYPES.put("BIGINT", new TypeDesc(DBPDataKind.NUMERIC, Types.BIGINT, 20, 0, 0));
        PREDEFINED_TYPES.put("BINARY", new TypeDesc(DBPDataKind.BINARY, Types.BINARY, 254, null, null));
        PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 2147483647, null, null));
        PREDEFINED_TYPES.put("BOOLEAN", new TypeDesc(DBPDataKind.BOOLEAN, Types.BOOLEAN, null, null, null));
        PREDEFINED_TYPES.put("CHARACTER", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 254, null, null));
        PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 2147483647, null, null));
        PREDEFINED_TYPES.put("DATE", new TypeDesc(DBPDataKind.DATETIME, Types.DATE, 10, null, null));
        PREDEFINED_TYPES.put("DBCLOB", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 1073741823, null, null));
        PREDEFINED_TYPES.put("DECIMAL", new TypeDesc(DBPDataKind.NUMERIC, Types.DECIMAL, 31, 0, 31));
        PREDEFINED_TYPES.put("DOUBLE", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 53, 0, 0));
        PREDEFINED_TYPES.put("GRAPHIC", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 127, null, null));
        PREDEFINED_TYPES.put("INTEGER", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 10, 0, 0));
        PREDEFINED_TYPES.put("LONG VARCHAR", new TypeDesc(DBPDataKind.STRING, Types.LONGVARCHAR, 32700, null, null));
        PREDEFINED_TYPES.put("LONG VARGRAPHIC", new TypeDesc(DBPDataKind.STRING, Types.LONGVARCHAR, 16350, null, null));
        PREDEFINED_TYPES.put("REAL", new TypeDesc(DBPDataKind.NUMERIC, Types.REAL, 24, 0, 0));
        PREDEFINED_TYPES.put("REFERENCE", new TypeDesc(DBPDataKind.REFERENCE, Types.REF, null, null, null));
        PREDEFINED_TYPES.put("ROW", new TypeDesc(DBPDataKind.STRUCT, Types.ROWID, null, null, null));
        PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBPDataKind.NUMERIC, Types.SMALLINT, 5, 0, 0));
        PREDEFINED_TYPES.put("TIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIME, 8, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 32, 0, 12));
        PREDEFINED_TYPES.put("VARBINARY", new TypeDesc(DBPDataKind.BINARY, Types.VARBINARY, 32762, null, null));
        PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 4000, null, null));
        PREDEFINED_TYPES.put("VARGRAPHIC", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 16336, null, null));
        PREDEFINED_TYPES.put("XML", new TypeDesc(DBPDataKind.CONTENT, Types.SQLXML, null, null, null));

        PREDEFINED_TYPES.put("CURSOR", new TypeDesc(DBPDataKind.UNKNOWN, DB2Constants.EXT_TYPE_CURSOR, null, null, null));
        PREDEFINED_TYPES.put(DB2Constants.TYPE_NAME_DECFLOAT,
            new TypeDesc(DBPDataKind.NUMERIC, DB2Constants.EXT_TYPE_DECFLOAT, 34, 0, 0));
    }

}
