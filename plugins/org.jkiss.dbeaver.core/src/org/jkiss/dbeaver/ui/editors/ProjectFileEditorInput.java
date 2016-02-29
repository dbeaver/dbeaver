/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * ProjectFileEditorInput
 */
public class ProjectFileEditorInput extends PlatformObject implements IFileEditorInput, IPathEditorInput {
	private IFile file;

	/**
	 * Creates an editor input based of the given file resource.
	 *
	 * @param file the file resource
	 */
	public ProjectFileEditorInput(IFile file) {
		if (file == null)
			throw new IllegalArgumentException();
		this.file = file;

	}

	public int hashCode() {
		return file.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ProjectFileEditorInput)) {
			return false;
		}
		ProjectFileEditorInput other = (ProjectFileEditorInput) obj;
		return file.equals(other.file);
	}

	@Override
    public boolean exists() {
		return file.exists();
	}

	@Override
	public IFile getFile() {
		return file;
	}

    public IProject getProject()
    {
        if (getFile() == null || !getFile().exists()) {
            return null;
        }
        return getFile().getProject();
    }

	@Override
    public ImageDescriptor getImageDescriptor() {
		return DBeaverIcons.getImageDescriptor(DBIcon.TYPE_UNKNOWN);
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	@Override
    public String getName() {
		return file.getName();
	}

	@Override
    public IPersistableElement getPersistable() {
		return null;
	}

	public IStorage getStorage() {
		return file;
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	@Override
    public String getToolTipText() {
		return file.getFullPath().makeRelative().toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getClass().getName() + "(" + file.getFullPath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Allows for the return of an {@link IWorkbenchAdapter} adapter.
	 *
	 * @since 3.5
	 *
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
    public Object getAdapter(Class adapter) {
		if (IWorkbenchAdapter.class.equals(adapter)) {
			return new WorkbenchAdapter() {
				@Override
                public ImageDescriptor getImageDescriptor(Object object) {
					return ProjectFileEditorInput.this.getImageDescriptor();
				}

				@Override
                public String getLabel(Object o) {
					return ProjectFileEditorInput.this.getName();
				}

				@Override
                public Object getParent(Object o) {
					return ProjectFileEditorInput.this.file.getParent();
				}
			};
		}

		return super.getAdapter(adapter);
	}

    @Override
    public IPath getPath()
    {
        if (file == null) {
        	return null;
        }
        return file.getLocation();
    }
}
