/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Oracle data type
 */
public class OracleDataType implements DBSDataType, DBSEntityQualified, OracleSourceObject {

    static final Log log = LogFactory.getLog(OracleForeignKey.class);

    static class TypeDesc {
        final int valueType;
        final int precision;
        final int minScale;
        final int maxScale;
        private TypeDesc(int valueType, int precision, int minScale, int maxScale)
        {
            this.valueType = valueType;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }

    static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<String, TypeDesc>();
    static  {
        PREDEFINED_TYPES.put("BFILE", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("BINARY ROWID", new TypeDesc(java.sql.Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("BINARY_DOUBLE", new TypeDesc(java.sql.Types.DOUBLE, 63, 127, -84));
        PREDEFINED_TYPES.put("BINARY_FLOAT", new TypeDesc(java.sql.Types.FLOAT, 63, 127, -84));
        PREDEFINED_TYPES.put("BLOB", new TypeDesc(java.sql.Types.BLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("CANONICAL", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("CFILE", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("CHAR", new TypeDesc(java.sql.Types.CHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("CLOB", new TypeDesc(java.sql.Types.CLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("CONTIGUOUS ARRAY", new TypeDesc(java.sql.Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("DATE", new TypeDesc(java.sql.Types.DATE, 0, 0, 0));
        PREDEFINED_TYPES.put("DECIMAL", new TypeDesc(java.sql.Types.DECIMAL, 63, 127, -84));
        PREDEFINED_TYPES.put("DOUBLE PRECISION", new TypeDesc(java.sql.Types.DOUBLE, 63, 127, -84));
        PREDEFINED_TYPES.put("FLOAT", new TypeDesc(java.sql.Types.FLOAT, 63, 127, -84));
        PREDEFINED_TYPES.put("INTEGER", new TypeDesc(java.sql.Types.INTEGER, 63, 127, -84));
        PREDEFINED_TYPES.put("INTERVAL DAY TO SECOND", new TypeDesc(java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("INTERVAL YEAR TO MONTH", new TypeDesc(java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("LOB POINTER", new TypeDesc(java.sql.Types.BLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("NAMED COLLECTION", new TypeDesc(java.sql.Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("NAMED OBJECT", new TypeDesc(java.sql.Types.STRUCT, 0, 0, 0));
        PREDEFINED_TYPES.put("NUMBER", new TypeDesc(java.sql.Types.NUMERIC, 63, 127, -84));
        PREDEFINED_TYPES.put("OCTET", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("OID", new TypeDesc(java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("POINTER", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("RAW", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("REAL", new TypeDesc(java.sql.Types.REAL, 63, 127, -84));
        PREDEFINED_TYPES.put("REF", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("SIGNED BINARY INTEGER", new TypeDesc(java.sql.Types.INTEGER, 63, 127, -84));
        PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(java.sql.Types.SMALLINT, 63, 127, -84));
        PREDEFINED_TYPES.put("TABLE", new TypeDesc(java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("TIME", new TypeDesc(java.sql.Types.TIME, 0, 0, 0));
        PREDEFINED_TYPES.put("TIME WITH TZ", new TypeDesc(java.sql.Types.TIME, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TZ", new TypeDesc(java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH TZ", new TypeDesc(java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TIME ZONE", new TypeDesc(java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH TIME ZONE", new TypeDesc(java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("UNSIGNED BINARY INTEGER", new TypeDesc(java.sql.Types.BIGINT, 63, 127, -84));
        PREDEFINED_TYPES.put("UROWID", new TypeDesc(java.sql.Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("VARCHAR2", new TypeDesc(java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("VARYING ARRAY", new TypeDesc(java.sql.Types.ARRAY, 0, 0, 0));

        PREDEFINED_TYPES.put("VARRAY", new TypeDesc(java.sql.Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("ROWID", new TypeDesc(java.sql.Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("LONG", new TypeDesc(java.sql.Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("LONG RAW", new TypeDesc(java.sql.Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("NVARCHAR2", new TypeDesc(java.sql.Types.NVARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("NCHAR", new TypeDesc(java.sql.Types.NCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("NCLOB", new TypeDesc(java.sql.Types.NCLOB, 0, 0, 0));
    }
    
    private DBSObject owner;
    private String typeName;
    private String typeCode;
    private byte[] typeOID;
    private Object superType;
    private final AttributeCache attributeCache;
    private final MethodCache methodCache;
    private boolean flagPredefined;
    private boolean flagIncomplete;
    private boolean flagFinal;
    private boolean flagInstantiable;
    private TypeDesc typeDesc;
    private int precision = 0;

    private boolean persisted;

    public OracleDataType(DBSObject owner, String typeName, boolean persisted)
    {
        this.owner = owner;
        this.typeName = typeName;
        this.persisted = persisted;
        this.attributeCache = new AttributeCache();
        this.methodCache = new MethodCache();

        findTypeDesc(typeName);
    }

    public OracleDataType(DBSObject owner, ResultSet dbResult)
    {
        this.owner = owner;
        this.persisted = true;
        this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        this.typeCode = JDBCUtils.safeGetString(dbResult, "TYPECODE");
        this.typeOID = JDBCUtils.safeGetBytes(dbResult, "TYPE_OID");
        this.flagPredefined = JDBCUtils.safeGetBoolean(dbResult, "PREDEFINED", OracleConstants.YES);
        this.flagIncomplete = JDBCUtils.safeGetBoolean(dbResult, "INCOMPLETE", OracleConstants.YES);
        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", OracleConstants.YES);
        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", OracleConstants.YES);
        String superTypeOwner = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_OWNER");
        boolean hasAttributes;
        boolean hasMethods;
        if (!CommonUtils.isEmpty(superTypeOwner)) {
            this.superType = new OracleLazyReference(
                superTypeOwner,
                JDBCUtils.safeGetString(dbResult, "SUPERTYPE_NAME"));
            hasAttributes = JDBCUtils.safeGetInt(dbResult, "LOCAL_ATTRIBUTES") > 0;
            hasMethods = JDBCUtils.safeGetInt(dbResult, "LOCAL_METHODS") > 0;
        } else {
            hasAttributes = JDBCUtils.safeGetInt(dbResult, "ATTRIBUTES") > 0;
            hasMethods = JDBCUtils.safeGetInt(dbResult, "METHODS") > 0;
        }
        attributeCache = hasAttributes ? new AttributeCache() : null;
        methodCache = hasMethods ? new MethodCache() : null;
        
        if (owner instanceof OracleDataSource && flagPredefined) {
            // Determine value type for predefined types
            findTypeDesc(typeName);
        }
    }

    private void findTypeDesc(String typeName)
    {
        if (typeName.startsWith("PL/SQL")) {
            // Don't care about PL/SQL types
            return;
        }
        for (;;) {
            final int precBeginPos = typeName.indexOf('(');
            if (precBeginPos == -1) {
                break;
            }
            int precEndPos = typeName.indexOf(')', precBeginPos);
            try {
                precision = Integer.parseInt(typeName.substring(precBeginPos + 1, precEndPos));
            } catch (NumberFormatException e) {
                log.error(e);
            }
            typeName = typeName.substring(0, precBeginPos) + typeName.substring(precEndPos + 1);
        }
        this.typeDesc = PREDEFINED_TYPES.get(typeName);
        if (this.typeDesc == null) {
            log.warn("Unknown predefined type: " + typeName);
        }
    }

    public OracleSchema getSourceOwner()
    {
        return owner instanceof OracleSchema ? (OracleSchema)owner : null;
    }

    public OracleSourceType getSourceType()
    {
        return OracleSourceType.TYPE;
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public int getValueType()
    {
        return typeDesc == null ? java.sql.Types.OTHER : typeDesc.valueType;
    }

    public DBSDataKind getDataKind()
    {
        return DBSDataKind.UNKNOWN;
    }

    public int getPrecision()
    {
        if (precision != 0) {
            return precision;
        }
        return typeDesc == null ? 0 : typeDesc.precision;
    }

    public int getMinScale()
    {
        return typeDesc == null ? 0 : typeDesc.minScale;
    }

    public int getMaxScale()
    {
        return typeDesc == null ? 0 : typeDesc.maxScale;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return owner instanceof OracleSchema ?
            owner :
            owner instanceof OracleDataSource ? ((OracleDataSource) owner).getContainer() : null;
    }

    public OracleDataSource getDataSource()
    {
        return owner instanceof OracleSchema ?
            ((OracleSchema)owner).getDataSource() :
            owner instanceof OracleDataSource ? (OracleDataSource) owner : null;
    }

    @Property(name = "Type Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return typeName;
    }

    @Property(name = "Code", viewable = true, editable = true, order = 2)
    public String getTypeCode()
    {
        return typeCode;
    }

    public byte[] getTypeOID()
    {
        return typeOID;
    }

    @Property(name = "Super Type", viewable = true, editable = true, order = 3)
    public OracleDataType getSuperType(DBRProgressMonitor monitor)
    {
        if (superType  == null) {
            return null;
        } else if (superType instanceof OracleDataType) {
            return (OracleDataType)superType;
        } else {
            try {
                OracleLazyReference olr = (OracleLazyReference) superType;
                final OracleSchema superSchema = getDataSource().getSchema(monitor, olr.schemaName);
                if (superSchema == null) {
                    log.warn("Referenced schema '" + olr.schemaName + "' not found for super type '" + olr.objectName + "'");
                } else {
                    superType = superSchema.dataTypeCache.getObject(monitor, superSchema, olr.objectName);
                    if (superType == null) {
                        log.warn("Referenced type '" + olr.objectName + "' not found in schema '" + olr.schemaName + "'");
                    } else {
                        return (OracleDataType)superType;
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
            superType = null;
            return null;
        }
    }

    @Property(name = "Predefined", viewable = true, order = 4)
    public boolean isPredefined()
    {
        return flagPredefined;
    }

    @Property(name = "Incomplete", viewable = true, order = 5)
    public boolean isIncomplete()
    {
        return flagIncomplete;
    }

    @Property(name = "Final", viewable = true, order = 6)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(name = "Instantiable", viewable = true, order = 7)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    public boolean hasAttributes()
    {
        return attributeCache != null;
    }

    public boolean hasMethods()
    {
        return methodCache != null;
    }

    @Association
    public Collection<OracleDataTypeAttribute> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        return attributeCache != null ? attributeCache.getObjects(monitor, this) : null;
    }

    @Association
    public Collection<OracleDataTypeMethod> getMethods(DBRProgressMonitor monitor)
        throws DBException
    {
        return methodCache != null ? methodCache.getObjects(monitor, this) : null;
    }

    public String getFullQualifiedName()
    {
        return owner instanceof OracleSchema ?
            DBUtils.getFullQualifiedName(getDataSource(), owner, this) :
            typeName;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

    @Override
    public String toString()
    {
        return getFullQualifiedName();
    }

    public static OracleDataType resolveDataType(DBRProgressMonitor monitor, OracleDataSource dataSource, String typeOwner, String typeName)
    {
        OracleSchema typeSchema = null;
        OracleDataType type = null;
        if (typeOwner != null) {
            try {
                typeSchema = dataSource.getSchema(monitor, typeOwner);
                if (typeSchema == null) {
                    OracleUtils.log.error("Type attr schema '" + typeOwner + "' not found");
                } else {
                    type = typeSchema.getDataType(monitor, typeName);
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else {
            type = (OracleDataType)dataSource.getDataType(typeName);
        }
        if (type == null) {
            log.debug("Data type '" + typeName + "' not found - declare new one");
            type = new OracleDataType(typeSchema == null ? dataSource : typeSchema, typeName, true);
            type.flagPredefined = true;
            if (typeSchema == null) {
                dataSource.dataTypeCache.cacheObject(type);
            } else {
                typeSchema.dataTypeCache.cacheObject(type);
            }
        }
        return type;
    }

    private class AttributeCache extends JDBCObjectCache<OracleDataType, OracleDataTypeAttribute> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPE_ATTRS " +
                "WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
            dbStat.setString(1, OracleDataType.this.owner.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }
        @Override
        protected OracleDataTypeAttribute fetchObject(JDBCExecutionContext context, OracleDataType owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeAttribute(context.getProgressMonitor(), OracleDataType.this, resultSet);
        }
    }

    private class MethodCache extends JDBCObjectCache<OracleDataType, OracleDataTypeMethod> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT m.*,r.RESULT_TYPE_OWNER,RESULT_TYPE_NAME,RESULT_TYPE_MOD\n" +
                "FROM SYS.ALL_TYPE_METHODS m\n" +
                "LEFT OUTER JOIN SYS.ALL_METHOD_RESULTS r ON r.OWNER=m.OWNER AND r.TYPE_NAME=m.TYPE_NAME AND r.METHOD_NAME=m.METHOD_NAME AND r.METHOD_NO=m.METHOD_NO\n" +
                "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" +
                "ORDER BY m.METHOD_NO");
            dbStat.setString(1, OracleDataType.this.owner.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected OracleDataTypeMethod fetchObject(JDBCExecutionContext context, OracleDataType owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeMethod(context.getProgressMonitor(), OracleDataType.this, resultSet);
        }
    }

}
