/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.runtime.IPersistentStorage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * BinaryEditorInput
 */
public class BinaryEditorInput implements IEditorInput, IStorageEditorInput {

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

/*
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
*/

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

    @Override
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
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IStorage.class) {
            return adapter.cast(getStorage());
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
        public <T> T getAdapter(Class<T> adapter)
        {
            return null;
        }

        @Override
        public void setContents(IProgressMonitor monitor, InputStream stream) throws CoreException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copyStream(stream, baos);
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
