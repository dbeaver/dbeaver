/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;

import java.awt.*;

public class AWTUtils {

    private static final Log log = Log.getLog(AWTUtils.class);

    public static boolean isDesktopSupported() {
        try {
            return !GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported();
        } catch (Throwable e) {
            log.warn("AWT initialization error", e);
            return false;
        }
    }

    public static java.awt.Color makeAWTColor(org.eclipse.swt.graphics.Color src) {
        org.eclipse.swt.graphics.RGB swtBgColor = src.getRGB();
        return new Color(swtBgColor.red, swtBgColor.green, swtBgColor.blue);
    }

}
