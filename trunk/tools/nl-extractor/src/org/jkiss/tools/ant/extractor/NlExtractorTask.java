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
package org.jkiss.tools.ant.extractor;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Extract resources properties files from the project for localization.
 */
public class NlExtractorTask extends Task
{
    public static final String PROPERTIES_EXT = ".properties";
    public static final FilenameFilter PROPERTIES_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(PROPERTIES_EXT);
        }
    };

    /**
     * The project base folder.
     */
    private String baseLocation;
    /**
     * Extractor target location.
     */
    private String targetLocation;
    /**
     * Required locales (for example, "fr, de, it"). Can be null. 
     * English locale is always extracted.
     */
    private String locales;
    /**
     * A charset name of target properties files. 
     * Can be empty (then standard ISO-8859-1 charset is used) 
     */
    private String targetEncoding;

    private File baseDir;
    private File targetDir;
    private Set<String> localesSet = new HashSet<String>();

    public void setTargetLocation(String msg)
    {
        this.targetLocation = msg;
    }

    public void setBaseLocation(String baseLocation)
    {
        this.baseLocation = baseLocation;
    }

    public void setLocales(String locales)
    {
        this.locales = locales;
    }

    public void setTargetEncoding(String targetEncoding)
    {
        this.targetEncoding = targetEncoding;
    }

    // The method executing the task
    @Override
    public void execute() throws BuildException
    {
/*
        Project prj = this.getProject();
        File baseDir = prj.getBaseDir();
        String defaultTarget = prj.getDefaultTarget();
        String dscrptn = prj.getDescription();
        Hashtable inheritedProperties = prj.getInheritedProperties();
        Hashtable userProperties = prj.getUserProperties();
        Target owningTarget = this.getOwningTarget();
        Hashtable properties = prj.getProperties();
*/
        System.out.println("baseLocation = " + baseLocation);
        System.out.println("targetLocation = " + targetLocation);
        System.out.println("locales = " + locales);

        baseDir = new File(baseLocation);
        targetDir = new File(targetLocation);
        StringTokenizer st = new StringTokenizer(locales, ", ");
        while (st.hasMoreTokens()) {
            localesSet.add(st.nextToken());
        }
        //System.out.println(localesSet);
        try {
            processDirectory (baseDir);
        } catch (IOException e) {
            //e.printStackTrace();
            throw new BuildException(e.getMessage(), e);
        }
    }

    private void processDirectory(File dir) throws IOException
    {
        File pluginXml = new File(dir, "plugin.xml");
        boolean pluginRoot = pluginXml.exists();
        for (File file : dir.listFiles()) {
            if (pluginRoot && file.getName().equals("bin")) {
                continue;
            }
            if (file.isDirectory()) {
                processDirectory(file);
            }
            else {
                String filename = file.getName();
                if (PROPERTIES_FILTER.accept(null, filename)) {
                    String basePropertiesName = (file.getName()).substring(0, filename.length() - PROPERTIES_EXT.length());
                    if (new File(dir, basePropertiesName + "_ru.properties").exists()) { // russian localization exists
                        String relativePath = baseDir.toURI().relativize(file.toURI()).toString();
                        //System.out.println(relativePath);
                        String relativeDir = relativePath.substring(0, relativePath.lastIndexOf("/"));
                        //System.out.println(relativeDir);
                        File targetBase = new File(targetDir, relativeDir);
                        targetBase.mkdirs();
                        for (File propertiesFile : dir.listFiles(new LocalPropertiesFilenameFilter(basePropertiesName))) {
                            copyFile(propertiesFile, new File(targetBase, propertiesFile.getName()), targetEncoding);
                        }
                    }
                }
            }
        }
    }

    private static void copyFile(File source, File target, String encoding) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
        OutputStreamWriter out;
        boolean doEncode = encoding != null && encoding.length() > 0;
        if (doEncode) {
            out = new OutputStreamWriter(new FileOutputStream(target), encoding);
        }
        else {
            out = new OutputStreamWriter(new FileOutputStream(target));
        }
        BufferedWriter writer = new BufferedWriter(out);
        String line = null;
        char[] convtBuf = new char[1024];
        while ((line=reader.readLine()) != null) {
            if (doEncode) {
                line = loadConvert(line.toCharArray(), 0, line.length(), convtBuf);
            }
            writer.write(line);
            writer.newLine();   // Write system dependent end of line.
        }
        reader.close();  // Close to unlock.
        writer.close();  // Close to unlock and flush to disk.
    }

    /**
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
     * 
     * Got from the java.util.Properties class.
     */
    private static String loadConvert (char[] in, int off, int len, char[] convtBuf) {
        if (convtBuf.length < len) {
            int newLen = len * 2;
            if (newLen < 0) {
                newLen = Integer.MAX_VALUE;
            }
            convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf;
        int outLen = 0;
        int end = off + len;

        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                aChar = in[off++];
                if(aChar == 'u') {
                    // Read the xxxx
                    int value=0;
                    for (int i=0; i<4; i++) {
                        aChar = in[off++];
                        switch (aChar) {
                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7': case '8': case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a': case 'b': case 'c':
                            case 'd': case 'e': case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A': case 'B': case 'C':
                            case 'D': case 'E': case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed \\uxxxx encoding.");
                        }
                    }
                    out[outLen++] = (char)value;
                } else {
                    if (aChar == 't') aChar = '\t';
                    else if (aChar == 'r') aChar = '\r';
                    else if (aChar == 'n') aChar = '\n';
                    else if (aChar == 'f') aChar = '\f';
                    out[outLen++] = aChar;
                }
            } else {
                out[outLen++] = (char)aChar;
            }
        }
        return new String (out, 0, outLen);
    }

    private class LocalPropertiesFilenameFilter implements FilenameFilter
    {
        private String basePropertiesName;

        private LocalPropertiesFilenameFilter(String basePropertiesName)
        {
            this.basePropertiesName = basePropertiesName;
        }

        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(PROPERTIES_EXT) && 
                    (name.startsWith(basePropertiesName) || (name.startsWith(basePropertiesName) && isPropertiesAccepted(name)));
        }

        private boolean isPropertiesAccepted(String fileName) {
            if (localesSet.isEmpty()) {
                return true;
            }
            for (String locale : localesSet) {
                if (fileName.contains("_" + locale)) {
                    return true;
                }
            }
            return false;
        }
    }
}
