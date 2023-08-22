package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBTriggerColumn extends AbstractTriggerColumn {
    private static final Log log = Log.getLog(YashanDBTriggerColumn.class);

    private YashanDBTrigger trigger;
    private String name;
    private YashanDBTableColumn tableColumn;
    private boolean columnList;

    public YashanDBTriggerColumn(
            DBRProgressMonitor monitor,
            YashanDBTrigger trigger,
            YashanDBTableColumn tableColumn,
            ResultSet dbResult) throws DBException {
        this.trigger = trigger;
        this.tableColumn = tableColumn;
        this.name = JDBCUtils.safeGetString(dbResult, "TRIGGER_COLUMN_NAME");
        this.columnList = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_LIST", "YES");
    }

    YashanDBTriggerColumn(YashanDBTrigger trigger,YashanDBTriggerColumn source){
        this.trigger = trigger;
        this.tableColumn = source.tableColumn;
        this.columnList = source.columnList;
    }


    @Override
    public YashanDBTrigger getTrigger() {
        return trigger;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    @Property(viewable = true, order = 2)
    public YashanDBTableColumn getTableColumn() {
        return tableColumn;
    }

    @Override
    public int getOrdinalPosition() {
        return 0;
    }

    @Nullable
    @Override
    public String getDescription() {
        return tableColumn.getDescription();
    }

    @Override
    public YashanDBTrigger getParentObject() {
        return trigger;
    }

    @NotNull
    @Override
    public YashanDBDataSource getDataSource() {
        return trigger.getDataSource();
    }

    @Override
    public String toString() {
        return getName();
    }
}
