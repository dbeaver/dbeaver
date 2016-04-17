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

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * StringEditorInput
 */
public class StringEditorInput implements IEditorInput, IStorageEditorInput {

    public static final IEditorInput EMPTY_INPUT = new StringEditorInput("<empty>", "", true, ContentUtils.DEFAULT_CHARSET);
    private String name;
    private StringBuilder buffer;
    private boolean readOnly;
    private IStorage storage;
    private String encoding;

    public StringEditorInput(String name, CharSequence value, boolean readOnly, String encoding) {
        this.name = name;
        this.buffer = new StringBuilder(value);
        this.readOnly = readOnly;
        this.encoding = encoding;
	}

/*
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
*/

	@Override
    public boolean exists() {
		return true;
	}

	@Override
    public ImageDescriptor getImageDescriptor() {
		return DBeaverIcons.getImageDescriptor(DBIcon.TREE_INFO);
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

    @Override
	public IStorage getStorage() {
        if (storage == null) {
            storage = new StringStorage();
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

    private class StringStorage implements IPersistentStorage, IEncodedStorage {
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

        @Override
        public void setContents(IProgressMonitor monitor, InputStream stream) throws CoreException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copyStream(stream, baos);
                buffer.setLength(0);
                buffer.append(new String(baos.toByteArray(), encoding));
            } catch (IOException e) {
                throw new CoreException(GeneralUtils.makeExceptionStatus(e));
            }
        }

        @Override
        public String getCharset() throws CoreException {
            return encoding;
        }
    }
}