/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * ProjectFileEditorInput
 */
public class ProjectFileEditorInput extends PlatformObject implements IPathEditorInput {
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

	/* (non-Javadoc)
	 * Method declared on IFileEditorInput.
    */
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
		return DBIcon.TYPE_UNKNOWN.getImageDescriptor();
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
		} else if (IFile.class.equals(adapter)) {
            return file;
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
