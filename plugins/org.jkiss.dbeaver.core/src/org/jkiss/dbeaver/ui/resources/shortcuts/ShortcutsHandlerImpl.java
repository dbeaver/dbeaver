/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.resources.shortcuts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.resources.AbstractResourceHandler;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/**
 * Windows Shell Link (.lnk) handler
 */
public class ShortcutsHandlerImpl extends AbstractResourceHandler {
    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException {
        try {
            final File path = resource.getLocation().toFile();
            final File resolved = resolve(path);
            if (resolved.exists()) {
                EditorUtils.openExternalFileEditor(resolved, UIUtils.getActiveWorkbenchWindow());
            } else if (DBWorkbench.getPlatformUI().confirmAction(CoreMessages.resource_shortcut_deleted_title, NLS.bind(CoreMessages.resource_shortcut_deleted_message, resolved.getName()))) {
                resource.delete(true, new NullProgressMonitor());
            }
        } catch (IOException e) {
            throw new DBException("Error resolving shell link path", e);
        }
    }

    @Override
    public int getFeatures(IResource resource) {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    /**
     * Resolves absolute path from given Shell Link file.
     * <p>
     * See <a href="http://msdn.microsoft.com/en-us/library/dd871305%28PROT.10%29.aspx">Shell Link (.LNK) Binary File Format</a>
     */
    @NotNull
    private static File resolve(@NotNull File shellLinkFile) throws IOException {
        final ByteBuffer buf = ByteBuffer
            .wrap(Files.readAllBytes(shellLinkFile.toPath()))
            .order(ByteOrder.LITTLE_ENDIAN);

        if ((buf.getInt(20) & 1) > 0) {
            // Check the flags, if LinkTargetIDList is present, then seek to sizeof(ShellLinkHeader) + sizeof(LinkTargetIDList)
            buf.position(buf.getInt(0) + buf.getShort(76) + 2);
        } else {
            // Otherwise, seek past linkFlags
            buf.position(24);
        }

        // There's no better way to determine whether path is stored in unicode format
        final boolean unicode = buf.getInt(buf.position() + 4) >= 36;
        final StringBuilder path = new StringBuilder();

        // Seek past fields we're not interested in (if `unicode` there are three additional fields)
        buf.position(buf.position() + buf.getInt(buf.position() + (unicode ? 28 : 16)));

        while (true) {
            final char ch = unicode ? buf.getChar() : (char) buf.get();
            if (ch == 0) {
                // Hit string terminator, stop
                break;
            }
            path.append(ch);
            if (path.length() > 260) {
                // Maximum path length on Windows is 260 characters. Let's pretend no one uses NTFS' `\\?\` prefix
                break;
            }
        }

        return new File(path.toString());
    }
}
