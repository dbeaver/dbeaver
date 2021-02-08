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

package org.jkiss.dbeaver.model.runtime.features;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
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

    private static final Log log = Log.getLog(DBRFeatureRegistry.class);
    private static final String NOTIFICATIONS_CONFIG_FILE = "notifications-config.xml";

    private final Map<String, DBRFeature> allFeatures = new HashMap<>();
    private final Map<String, DBRFeature> commandFeatures = new HashMap<>();
    private final Map<String, DBRNotificationDescriptor> notificationSettings = new HashMap<>();

    private static DBRFeatureRegistry instance = null;

    public synchronized static DBRFeatureRegistry getInstance() {
        if (instance == null) {
            instance = new DBRFeatureRegistry();
        }
        return instance;
    }

    private DBRFeatureRegistry() {
        // Load notifications settings
        File ncFile = getNotificationsConfigFile();
        if (ncFile.exists()) {

        }
    }

    @NotNull
    private static File getNotificationsConfigFile() {
        return new File(ModelActivator.getInstance().getStateLocation().toFile(), NOTIFICATIONS_CONFIG_FILE);
    }

    public synchronized void registerFeatures(Class<?> theClass) {
        for (Field field : theClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 || field.getType() != DBRFeature.class) {
                continue;
            }
            try {
                DBRFeature feature = (DBRFeature) field.get(null);
                if (feature != null) {
                    String id = theClass.getSimpleName() + "." + field.getName();
                    feature.setId(id);
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

    public void setNotificationSettings(DBRFeature feature, DBRNotificationDescriptor notificationDescriptor) {
        notificationSettings.put(feature.getId(), notificationDescriptor);
    }

    public static void useFeature(DBRFeature feature, Map<String, Object> parameters) {
        QMUtils.getDefaultHandler().handleFeatureUsage(feature, parameters);
    }

}