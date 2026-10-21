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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CDataHierarchyTestDriver implements Driver {
    private static final Driver REGISTERED_DRIVER = new CDataHierarchyTestDriver();

    static {
        AtomicInteger registrations = (AtomicInteger) System.getProperties().get("cdata.hierarchy.test.registrations");
        try {
            DriverManager.registerDriver(REGISTERED_DRIVER, registrations::decrementAndGet);
            registrations.incrementAndGet();
        } catch (SQLException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static void deregister() throws SQLException {
        DriverManager.deregisterDriver(REGISTERED_DRIVER);
    }

    @NotNull
    @Override
    public Connection connect(@NotNull String url, @NotNull Properties properties) throws SQLException {
        if (Boolean.getBoolean("cdata.hierarchy.test.fail")) {
            throw new SQLException("Configuration connection failed");
        }
        ClassLoader loader = getClass().getClassLoader();
        ResultSet result = (ResultSet) Proxy.newProxyInstance(loader, new Class<?>[] {ResultSet.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "next" -> true;
                case "getString" -> "{\"basic\":[],\"advanced\":[]}";
                default -> null;
            };
        });
        Statement statement = (Statement) Proxy.newProxyInstance(loader, new Class<?>[] {Statement.class}, (proxy, method, args) ->
            method.getName().equals("executeQuery") ? result : null);
        return (Connection) Proxy.newProxyInstance(loader, new Class<?>[] {Connection.class}, (proxy, method, args) ->
            method.getName().equals("createStatement") ? statement : null);
    }

    @Override
    public boolean acceptsURL(@Nullable String url) {
        return true;
    }

    @NotNull
    @Override
    public DriverPropertyInfo[] getPropertyInfo(@Nullable String url, @Nullable Properties properties) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @NotNull
    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }
}
