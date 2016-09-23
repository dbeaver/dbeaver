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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.IEditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueController;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Hex editor manager
 */
public class ImageStreamValueManager implements IStreamValueManager {

    private static final Log log = Log.getLog(ImageStreamValueManager.class);

    @Override
    public MatchType matchesTo(@NotNull DBRProgressMonitor monitor, @NotNull DBSTypedObject attribute, @Nullable DBDContent value) {
        // Applies to image values
        ImageDetector imageDetector = new ImageDetector(value);
        if (!DBUtils.isNullValue(value)) {
            try {
                imageDetector.run(monitor);
            } catch (Throwable e) {
                return MatchType.NONE;
            }
        }
        return imageDetector.isImage() ? MatchType.EXCLUSIVE : MatchType.NONE;
    }

    @Override
    public IStreamValueEditor createPanelEditor(@NotNull final IValueController controller)
        throws DBException
    {
        return new ImagePanelEditor();
    }

    @Override
    public IEditorPart createEditorPart(@NotNull IValueController controller) {
        return new ImageEditorPart();
    }

    private static class ImageDetector implements DBRRunnableWithProgress {
        private final DBDContent content;
        private boolean isImage;

        private ImageDetector(DBDContent content)
        {
            this.content = content;
        }

        public boolean isImage()
        {
            return isImage;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            if (!content.isNull()) {
                try {
                    DBDContentStorage contents = content.getContents(monitor);
                    if (contents != null) {
                        try (InputStream contentStream = contents.getContentStream()) {
                            new ImageLoader().load(contentStream);
                        }
                        isImage = true;
                    }
                }
                catch (Exception e) {
                    // this is not an image
                    log.debug("Can't detect image type: " + e.getMessage());
                }
            }
        }
    }

}
