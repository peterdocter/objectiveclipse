/*******************************************************************************
 * Copyright (c) 2009 Andrew Gvozdev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Gvozdev - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.core.internal.errorparsers.tests;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

/**
 * This class contains utility methods for creating resources
 * such as projects, files, folders etc. which are being used
 * in test fixture of unit tests.
 *
 * Some classes with similar idea worth to look at:
 * org.eclipse.core.filebuffers.tests.ResourceHelper,
 * org.eclipse.cdt.ui.tests.text.ResourceHelper.
 *
 * @since 6.0
 */

public class ResourceHelper {
	private final static IProgressMonitor NULL_MONITOR = new NullProgressMonitor();

	/**
	 * Creates CDT project in a specific location and opens it.
	 *
	 * @param projectName - project name.
	 * @param locationInWorkspace - location relative to workspace root.
	 * @return - new {@link IProject}.
	 * @throws CoreException - if the project can't be created.
	 * @throws OperationCanceledException...
	 */
	public static IProject createCDTProject(String projectName, String locationInWorkspace) throws OperationCanceledException, CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(projectName);
		IProjectDescription description = workspace.newProjectDescription(projectName);
		if(locationInWorkspace != null) {
			IPath absoluteLocation = root.getLocation().append(locationInWorkspace);
			description.setLocation(absoluteLocation);
		}
		project = CCorePlugin.getDefault().createCDTProject(description, project, NULL_MONITOR);
		Assert.assertNotNull(project);

		project.open(null);
		Assert.assertTrue(project.isOpen());

