/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPLogLocations;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.IOUtils.FileNameParts;

class DesktopLogLocations implements DBPLogLocations {
    @Nullable
    private final File debugLog;
    private final String logFileName;
    private final String logFileNameExtension;

    @Nullable
    private Predicate<String> logFileNamePattern;

    DesktopLogLocations() {
        DBPPreferenceStore preferenceStore = DBeaverActivator.getInstance().getPreferences();
        if (!preferenceStore.getBoolean(DBeaverPreferences.LOGS_DEBUG_ENABLED)) {
            debugLog = null;
            logFileName = null;
            logFileNameExtension = null;
            return;
        }

        String debugLogPath = preferenceStore.getString(DBeaverPreferences.LOGS_DEBUG_LOCATION);
        if (CommonUtils.isEmpty(debugLogPath)) {
            debugLogPath = GeneralUtils.getMetadataFolder().resolve(DBConstants.DEBUG_LOG_FILE_NAME).toAbsolutePath().toString();
        }
        debugLogPath = GeneralUtils.replaceVariables(debugLogPath, new SystemVariablesResolver());
        debugLog = new File(debugLogPath);

        FileNameParts fileNameParts = IOUtils.getFileNameParts(debugLog);
        logFileName = fileNameParts.nameWithoutExtension();
        logFileNameExtension = fileNameParts.extension();
    }

    @Nullable
    private Predicate<String> getLogFileNamePattern() {
        if (logFileName == null || logFileNameExtension == null) {
            return null;
        }
        if (logFileNamePattern == null) {
            String logFileNameRegexStr = "^" + Pattern.quote(logFileName) + "\\-[0-9]+" + Pattern.quote(logFileNameExtension) + "$";
            logFileNamePattern = Pattern.compile(logFileNameRegexStr).asMatchPredicate();
        }
        return logFileNamePattern;
    }

    @Nullable
    @Override
    public File getDebugLog() {
        return debugLog;
    }

    @NotNull
    @Override
    public List<File> getDebugLogFiles() {
        Predicate<String> logFileNamePredicate = getLogFileNamePattern();
        if (logFileNamePredicate == null) {
            // debug logs are disabled
            return List.of();
        }
        List<File> result = new ArrayList<>();
        if (debugLog != null) {
            if (debugLog.exists()) {
                result.add(debugLog);
            }
            Collections.addAll(result, debugLog.getParentFile().listFiles((File dir, String name) -> logFileNamePredicate.test(name)));
        }
        return result;
    }

    @Nullable
    @Override
    public File proposeDebugLogRotation() {
        File debugLogFolder = getDebugLogFolder();
        if (debugLogFolder == null) {
            return null;
        }
        return new File(debugLogFolder, logFileName + "-" + System.currentTimeMillis() + logFileNameExtension);
    }
}
