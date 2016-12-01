/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.runtime;

import org.eclipse.swt.program.Program;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * WebUtils
 */
public class WebUtils {
    private static final Log log = Log.getLog(WebUtils.class);


    public static InputStream openConnectionStream(String urlString) throws IOException {
        URLConnection connection = openConnection(urlString);
        return connection.getInputStream();
    }

    public static URLConnection openConnection(String urlString) throws IOException {

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
                DBeaverCore.getProductTitle());
        }
        connection.connect();
        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection httpConnection = (HttpURLConnection) connection;
            if (httpConnection.getResponseCode() != 200) {
                throw new IOException("File not found '" + urlString + "': " + httpConnection.getResponseMessage());
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
