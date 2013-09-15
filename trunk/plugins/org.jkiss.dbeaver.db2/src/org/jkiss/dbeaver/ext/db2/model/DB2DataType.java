/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2DataTypeMetaType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 data types
 * 
 * @author Denis Forveille
 */
public class DB2DataType extends DB2Object<DBSObject> implements DBSDataType, DBPQualifiedObject {

   private static final Log                   LOG              = LogFactory.getLog(DB2DataType.class);

   private static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<String, TypeDesc>(32);   // See init below

   private DB2Schema                          db2Schema;

   private TypeDesc                           typeDesc;

   private Integer                            db2TypeId;

   private String                             owner;
   private DB2OwnerType                       ownerType;

   private String                             moduleName;

   private String                             sourceSchema;
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

   // TODO DF: add missing attributes

   // -----------------------
   // Constructors
   // -----------------------

   public DB2DataType(DBSObject owner, ResultSet dbResult) {
      super(owner, JDBCUtils.safeGetStringTrimmed(dbResult, "TYPENAME"), true);

      this.db2TypeId = JDBCUtils.safeGetInteger(dbResult, "TYPEID");

      this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
      this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
      this.moduleName = JDBCUtils.safeGetString(dbResult, "TYPEMODULENAME");
      this.sourceSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCESCHEMA");
      this.sourceModuleName = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCEMODULENAME");
      this.sourceName = JDBCUtils.safeGetString(dbResult, "SOURCENAME");
      this.metaType = CommonUtils.valueOf(DB2DataTypeMetaType.class, JDBCUtils.safeGetString(dbResult, "METATYPE"));
      this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
      this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
      this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
      this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
      // DB2 v10 this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
      // DB2 v10 this.constraintText = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TEXT");
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

      // Store associated DB2Schema
      if (owner instanceof DB2Schema) {
         this.db2Schema = (DB2Schema) owner;
      } else {
         String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPESCHEMA");
         try {
            this.db2Schema = ((DB2DataSource) owner).getSchema(VoidProgressMonitor.INSTANCE, schemaName);
         } catch (DBException e) {
            LOG.error("Impossible! Schema '" + schemaName + "' for dataType '" + name + "' not found??");
         }
      }

      // Determine DBSKind and javax.sql.Types
      // TODO DF: not 100% accurate...
      TypeDesc tempTypeDesc;
      tempTypeDesc = PREDEFINED_TYPES.get(name);
      if (tempTypeDesc == null) {
         LOG.debug(name + " is not a predefined type. Trying with source type.");
         tempTypeDesc = PREDEFINED_TYPES.get(sourceName);
         if (tempTypeDesc != null) {
            LOG.debug(name + " DBSDataKind set to source type : " + tempTypeDesc.dataKind);
         } else {
            LOG.warn("No predefined type found neither from source. Set its DBSDataKind to UNKNOWN/OTHER");
            tempTypeDesc = new TypeDesc(DBSDataKind.UNKNOWN, Types.OTHER);
         }
      }
      this.typeDesc = tempTypeDesc;

   }

   @Override
   public String getTypeName() {
      return name;
   }

   @Override
   public String getFullQualifiedName() {
      return db2Schema.getName() + "." + name;
   }

   // -----------------
   // TODO DF: What to do with those methods? read data from JDBC metadata?
   // -----------------

   @Override
   public int getPrecision() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public int getMinScale() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public int getMaxScale() {
      // TODO Auto-generated method stub
      return 0;
   }

