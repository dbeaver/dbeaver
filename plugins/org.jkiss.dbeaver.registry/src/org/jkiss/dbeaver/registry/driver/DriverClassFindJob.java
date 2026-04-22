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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DriverClassFindJob implements DBRRunnableWithProgress {

    private static final Log log = Log.getLog(DriverClassFindJob.class);

    public static final String OBJECT_CLASS_NAME = "java/lang/Object";
    private final List<String> driverClassNames = new ArrayList<>();

    private final DriverDescriptor driver;
    private final String interfaceName;
    private final boolean isInterface;

    public DriverClassFindJob(@NotNull DBPDriver driver, @NotNull String interfaceName, boolean isInterface) {
        this.driver = (DriverDescriptor) driver;
        this.interfaceName = interfaceName.replace(".", "/");
        this.isInterface = isInterface;
    }

    @NotNull
    public List<String> getDriverClassNames() {
        return driverClassNames;
    }

    @Override
    public void run(@NotNull DBRProgressMonitor monitor) {
        findDriverClasses(monitor);
    }

    private void findDriverClasses(@NotNull DBRProgressMonitor monitor) {
        java.util.List<Path> libFiles = driver.getDefaultDriverLoader().getAllLibraryFiles(monitor);
        java.util.List<URL> libURLs = new ArrayList<>();
        for (Path libFile : libFiles) {
            if (libFile != null && Files.exists(libFile) && !Files.isDirectory(libFile)) {
                try {
                    libURLs.add(libFile.toUri().toURL());
                } catch (MalformedURLException e) {
                    log.debug(e);
                }
            }
        }
        ClassLoader findCL = new URLClassLoader(libURLs.toArray(new URL[0]));

        for (Path libFile : libFiles) {
            if (monitor.isCanceled()) {
                break;
            }
            if (!Files.isDirectory(libFile)) {
                findDriverClasses(monitor, findCL, libFile);
            }
        }
    }

    private void findDriverClasses(@NotNull DBRProgressMonitor monitor, @NotNull ClassLoader findCL, @NotNull Path libFile) {
        String jarName = libFile.getFileName().toString();
        if (!jarName.endsWith(DBPDriverLibrary.FILE_EXT_JAR) && !jarName.endsWith(DBPDriverLibrary.FILE_EXT_ZIP)) {
            // Dummy file type validation
            return;
        }
        try (JarFile currentFile = new JarFile(libFile.toFile(), false)) {
            monitor.beginTask(jarName, currentFile.size());

            for (Enumeration<?> e = currentFile.entries(); e.hasMoreElements(); ) {
                {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    JarEntry current = (JarEntry) e.nextElement();
                    String fileName = current.getName();
                    if (fileName.endsWith(DBPDriverLibrary.FILE_EXT_CLASS) && !fileName.contains("$")) {
                        String className = fileName
                            .replaceAll("/", ".")
                            .replace(DBPDriverLibrary.FILE_EXT_CLASS, "");
                        monitor.subTask(className);
                        try {
                            if (implementsInterface(currentFile, current, 0)) {
                                driverClassNames.add(className);
                            }
                        } catch (Throwable e1) {
                            // do nothing
                        }
                        monitor.worked(1);
                    }
                }
            }
            monitor.done();
        } catch (IOException e) {
            log.debug(e);
        }
    }

    private boolean implementsInterface(@NotNull JarFile currentFile, @NotNull JarEntry current, int depth) throws IOException {
        try (InputStream classStream = currentFile.getInputStream(current)) {
            ClassReader cr = new ClassReader(classStream);
            int access = cr.getAccess();
            if (depth == 0 && ((access & Opcodes.ACC_PUBLIC) == 0 || (access & Opcodes.ACC_ABSTRACT) != 0)) {
                return false;
            }
            final String superName = cr.getSuperName();
            if (isInterface) {
                String[] interfaces = cr.getInterfaces();
                if (ArrayUtils.contains(interfaces, interfaceName)) {
                    return true;
                } else if (!CommonUtils.isEmpty(superName) && !superName.equals(OBJECT_CLASS_NAME)) {
                    // Check recursively
                    JarEntry jarEntry = currentFile.getJarEntry(superName + DBPDriverLibrary.FILE_EXT_CLASS);
                    if (jarEntry != null) {
                        return implementsInterface(currentFile, jarEntry, depth + 1);
                    }
                }
                for (String intName : interfaces) {
                    JarEntry jarEntry = currentFile.getJarEntry(intName + DBPDriverLibrary.FILE_EXT_CLASS);
                    if (jarEntry != null) {
                        if (implementsInterface(currentFile, jarEntry, depth + 1)) {
                            return true;
                        }
                    }
                }
            } else if (superName != null) {
                // Superclass
                if (interfaceName.equals(superName)) {
                    return true;
                }
                JarEntry jarEntry = currentFile.getJarEntry(superName + DBPDriverLibrary.FILE_EXT_CLASS);
                if (jarEntry != null) {
                    if (implementsInterface(currentFile, jarEntry, depth + 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
