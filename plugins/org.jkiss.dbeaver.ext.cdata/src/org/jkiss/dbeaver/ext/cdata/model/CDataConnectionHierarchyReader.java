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
package org.jkiss.dbeaver.ext.cdata.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class CDataConnectionHierarchyReader {
    private CDataConnectionHierarchyReader() {
    }

    /**
     * Uses a temporary class loader to read connection fields without initializing the licensed runtime driver.
     * CData registers a driver instance with {@link java.sql.DriverManager} during class initialization.
     * Closing the class loader alone leaves that registration holding the driver and its classes in memory.
     * Calling CData's static {@code deregister()} in {@code finally} releases the temporary registration on both
     * success and failure, preventing repeated page openings from retaining a new driver copy each time.
     */
    @NotNull
    public static CDataConnectionHierarchy read(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataDriverDescriptor descriptor
    ) throws DBException {
        var resolved = descriptor.resolveDriver(monitor);
        if (monitor.isCanceled()) {
            throw new DBException("CData hierarchy loading was canceled");
        }
        // config connections require neither source credentials nor driver license activation
        try (var loader = new URLClassLoader(
            new URL[] {resolved.jarPath().toUri().toURL()}, ClassLoader.getPlatformClassLoader()
        )) {
            Class<?> driverClass = Class.forName(resolved.driverClassName(), false, loader);
            var deregister = driverClass.getMethod("deregister");
            try {
                Driver driver = (Driver) driverClass.getConstructor().newInstance();
                return read(driver, descriptor.getDriverInfo().jdbcName());
            } finally {
                // the driver's static initializer registers a separate instance that keeps this loader alive
                deregister.invoke(null);
            }
        } catch (ReflectiveOperationException | IOException e) {
            throw new DBException("Unable to load CData connection hierarchy", e);
        }
    }

    @NotNull
    public static CDataConnectionHierarchy read(@NotNull Driver driver, @NotNull String source) throws DBException {
        try (Connection connection = driver.connect("jdbc:cdata:" + source + ":config:", new Properties())) {
            if (connection == null) {
                throw new DBException("The driver does not support CData configuration connections");
            }
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT Definition FROM sys_connection_hierarchy WHERE Context = '_jdbc'")) {
                if (!result.next() || result.getString(1) == null) {
                    throw new DBException("The driver did not return a CData connection hierarchy");
                }
                return CDataConnectionHierarchy.parse(result.getString(1));
            }
        } catch (SQLException e) {
            throw new DBException("Unable to read CData connection hierarchy", e);
        }
    }
}
