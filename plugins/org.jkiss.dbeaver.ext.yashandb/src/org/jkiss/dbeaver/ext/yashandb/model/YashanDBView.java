package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.sql.ResultSet;
import java.util.Map;

public class YashanDBView extends YashanDBTableBase implements YashanDBSourceObject, DBSView {
    private static final Log log = Log.getLog(YashanDBView.class);

    @Override
    public String getDescription(DBRProgressMonitor monitor) {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

//    @Override
//    public boolean isFeatureSupported(String feature) {
//        return false;
//    }


    public class AdditionalInfo extends TableAdditionalInfo {
        private String typeText;
        private String oidText;
        private String typeOwner;
        private String typeName;
        private YashanDBView superView;

        @Property(viewable = false, order = 11)
        public String getTypeText() {
            return typeText;
        }

        public void setTypeText(String typeText) {
            this.typeText = typeText;
        }

        @Property(viewable = false, order = 12)
        public String getOidText() {
            return oidText;
        }

        public void setOidText(String oidText) {
            this.oidText = oidText;
        }

        @Property(viewable = false, editable = true, order = 5)
        public YashanDBView getSuperView() {
            return superView;
        }

        public void setSuperView(YashanDBView superView) {
            this.superView = superView;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();
    private String viewText;
    private String viewSourceText;
    private YashanDBDDLFormat currentDDLFormat;

    public YashanDBView(YashanDBSchema schema, String name) {
        super(schema, name, false);
    }

    public YashanDBView(YashanDBSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public YashanDBSourceType getSourceType() {
        // TODO Auto-generated method stub
        return YashanDBSourceType.VIEW;
    }

    public void setObjectDefinitionText(String source) {
        this.viewText = source;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.additionalInfo.loaded = false;
        this.viewText = null;
        this.viewSourceText = null;
        return super.refreshObject(monitor);
    }

    @Override
    protected String getTableTypeName() {
        return "VIEW";
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo() {
        return null;
    }

    public String getViewText() {
        return viewText;
    }

    public void setViewText(String viewText) {
        this.viewText = viewText;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        // TODO Auto-generated method stub
        if(viewText!=null){
            return viewText;
        }
        return YashanDBUtils.getTableOrViewDDL(monitor, getTableTypeName(), this, options);
    }

    @Override
    public DBSObjectState getObjectState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
        // TODO Auto-generated method stub


    }

    @Override
    public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) throws DBCException {
        // TODO Auto-generated method stub
        return null;
    }


}
