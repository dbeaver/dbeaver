/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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

    public StringBuilder getBuffer()
    {
        return buffer;
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