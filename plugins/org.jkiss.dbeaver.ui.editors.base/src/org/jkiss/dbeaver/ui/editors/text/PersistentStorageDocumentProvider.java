/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.StorageDocumentProvider;
import org.jkiss.dbeaver.runtime.IPersistentStorage;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;


/**
 * BaseTextDocumentProvider
 */
public class PersistentStorageDocumentProvider extends StorageDocumentProvider {

    public PersistentStorageDocumentProvider() {
    }

    @Override
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
        if (element instanceof IStorageEditorInput) {
            IStorage storage = ((IStorageEditorInput) element).getStorage();
            if (storage instanceof IPersistentStorage) {
                byte[] bytes = document.get().getBytes(StandardCharsets.UTF_8);
                ((IPersistentStorage) storage).setContents(monitor, new ByteArrayInputStream(bytes));
            }
        }
        super.doSaveDocument(monitor, element, document, overwrite);
    }
}
