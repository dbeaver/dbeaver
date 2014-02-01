/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AnnotationsExternationalizator {

    private static final String PLUGIN_FOLDER = "C:\\_WORK\\JKISS\\DBeaver\\SVN\\dbeaver\\plugins\\org.jkiss.dbeaver.oracle";
    private static final String PLUGIN_PROPERTIES_NAME = "plugin.properties";

    private static File pluginPropertiesFile = null;
    private static int counter = 1;
    private static FileWriter pluginPropertiesWriter = null;
    public static final String PROPERTY_PREFIX = "@Property(";
    public static final String NAME_SUFFIX = "name = \"";
    public static final String DESCRIPTION_SUFFIX = ", description = \"";
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

        // iterate file lines
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimLine = line.trim();

            // package name
            if (trimLine.startsWith(PACKAGE_PREFIX)) {
                int prefixLength = PACKAGE_PREFIX.length();
                packageName = trimLine.substring(prefixLength, trimLine.indexOf(";", prefixLength));
            }

            // getter with property
            if (trimLine.startsWith(PROPERTY_PREFIX) && trimLine.contains(NAME_SUFFIX)) {
                System.out.println(counter++ + ": " + trimLine);

                int nameValueIndex = trimLine.indexOf(NAME_SUFFIX) + NAME_SUFFIX.length();
                String namePropertyValue = trimLine.substring(nameValueIndex, trimLine.indexOf("\"", nameValueIndex));
                lines[i] = line.substring(0, line.indexOf("name = ")) + line.substring(line.indexOf(", ", line.indexOf("name = ")) + 2, line.length());
                line = lines[i];

                String methodName = null;

                // method (getter) name
                for (int k = i; k < lines.length; k++) {
                    String l = lines[k];
                    String trimL = l.trim();
                    int indexGet = trimL.indexOf("get");
                    if (indexGet < 0) {
                        indexGet = trimL.indexOf(" is");
                    }
                    int indexBrackets = trimL.indexOf("()");
                    int index = indexGet < indexBrackets ? indexGet : indexBrackets;
                    if (trimL.startsWith(PUBLIC_PREFIX) && index > 0) {
                        methodName = trimL.substring(indexGet + 3, trimL.indexOf("("));
                        methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                        //i = k + 1;
                        break;
                    }
                }

                System.out.println("meta." + packageName + "." + className + "." + methodName + ".name=" + namePropertyValue);

                // description
                String descrPropertyValue = null;
                int descrIndex = line.indexOf(DESCRIPTION_SUFFIX);
                if (descrIndex > 0) {
                    int beginIndex = descrIndex + DESCRIPTION_SUFFIX.length();
                    int endIndex = line.indexOf("\"", beginIndex);
                    descrPropertyValue = line.substring(beginIndex, endIndex);
                    lines[i] = line.substring(0, descrIndex) + line.substring(endIndex, line.length());
                }
                System.out.println("meta." + packageName + "." + className + "." + methodName + ".description=" + descrPropertyValue);
                System.out.println(lines[i]);
                System.out.println("");

                pluginPropertiesWriter.write("\nmeta." + packageName + "." + className + "." + methodName + ".name=" + namePropertyValue);
                if (descrPropertyValue != null) {
                    pluginPropertiesWriter.write("\nmeta." + packageName + "." + className + "." + methodName + ".description=" + descrPropertyValue);
                }
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
