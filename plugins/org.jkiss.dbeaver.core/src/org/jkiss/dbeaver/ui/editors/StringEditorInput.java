/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.ui.DBIcon;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * StringEditorInput
 */
public class StringEditorInput implements IEditorInput {

    private String name;
    private StringBuilder buffer;
    private boolean readOnly;
    private IStorage storage;

	public StringEditorInput(String name, CharSequence value, boolean readOnly) {
        this.name = name;
        this.buffer = new StringBuilder(value);
        this.readOnly = readOnly;
	}

	public int hashCode() {
		return buffer.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof StringEditorInput)) {
			return false;
		}
		StringEditorInput other = (StringEditorInput) obj;
		return buffer.equals(other.buffer);
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return DBIcon.INFO.getImageDescriptor();
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	public String getName() {
		return name;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public IStorage getStorage() {
        if (storage == null) {
            storage = new IStorage() {
                public InputStream getContents() throws CoreException
                {
                    return new ByteArrayInputStream(buffer.toString().getBytes());
                }

                public IPath getFullPath()
                {
                    return null;
                }

                public String getName()
                {
                    return name;
                }

                public boolean isReadOnly()
                {
                    return readOnly;
                }

                public Object getAdapter(Class adapter)
                {
                    return null;
                }
            };
        }
		return storage;
	}

	public String getToolTipText() {
		return name;
	}

	public String toString() {
		return buffer.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public Object getAdapter(Class adapter) {
        if (adapter == IStorage.class) {
            return getStorage();
        }
		return null;
	}

}