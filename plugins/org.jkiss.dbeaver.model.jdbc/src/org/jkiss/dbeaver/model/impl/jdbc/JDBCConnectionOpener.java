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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.access.DBAAuthSubjectCredentials;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;

import javax.security.auth.Subject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

class JDBCConnectionOpener implements DBRRunnableWithProgress, PrivilegedExceptionAction<Connection> {
    //private static final Log log = Log.getLog(JDBCConnectionOpener.class);

    private final DBPDriver driver;
    private final Driver driverInstance;
    private final String url;
    private final Properties connectProps;
    private final Object authResult;
    private Connection connection;
    private Throwable error;

    public JDBCConnectionOpener(
        @NotNull DBPDriver driver,
        @Nullable Driver driverInstance,
        @NotNull String url,
        @NotNull Properties connectProps,
        @Nullable Object authResult
    ) {
        this.driver = driver;
        this.driverInstance = driverInstance;
        this.url = url;
        this.connectProps = connectProps;
        this.authResult = authResult;
    }

    public Connection getConnection() {
        return connection;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public void run(DBRProgressMonitor monitor1) throws InvocationTargetException, InterruptedException {
        try {
            // Use PrivilegedAction in case we have explicit subject
            // Otherwise just open connection directly
            Connection jdbcConnection = null;
            boolean connected = false;
            if (authResult instanceof DBAAuthSubjectCredentials sc) {
                Subject authSubject = sc.getAuthSubject();
                if (authSubject != null) {
                    jdbcConnection = Subject.doAs(authSubject, this);
                    connected = true;
                }
            }
            if (!connected) {
                jdbcConnection = this.run();
            }
            connection = jdbcConnection;
        } catch (Throwable e) {
            error = e;
        }
    }

    @Override
    public Connection run() throws Exception {
        // Set context class loaded to driver class loader
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(driver.getClassLoader());
        try {
            // Reset DriverManager cache
            try {
                Field driversInitializedField = DriverManager.class.getDeclaredField("driversInitialized");
                driversInitializedField.setAccessible(true);
                driversInitializedField.set(null, false);
            } catch (Throwable e) {
                // Just ignore it
            }
            // Open connection
            if (driverInstance == null) {
                return DriverManager.getConnection(url, connectProps);
            } else {
                return driverInstance.connect(url, connectProps);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
