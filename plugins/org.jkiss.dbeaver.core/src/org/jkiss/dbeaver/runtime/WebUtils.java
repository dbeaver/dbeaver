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
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.*;
import java.util.Base64;

/**
 * WebUtils
 */
public class WebUtils {
    private static final Log log = Log.getLog(WebUtils.class);

    @NotNull
    public static URLConnection openConnection(String urlString) throws IOException {
        return openConnection(urlString, null);
    }

    @NotNull
    public static URLConnection openConnection(String urlString, DBAAuthInfo authInfo) throws IOException {

        log.debug("Open [" + urlString + "]");

        DBPPreferenceStore prefs = DBeaverCore.getGlobalPreferenceStore();
        String proxyHost = prefs.getString(DBeaverPreferences.UI_PROXY_HOST);
        Proxy proxy = null;
        if (!CommonUtils.isEmpty(proxyHost)) {
            int proxyPort = prefs.getInt(DBeaverPreferences.UI_PROXY_PORT);
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
            connection.setRequestProperty(
                "User-Agent",  //$NON-NLS-1$
                GeneralUtils.getProductTitle());
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
}
