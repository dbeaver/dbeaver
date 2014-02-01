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
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * An utility class. Performs packaging operations.
 */
public class Packager
{
    public static void packZip(File output, List<File> sources, String encoding) throws IOException
    {
        //System.out.println("Packaging to " + output.getName());
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output));
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);

        for (File source : sources) {
            if (source.isDirectory()) {
                zipDir(zipOut, "", source, encoding);
            } else {
                zipFile(zipOut, "", source, encoding);
            }
        }
        zipOut.flush();
        zipOut.close();
        //System.out.println("Done");
    }

    private static String buildPath(String path, String file)
    {
        if (path == null || path.isEmpty()) {
            return file;
        } else {
            return path + "/" + file;
        }
    }

    private static void zipDir(ZipOutputStream zos, String path, File dir, String encoding) throws IOException
    {
        if (!dir.canRead()) {
            System.out.println("Cannot read " + dir.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }

        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());
        //System.out.println("Adding Directory " + path);

        for (File source : files) {
            if (source.isDirectory()) {
                zipDir(zos, path, source, encoding);
            } else {
                zipFile(zos, path, source, encoding);
            }
        }

        //System.out.println("Leaving Directory " + path);
    }

    private static void zipFile(ZipOutputStream zos, String path, File file, String encoding) throws IOException
    {
        if (!file.canRead()) {
            System.out.println("Cannot read " + file.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }

        //System.out.println("Compressing " + file.getName());
        zos.putNextEntry(new ZipEntry(buildPath(path, file.getName())));

        boolean doEncode = encoding != null && encoding.length() > 0;
        BufferedReader reader;
        if (doEncode) {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        } else {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        }
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos));

        String line = null;
        while ((line=reader.readLine()) != null) {
            if (doEncode) {
                line = saveConvert(line, false, true);
            }
            writer.write(line);
            writer.newLine();   // Write system dependent end of line.
        }
        reader.close();  // Close to unlock.
        writer.flush();
        zos.closeEntry();
        //writer.close();  // Close to unlock and flush to disk.
    }

    /*
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
     * 
     * Got from the java.util.Properties class.
     */
    private static String saveConvert(String theString,
                               boolean escapeSpace,
                               boolean escapeUnicode) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for(int x=0; x<len; x++) {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\'); outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch(aChar) {
                case ' ':
                    if (x == 0 || escapeSpace)
                        outBuffer.append('\\');
                    outBuffer.append(' ');
                    break;
/*
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                    break;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                    break;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                    break;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\'); outBuffer.append(aChar);
                    break;
*/
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode ) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >>  8) & 0xF));
                        outBuffer.append(toHex((aChar >>  4) & 0xF));
                        outBuffer.append(toHex( aChar        & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }
    /**
     * Convert a nibble to a hex character
     * @param	nibble	the nibble to convert.
     */
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }
    /** A table of hex digits */
    private static final char[] hexDigit = {
            '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
}
