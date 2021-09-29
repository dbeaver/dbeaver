/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

public class PostgreToolBaseVacuumSettings extends SQLToolExecuteSettings<DBSObject> {
    private boolean isFull;
    private boolean isFreeze;
    private boolean isAnalyzed;
    private boolean isDisableSkipping;
    private boolean isSkipLocked;
    private boolean isIndexCleaning;
    private boolean isTruncated;

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isFreeze() {
        return isFreeze;
    }

    public void setFreeze(boolean freeze) {
        isFreeze = freeze;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isAnalyzed() {
        return isAnalyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        isAnalyzed = analyzed;
    }

    @Property(viewable = true, editable = true, updatable = true, visibleIf = PostgreVersionValidator9and6.class)
    public boolean isDisableSkipping() {
        return isDisableSkipping;
    }

    public void setDisableSkipping(boolean disableSkipping) {
        isDisableSkipping = disableSkipping;
    }

    @Property(viewable = true, editable = true, updatable = true, visibleIf = PostgreVersionValidator12.class)
    public boolean isSkipLocked() {
        return isSkipLocked;
    }

    public void setSkipLocked(boolean skipLocked) {
        isSkipLocked = skipLocked;
    }

    @Property(viewable = true, editable = true, updatable = true, visibleIf = PostgreVersionValidator12.class)
    public boolean isIndexCleaning() {
        return isIndexCleaning;
    }

    public void setIndexCleaning(boolean indexCleaning) {
        isIndexCleaning = indexCleaning;
    }

    @Property(viewable = true, editable = true, updatable = true, visibleIf = PostgreVersionValidator12.class)
    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean truncated) {
        isTruncated = truncated;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        isFull = JSONUtils.getBoolean(config, "full");
        isFreeze = JSONUtils.getBoolean(config, "freeze");
        isAnalyzed = JSONUtils.getBoolean(config, "analyze");
        isDisableSkipping = JSONUtils.getBoolean(config, "disable_page_skipping");
        isSkipLocked = JSONUtils.getBoolean(config, "skip_locked");
        isIndexCleaning = JSONUtils.getBoolean(config, "index_cleanup");
        isTruncated = JSONUtils.getBoolean(config, "truncate");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("full", isFull);
        config.put("freeze", isFreeze);
        config.put("analyze", isAnalyzed);
        config.put("disable_page_skipping", isDisableSkipping);
        config.put("skip_locked", isSkipLocked);
        config.put("index_cleanup", isIndexCleaning);
        config.put("truncate", isTruncated);
    }
}
