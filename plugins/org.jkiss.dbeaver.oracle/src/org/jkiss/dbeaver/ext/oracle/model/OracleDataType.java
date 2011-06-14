/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle data type
 */
public class OracleDataType implements DBSDataType, OracleLazyObject<OracleDataType> {

    static final Log log = LogFactory.getLog(OracleForeignKey.class);

    private OracleSchema schema;
    private String typeName;
    private String typeCode;
    private byte[] typeOID;
    private byte[] typeID;
    private OracleLazyObject<OracleDataType> superType;
    private boolean hasAttributes, hasMethods;
    private boolean flagPredefined;
    private boolean flagIncomplete;
    private boolean flagFinal;
    private boolean flagInstantiable;

    private boolean persisted;

    private List<OracleDataTypeAttribute> attributes;
    private List<OracleDataTypeMethod> methods;

    public OracleDataType(OracleSchema schema, boolean persisted)
    {
        this.schema = schema;
        this.persisted = persisted;
    }

    public OracleDataType(OracleSchema schema, ResultSet dbResult)
    {
        this(schema, true);
        this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        this.typeCode = JDBCUtils.safeGetString(dbResult, "TYPECODE");
        this.typeOID = JDBCUtils.safeGetBytes(dbResult, "TYPE_OID");
        this.typeID = JDBCUtils.safeGetBytes(dbResult, "TYPE_ID");
        this.flagPredefined = JDBCUtils.safeGetBoolean(dbResult, "PREDEFINED", OracleConstants.YES);
        this.flagIncomplete = JDBCUtils.safeGetBoolean(dbResult, "INCOMPLETE", OracleConstants.YES);
        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", OracleConstants.YES);
        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", OracleConstants.YES);
        String superTypeOwner = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_OWNER");
        if (!CommonUtils.isEmpty(superTypeOwner)) {
            String superTypeName = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_NAME");
            this.superType = new OracleLazyReference<OracleDataType>(superTypeOwner, superTypeName);
            this.hasAttributes = JDBCUtils.safeGetInt(dbResult, "LOCAL_ATTRIBUTES") > 0;
            this.hasMethods = JDBCUtils.safeGetInt(dbResult, "LOCAL_METHODS") > 0;
        } else {
            this.hasAttributes = JDBCUtils.safeGetInt(dbResult, "ATTRIBUTES") > 0;
            this.hasMethods = JDBCUtils.safeGetInt(dbResult, "METHODS") > 0;
        }
    }

    boolean resolveLazyReference(DBRProgressMonitor monitor)
    {
        if (superType  == null || superType instanceof OracleDataType) {
            return true;
        } else {
            try {
                OracleLazyReference olr = (OracleLazyReference) superType;
                final OracleSchema superSchema = getDataSource().getSchema(monitor, olr.schemaName);
                if (superSchema == null) {
                    log.warn("Referenced schema '" + olr.schemaName + "' not found for super type '" + olr.objectName + "'");
                    return false;
                }
                superType = superSchema.getDataTypeCache().getObject(monitor, olr.objectName);
                if (superType == null) {
                    log.warn("Referenced type '" + olr.objectName + "' not found in schema '" + olr.schemaName + "'");
                    return false;
                }
                return true;
            } catch (DBException e) {
                log.error(e);
                return false;
            }
        }
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public int getValueType()
    {
        return java.sql.Types.OTHER;
    }

    public DBSDataKind getDataKind()
    {
        return DBSDataKind.UNKNOWN;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return schema;
    }

    public OracleDataSource getDataSource()
    {
        return schema.getDataSource();
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

    public byte[] getTypeID()
    {
        return typeID;
    }

    @Property(name = "Super Type", viewable = true, editable = true, order = 3)
    public OracleDataType getSuperType()
    {
        return superType == null ? null : superType.getObject();
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
        return hasAttributes;
    }

    public boolean hasMethods()
    {
        return hasMethods;
    }

    public List<OracleDataTypeAttribute> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (attributes == null && hasAttributes) {
            List<OracleDataTypeAttribute> tmpAttrs = new ArrayList<OracleDataTypeAttribute>();
            final JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load type attributes");
            try {
                final JDBCPreparedStatement dbStat = context.prepareStatement(
                    "SELECT * FROM SYS.ALL_TYPE_ATTRS WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
                try {
                    dbStat.setString(1, schema.getName());
                    dbStat.setString(2, getName());
                    final JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            tmpAttrs.add(
                                new OracleDataTypeAttribute(monitor, this, dbResult));
                        }
                    } finally {
                        dbResult.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                throw new DBException("Can't read type attributes", e);
            } finally {
                context.close();
            }
            this.attributes = tmpAttrs;
        }
        return attributes;
    }

    public List<OracleDataTypeMethod> getMethods(DBRProgressMonitor monitor)
        throws DBException
    {
        if (methods == null && hasMethods) {
            List<OracleDataTypeMethod> tmpMethods = new ArrayList<OracleDataTypeMethod>();
            final JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load type methods");
            try {
                final JDBCPreparedStatement dbStat = context.prepareStatement(
                    "SELECT * FROM SYS.ALL_TYPE_METHODS WHERE OWNER=? AND TYPE_NAME=? ORDER BY METHOD_NO");
                try {
                    dbStat.setString(1, schema.getName());
                    dbStat.setString(2, getName());
                    final JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            tmpMethods.add(
                                new OracleDataTypeMethod(monitor, this, dbResult));
                        }
                    } finally {
                        dbResult.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                throw new DBException("Can't read type methods", e);
            } finally {
                context.close();
            }
            this.methods = tmpMethods;
        }
        return methods;
    }

    public OracleDataType getObject()
    {
        return this;
    }
}
