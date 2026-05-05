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
package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.dbeaver.Log;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Class loader loads libraries from Oracle home folder.
 */
public class OCIClassLoader extends ClassLoader
{
    private static final Log log = Log.getLog(OCIClassLoader.class);

    //private OracleHomeDescriptor oracleHomeDescriptor;
    private File[] oraHomeLibraries;

    public OCIClassLoader(OracleHomeDescriptor oracleHomeDescriptor, ClassLoader parent)
    {
        super(parent);
        //this.oracleHomeDescriptor = oracleHomeDescriptor;

        File oraHomeFile = new File(oracleHomeDescriptor.getName());
        File dllFolder = OCIUtils.isInstantClient(oracleHomeDescriptor.getName()) ? oraHomeFile : new File(oraHomeFile, "bin");
        if (dllFolder.exists()) {
            oraHomeLibraries = dllFolder.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(System.mapLibraryName("")); // OS dependent library extension
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
            log.warn("Binary folder isn't found in Oracle home " + oracleHomeDescriptor.getName());
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
