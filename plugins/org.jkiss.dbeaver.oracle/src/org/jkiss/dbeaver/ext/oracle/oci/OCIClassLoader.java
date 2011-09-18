/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLClassLoader;

/**
 * Class loader loads libraries from Oracle home folder.
 */
public class OCIClassLoader extends URLClassLoader
{
    private OracleHomeDescriptor oracleHomeDescriptor;
    private File[] oraHomeLibraries;

    public OCIClassLoader(OracleHomeDescriptor oracleHomeDescriptor, ClassLoader parent)
    {
        super(oracleHomeDescriptor.getLibraries(), parent);
        this.oracleHomeDescriptor = oracleHomeDescriptor;

        File oraHomeFile = new File(oracleHomeDescriptor.getOraHome());
        File binFolder = new File(oraHomeFile, "BIN");
        if (binFolder != null && binFolder.exists()) {
            oraHomeLibraries = binFolder.listFiles(new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(".dll"); // TODO win32 ONLY!!!
                }
            });
        }
        else {
            OCIUtils.log.warn("BIN folder isn't found in Oracle home " + oracleHomeDescriptor.getOraHome());
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