   public int getEquivalentSqlType() {
      return typeDesc.sqlType;
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
   public String getName() {
      return name;
   }

   @Property(viewable = true, editable = false, order = 2)
   public DB2Schema getSchema() {
      return db2Schema;
   }

   @Property(viewable = true, editable = false, order = 3)
   public String getModuleName() {
      return moduleName;
   }

   @Override
   @Property(viewable = false, editable = false)
   public int getTypeID() {
      return typeDesc.sqlType;
   }

   @Override
   @Property(viewable = true, editable = false, order = 4)
   public DBSDataKind getDataKind() {
      return typeDesc.dataKind;
   }

   @Override
   @Property(viewable = true, editable = false, order = 5)
   public long getMaxLength() {
      return length;
   }

   @Override
   @Property(viewable = true, editable = false, order = 6)
   public int getScale() {
      return scale;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwner() {
      return owner;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public DB2OwnerType getOwnerType() {
      return ownerType;
   }

   @Property(viewable = false, editable = false)
   public String getSourceSchema() {
      return sourceSchema;
   }

   @Property(viewable = false, editable = false)
   public String getSourceModuleName() {
      return sourceModuleName;
   }

   @Property(viewable = false, editable = false)
   public String getSourceName() {
      return sourceName;
   }

   @Property(viewable = false, editable = false)
   public DB2DataTypeMetaType getMetaType() {
      return metaType;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getCreateTime() {
      return createTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getAlterTime() {
      return alterTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getLastRegenTime() {
      return lastRegenTime;
   }

   @Property(viewable = false, editable = false)
   public String getConstraintText() {
      return constraintText;
   }

   @Override
   @Property(viewable = false, editable = false)
   public String getDescription() {
      return remarks;
   }

   @Property(viewable = false, editable = false)
   public Integer getDb2TypeId() {
      return db2TypeId;
   }

   // --------------
   // Helper Objects
   // --------------
   private static final class TypeDesc {
      private final DBSDataKind dataKind;
      private final int         sqlType;

      private TypeDesc(DBSDataKind dataKind, int sqlType) {
         this.dataKind = dataKind;
         this.sqlType = sqlType;
      }
   }

   static {
      PREDEFINED_TYPES.put("ARRAY", new TypeDesc(DBSDataKind.ARRAY, Types.ARRAY));
      PREDEFINED_TYPES.put("BIGINT", new TypeDesc(DBSDataKind.NUMERIC, Types.BIGINT));
      PREDEFINED_TYPES.put("BINARY", new TypeDesc(DBSDataKind.BINARY, Types.BINARY));
      PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBSDataKind.LOB, Types.BLOB));
      PREDEFINED_TYPES.put("BOOLEAN", new TypeDesc(DBSDataKind.BOOLEAN, Types.BOOLEAN));
      PREDEFINED_TYPES.put("CHARACTER", new TypeDesc(DBSDataKind.STRING, Types.CHAR));
      PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBSDataKind.LOB, Types.CLOB));
      PREDEFINED_TYPES.put("CURSOR", new TypeDesc(DBSDataKind.UNKNOWN, Types.OTHER));
      PREDEFINED_TYPES.put("DATE", new TypeDesc(DBSDataKind.DATETIME, Types.DATE));
      PREDEFINED_TYPES.put("DBCLOB", new TypeDesc(DBSDataKind.LOB, Types.CLOB));
      PREDEFINED_TYPES.put("DECFLOAT", new TypeDesc(DBSDataKind.NUMERIC, Types.DECIMAL));
      PREDEFINED_TYPES.put("DECIMAL", new TypeDesc(DBSDataKind.NUMERIC, Types.DECIMAL));
      PREDEFINED_TYPES.put("DOUBLE", new TypeDesc(DBSDataKind.NUMERIC, Types.DOUBLE));
      PREDEFINED_TYPES.put("GRAPHIC", new TypeDesc(DBSDataKind.STRING, Types.CHAR));
      PREDEFINED_TYPES.put("INTEGER", new TypeDesc(DBSDataKind.NUMERIC, Types.INTEGER));
      PREDEFINED_TYPES.put("LONG VARCHAR", new TypeDesc(DBSDataKind.STRING, Types.LONGVARCHAR));
      PREDEFINED_TYPES.put("LONG VARGRAPHIC", new TypeDesc(DBSDataKind.STRING, Types.LONGVARCHAR));
      PREDEFINED_TYPES.put("REAL", new TypeDesc(DBSDataKind.NUMERIC, Types.REAL));
      PREDEFINED_TYPES.put("REFERENCE", new TypeDesc(DBSDataKind.REFERENCE, Types.REF));
      PREDEFINED_TYPES.put("ROW", new TypeDesc(DBSDataKind.STRUCT, Types.ROWID));
      PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBSDataKind.NUMERIC, Types.SMALLINT));
      PREDEFINED_TYPES.put("TIME", new TypeDesc(DBSDataKind.DATETIME, Types.TIME));
      PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBSDataKind.DATETIME, Types.TIMESTAMP));
      PREDEFINED_TYPES.put("VARBINARY", new TypeDesc(DBSDataKind.BINARY, Types.VARBINARY));
      PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBSDataKind.STRING, Types.VARCHAR));
      PREDEFINED_TYPES.put("VARGRAPHIC", new TypeDesc(DBSDataKind.STRING, Types.VARCHAR));
      PREDEFINED_TYPES.put("XML", new TypeDesc(DBSDataKind.UNKNOWN, Types.SQLXML));
   }

}
