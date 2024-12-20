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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Theme font annotation
 */
public class ThemeListener {

    private static final Log log = Log.getLog(ThemeListener.class);
    private final Map<String, Field[]> fieldMap = new HashMap<>();
    private final IThemeManager themeManager;

    public ThemeListener() {
        themeManager = PlatformUI.getWorkbench().getThemeManager();

        IPropertyChangeListener themeChangeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updateThemeProperty(event);
            }
        };
        themeManager.addPropertyChangeListener(themeChangeListener);

        for (Field field : getClass().getFields()) {
            ThemeParameter annotation = field.getAnnotation(ThemeParameter.class);
            if (annotation != null) {
                Field[] fields = fieldMap.get(annotation.value());
                if (fields == null) {
                    fields = new Field[]{field};
                } else {
                    fields = ArrayUtils.add(Field.class, fields, field);
                }
                fieldMap.put(annotation.value(), fields);
            }
        }
        
        // Fill initial values
        ITheme currentTheme = themeManager.getCurrentTheme();
        try {
            for (Map.Entry<String, Field[]> prop : fieldMap.entrySet()) {
                Field[] fields = prop.getValue();
                setPropertyValue(fields, currentTheme, prop.getKey());
            }
        } catch (IllegalAccessException e) {
            log.debug("Error filling initial theme properties", e);
        }
    }

    private void updateThemeProperty(PropertyChangeEvent event) {
        String property = event.getProperty();
        Field[] fields = fieldMap.get(property);
        if (fields != null) {
            ITheme currentTheme = themeManager.getCurrentTheme();
            try {
                setPropertyValue(fields, currentTheme, property);
            } catch (IllegalAccessException e) {
                log.debug(e);
            }
        }
    }

    private void setPropertyValue(Field[] fields, ITheme currentTheme, String property) throws IllegalAccessException {
        for (Field field : fields) {
            if (Color.class.isAssignableFrom(field.getType())) {
                field.set(this, currentTheme.getColorRegistry().get(property));
            } else if (Font.class.isAssignableFrom(field.getType())) {
                ThemeParameter param = field.getAnnotation(ThemeParameter.class);
                if (param != null && param.italic()) {
                    field.set(this, currentTheme.getFontRegistry().getItalic(property));
                } else if (param != null && param.bold()) {
                    field.set(this, currentTheme.getFontRegistry().getBold(property));
                } else {
                    field.set(this, currentTheme.getFontRegistry().get(property));
                }
            }
        }
    }
}
