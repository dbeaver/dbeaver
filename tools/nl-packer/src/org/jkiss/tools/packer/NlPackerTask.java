/*
 * Copyright (C) 2010-2014 Serge Rieder serge@jkiss.org
 * Copyright (C) 2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.tools.packer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Extract resources properties files from the project for localization.
 * 
 * @author Eugene Fradkin (eugene.fradkin@gmail.com)
 */
public class NlPackerTask
{
    public static final FilenameFilter PROPERTIES_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".properties");
        }
    };
    public static final FilenameFilter JARS_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".jar");
        }
    };
    public static final String JKISS_PREFIX = "org.jkiss.dbeaver";
    public static final FilenameFilter JKISS_PLUGINS_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.startsWith(JKISS_PREFIX) && name.endsWith(".jar");
        }
    };

    private String dbeaverLocation;
    private String nlPropertiesLocation;
    private String sourceEncoding;

    // The method executing the task
    public void execute() throws IOException
    {
/*
        System.out.println("dbeaverLocation = " + dbeaverLocation);
        System.out.println("nlPropertiesLocation = " + nlPropertiesLocation);
*/

        File dbeaverDir = new File(dbeaverLocation);
        if (!dbeaverDir.exists() || !dbeaverDir.isDirectory()) {
            throw new IOException("Can't find DBeaver directory " + dbeaverLocation);
        }

        File nlPropertiesDir = new File(nlPropertiesLocation);
        if (!nlPropertiesDir.exists() || !nlPropertiesDir.isDirectory()) {
            throw new IOException("Can't find nl-properties directory " + nlPropertiesLocation);
        }

        File pluginsDir = new File(dbeaverDir, "plugins");
        File[] dbeaverPlugins = pluginsDir.listFiles(JKISS_PLUGINS_FILTER);

        File[] nlPropertiesDirs = nlPropertiesDir.listFiles();
        for (File dbeaverPlugin : dbeaverPlugins) {
            String dbeaverPluginName = dbeaverPlugin.getName();
            if (dbeaverPluginName.indexOf(".ext") == JKISS_PREFIX.length()) {
                dbeaverPluginName = JKISS_PREFIX + dbeaverPluginName.substring(JKISS_PREFIX.length() + 4);
            }
            //System.out.println(":: " + dbeaverPluginName);
            for (File propertiesDir : nlPropertiesDirs) {
                final String pluginName = propertiesDir.getName();
                if (dbeaverPluginName.startsWith(pluginName)) { // searching an appropriate plugin
                    try {
                        List<File> filesToPack = new ArrayList<File>();
                        filesToPack.addAll(Arrays.asList(propertiesDir.listFiles(PROPERTIES_FILTER)));
                        File srcDir = new File(propertiesDir, "src");
                        filesToPack.addAll(Arrays.asList(srcDir.listFiles()));
                        File pluginZipFile = new File(nlPropertiesLocation, pluginName + ".zip");

                        Packager.packZip(pluginZipFile, filesToPack, sourceEncoding);

                        localizePlugin(Arrays.asList(pluginZipFile), dbeaverPlugin);

                        if (!pluginZipFile.delete()) {
                            System.out.println("Can't delete temp packed plugin file " + pluginZipFile.getAbsolutePath());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        System.out.println("DBeaver at " + dbeaverLocation + " is successfully localized from " + nlPropertiesLocation + ".");
    }

    public static void localizePlugin(List<File> babelFiles, File pluginFile) throws IOException
    {
        if (!pluginFile.exists() || babelFiles == null || babelFiles.isEmpty()) {
            return;
        }
        // get a temp file
        File tempFile = new File(pluginFile.getName() + ".tmp");

        ZipFile destZip = null;
        ZipOutputStream out = null;
        try {
            boolean renameOk = pluginFile.renameTo(tempFile);
            if (!renameOk) {
                throw new IOException("could not rename the file " + pluginFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
            }

            try {
                out = new ZipOutputStream(new FileOutputStream(pluginFile));
            } catch (FileNotFoundException e) {
                System.out.println("Can't create an output stream for destination zip file " + pluginFile.getAbsolutePath());
                return;
            }

            try {
                destZip = new ZipFile(tempFile);
            } catch (IOException e) {
                System.out.println("A problem with processing destination zip file " + pluginFile.getAbsolutePath());
                return;
            }

            for (File sourceZipFile : babelFiles) {
                // append source file entries
                ZipFile sourceZip = null;
                try {
                    sourceZip = new ZipFile(sourceZipFile);
                } catch (IOException e) {
                    System.out.println("A problem with processing source zip file " + sourceZipFile.getAbsolutePath());
                    continue;
                }
                Enumeration<? extends ZipEntry> srcEntries = sourceZip.entries();
                while (srcEntries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) srcEntries.nextElement();
                    String entryName = entry.getName();
                    //System.out.println("entryName = " + entryName);
                    ZipEntry newEntry = new ZipEntry(entryName);
                    try {
                        out.putNextEntry(newEntry);

                        BufferedInputStream bis = new BufferedInputStream(sourceZip.getInputStream(entry));
                        while (bis.available() > 0) {
                            out.write(bis.read());
                        }
                        out.closeEntry();
                        bis.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                        System.out.println("Can't copy " + entryName + " from " + sourceZipFile.getAbsolutePath() +
                                " to " + pluginFile.getAbsolutePath() + ". " + e.getMessage());
                        //break;
                    }
                }
                try {
                    sourceZip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // append dest file own entries
            Enumeration<? extends ZipEntry> destEntries = destZip.entries();
            while (destEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) destEntries.nextElement();
                //System.out.println(entry.getName());
                String entryName = entry.getName();
                ZipEntry newEntry = new ZipEntry(entryName);
                try {
                    out.putNextEntry(newEntry);

                    BufferedInputStream bis = new BufferedInputStream(destZip.getInputStream(entry));
                    while (bis.available() > 0) {
                        out.write(bis.read());
                    }
                    out.closeEntry();
                    bis.close();
                } catch (IOException e) {
                    // do nothing
                    //e.printStackTrace();
                    //System.out.println("Can't copy " + entryName + " from temporary file to " +
                    //        pluginFile.getAbsolutePath() + ". " + e.getMessage());
                    //break;
                }
            }

        } finally {
            try {
                if (out != null) {
                    // Complete the ZIP file
                    try {
                        out.close();
                    } catch (IOException e) {
                        System.out.println("Can't close output stream for destination " + pluginFile.getAbsolutePath());
                    }
                }
                if (destZip != null) {
                    destZip.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!tempFile.delete()) {
                System.out.println("Can't delete temp file " + tempFile.getAbsolutePath());
            }
        }
    }

    public static final void main(String[] args) throws IOException
    {
        if (args == null || args.length < 2) {
            System.out.println("Run the utility with parameters: java -cp . org.jkiss.tools.packer.NlPackerTask <dbeaver location> <NL properties location> [<charset (encoding) name>]");
        }
        else {
            NlPackerTask nlPackerTask = new NlPackerTask();
            nlPackerTask.dbeaverLocation = args[0];
            nlPackerTask.nlPropertiesLocation = args[1];
            if (args.length > 2) {
                nlPackerTask.sourceEncoding = args[2];
            }
            nlPackerTask.execute();
        }
    }
}
