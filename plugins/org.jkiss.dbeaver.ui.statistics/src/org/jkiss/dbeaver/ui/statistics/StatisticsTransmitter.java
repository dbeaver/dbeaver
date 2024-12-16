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
package org.jkiss.dbeaver.ui.statistics;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

public class StatisticsTransmitter {

    private static final Log log = Log.getLog(StatisticsTransmitter.class);

    private static final String ENDPOINT = "https://stats.dbeaver.com/send-statistics";

    private final String workspaceId;

    public StatisticsTransmitter(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void send(boolean detached) {
        if (detached) {
            new AbstractJob("Usage statistics transmitter") {
                {
                    setSystem(true);
                }
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    sendStatistics(monitor, false);
                    return Status.OK_STATUS;
                }
            }.schedule(3000);
        } else {
            sendStatistics(new LoggingProgressMonitor(log), true);
        }
    }

    private void sendStatistics(DBRProgressMonitor monitor, boolean sendActiveSession) {
        try {
            String appSessionId = DBWorkbench.getPlatform().getApplication().getApplicationRunId();
            Path activityLogsFolder = FeatureStatisticsCollector.getActivityLogsFolder();
            try (Stream<Path> list = Files.list(activityLogsFolder)) {
                List<Path> logFiles = list
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .toList();
                for (Path logFile : logFiles) {
                    String fileName = logFile.getFileName().toString();
                    fileName = fileName.substring(0, fileName.length() - 4);
                    String[] parts = fileName.split("_");
                    if (parts.length != 2) {
                        continue;
                    }
                    String timestamp = parts[0];
                    String sessionId = parts[1];
                    if (sendActiveSession) {
                        if (sessionId.equals(appSessionId)) {
                            sendLogFile(logFile, timestamp, sessionId);
                            break;
                        }
                    } else {
                        if (sessionId.equals(appSessionId)) {
                            // This is active session
                            continue;
                        }
                        sendLogFile(logFile, timestamp, sessionId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error sending statistics", e);
        }
    }

    private void sendLogFile(Path logFile, String timestamp, String sessionId) {
        //log.debug("Sending statistics file '" + logFile.toAbsolutePath() + "'");
        try {
            URLConnection urlConnection = WebUtils.openURLConnection(
                ENDPOINT + "?session=" + sessionId + "&time=" + timestamp,
                null,
                workspaceId,
                "POST",
                0,
                5000,
                Map.of(
                    "Content-Type", "text/plain",
                    "Locale", Locale.getDefault().toString(),
                    "Country", Locale.getDefault().getISO3Country(),
                    "Timezone", TimeZone.getDefault().getID(),
                    "Application-Name", GeneralUtils.getProductName(),
                    "Application-Version", GeneralUtils.getProductVersion().toString(),
                    "OS", CommonUtils.notEmpty(System.getProperty(StandardConstants.ENV_OS_NAME))));

            ((HttpURLConnection)urlConnection).setFixedLengthStreamingMode(Files.size(logFile));

            try (OutputStream outputStream = urlConnection.getOutputStream()) {
                Files.copy(logFile, outputStream);
            }
            try (InputStream inputStream = urlConnection.getInputStream()) {
                if (inputStream != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    IOUtils.copyStream(inputStream, buffer);
                    log.debug("Statistics sent (" + buffer.toString(StandardCharsets.UTF_8) + ")");
                }
            } catch (IOException e) {
                log.debug("Error reading statistics server response");
            }
            ((HttpURLConnection) urlConnection).disconnect();

            Files.delete(logFile);
        } catch (Exception e) {
            log.debug("Error sending statistics file '" + logFile.toAbsolutePath() + "'.", e);

            try {
                Files.delete(logFile);
            } catch (IOException ex) {
                log.debug("Error deleting file with usage statistics '" + logFile.toAbsolutePath() + "'.", e);
            }
        }
    }

}
