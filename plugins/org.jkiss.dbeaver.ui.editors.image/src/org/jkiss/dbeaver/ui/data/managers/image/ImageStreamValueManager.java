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
package org.jkiss.dbeaver.ui.data.managers.image;

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
        return imageDetector.isImage() ? MatchType.PRIMARY : MatchType.NONE;
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
