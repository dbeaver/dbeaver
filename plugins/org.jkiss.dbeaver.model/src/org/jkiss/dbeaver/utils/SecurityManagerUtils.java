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
package org.jkiss.dbeaver.utils;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.osgi.framework.AdminPermission;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;

public class SecurityManagerUtils {
    private static final List<Permission> DEFAULT_PERMISSIONS = List.of(new SocketPermission("*", "connect"),
        new NetPermission("*"),
        new ReflectPermission("*"),
        new AdminPermission(),
        new RuntimePermission("accessDeclaredMembers"),
        new PropertyPermission("*", "read"),
        new RuntimePermission("getClassLoader"),
        new RuntimePermission("createClassLoader"),
        new RuntimePermission("getenv.*")
    );

    public static List<Permission> getDefaultPermissions() {
        return new ArrayList<>(DEFAULT_PERMISSIONS);
    }

    public static <T> T executeWithAccessControlContext(AccessControlContext controlContext, Callable<T> callable) throws Throwable {
        try {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, controlContext);
        } catch (Throwable e) {
            Throwable throwable = e;
            if (throwable instanceof RuntimeException && throwable.getCause() != null) {
                throwable = throwable.getCause();
            }
            throw throwable;
        }
    }

    public static AccessControlContext controlContextOf(List<Permission> permissions) {

        Permissions noPermissions = new Permissions();
        for (Permission permission : permissions) {
            noPermissions.add(permission);
        }
        noPermissions.setReadOnly();

        return new AccessControlContext(
            new ProtectionDomain[]{new ProtectionDomain(null, noPermissions)}
        );
    }

    public static <T> T wrapDriverActions(DBPDataSourceContainer container, Callable<T> callable) throws Throwable {
        var driver = container.getDriver();
        if (System.getSecurityManager() != null
            && DBWorkbench.getPlatform().getApplication().isMultiuser()
            && container.isAccessCheckRequired()
        ) {
            //unsecured connection created by user
            var permissions = SecurityManagerUtils.getDefaultPermissions();
            permissions.addAll(getDriverFilesPermissions(driver));
            return SecurityManagerUtils.executeWithAccessControlContext(SecurityManagerUtils.controlContextOf(permissions), callable);
        } else {
            return callable.call();
        }
    }

    private static List<Permission> getDriverFilesPermissions(DBPDriver driver) {
        var driverFilesPermissions = new ArrayList<Permission>();
        var driverLibraries = driver.getDriverLibraries();
        for (DBPDriverLibrary driverLibrary : driverLibraries) {
            Path libraryFile = driverLibrary.getLocalFile();
            if (libraryFile == null) {
                continue;
            }
            //We need different permissions to work with a file by relative path and by absolute
            String relativeFilePath = libraryFile.toString();
            String absoluteFilePath = libraryFile.toAbsolutePath().toString();
            if (Files.isDirectory(libraryFile)) {
                driverFilesPermissions.add(new FilePermission(relativeFilePath, "read")); // access to directory
                driverFilesPermissions.add(new FilePermission(absoluteFilePath, "read"));
                absoluteFilePath += (File.separator + "*"); //access to all files in directory
                relativeFilePath += (File.separator + "*");
            }
            driverFilesPermissions.add(new FilePermission(relativeFilePath, "read"));
            driverFilesPermissions.add(new FilePermission(absoluteFilePath, "read"));
        }
        return driverFilesPermissions;
    }

}
