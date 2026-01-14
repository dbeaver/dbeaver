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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Theme font annotation
 */
public class ThemeListener {

    private static final Log log = Log.getLog(ThemeListener.class);
    private final Map<String, Field[]> fieldMap = new HashMap<>();
    private final IThemeManager themeManager;
    private final Map<String, List<Consumer<String>>> propertyListeners = new HashMap<>();

    public ThemeListener() {
        themeManager = PlatformUI.getWorkbench().getThemeManager();

        IPropertyChangeListener themeChangeListener = this::updateThemeProperty;
        themeManager.addPropertyChangeListener(themeChangeListener);


        for (Field field : getClass().getFields()) {
            String propId = null;
            ThemeColor colorAnno = field.getAnnotation(ThemeColor.class);
            if (colorAnno != null) {
                if (!Color.class.isAssignableFrom(field.getType())) {
                    log.error("Bad color annotation " + field);
                } else {
                    propId = colorAnno.value();

                }
            } else {
                ThemeFont fontAnno = field.getAnnotation(ThemeFont.class);
                if (fontAnno != null) {
                    if (!Font.class.isAssignableFrom(field.getType())) {
                        log.error("Bad color annotation " + field);
                    } else {
                        propId = fontAnno.value();
                    }
                }
            }
            if (propId != null) {
                Field[] fields = fieldMap.get(propId);
                if (fields == null) {
                    fields = new Field[]{field};
                } else {
                    fields = ArrayUtils.add(Field.class, fields, field);
                }
                fieldMap.put(propId, fields);
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

        List<Consumer<String>> listeners = propertyListeners.get(property);
        if (listeners != null) {
            for (Consumer<String> listener : listeners) {
                listener.accept(property);
            }
        }
    }

    private void setPropertyValue(Field[] fields, ITheme currentTheme, String property) throws IllegalAccessException {
        for (Field field : fields) {
            if (Color.class.isAssignableFrom(field.getType())) {
                Color value = currentTheme.getColorRegistry().get(property);
                if (value == null) {
                    log.error("Color '" + property + "' not found in registry");
                }
                field.set(this, value);
            } else if (Font.class.isAssignableFrom(field.getType())) {
                ThemeFont param = field.getAnnotation(ThemeFont.class);
                if (param != null && param.italic()) {
                    Font font = currentTheme.getFontRegistry().getItalic(property);
                    if (font == null) {
                        log.error("Font '" + property + "' (italic) not found in registry");
                    }
                    field.set(this, font);
                } else if (param != null && param.bold()) {
                    Font font;
                    if (RuntimeUtils.isMacOS()) {
                        // Create bold here because default getBold is broken on MacOS
                        // getBold implementation differs from our makeBoldFont
                        // because it doesn't copy all font metrics
                        Object oldValue = field.get(this);
                        if (oldValue instanceof Font oldFont) {
                            oldFont.dispose();
                        }
                        Font normalFont = currentTheme.getFontRegistry().get(property);
                        font = UIUtils.makeBoldFont(normalFont);
                        if (normalFont.getDevice() instanceof Display display) {
                            display.disposeExec(font::dispose);
                        }
                    } else {
                        font = currentTheme.getFontRegistry().getBold(property);
                    }
                    field.set(this, font);
                } else {
                    Font font = currentTheme.getFontRegistry().get(property);
                    if (font == null) {
                        log.error("Font '" + property + "' not found in registry");
                    }
                    field.set(this, font);
                }
            }
        }
    }

    public synchronized void addPropertyListener(
        @NotNull String property,
        @NotNull Consumer<String> listener,
        @Nullable Control control
    ) {
        propertyListeners.computeIfAbsent(property, p -> new ArrayList<>()).add(listener);
        if (control != null) {
            control.addDisposeListener(e -> removePropertyListener(property, listener));
        }
    }

    public synchronized void removePropertyListener(String property, Consumer<String> listener) {
        List<Consumer<String>> consumers = propertyListeners.get(property);
        if (consumers == null) {
            log.debug("No property '" + property + "' consumers");
        } else {
            if (!consumers.remove(listener)) {
                log.debug("Property '" + property + "' consumer '" + listener + "' not found");
            } else {
                if (consumers.isEmpty()) {
                    propertyListeners.remove(property);
                }
            }
        }
    }
}
