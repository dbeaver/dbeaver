/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

	@Override
    public boolean exists() {
		return true;
	}

	@Override
    public ImageDescriptor getImageDescriptor() {
		return DBIcon.TREE_INFO.getImageDescriptor();
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	@Override
    public String getName() {
		return name;
	}

	@Override
    public IPersistableElement getPersistable() {
		return null;
	}

	public IStorage getStorage() {
        if (storage == null) {
            storage = new IStorage() {
                @Override
                public InputStream getContents() throws CoreException
                {
                    return new ByteArrayInputStream(buffer.toString().getBytes());
                }

                @Override
                public IPath getFullPath()
                {
                    return null;
                }

                @Override
                public String getName()
                {
                    return name;
                }

                @Override
                public boolean isReadOnly()
                {
                    return readOnly;
                }

                @Override
                public Object getAdapter(Class adapter)
                {
                    return null;
                }
            };
        }
		return storage;
	}

	@Override
    public String getToolTipText() {
		return name;
	}

	public String toString() {
		return buffer.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
    public Object getAdapter(Class adapter) {
        if (adapter == IStorage.class) {
            return getStorage();
        }
		return null;
	}

}