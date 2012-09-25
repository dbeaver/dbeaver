/*
 * Copyright (C) 2010-2012 Serge Rieder
 * eugene.fradkin@gmail.com
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

package org.jkiss.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnnotationsExternationalizator {

    private static final String PLUGIN_FOLDER = "C:\\_WORK\\JKISS\\DBeaver\\SVN\\dbeaver\\plugins\\org.jkiss.dbeaver.core";
    private static final String PLUGIN_PROPERTIES_NAME = "plugin.properties";

    private static File pluginPropertiesFile = null;
    private static int counter = 1;
    private static FileWriter pluginPropertiesWriter = null;
    public static final String PROPERTY_NAME_PREFIX = "@Property(name = \"";
    public static final String PACKAGE_PREFIX = "package ";
    public static final String CLASS_PREFIX = " class ";
    public static final String PUBLIC_PREFIX = "public ";

    public static final void main(String[] args) throws IOException {

        try {
            pluginPropertiesFile = new File(PLUGIN_FOLDER, PLUGIN_PROPERTIES_NAME);
            pluginPropertiesWriter = new FileWriter(pluginPropertiesFile, true);

            File folder = new File(PLUGIN_FOLDER);
            processFolder(folder);
        }
        finally {
            if (pluginPropertiesWriter != null) {
                pluginPropertiesWriter.close();
            }
        }
    }

    private static void processFolder(File folder) throws IOException {
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                processFolder(file);
            }
            else {
                if (file.getName().endsWith(".java")) {
                    processJavaClass(file);
                }
            }
        }
    }

    private static void processJavaClass(File javaFile) throws IOException {
        String javaFileName = javaFile.getName();
        String packageName = null;
        String className = javaFileName.substring(0, javaFileName.length() - 5);
        String[] lines = readLines(javaFile);
        boolean hasChanges = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimLine = line.trim();

            if (trimLine.startsWith(PACKAGE_PREFIX)) {
                int prefixLength = PACKAGE_PREFIX.length();
                packageName = trimLine.substring(prefixLength, trimLine.indexOf(";", prefixLength));
            }

            if (trimLine.startsWith(PROPERTY_NAME_PREFIX)) {
                int prefixLength = PROPERTY_NAME_PREFIX.length();
                String propertyName = trimLine.substring(prefixLength, trimLine.indexOf("\"", prefixLength));
                lines[i] = line.substring(0, line.indexOf("name = ")) + line.substring(line.indexOf(", ") + 2, line.length());
                String methodName = null;

                for (int k = i; k < lines.length; k++) {
                    line = lines[k];
                    trimLine = line.trim();
                    int indexGet = trimLine.indexOf("get");
                    int indexBrackets = trimLine.indexOf("()");
                    int index = indexGet < indexBrackets ? indexGet : indexBrackets;
                    if (trimLine.startsWith(PUBLIC_PREFIX) && index > 0) {
                        methodName = trimLine.substring(indexGet + 3, trimLine.indexOf("("));
                        methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                        i = k + 1;
                        break;
                    }
                }
                //System.out.println(counter++ + ": [" + packageName + "." + className + "." + methodName + "] " + propertyName);
                //System.out.println(counter++ + ": meta." + packageName + "." + className + ".name." + methodName + "=" + propertyName);
                pluginPropertiesWriter.write("\nmeta." + packageName + "." + className + "." + methodName + ".name=" + propertyName);
                hasChanges = true;
            }
        }

        if (hasChanges) {
            FileWriter javaClassWriter = null;
            try {
                javaClassWriter = new FileWriter(javaFile);
                for (String line : lines) {
                    javaClassWriter.write(line + "\n");
                }
            }
            finally {
                javaClassWriter.close();
            }
        }
    }

    private static String[] readLines(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }
}
