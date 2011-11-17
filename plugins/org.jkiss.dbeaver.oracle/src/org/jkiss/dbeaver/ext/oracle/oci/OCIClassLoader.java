/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLClassLoader;

/**
 * Class loader loads libraries from Oracle home folder.
 */
public class OCIClassLoader extends URLClassLoader
{
    static final Log log = LogFactory.getLog(OCIClassLoader.class);

    private OracleHomeDescriptor oracleHomeDescriptor;
    private File[] oraHomeLibraries;

    public OCIClassLoader(OracleHomeDescriptor oracleHomeDescriptor, ClassLoader parent)
    {
        super(oracleHomeDescriptor.getLibraries(), parent);
        this.oracleHomeDescriptor = oracleHomeDescriptor;

        File oraHomeFile = new File(oracleHomeDescriptor.getHomeId());
        File dllFolder = new File(oraHomeFile, oracleHomeDescriptor.isInstantClient() ? "" : "BIN");
        if (dllFolder.exists()) {
            oraHomeLibraries = dllFolder.listFiles(new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(System.mapLibraryName("")); // OS depenent library extension
                }
            });
/*
            System.loadLibrary("KERNEL32");
            System.loadLibrary("USER32");
            System.loadLibrary("WINMM");
            System.loadLibrary("WS2_32");
            System.loadLibrary("PSAPI");
            System.loadLibrary("OLE32");
            System.loadLibrary("ADVAPI32");
            System.loadLibrary("MSVCRT");
            System.load(dllFolder.getAbsolutePath() + "/OCI.DLL");
            System.load(dllFolder.getAbsolutePath() + "/MSVCR80.DLL");
            System.load(dllFolder.getAbsolutePath() + "/ORACORE11.DLL");
            System.load(dllFolder.getAbsolutePath() + "/ORAUNLS11.DLL");
            System.load(dllFolder.getAbsolutePath() + "/ORAUTS.DLL");
            System.load(dllFolder.getAbsolutePath() + "/ORANLS11.DLL");
            System.load(dllFolder.getAbsolutePath() + "/ORAUTS.DLL");
            System.load(dllFolder.getAbsolutePath() + "/ADVAPI32.DLL");
*/
        }
        else {
            log.warn("Binary folder isn't found in Oracle home " + oracleHomeDescriptor.getHomeId());
        }
    }

    @Override
    protected String findLibrary(String libname)
    {
        String nativeName = System.mapLibraryName(libname);
        for (File library : oraHomeLibraries) {
            if (library.getName().equalsIgnoreCase(nativeName)) {
                return library.getAbsolutePath();
            }
        }
        return super.findLibrary(libname);
    }
}
