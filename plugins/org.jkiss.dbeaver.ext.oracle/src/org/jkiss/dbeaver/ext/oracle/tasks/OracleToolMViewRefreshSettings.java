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
package org.jkiss.dbeaver.ext.oracle.tasks;

import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

public class OracleToolMViewRefreshSettings extends SQLToolExecuteSettings<OracleMaterializedView> {
    private boolean isFast;
    private boolean isForce;
    private boolean isComplete;
    private boolean isAlways;
    private boolean isRecomputed;

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isFast() {
        return isFast;
    }

    public void setFast(boolean fast) {
        isFast = fast;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isForce() {
        return isForce;
    }

    public void setForce(boolean force) {
        isForce = force;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isAlways() {
        return isAlways;
    }

    public void setAlways(boolean always) {
        isAlways = always;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isRecomputed() {
        return isRecomputed;
    }

    public void setRecomputed(boolean recomputed) {
        isRecomputed = recomputed;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        isFast = JSONUtils.getBoolean(config, "fast");
        isForce = JSONUtils.getBoolean(config, "force");
        isComplete = JSONUtils.getBoolean(config, "complete");
        isAlways = JSONUtils.getBoolean(config, "always");
        isRecomputed = JSONUtils.getBoolean(config, "recompute_partitions");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("fast", isFast);
        config.put("force", isForce);
        config.put("complete", isComplete);
        config.put("always", isAlways);
        config.put("recompute_partitions", isRecomputed);
    }
}
