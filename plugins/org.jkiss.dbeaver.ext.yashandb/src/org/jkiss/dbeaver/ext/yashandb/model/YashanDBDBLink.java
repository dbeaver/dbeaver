package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/8 17:22
 */
public class YashanDBDBLink extends YashanDBGlobalObject implements DBPScriptObject {

    private static final Log log = Log.getLog(YashanDBDBLink.class);

    String name;
    private String userName;
    private String host;
    private Date created;
    private String password;
    private String owner;

    //    protected YashanDBDBLink(DBRProgressMonitor progressMonitor, YashanDBSchema schema, ResultSet dbResult)
//    {
//        super(schema, JDBCUtils.safeGetString(dbResult, "DB_LINK"), true);
//        this.userName = JDBCUtils.safeGetString(dbResult, "USERNAME");
//        this.host = JDBCUtils.safeGetString(dbResult, "HOST");
//        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
//    }
    protected YashanDBDBLink(YashanDBDataSource source, ResultSet dbResult) {
        super(source, dbResult != null);
        this.name=JDBCUtils.safeGetString(dbResult, "DB_LINK");
        this.userName = JDBCUtils.safeGetString(dbResult, "USERNAME");
        this.host = JDBCUtils.safeGetString(dbResult, "HOST");
        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.password="******";
    }

    public YashanDBDBLink(YashanDBDataSource source, String name) {
        super(source, false);
        this.name=name;
        this.userName = null;
        this.password = null;
        this.created = null;
        this.owner=null;
        this.host = null;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, editable = true)
    public String getName() {
        return name;
    }

    @Property(viewable = true,order = 2)
    public String getOwner(){
        return owner;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public String getUserName() {
        return userName;
    }

    @Property(order = 4, editable = true, updatable = true,viewable = true)
    public String getPassword() {
        return password;
    }

    @Property(viewable = true, editable = true, order = 5)
    public String getHost() {
        return host;
    }

    @Property(viewable = true, order = 6)
    public Date getCreated() {
        return created;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    //    public static Object resolveObject(DBRProgressMonitor monitor, YashanDBSchema schema, String dbLink) throws DBException {
//        if (CommonUtils.isEmpty(dbLink)) {
//            return null;
//        }
//        final YashanDBDBLink object = schema.dbLinkCache.getObject(monitor, schema, dbLink);
//        if (object == null) {
//            log.warn("DB Link '" + dbLink + "' not found in schema '" + schema.getName() + "'");
//            return dbLink;
//        }
//        return object;
//    }

    public String buildStatement(boolean isUpdate) {
        StringBuffer sb = new StringBuffer();
        sb.append(isUpdate ? "ALTER" : "CREATE");
        sb.append(" DATABASE LINK ").append(getName()).append(" CONNECT TO ");
        sb.append(getUserName());
        sb.append(" IDENTIFIED BY ");
        sb.append(getPassword());
        if (!isUpdate) {
            sb.append(" USING ").append("'").append(getHost()).append("'");
        }
        return sb.toString();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return buildStatement(false);
    }



}

