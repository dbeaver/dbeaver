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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class SharedFonts {

    private final Map<String, Font> fontMap = new HashMap<>();

    public SharedFonts() {

    }

    @NotNull
    public Font getFont(Device device, FontData fontData) {
        synchronized (fontMap) {
            String fontKey = toString(fontData);
            Font font = fontMap.get(fontKey);
            if (font == null) {
                font = new Font(device, fontData);
                fontMap.put(fontKey, font);
            }
            return font;
        }
    }

    public Font getFont(Device device, String fontData) {
        String[] fontParts = fontData.split(":");
        FontData data = new FontData(
            fontParts[0],
            CommonUtils.toInt(fontParts[1]),
            CommonUtils.toInt(fontParts[2]));
        return getFont(device, data);
    }

    public static String toString(FontData fontData) {
        return fontData.getName() + ":" + fontData.getHeight() + ":" + fontData.getStyle();
    }

    public static String toString(Font font) {
        return toString(font.getFontData()[0]);
    }

    public static boolean equalFonts(Font font1, Font font2) {
        FontData data1 = font1.getFontData()[0];
        FontData data2 = font2.getFontData()[0];
        return CommonUtils.equalObjects(data1.getName(), data2.getName()) &&
            data1.getHeight() == data2.getHeight() &&
            data1.getStyle() == data2.getStyle();
    }

}
