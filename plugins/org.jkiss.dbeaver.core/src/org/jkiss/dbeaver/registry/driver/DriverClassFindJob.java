package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DriverClassFindJob implements IRunnableWithProgress {

    private static final Log log = Log.getLog(DriverClassFindJob.class);

    public static final String OBJECT_CLASS_NAME = "java/lang/Object";
    public static final String CLASS_FILE_EXT = ".class";
    private List<String> driverClassNames = new ArrayList<>();

    private final DriverDescriptor driver;
    private final String interfaceName;
    private final boolean isInterface;

    public DriverClassFindJob(DBPDriver driver, String interfaceName, boolean isInterface) {
        this.driver = (DriverDescriptor) driver;
        this.interfaceName = interfaceName;
        this.isInterface = isInterface;
    }

    public List<String> getDriverClassNames() {
        return driverClassNames;
    }

    @Override
    public void run(IProgressMonitor monitor) {
        findDriverClasses(monitor);
    }

    private void findDriverClasses(IProgressMonitor monitor) {
        java.util.List<File> libFiles = new ArrayList<>();
        java.util.List<URL> libURLs = new ArrayList<>();
        for (DBPDriverLibrary lib : driver.getDriverLibraries()) {
            File libFile = lib.getLocalFile();
            if (libFile != null && libFile.exists() && !libFile.isDirectory() && lib.getType() == DBPDriverLibrary.FileType.jar) {
                libFiles.add(libFile);
                try {
                    libURLs.add(libFile.toURI().toURL());
                } catch (MalformedURLException e) {
                    log.debug(e);
                }
            } else {
                final Collection<DriverDescriptor.DriverFileInfo> files = driver.getLibraryFiles(lib);
                if (files != null) {
                    for (DriverDescriptor.DriverFileInfo file : files) {
                        if (file.getFile() != null && file.getFile().exists()) {
                            libFiles.add(file.getFile());
                        }
                    }
                }
            }
        }
        ClassLoader findCL = new URLClassLoader(libURLs.toArray(new URL[libURLs.size()]));

        for (File libFile : libFiles) {
            if (monitor.isCanceled()) {
                break;
            }
            findDriverClasses(monitor, findCL, libFile);
        }
    }

    private void findDriverClasses(IProgressMonitor monitor, ClassLoader findCL, File libFile) {
        try {
            JarFile currentFile = new JarFile(libFile, false);
            monitor.beginTask(libFile.getName(), currentFile.size());

            for (Enumeration<?> e = currentFile.entries(); e.hasMoreElements(); ) {
                {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    JarEntry current = (JarEntry) e.nextElement();
                    String fileName = current.getName();
                    if (fileName.endsWith(CLASS_FILE_EXT) && !fileName.contains("$")) { //$NON-NLS-1$ //$NON-NLS-2$
                        String className = fileName.replaceAll("/", ".").replace(CLASS_FILE_EXT, ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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

    private boolean implementsInterface(JarFile currentFile, JarEntry current, int depth) throws IOException {
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
                    JarEntry jarEntry = currentFile.getJarEntry(superName + CLASS_FILE_EXT);
                    if (jarEntry != null) {
                        return implementsInterface(currentFile, jarEntry, depth + 1);
                    }
                }
                for (String intName : interfaces) {
                    JarEntry jarEntry = currentFile.getJarEntry(intName + CLASS_FILE_EXT);
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
                JarEntry jarEntry = currentFile.getJarEntry(superName + CLASS_FILE_EXT);
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
