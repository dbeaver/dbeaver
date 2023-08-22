package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.Map;
import java.util.Objects;

public class YashanDBSynonym extends YashanDBSchemaObject implements DBSAlias, DBPScriptObject {

    private String objectOwner;
    private String objectTypeName;
    private String objectName;
    private String dbLink;
    private boolean isPublic;

    public YashanDBSynonym(YashanDBSchema schema,String name){
        super(schema,name,false);
        this.objectName=null;
        this.objectOwner=null;
        this.objectTypeName=null;
        this.dbLink=null;
        this.isPublic=false;
    }

    public YashanDBSynonym(YashanDBSchema schema, ResultSet dbResult) {
        super(schema, JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"), true);
        this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        this.objectOwner = JDBCUtils.safeGetString(dbResult, "TABLE_OWNER");
        this.objectName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.dbLink = JDBCUtils.safeGetString(dbResult, "DB_LINK");
        this.isPublic= Objects.requireNonNull(JDBCUtils.safeGetString(dbResult, "OWNER")).equalsIgnoreCase("PUBLIC");
    }

    public YashanDBObjectType getObjectType() {
        return YashanDBObjectType.getByType(objectTypeName);
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return super.getName();
    }



    @Property(viewable = true,editable = true, order = 2)
    public String getObjectOwner() {
//        final YashanDBSchema schema = getDataSource().schemaCache.getCachedObject(objectOwner);
//        return schema == null ? objectOwner : schema;
        return objectOwner;
    }

    @Property(viewable = true,editable = true, order = 3)
    public String getObjectName() {
        return objectName;
    }

    @Property(viewable = true, order = 4)
    public String getObjectTypeName() {
        return objectTypeName;
    }

    @Property(viewable = true,editable = true,order = 5)
    public boolean getIsPublic() {
        return isPublic;
    }

    @Property(viewable = true,editable = true, linkPossible = true, order = 6)
    public Object getObject(DBRProgressMonitor monitor) throws DBException {
        if (objectTypeName == null) {
            return null;
        }
        return YashanDBObjectType.resolveObject(
                monitor,
                getDataSource(),
                dbLink,
                objectTypeName,
                objectOwner,
                objectName);
    }

    /** Cross-database access is not currently supported in Yashan, use String as return instead.*/
    @Property(viewable = true, order = 7)
    public String getDbLink() throws DBException {
        return dbLink;
    }

    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        Object object = getObject(monitor);
        if (object instanceof DBSObject) {
            return (DBSObject) object;
        }
        return null;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        if (YashanDBConstants.USER_PUBLIC.equals(getSchema().getName())) {
            return DBUtils.getQuotedIdentifier(this);
        }
        return super.getFullyQualifiedName(context);
    }

    public void setObjectOwner(String objectOwner) {
        this.objectOwner = objectOwner;
    }

    public void setObjectTypeName(String objectTypeName) {
        this.objectTypeName = objectTypeName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public void setDbLink(String dbLink) {
        this.dbLink = dbLink;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String buildStatement(){
        StringBuffer stmt=new StringBuffer();
        stmt.append("CREATE ");
        if(isPublic) {
            stmt.append("PUBLIC SYNONYM ").append(getName()).append(" ");
        }else {
            stmt.append("SYNONYM ").append(this.getSchema().getName()).append(".").append(getName()).append(" ");
        }

        if(getObjectName()==null){
            stmt.append("FOR ").append("*.*");
        }else {
            stmt.append("FOR ").append(getObjectOwner()).append(".").append(getObjectName());
        }
        return stmt.toString();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return buildStatement();
    }
}
