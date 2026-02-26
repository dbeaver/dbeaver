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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorInput;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

/**
 * Shared editor-connection presentation helpers used by switcher dropdowns.
 */
public final class EditorConnectionPresentationUtils {
    private static final Log log = Log.getLog(EditorConnectionPresentationUtils.class);
    private static final RGB BLACK = new RGB(0, 0, 0);
    private static final RGB WHITE = new RGB(255, 255, 255);
    private static final int BLEND_RATIO = 15;

    private EditorConnectionPresentationUtils() {
    }

    @Nullable
    public static String getConnectionName(@Nullable IEditorInput input) {
        try {
            if (input instanceof IEditorConnectionColorProvider provider) {
                return provider.getConnectionName();
            }
        } catch (Exception e) {
            log.debug("Could not get connection name from editor input", e);
        }
        return null;
    }

    @Nullable
    public static Color getConnectionColor(@Nullable IEditorInput input) {
        try {
            if (input instanceof IEditorConnectionColorProvider provider) {
                return provider.getConnectionColor();
            }
        } catch (Exception e) {
            log.debug("Could not get connection color from editor input", e);
        }
        return null;
    }

    @Nullable
    public static Color getConnectionBackground(@Nullable IEditorInput input) {
        return getConnectionBackground(getConnectionColor(input));
    }

    @Nullable
    public static Color getConnectionBackground(@Nullable Color connectionColor) {
        if (connectionColor == null) {
            return null;
        }
        try {
            Color listBackground = UIStyles.getDefaultTextBackground();
            SharedTextColors sharedColors = UIUtils.getSharedTextColors();
            boolean listIsDark = UIUtils.isDark(listBackground.getRGB());
            RGB blended = UIUtils.blend(
                listIsDark ? BLACK : WHITE,
                connectionColor.getRGB(),
                BLEND_RATIO
            );
            return sharedColors.getColor(blended);
        } catch (Exception e) {
            log.debug("Could not blend connection color for list background", e);
            return null;
        }
    }
}
