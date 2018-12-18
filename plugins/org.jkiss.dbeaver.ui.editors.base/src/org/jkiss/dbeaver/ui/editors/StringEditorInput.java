/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.IPersistentStorage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * StringEditorInput
 */
public class StringEditorInput implements INonPersistentEditorInput, IStorageEditorInput {

    public static final IEditorInput EMPTY_INPUT = new StringEditorInput("<empty>", "", true, GeneralUtils.getDefaultFileEncoding());
    private String name;
    private StringBuilder buffer;
    private boolean readOnly;
    private StringStorage storage;
    private Charset charset;
    private Map<String, Object> properties = new HashMap<>();

    public StringEditorInput(String name, CharSequence value, boolean readOnly, String charset) {
        this.name = name;
        this.buffer = new StringBuilder(value);
        this.readOnly = readOnly;
        this.charset = Charset.forName(charset);
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
	public StringStorage getStorage() {
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

    public void setText(String text) {
        buffer.setLength(0);
        buffer.append(text);
    }

    public String toString() {
		return buffer.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IStorage.class) {
            return adapter.cast(getStorage());
        }
		return null;
	}

    @Nullable
    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public void setProperty(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    public class StringStorage implements IPersistentStorage, IEncodedStorage {
        @Override
        public InputStream getContents() {
            return new ByteArrayInputStream(buffer.toString().getBytes(charset));
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
                buffer.setLength(0);
                buffer.append(new String(baos.toByteArray(), charset));
            } catch (IOException e) {
                throw new CoreException(GeneralUtils.makeExceptionStatus(e));
            }
        }

        @Override
        public String getCharset() {
            return charset.name();
        }

        public void setString(String str) {
            buffer.setLength(0);
            if (str != null) {
                buffer.append(str);
            }
        }

        public String getString() {
            return buffer.toString();
        }
    }

}