		return project;
	}

	/**
	 * Creates a project in the workspace and opens it.
	 *
	 * @param projectName - project name.
	 * @return - new {@link IProject}.
	 * @throws CoreException - if the project can't be created.
	 * @throws OperationCanceledException...
	 */
	public static IProject createCDTProject(String projectName) throws OperationCanceledException, CoreException {
		return createCDTProject(projectName, null);
	}

	/**
	 * Creates a file with specified content.
	 *
	 * @param file - file name.
	 * @param contents - contents of the file.
	 * @return file handle.
	 * @throws CoreException - if the file can't be created.
	 */
	public static IFile createFile(IFile file, String contents) throws CoreException {
		if (contents == null) {
			contents= "";
		}

		InputStream inputStream = new ByteArrayInputStream(contents.getBytes());
		file.create(inputStream, true, NULL_MONITOR);
		return file;
	}

	/**
	 * Creates new file from project root with empty content. The filename
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param name - filename.
	 * @return file handle.
	 * @throws CoreException if something goes wrong.
	 */
	public static IFile createFile(IProject project, String name) throws CoreException {
		return createFile(project.getFile(name), null);
	}

	/**
	 * Creates new file from workspace root with empty content. The filename
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 * @param name - filename.
	 * @return full path of the created file.
	 *
	 * @throws CoreException...
	 * @throws IOException...
	 */
	public static IPath createWorkspaceFile(String name) throws CoreException, IOException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath fullPath = workspaceRoot.getLocation().append(name);
		java.io.File file = new java.io.File(fullPath.toOSString());
		if (!file.exists()) {
			boolean result = file.createNewFile();
			Assert.assertTrue(result);
		}
		Assert.assertTrue(file.exists());

		return fullPath;
	}

	/**
	 * Creates new folder from project root. The folder name
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param project - project where to create the folder.
	 * @param name - folder name.
	 * @return folder handle.
	 * @throws CoreException if something goes wrong.
	 */
	public static IFolder createFolder(IProject project, String name) throws CoreException {
		IFolder folder= project.getFolder(name);
		if (!folder.exists()) {
			folder.create(true, true, NULL_MONITOR);
		}
		return folder;
	}

	/**
	 * Creates new folder from workspace root. The folder name
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param name - folder name.
	 * @return full folder path.
	 * @throws IOException if something goes wrong.
	 */
	public static IPath createWorkspaceFolder(String name) throws IOException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath fullPath = workspaceRoot.getLocation().append(name);
		java.io.File folder = new java.io.File(fullPath.toOSString());
		if (!folder.exists()) {
			boolean result = folder.mkdirs();
			Assert.assertTrue(result);
		}
		Assert.assertTrue(folder.exists());

		return fullPath;
	}

	/**
	 * Creates new eclipse file-link from project root to file system file. The filename
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param fileLink - filename of the link being created.
	 * @param realFile - file on the file system, the target of the link.
	 * @return file handle.
	 * @throws CoreException if something goes wrong.
	 */
	public static IFile createLinkedFile(IProject project, String fileLink, IPath realFile) throws CoreException {
		IFile file = project.getFile(fileLink);
		file.createLink(realFile, IResource.REPLACE, null);
		Assert.assertTrue(file.exists());

		return file;
	}

	/**
	 * Creates new eclipse file-link from project root to file system file. The filename
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param fileLink - filename of the link being created.
	 * @param realFile - file on the file system, the target of the link.
	 * @return file handle.
	 * @throws CoreException if something goes wrong.
	 */
	public static IFile createLinkedFile(IProject project, String fileLink, String realFile) throws CoreException {
		return createLinkedFile(project, fileLink, new Path(realFile));
	}

	/**
	 * Creates new eclipse folder-link from project root to file system folder. The folder name
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param folderLink - name of the link being created.
	 * @param realFolder - folder on the file system, the target of the link.
	 * @return file handle.
	 * @throws CoreException if something goes wrong.
	 */
	public static IFolder createLinkedFolder(IProject project, String folderLink, IPath realFolder) throws CoreException {
		IFolder folder = project.getFolder(folderLink);
		folder.createLink(realFolder, IResource.REPLACE, null);
		Assert.assertTrue(folder.exists());

		return folder;
	}

	/**
	 * Creates new eclipse folder-link from project root to file system folder. The folder name
	 * can include relative path as a part of the name but the the path
	 * has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param folderLink - name of the link being created.
	 * @param realFolder - folder on the file system, the target of the link.
	 * @return file handle.
	 * @throws CoreException if something goes wrong.
	 */
	public static IFolder createLinkedFolder(IProject project, String folderLink, String realFolder) throws CoreException {
		return createLinkedFolder(project, folderLink, new Path(realFolder));
	}

	/**
	 * Creates new symbolic file system link from file or folder on project root
	 * to another file system file. The filename can include relative path
	 * as a part of the name but the the path has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param linkName - name of the link being created.
	 * @param realPath - file or folder on the file system, the target of the link.
	 * @return file handle.
	 *
	 * @throws UnsupportedOperationException on Windows where links are not supported.
	 * @throws IOException...
	 * @throws CoreException...
	 */
	public static IResource createSymbolicLink(IProject project, String linkName, IPath realPath)
		throws IOException, CoreException, UnsupportedOperationException {

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			throw new UnsupportedOperationException("Windows links .lnk are not supported.");
		}

		IPath linkedPath = project.getLocation().append(linkName);
		String command = "ln -s " + realPath.toOSString() + ' ' + linkedPath.toOSString();
		Process process = Runtime.getRuntime().exec(command);

		try {
			process.waitFor();
		} catch (InterruptedException e) {
		}

		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		IResource resource = project.getFile(linkName);
		if (!resource.exists()) {
			resource = project.getFolder(linkName);
		}
		Assert.assertTrue(resource.exists());

		return resource;
	}

	/**
	 * Creates new symbolic file system link from file or folder on project root
	 * to another file system file. The filename can include relative path
	 * as a part of the name but the the path has to be present on disk.
	 *
	 * @param project - project where to create the file.
	 * @param linkName - name of the link being created.
	 * @param realPath - file or folder on the file system, the target of the link.
	 * @return file handle.
	 *
	 * @throws UnsupportedOperationException on Windows where links are not supported.
	 * @throws IOException...
	 * @throws CoreException...
	 */
	public static IResource createSymbolicLink(IProject project, String linkName, String realPath)
		throws IOException, CoreException, UnsupportedOperationException {

		return createSymbolicLink(project, linkName, new Path(realPath));
	}

	/**
	 * Conversion from Windows path to Cygwin path.
	 *
	 * @param windowsPath - Windows path.
	 * @return Cygwin style converted path.
	 * @throws UnsupportedOperationException if Cygwin is unavailable.
	 * @throws IOException on IO problem.
	 */
	public static String windowsToCygwinPath(String windowsPath) throws IOException, UnsupportedOperationException {
		if (!Platform.getOS().equals(Platform.OS_WIN32)) {
			// Don't run this on non-windows platforms
			throw new UnsupportedOperationException("Not a Windows system, Cygwin is unavailable.");
		}
		String[] args = {"cygpath", "-u", windowsPath};
		Process cygpath = Runtime.getRuntime().exec(args);
		BufferedReader stdout = new BufferedReader(new InputStreamReader(cygpath.getInputStream()));

		String cygwinPath = stdout.readLine();
		if (cygwinPath == null) {
			throw new UnsupportedOperationException("Cygwin utility cygpath is not available.");
		}
		return cygwinPath.trim();
	}

	/**
	 * Conversion from Cygwin path to Windows path.
	 *
	 * @param cygwinPath - Cygwin path.
	 * @return Windows style converted path.
	 * @throws UnsupportedOperationException if Cygwin is unavailable.
	 * @throws IOException on IO problem.
	 */
	public static String cygwinToWindowsPath(String cygwinPath) throws IOException, UnsupportedOperationException {
		if (!Platform.getOS().equals(Platform.OS_WIN32)) {
			// Don't run this on non-windows platforms
			throw new UnsupportedOperationException("Not a Windows system, Cygwin is unavailable.");
		}
		String[] args = {"cygpath", "-w", cygwinPath};
		Process cygpath = Runtime.getRuntime().exec(args);
		BufferedReader stdout = new BufferedReader(new InputStreamReader(cygpath.getInputStream()));

		String windowsPath = stdout.readLine();
		if (windowsPath == null) {
			throw new UnsupportedOperationException("Cygwin utility cygpath is not available.");
		}
		return windowsPath.trim();
	}

}