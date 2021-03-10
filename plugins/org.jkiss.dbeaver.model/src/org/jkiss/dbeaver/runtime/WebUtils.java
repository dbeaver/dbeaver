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
package org.jkiss.dbeaver.runtime;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.net.*;
import java.util.Base64;

/**
 * WebUtils
 */
public class WebUtils {
    private static final Log log = Log.getLog(WebUtils.class);
    private static final int MAX_RETRY_COUNT = 10;

    @NotNull
    public static URLConnection openConnection(String urlString, String referrer) throws IOException {
        return openConnection(urlString, null, referrer);
    }

    @NotNull
    public static URLConnection openConnection(String urlString, DBPAuthInfo authInfo, String referrer) throws IOException {
        return openURLConnection(urlString, authInfo, referrer, 1);
    }

    /**
     * Opens URL connection
     * @param urlString   URL
     * @param authInfo    authenticate info.
     * @param referrer    Referrer (who opens the URL?)
     * @param retryNumber retry number
     * @return  URL connection
     */
    @NotNull
    private static URLConnection openURLConnection(String urlString, DBPAuthInfo authInfo, String referrer, int retryNumber) throws IOException {
        if (retryNumber > MAX_RETRY_COUNT) {
            throw new IOException("Too many redirects (" + retryNumber + ")");
        } else if (retryNumber > 1) {
            log.debug("Retry number " + retryNumber);
        }
        log.debug("Open [" + urlString + "]");

        DBPPreferenceStore prefs = DBWorkbench.getPlatform().getPreferenceStore();
        String proxyHost = prefs.getString(ModelPreferences.UI_PROXY_HOST);
        Proxy proxy = null;
        if (!CommonUtils.isEmpty(proxyHost)) {
            int proxyPort = prefs.getInt(ModelPreferences.UI_PROXY_PORT);
            if (proxyPort <= 0) {
                log.warn("Invalid proxy port: " + proxyPort);
            }
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        URL url = new URL(urlString);
        final URLConnection connection = (proxy == null ? url.openConnection() : url.openConnection(proxy));
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(10000);
        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setRequestMethod("GET"); //$NON-NLS-1$
            httpConnection.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            connection.setRequestProperty(
                "User-Agent",  //$NON-NLS-1$
                GeneralUtils.getProductTitle());
            if (referrer != null) {
                connection.setRequestProperty(
                        "X-Referrer",  //$NON-NLS-1$
                        referrer);
            }
            if (authInfo != null && !CommonUtils.isEmpty(authInfo.getUserName())) {
                // Set auth info
                String encoded = Base64.getEncoder().encodeToString(
                    (authInfo.getUserName() + ":" + CommonUtils.notEmpty(authInfo.getUserPassword())).getBytes(GeneralUtils.UTF8_CHARSET));
                connection.setRequestProperty("Authorization", "Basic " + encoded);
            }
        }
        connection.connect();
        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection httpConnection = (HttpURLConnection) connection;
            final int responseCode = httpConnection.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = connection.getHeaderField("Location");
                    return openURLConnection(newUrl, authInfo, referrer, retryNumber + 1);
                }
                throw new IOException("Can't open '" + urlString + "': " + httpConnection.getResponseMessage());
            }
        }

        return connection;
    }

    public static long downloadRemoteFile(@NotNull DBRProgressMonitor monitor, String taskName, String externalURL, File localFile, DBPAuthInfo authInfo) throws IOException, InterruptedException {
        try (final OutputStream outputStream = new FileOutputStream(localFile)) {
            return downloadRemoteFile(monitor, taskName, externalURL, outputStream, authInfo);
        }
    }

    public static long downloadRemoteFile(@NotNull DBRProgressMonitor monitor, String taskName, String externalURL, OutputStream outputStream, DBPAuthInfo authInfo) throws IOException, InterruptedException {
        final URLConnection connection = openConnection(externalURL, authInfo, null);
        final int contentLength = connection.getContentLength();
        final byte[] buffer = new byte[8192];
        final ByteNumberFormat numberFormat = new ByteNumberFormat();
        numberFormat.setUseLongUnitNames(true);

        // The value of getContentLength() may be -1 and this should not be handled, see IProgressMonitor#UNKNOWN
        monitor.beginTask(taskName + " - " + externalURL, contentLength);
        try (final InputStream inputStream = connection.getInputStream()) {
            final long startTime = System.currentTimeMillis();
            long updateTime = 0;
            long totalRead = 0;

            while (true) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
                long currentTime = System.currentTimeMillis();
                if (currentTime - updateTime > 1000) {
                    if (contentLength >= 0) {
                        long elapsedTime = currentTime - startTime;
                        long totalDownloadTime = (long) (elapsedTime * contentLength / (double) totalRead);
                        long remainingDownloadTime = totalDownloadTime - elapsedTime;
                        updateTime = currentTime;
                        monitor.subTask(NLS.bind(ModelMessages.dialog_web_download_text_known, new Object[]{
                            numberFormat.format(totalRead),
                            numberFormat.format(contentLength),
                            String.format("%.2f%%", totalRead / (double) contentLength * 100),
                            remainingDownloadTime > 0 ? RuntimeUtils.formatExecutionTime(remainingDownloadTime) : "-"
                        }));
                    } else {
                        monitor.subTask(NLS.bind(ModelMessages.dialog_web_download_text_unknown, numberFormat.format(totalRead)));
                    }
                }
                final int count = inputStream.read(buffer);
                if (count <= 0) {
                    return totalRead;
                }
                outputStream.write(buffer, 0, count);
                monitor.worked(count);
                totalRead += count;
            }
        } finally {
            monitor.done();
        }
    }

}
