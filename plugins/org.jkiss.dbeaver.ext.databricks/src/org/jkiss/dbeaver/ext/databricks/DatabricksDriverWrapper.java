/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.databricks;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Delegating wrapper for the Databricks JDBC driver.
 * <p>
 * The native (OSS) driver merges URL parameters and client properties into a single map
 * and fails with a "duplicate properties" error if the same parameter is specified in both
 * the JDBC URL and connection properties (e.g. HttpPath or PWD provided in the URL
 * and in the authentication form). Duplicated parameters are removed from the URL
 * before opening a connection, values from connection properties (authentication form,
 * driver properties) take precedence.
 */
public class DatabricksDriverWrapper implements Driver {

    private static final Log log = Log.getLog(DatabricksDriverWrapper.class);

    private final Driver delegate;

    public DatabricksDriverWrapper(@NotNull Driver delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return delegate.connect(removeDuplicatedUrlParameters(url, info), info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return delegate.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return delegate.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Nullable
    public static String removeDuplicatedUrlParameters(@Nullable String url, @Nullable Properties info) {
        if (url == null || info == null || info.isEmpty()) {
            return url;
        }
        int paramsStart = url.indexOf(';');
        if (paramsStart < 0) {
            return url;
        }
        Set<String> propertyNames = new HashSet<>();
        for (Object key : info.keySet()) {
            propertyNames.add(key.toString().toLowerCase(Locale.ENGLISH));
        }
        StringBuilder result = new StringBuilder(url.substring(0, paramsStart));
        List<String> removed = new ArrayList<>();
        for (String part : url.substring(paramsStart + 1).split(";")) {
            if (part.isEmpty()) {
                continue;
            }
            int div = part.indexOf('=');
            String name = div < 0 ? part : part.substring(0, div);
            if (propertyNames.contains(name.toLowerCase(Locale.ENGLISH))) {
                removed.add(name);
            } else {
                result.append(';').append(part);
            }
        }
        if (removed.isEmpty()) {
            return url;
        }
        log.debug("Skip JDBC URL parameters overridden by connection properties: " + removed);
        return result.toString();
    }
}
