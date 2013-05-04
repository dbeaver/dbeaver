/*
 * UnsignJarTask.java
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ant task for unsigning JAR files.
 *
 * @author Dannes Wessels
 */
public class UnsignJarTask extends Task {

    private List<FileSet> filesets = new ArrayList<FileSet>();

    /**
     * Called by the project to let the task do its work.
     *
     * @throws BuildException if something goes wrong with the build
     */
    @Override
    public void execute() throws BuildException
    {

        // Search all file sets
        for (FileSet fs : filesets) {

            DirectoryScanner scanner = fs.getDirectoryScanner(getProject());
            scanner.scan();
            String[] files = scanner.getIncludedFiles();

            UnsignJarImpl uji = new UnsignJarImpl();
            try {

                // Perform action per entry.
                for (String file : files) {
                    log("Unsign jar " + file);
                    uji.unsign(new File(scanner.getBasedir(), file));
                }
            } catch (IOException ex) {
                throw new BuildException(ex.getMessage());
            }
        }
    }

    /**
     * Create FileSet object that will be filled by ant.
     *
     * @return Empty FileSet object.
     */
    public FileSet createFileSet()
    {
        FileSet fileSet = new FileSet();
        return fileSet;
    }

    /**
     * Add FileSet to Task. FileSet need to be created first, method can
     * be called multiple times allowing adding files in steps.
     *
     * @param set Set of files that need to be executed.
     */
    public void addFileset(FileSet set)
    {
        filesets.add(set);
    }
}
