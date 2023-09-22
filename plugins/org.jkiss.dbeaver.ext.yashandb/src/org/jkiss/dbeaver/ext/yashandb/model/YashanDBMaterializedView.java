package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/6 18:44
 */
public class YashanDBMaterializedView extends YashanDBTableBase implements YashanDBSourceObject, DBSObjectLazy<YashanDBDataSource> {
    private static final Log log = Log.getLog(YashanDBMaterializedView.class);

    @Override
    public boolean isFeatureSupported(String feature) {
        return super.isFeatureSupported(feature);
    }

    public static class AdditionalInfo extends TableAdditionalInfo {
        private volatile boolean loaded = false;

        private boolean mviewValid;
        private Object container;
        private boolean updatable;
        private boolean rewriteEnabled;
        private boolean valid;
        private String rewriteCapability;
        private String refreshMode;
        private String refreshMethod;
        private String buildMode;
        private String fastRefreshable;
        private String lastRefreshType;
        private Date lastRefreshDate;
        private String staleness;

        @Property(viewable = false, order = 14)
        public boolean isUpdatable() {
            return updatable;
        }

        @Property(viewable = false, order = 15)
        public boolean isRewriteEnabled() {
            return rewriteEnabled;
        }

        @Property(viewable = false, order = 16)
        public String getRewriteCapability() {
            return rewriteCapability;
        }

        @Property(viewable = false, order = 17)
        public String getRefreshMode() {
            return refreshMode;
        }

        @Property(viewable = false, order = 18)
        public String getRefreshMethod() {
            return refreshMethod;
        }

        @Property(viewable = false, order = 19)
        public String getBuildMode() {
            return buildMode;
        }

        @Property(viewable = false, order = 20)
        public String getFastRefreshable() {
            return fastRefreshable;
        }

        @Property(viewable = false, order = 21)
        public String getLastRefreshType() {
            return lastRefreshType;
        }

        @Property(viewable = false, order = 22)
        public Date getLastRefreshDate() {
            return lastRefreshDate;
        }

        @Property(viewable = false, order = 23)
        public String getStaleness() {
            return staleness;
        }

    }

    public String getComment(DBRProgressMonitor monitor) {
        return null;
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();
    private String query;
    private YashanDBDDLFormat currentDDLFormat;

    public YashanDBMaterializedView(YashanDBSchema schema, String name) {
        super(schema, name, false);
    }

    public YashanDBMaterializedView(
            YashanDBSchema schema,
            ResultSet dbResult) {
        super(schema, dbResult);
    }

    @Override
    public YashanDBSourceType getSourceType() {
        return YashanDBSourceType.MATERIALIZED_VIEW;
    }

    @Override
    public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) throws DBCException {
        return null;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (query != null) return query;
        return YashanDBUtils.getTableOrViewDDL(monitor, "MATERIALIZED VIEW", this, options);
    }

    public void setObjectDefinitionText(String source) {
        this.query = source;
    }

    public String getMViewText() {
        return query;
    }

    public void setCurrentDDLFormat(YashanDBDDLFormat currentDDLFormat) {
        this.currentDDLFormat = currentDDLFormat;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        this.valid = YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.MATERIALIZED_VIEW);
    }

    @Override
    public Object getLazyReference(Object propertyId) {
        return additionalInfo.container;
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    @Override
    protected String getTableTypeName() {
        return "MATERIALIZED_VIEW";
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        getContainer().constraintCache.clearObjectCache(this);
        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

}

