/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * BinaryEditorInput
 */
public class BinaryEditorInput implements IEditorInput {

    private String name;
    private byte[] value;
    private boolean readOnly;
    private IStorage storage;
    private String encoding;

    public BinaryEditorInput(String name, byte[] value, boolean readOnly, String encoding) {
        this.name = name;
        this.value = value;
        this.readOnly = readOnly;
        this.encoding = encoding;
	}

	public int hashCode() {
		return Arrays.hashCode(value);
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BinaryEditorInput)) {
			return false;
		}
		BinaryEditorInput other = (BinaryEditorInput) obj;
		return Arrays.equals(value, other.value);
	}

	@Override
    public boolean exists() {
		return true;
	}

	@Override
    public ImageDescriptor getImageDescriptor() {
		return DBeaverIcons.getImageDescriptor(DBIcon.TREE_INFO);
	}

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
            storage = new ByteStorage();
        }
		return storage;
	}

	@Override
    public String getToolTipText() {
		return name;
	}

    public String toString() {
		return new String(value);
	}

    public byte[] getValue() {
        return value;
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IStorage.class) {
            return getStorage();
        }
		return null;
	}

    private class ByteStorage implements IPersistentStorage,IEncodedStorage {
        @Override
        public InputStream getContents() throws CoreException
        {
            return new ByteArrayInputStream(value);
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
                IOUtils.copyStream(stream, baos, 10000);
            } catch (IOException e) {
                throw new CoreException(GeneralUtils.makeExceptionStatus(e));
            }
            value = baos.toByteArray();
        }

        @Override
        public String getCharset() throws CoreException {
            return encoding;
        }
    }
}
