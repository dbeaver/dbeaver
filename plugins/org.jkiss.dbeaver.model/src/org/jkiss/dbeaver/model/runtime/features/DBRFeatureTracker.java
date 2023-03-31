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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DBeaver feature tracker
 */
class DBRFeatureTracker {

    private static final Log log = Log.getLog(DBRFeatureTracker.class);

    public static final String PREF_FEATURE_TRACKING_ENABLED = "feature.tracking.enabled";
    private static final long TRACK_PERIOD = 5000;
    public static final String ACTIVITY_LOGS_DIR = ".activity-logs";

    private final List<TrackingMessage> messages = new ArrayList<>();
    private final long applicationStartTime;
    private AbstractJob trackMonitor;

    private BufferedWriter trackStream;

    private class TrackingMessage {
        long timestamp;
        DBRFeature feature;
        Map<String, Object> parameters;

        public TrackingMessage(DBRFeature feature, Map<String, Object> parameters) {
            this.timestamp = System.currentTimeMillis();
            this.feature = feature;
            this.parameters = parameters;
        }

        String toPlainText() {
            StringBuilder text = new StringBuilder();
            text.append(timestamp - applicationStartTime).append(":").append(feature.getId());
            if (parameters != null) {
                text.append(":");
                boolean first = true;
                for (Map.Entry<?, ?> entry : parameters.entrySet()) {
                    if (first) {
                        text.append("&");
                    }
                    text.append(normalizeString(entry.getKey())).append("=").append(normalizeString(entry.getValue()));
                    first = false;
                }
            }
            return text.toString();
        }

        private String normalizeString(Object key) {
            if (key == null) {
                return "";
            }
            return key.toString()
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("&", "\\&")
                .replace("=", "\\=");
        }
    }

    DBRFeatureTracker() {
        if (isTrackingEnabled()) {
            startMonitor();
        }
        applicationStartTime = DBWorkbench.getPlatform().getApplication().getApplicationStartTime();
    }

    private void startMonitor() {
        if (trackMonitor != null) {
            trackMonitor.cancel();
        }
        trackMonitor = new AbstractJob("Features") {
            {
                setSystem(true);
                setUser(false);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                flushStatistics();
                if (!isCanceled()) {
                    schedule(TRACK_PERIOD);
                }
                return Status.OK_STATUS;
            }
        };
    }

    public void dispose() {
        stopMonitor();
        if (trackStream != null) {
            try {
                trackStream.close();
            } catch (IOException e) {
                log.debug(e);
            }
            trackStream = null;
        }
    }

    private BufferedWriter getTrackStream() throws IOException {
        if (trackStream == null) {
            Path logsDir = GeneralUtils.getMetadataFolder().resolve(ACTIVITY_LOGS_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }
            Path logFile = logsDir
                .resolve(DBWorkbench.getPlatform().getApplication().getApplicationRunId() + ".log");
            trackStream = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8);
        }
        return trackStream;
    }

    private void flushStatistics() {
        if (DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }
        TrackingMessage[] messagesCopy;
        synchronized (messages) {
            if (messages.isEmpty()) {
                return;
            }
            messagesCopy = messages.toArray(new TrackingMessage[0]);
            messages.clear();
        }

        if (!isTrackingEnabled()) {
            return;
        }

        try {
            BufferedWriter out = getTrackStream();

            for (TrackingMessage message : messagesCopy) {
                out.write(message.toPlainText());
                out.write("\n");
            }
            out.flush();
        } catch (IOException e) {
            log.debug("Statistics flush error", e);
        }
    }

    private void stopMonitor() {
        if (trackMonitor != null) {
            trackMonitor.cancel();
            trackMonitor = null;
        }
    }

    public boolean isTrackingEnabled() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_FEATURE_TRACKING_ENABLED);
    }

    public void setTrackingEnabled(boolean enabled) {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_FEATURE_TRACKING_ENABLED, enabled);
        if (enabled) {
            startMonitor();
        } else {
            stopMonitor();
        }
    }

    void trackFeature(DBRFeature feature, Map<String, Object> parameters) {
        synchronized (messages) {
            messages.add(new TrackingMessage(feature, parameters));
        }
    }

}
