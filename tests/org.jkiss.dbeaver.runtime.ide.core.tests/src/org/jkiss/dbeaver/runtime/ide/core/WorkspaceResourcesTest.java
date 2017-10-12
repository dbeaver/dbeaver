package org.jkiss.dbeaver.runtime.ide.core;

import java.io.ByteArrayInputStream;
import java.net.URI;

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
	private static URI folderLocation;
	private static URI fileLocation;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception
	{
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
		folderLocation = tempFolder.newFolder().toURI();
		fileLocation = tempFolder.newFile().toURI();
	}

	@After
	public void tearDown() throws Exception
	{
		if (project.exists()) {
			project.delete(true, true, null);
		}
	}

	@Test
	public void testLinkFileNegative()
	{
		Assert.assertFalse(WorkspaceResources.linkFile(null, fileLocation, null).isOK());
		Assert.assertFalse(WorkspaceResources.linkFile(file, null, null).isOK());
		Assert.assertFalse(WorkspaceResources.linkFile(file, fileLocation, null).isOK());
	}

	@Test
	public void testLinkFilePositive()
	{
		IFile another = folder.getFile("another");
		IStatus linkFile = WorkspaceResources.linkFile(another, fileLocation, null);
		Assert.assertTrue(linkFile.isOK());
		Assert.assertTrue(another.isLinked());
		URI locationURI = another.getLocationURI();
		Assert.assertTrue(fileLocation.equals(locationURI));
	}

	@Test
	public void testLinkFolderNegative()
	{
		Assert.assertFalse(WorkspaceResources.linkFolder(null, folderLocation, null).isOK());
		Assert.assertFalse(WorkspaceResources.linkFolder(folder, null, null).isOK());
		Assert.assertFalse(WorkspaceResources.linkFolder(folder, folderLocation, null).isOK());
	}

	@Test
	public void testLinkFolderPositive()
	{
		IFolder another = folder.getFolder("another");
		IStatus linkFolder = WorkspaceResources.linkFolder(another, folderLocation, null);
		Assert.assertTrue(linkFolder.isOK());
		Assert.assertTrue(another.isLinked());
		URI locationURI = another.getLocationURI();
		Assert.assertTrue(folderLocation.equals(locationURI));
	}
}
