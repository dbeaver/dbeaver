/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.program.Program;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.net.*;
import java.text.NumberFormat;
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
    public static URLConnection openConnection(String urlString, DBAAuthInfo authInfo, String referrer) throws IOException {
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
    private static URLConnection openURLConnection(String urlString, DBAAuthInfo authInfo, String referrer, int retryNumber) throws IOException {
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

    public static void openWebBrowser(String url)
    {
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://")) {
            url = "http://" + url;
        }
        Program.launch(url);
    }

    public static void downloadRemoteFile(@NotNull DBRProgressMonitor monitor, String taskName, String externalURL, File localFile, DBAAuthInfo authInfo) throws IOException, InterruptedException {
        final URLConnection connection = openConnection(externalURL, authInfo, null);

        int contentLength = connection.getContentLength();
        if (contentLength < 0) {
            contentLength = 0;
        }
        int bufferLength = contentLength / 10;
        if (bufferLength > 1000000) {
            bufferLength = 1000000;
        }
        if (bufferLength < 50000) {
            bufferLength = 50000;
        }
        monitor.beginTask(taskName + " - " + externalURL, contentLength);
        boolean success = false;
        try (final OutputStream outputStream = new FileOutputStream(localFile)) {
            try (final InputStream inputStream = connection.getInputStream()) {
                final NumberFormat numberFormat = NumberFormat.getNumberInstance();
                byte[] buffer = new byte[bufferLength];
                int totalRead = 0;
                for (;;) {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    //monitor.subTask(numberFormat.format(totalRead) + "/" + numberFormat.format(contentLength));
                    final int count = inputStream.read(buffer);
                    if (count <= 0) {
                        success = true;
                        break;
                    }
                    outputStream.write(buffer, 0, count);
                    monitor.worked(count);
                    totalRead += count;
                }
            }
        } finally {
            if (!success) {
                if (!localFile.delete()) {
                    log.warn("Can't delete local file '" + localFile.getAbsolutePath() + "'");
                }
            }
            monitor.done();
        }
    }

}
