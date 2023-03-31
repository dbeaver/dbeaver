/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.runtime.features;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBeaver feature registry
 */
public class DBRFeatureRegistry {

    public static final String PREF_FEATURE_TRACKING_ENABLED = "feature.tracking.enabled";
    private static final Log log = Log.getLog(DBRFeatureRegistry.class);

    private static DBRFeatureTracker tracker;

    private final Map<String, DBRFeature> allFeatures = new HashMap<>();
    private final Map<String, DBRFeature> commandFeatures = new HashMap<>();

    private static DBRFeatureRegistry instance = null;

    public synchronized static DBRFeatureRegistry getInstance() {
        if (instance == null) {
            instance = new DBRFeatureRegistry();
        }
        return instance;
    }

    private DBRFeatureRegistry() {
        if (!DBWorkbench.getPlatform().getApplication().isHeadlessMode() &&
            !DBWorkbench.getPlatform().getApplication().isMultiuser())
        {
            tracker = new DBRFeatureTracker();
        }
    }

    public static boolean isTrackingEnabled() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_FEATURE_TRACKING_ENABLED);
    }

    public static void setTrackingEnabled(boolean enabled) {
        if (enabled == isTrackingEnabled()) {
            return;
        }
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_FEATURE_TRACKING_ENABLED, enabled);
        if (enabled) {
            tracker.startMonitor();
        } else {
            tracker.stopMonitor();
        }
    }

    public void endTracking() {
        if (tracker != null) {
            tracker.dispose();
        }
    }

    public synchronized void registerFeatures(Class<?> theClass) {
        for (Field field : theClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 || field.getType() != DBRFeature.class) {
                continue;
            }
            try {
                DBRFeature feature = (DBRFeature) field.get(null);
                if (feature != null) {
                    String id = field.getName();
                    feature.setId(id);
                    if (allFeatures.containsKey(id)) {
                        log.warn("Duplicate feature definition: " + id);
                    }
                    allFeatures.put(id, feature);
                    if (!CommonUtils.isEmpty(feature.getCommandId())) {
                        commandFeatures.put(feature.getCommandId(), feature);
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public List<DBRFeature> getAllFeatures() {
        return new ArrayList<>(allFeatures.values());
    }

    public DBRFeature findCommandFeature(String commandId) {
        return commandFeatures.get(commandId);
    }

    public static void useFeature(DBRFeature feature, Map<String, Object> parameters) {
        if (tracker != null) {
            tracker.trackFeature(feature, parameters);
        }
        QMUtils.getDefaultHandler().handleFeatureUsage(feature, parameters);
    }

}
