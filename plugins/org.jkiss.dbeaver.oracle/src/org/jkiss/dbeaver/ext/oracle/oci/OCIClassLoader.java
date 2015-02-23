/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.dbeaver.core.Log;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Class loader loads libraries from Oracle home folder.
 */
public class OCIClassLoader extends ClassLoader
{
    static final Log log = Log.getLog(OCIClassLoader.class);

    //private OracleHomeDescriptor oracleHomeDescriptor;
    private File[] oraHomeLibraries;

    public OCIClassLoader(OracleHomeDescriptor oracleHomeDescriptor, ClassLoader parent)
    {
        super(parent);
        //this.oracleHomeDescriptor = oracleHomeDescriptor;

        File oraHomeFile = new File(oracleHomeDescriptor.getHomeId());
        File dllFolder = oracleHomeDescriptor.isInstantClient() ? oraHomeFile : new File(oraHomeFile, "bin");
        if (dllFolder.exists()) {
            oraHomeLibraries = dllFolder.listFiles(new FilenameFilter()
            {
                @Override
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
    public String findLibrary(String libname)
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
