/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.runtime.ide.core;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class WorkspaceResourcesTest {

    private static IProject project;
    private static IFolder folder;
    private static IFile file;
    private static Path folderLocation;
    private static Path fileLocation;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Assert.assertTrue(Platform.isRunning());

        project = ResourcesPlugin.getWorkspace().getRoot().getProject("some_project");
        try {
            project.create(null);
            project.open(null);
            folder = project.getFolder("some_folder");
            folder.create(true, true, null);
            file = folder.getFile("some_file");
            file.create(new ByteArrayInputStream("some_content".getBytes()), IResource.NONE, null);
        } catch (CoreException e) {
            System.out.println(e);
        }
        folderLocation = Paths.get(tempFolder.newFolder().toURI());
        fileLocation = Paths.get(tempFolder.newFile().toURI());
    }

    @After
    public void tearDown() throws Exception {
        if (project.exists()) {
            project.delete(true, true, null);
        }
    }

    @Test
    public void testLinkFilesNegative() {
        Assert.assertFalse(WorkspaceResources.createLinkedFiles(null, null, fileLocation).isOK());
        Assert.assertFalse(WorkspaceResources.createLinkedFiles(file.getParent(), null, (Path) null).isOK());
    }

    @Test
    public void testLinkFilesPositive() {
        IStatus linkFile = WorkspaceResources.createLinkedFiles(folder, null, fileLocation);
        IFile another = folder.getFile(new org.eclipse.core.runtime.Path(fileLocation.getFileName().toString()));
        Assert.assertTrue(linkFile.isOK());
        Assert.assertTrue(another.isLinked());
        //URI locationURI = another.getLocationURI();
        //Assert.assertTrue(fileLocation.equals(Paths.get(locationURI)));
    }

    @Test
    public void testLinkFoldersNegative() {
        Assert.assertFalse(WorkspaceResources.createLinkedFolders(null, null, folderLocation).isOK());
        Assert.assertFalse(WorkspaceResources.createLinkedFolders(folder.getParent(), null, (Path) null).isOK());
    }

    @Test
    public void testLinkFoldersPositive() {
        IStatus linkFolder = WorkspaceResources.createLinkedFolders(folder, null, folderLocation);
        IFolder another = folder.getFolder(new org.eclipse.core.runtime.Path(folderLocation.getFileName().toString()));
        Assert.assertTrue(linkFolder.isOK());
        Assert.assertTrue(another.isLinked());
        //URI locationURI = another.getLocationURI();
        //Assert.assertTrue(folderLocation.equals(Paths.get(locationURI)));
    }
}
