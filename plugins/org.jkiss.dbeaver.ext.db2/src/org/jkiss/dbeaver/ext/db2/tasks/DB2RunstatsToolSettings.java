package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

public class DB2RunstatsToolSettings extends SQLToolExecuteSettings<DB2TableBase> {

    private final static String[] columnStats = new String[] {"ON ALL COLUMNS WITH DISTRIBUTION ON ALL COLUMNS", "ON ALL COLUMNS", ""};
    private final static String[] indexStats = new String[] {"AND SAMPLED DETAILED INDEXES ALL", "AND INDEXES ALL", ""};
    private String columnStat;
    private String indexStat;
    private boolean isTableSampling;
    private int samplePercent;

    @Property(viewable = true, editable = true, updatable = true, order = 1, listProvider = DB2RunstatsToolSettings.CheckStorageOptionListProvider.class)
    public String getColumnStat() {
        if (columnStat == null) {
            columnStat = columnStats[0];
        }
        return columnStat;
    }

    public void setColumnStat(String columnStat) {
        this.columnStat = columnStat;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2, listProvider = DB2RunstatsToolSettings.CheckTriggersOptionListProvider.class)
    public String getIndexStat() {
        if (indexStat == null) {
            indexStat = indexStats[0];
        }
        return indexStat;
    }

    public void setIndexStat(String storageOption) {
        this.indexStat = storageOption;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public boolean isTableSampling() {
        return isTableSampling;
    }

    public void setTableSampling(boolean tableSampling) {
        isTableSampling = tableSampling;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 4, valueValidator = DB2RunstatsToolSettings.DB2StatisticPercentLimiter.class)
    public int getSamplePercent() {
        return samplePercent;
    }

    public void setSamplePercent(int samplePercent) {
        this.samplePercent = samplePercent;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        columnStat = JSONUtils.getString(config, "column_stat");
        indexStat = JSONUtils.getString(config, "index_stat");
        isTableSampling = JSONUtils.getBoolean(config, "is_table_sampling");
        samplePercent = JSONUtils.getInteger(config, "sample_percent");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("column_stat", columnStat);
        config.put("index_stat", indexStat);
        config.put("is_table_sampling", isTableSampling);
        config.put("sample_percent", samplePercent);
    }

    public static class CheckStorageOptionListProvider implements IPropertyValueListProvider<DB2RunstatsToolSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(DB2RunstatsToolSettings object) {
            return columnStats;
        }
    }

    public static class CheckTriggersOptionListProvider implements IPropertyValueListProvider<DB2RunstatsToolSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(DB2RunstatsToolSettings object) {
            return indexStats;
        }
    }

    private class DB2StatisticPercentLimiter implements IPropertyValueValidator<DBSObject, Integer> {

        @Override
        public boolean isValidValue(DBSObject object, Integer value) throws IllegalArgumentException {
            return 1 <= value && value <= 100;
        }
    }

}
