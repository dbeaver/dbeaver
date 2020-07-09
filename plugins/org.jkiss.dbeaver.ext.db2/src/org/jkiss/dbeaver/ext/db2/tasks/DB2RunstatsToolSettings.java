/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

import static org.jkiss.dbeaver.ext.db2.tasks.DB2RunstatsOptions.*;

public class DB2RunstatsToolSettings extends SQLToolExecuteSettings<DB2TableBase> {

    private final static String[] columnStats = new String[] {colsAll.getDesc(), colsAllAndDistribution.getDesc(), colsNo.getDesc()};
    private final static String[] indexStats = new String[] {indexesDetailed.getDesc(), indexesAll.getDesc(), indexesNo.getDesc()};
    private String columnStat;
    private String indexStat;
    private boolean isTableSampling;
    private int samplePercent;

    @Property(viewable = true, editable = true, updatable = true, order = 1, listProvider = DB2RunstatsToolSettings.CheckStorageOptionListProvider.class)
    public String getColumnStat() {
        if (columnStat == null) {
            columnStat = DB2RunstatsOptions.getOption(columnStats[0]).getDesc();
        }
        return columnStat;
    }

    public void setColumnStat(String columnStat) {
        this.columnStat = columnStat;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2, listProvider = DB2RunstatsToolSettings.CheckTriggersOptionListProvider.class)
    public String getIndexStat() {
        if (indexStat == null) {
            indexStat = DB2RunstatsOptions.getOption(indexStats[0]).getDesc();
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

    @Property(viewable = true, editable = true, updatable = true, order = 4) // , valueValidator = DB2StatisticPercentLimiter.class
    public int getSamplePercent() {
        return samplePercent;
    }

    public void setSamplePercent(int samplePercent) {
        this.samplePercent = samplePercent;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        columnStat = JSONUtils.getString(config, "column_stat"); //$NON-NLS-1$
        indexStat = JSONUtils.getString(config, "index_stat"); //$NON-NLS-1$
        isTableSampling = JSONUtils.getBoolean(config, "is_table_sampling"); //$NON-NLS-1$
        samplePercent = JSONUtils.getInteger(config, "sample_percent"); //$NON-NLS-1$
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("column_stat", columnStat); //$NON-NLS-1$
        config.put("index_stat", indexStat); //$NON-NLS-1$
        config.put("is_table_sampling", isTableSampling); //$NON-NLS-1$
        config.put("sample_percent", samplePercent); //$NON-NLS-1$
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

    public static class DB2StatisticPercentLimiter implements IPropertyValueValidator<DB2RunstatsToolSettings, Integer> {

        @Override
        public boolean isValidValue(DB2RunstatsToolSettings object, Integer value) throws IllegalArgumentException {
            return 0 <= value && value <= 100; //change to 1
        }
    }

}
