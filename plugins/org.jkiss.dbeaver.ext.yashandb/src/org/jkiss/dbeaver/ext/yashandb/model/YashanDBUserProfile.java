package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/6 15:02
 */
public class YashanDBUserProfile extends YashanDBGlobalObject implements DBPScriptObject {
    private static final Log log = Log.getLog(YashanDBUserProfile.class);

    private String name;
    private String passwordReuseTime;
    private String passwordLockTime;
    private String passwordGraceTime;
    private String passwordReuseMax;
    private String passwordLifeTime;
    private String failLoginAttempts;
    String sourceText;

    public YashanDBUserProfile(YashanDBDataSource dataSource,String name){
        super(dataSource,false);
        this.name=name;
        this.failLoginAttempts = "UNLIMITED";
        this.passwordGraceTime = "UNLIMITED";
        this.passwordLifeTime = "UNLIMITED";
        this.passwordLockTime = "UNLIMITED";
        this.passwordReuseMax = "UNLIMITED";
        this.passwordReuseTime = "UNLIMITED";
    }

    public YashanDBUserProfile(YashanDBDataSource dataSource, ResultSet resultSet) {
        super(dataSource, resultSet != null);
        this.name = JDBCUtils.safeGetString(resultSet, "PROFILE");
        String[] parms = JDBCUtils.safeGetString(resultSet, "PARMS").split(",");
        String[] limits = JDBCUtils.safeGetString(resultSet, "LIMITS").split(",");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < parms.length; i++) {
            map.put(parms[i], limits[i]);
        }
//        String sss = "PASSWORD_GRACE_TIME,PASSWORD_LOCK_TIME,PASSWORD_REUSE_MAX,PASSWORD_REUSE_TIME,PASSWORD_LIFE_TIME,FAILED_LOGIN_ATTEMPTS";
        this.failLoginAttempts = map.get("FAILED_LOGIN_ATTEMPTS");
        this.passwordGraceTime = map.get("PASSWORD_GRACE_TIME");
        this.passwordLifeTime = map.get("PASSWORD_LIFE_TIME");
        this.passwordLockTime = map.get("PASSWORD_LOCK_TIME");
        this.passwordReuseMax = map.get("PASSWORD_REUSE_MAX");
        this.passwordReuseTime = map.get("PASSWORD_REUSE_TIME");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, editable = true)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2, editable = true, updatable = true)
    public String getPasswordReuseTime() {
        return passwordReuseTime;
    }

    @Property(viewable = true, order = 3, editable = true, updatable = true)
    public String getPasswordLockTime() {
        return passwordLockTime;
    }

    @Property(viewable = true, order = 4, editable = true, updatable = true)
    public String getPasswordGraceTime() {
        return passwordGraceTime;
    }

    @Property(viewable = true, order = 5, editable = true, updatable = true)
    public String getPasswordReuseMax() {
        return passwordReuseMax;
    }

    @Property(viewable = true, order = 6, editable = true, updatable = true)
    public String getPasswordLifeTime() {
        return passwordLifeTime;
    }

    @Property(viewable = true, order = 7, editable = true, updatable = true)
    public String getFailLoginAttempts() {
        return failLoginAttempts;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPasswordReuseTime(String passwordReuseTime) {
        this.passwordReuseTime = passwordReuseTime;
    }

    public void setPasswordLockTime(String passwordLockTime) {
        this.passwordLockTime = passwordLockTime;
    }

    public void setPasswordGraceTime(String passwordGraceTime) {
        this.passwordGraceTime = passwordGraceTime;
    }

    public void setPasswordReuseMax(String passwordReuseMax) {
        this.passwordReuseMax = passwordReuseMax;
    }

    public void setPasswordLifeTime(String passwordLifeTime) {
        this.passwordLifeTime = passwordLifeTime;
    }

    public void setFailLoginAttempts(String failLoginAttempts) {
        this.failLoginAttempts = failLoginAttempts;
    }

    public String buildStatement(boolean isUpdate) {
        StringBuffer sb = new StringBuffer();
        sb.append(isUpdate?"ALTER":"CREATE");
        sb.append(" PROFILE ").append(getName()).append(" LIMIT ");
        if (getFailLoginAttempts() != null) sb.append(" FAILED_LOGIN_ATTEMPTS ").append(getFailLoginAttempts());
        if (getPasswordLifeTime() != null) sb.append(" PASSWORD_LIFE_TIME ").append(getPasswordLifeTime());
        if (getPasswordReuseTime() != null) sb.append(" PASSWORD_REUSE_TIME ").append(getPasswordReuseTime());
        if (getPasswordReuseMax() != null) sb.append(" PASSWORD_REUSE_MAX ").append(getPasswordReuseMax());
        if (getPasswordLockTime() != null) sb.append(" PASSWORD_LOCK_TIME ").append(getPasswordLockTime());
        if (getPasswordGraceTime() != null) sb.append(" PASSWORD_GRACE_TIME ").append(getPasswordGraceTime());
        return sb.toString();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return buildStatement(false);
    }


    @Association
    public Collection<ProfileResource> getResources(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().profileCache.getChildren(monitor, getDataSource(), this);
    }

    /**
     * ProfileResource
     */
    public static class ProfileResource extends YashanDBObject<YashanDBUserProfile> {
        private static final Log log = Log.getLog(ProfileResource.class);

        private String type;
        private String limit;

        public ProfileResource(YashanDBUserProfile profile, ResultSet resultSet) {
            super(profile, JDBCUtils.safeGetString(resultSet, "RESOURCE_NAME"), true);
            this.type = JDBCUtils.safeGetString(resultSet, "RESOURCE_TYPE");
            this.limit = JDBCUtils.safeGetString(resultSet, "LIMIT");
        }

        @NotNull
        @Override
        @Property(viewable = true, order = 1)
        public String getName() {
            return super.getName();
        }

        @Property(viewable = true, order = 2)
        public String getType() {
            return type;
        }

        @Property(viewable = true, order = 3)
        public String getLimit() {
            return limit;
        }
    }

}

