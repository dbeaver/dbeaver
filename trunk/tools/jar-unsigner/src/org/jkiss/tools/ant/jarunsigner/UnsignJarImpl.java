/*
 * UnsignJarImpl.java
 *
 * Copyright (C) 2006  Dannes Wessels (dizzzz_at_gmail_com)
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package org.jkiss.tools.ant.jarunsigner;

import java.io.*;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Implementation class for for unsiging JAR files.
 *
 * @author Dannes Wessels
 */
public class UnsignJarImpl {

    /**
     * Unsign the specified jarfile.
     *
     * @param jarfile File containing signed JAR file.
     */
    public void unsign(File jarfile) throws IOException
    {

        if (!jarfile.canRead()) {
            System.err.println("Cannot read file " + jarfile.getAbsolutePath());
            return;
        }

        if (!jarfile.canWrite()) {
            System.err.println("Cannot write file " + jarfile.getAbsolutePath());
            return;
        }

        File tmpFile = null;
        try {
            tmpFile = new File(jarfile.getParent(), "unsigner_" + UUID.randomUUID().toString() + ".tmp");
            BufferedOutputStream bos
                = new BufferedOutputStream(new FileOutputStream(tmpFile));
            BufferedInputStream bis
                = new BufferedInputStream(new FileInputStream(jarfile));

            unsign(bis, bos);

            bos.close();
            bis.close();

            // Cleanup
            jarfile.delete();
            tmpFile.renameTo(jarfile);

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Unsign JAR file supplied as input stream, write results to output stream.
     *
     * @param is InputStream with signed JAR file.
     * @param os OutputStream containing unsigned JAR file.
     */
    public void unsign(InputStream is, OutputStream os) throws IOException
    {

        JarInputStream jis = new JarInputStream(is);

        JarOutputStream jos = null;

        // Clean manifest, start OutputStream
        Manifest manifest = jis.getManifest();
        if (manifest == null) {
            jos = new JarOutputStream(os);

        } else {
            manifest.getEntries().clear();
            jos = new JarOutputStream(os, manifest);
        }

        JarEntry src;
        while ((src = jis.getNextJarEntry()) != null) {

            String name = src.getName();
            String lowercaseName = name.toLowerCase();

            // Filter files used for jar signing
            if (lowercaseName.startsWith("meta-inf") &&
                (lowercaseName.endsWith(".rsa") || lowercaseName.endsWith(".dsa") || lowercaseName.endsWith(".sf"))) {
                //No op

            } else {
                //buffer = getNextResource(jis);
                JarEntry dest = new JarEntry(name);
                jos.putNextEntry(dest);
                copyContent(jis, jos);
            }
        }

        jos.close();
        jis.close();
    }

    private static void copyContent(JarInputStream jis, JarOutputStream jos) throws IOException
    {

        byte[] buffer = new byte[4096];
        int len = 0;
        while ((len = jis.read(buffer)) != -1) {
            jos.write(buffer, 0, len);
        }
    }


}
