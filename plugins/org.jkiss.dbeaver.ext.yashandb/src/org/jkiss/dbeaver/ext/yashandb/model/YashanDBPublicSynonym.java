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

public class YashanDBPublicSynonym extends YashanDBGlobalObject implements DBSAlias{
    private String name;
    private String objectOwner;
    private String objectTypeName;
    private String objectName;
    private String dbLink;
    private boolean isPublic;



    protected YashanDBPublicSynonym(YashanDBDataSource source, ResultSet dbResult) {
        super(source,dbResult!=null);
        this.name=JDBCUtils.safeGetString(dbResult,"SYNONYM_NAME");
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
    @Property(viewable = true,  order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public String getObjectOwner() {
        return objectOwner;
    }

    @Property(viewable = true, order = 3)
    public String getObjectName() {
        return objectName;
    }

    @Property(viewable = true, order = 4)
    public String getObjectTypeName() {
        return objectTypeName;
    }

    @Property(viewable = true,order = 5)
    public boolean getIsPublic() {
        return isPublic;
    }

    @Property(viewable = true, linkPossible = true, order = 6)
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

    public void setName(String name) {
        this.name = name;
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

}