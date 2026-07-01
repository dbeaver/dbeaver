/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.IWorkbenchThemeConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatThemeSettings;
import org.jkiss.dbeaver.ui.ai.internal.AIUIActivator;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Map;

public class WebCSSInitializer {

    private static final Log log = Log.getLog(WebCSSInitializer.class);

    private static final String WEB_ROOT = "web";
    private static final String WEB_CSS_PATH = WEB_ROOT + "/styles.css";
    private static final String WEB_HTML_PATH = WEB_ROOT + "/index.html";

    private final Path directory;

    public WebCSSInitializer() throws IOException {
        var activator = AIUIActivator.getInstance();
        directory = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "dbeaver-ai-chat");
        Enumeration<URL> resources = activator.getBundle().findEntries(WEB_ROOT, "*", true);
        if (resources == null) {
            log.error("Resource folder not found: " + WEB_ROOT);
            return;
        }
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String resourcePath = getWebResourcePath(resource);
            if (resourcePath == null || resourcePath.endsWith("/")) {
                continue;
            }
            try (InputStream is = resource.openStream()) {
                copyWebResource(resourcePath, is);
            }
        }
    }

    @Nullable
    private static String getWebResourcePath(@NotNull URL resource) {
        String path = resource.getPath();
        int webPathIndex = path.indexOf(WEB_ROOT + '/');
        if (webPathIndex < 0) {
            log.error("Unexpected web resource path: " + path);
            return null;
        }
        return path.substring(webPathIndex);
    }

    private void copyWebResource(@NotNull String resource, @NotNull InputStream is) throws IOException {
        var path = directory.resolve(resource);
        Files.createDirectories(path.getParent());
        if (resource.equals(WEB_CSS_PATH)) {
            String cssContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            cssContent = updateCss(cssContent);
            Files.writeString(path, cssContent);
        } else {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @NotNull
    public String getWebHtmlPath() {
        return directory.resolve(WEB_HTML_PATH).toUri().toString();
    }

    @NotNull
    private String updateCss(@NotNull String cssContent) {
        Map<String, String> values = fillValues();
        for (var entry : values.entrySet()) {
            cssContent = cssContent.replace(entry.getKey(), entry.getValue());
        }
        return cssContent;
    }

    @NotNull
    private Map<String, String> fillValues() {
        final ColorRegistry registry = UIUtils.getColorRegistry();
        String promptBackground = colorToHex(AIChatThemeSettings.instance.promptBackgroundColor);
        String promptBorder = colorToHex(AIChatThemeSettings.instance.promptBorderColor);
        String waningBackground = colorToHex(BaseThemeSettings.instance.colorWarning);
        String errorBackground = colorToHex(BaseThemeSettings.instance.colorError);
        String chatBackground = colorToHex(UIStyles.getDefaultTextBackground());
        String chatInactiveBackground = colorToHex(UIStyles.getDefaultWidgetBackground());
        String textColor = colorToHex(registry.get(IWorkbenchThemeConstants.ACTIVE_TAB_TEXT_COLOR));
        String themeSchema = UIStyles.isDarkTheme() ? "dark" : "light";

        String hyperLinkColor = colorToHex(registry.get(JFacePreferences.HYPERLINK_COLOR));
        String borderColor = colorToHex(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));

        FontData[] mainFont = BaseThemeSettings.instance.baseFont.getFontData();
        String fontSize = getFontSize(mainFont);
        String fontFamily = getFontFamily(mainFont, "Arial");

        FontData[] monoFont = BaseThemeSettings.instance.monospaceFont.getFontData();
        String monoFontSize = getFontSize(monoFont);
        String monoFontFamily = getFontFamily(monoFont, "monospace");

        return Map.ofEntries(
            Map.entry("{{BACKGROUND_COLOR}}", chatBackground),
            Map.entry("{{BACKGROUND_COLOR_INACTIVE}}", chatInactiveBackground),
            Map.entry("{{TEXT_COLOR}}", textColor),
            Map.entry("{{PROMPT_BACKGROUND}}", promptBackground),
            Map.entry("{{PROMPT_BORDER}}", promptBorder),
            Map.entry("{{WARNING_BACKGROUND}}", waningBackground),
            Map.entry("{{ERROR_BACKGROUND}}", errorBackground),
            Map.entry("{{FONT_SIZE}}", fontSize),
            Map.entry("{{FONT_FAMILY}}", fontFamily),
            Map.entry("{{MONOSPACE_FONT_SIZE}}", monoFontSize),
            Map.entry("{{MONOSPACE_FONT_FAMILY}}", monoFontFamily),
            Map.entry("{{HYPERLINK_COLOR}}", hyperLinkColor),
            Map.entry("{{BORDER_COLOR}}", borderColor),
            Map.entry("{{COLOR_SCHEME}}", themeSchema)
        );
    }

    @NotNull
    private static String getFontSize(@NotNull FontData[] mainFont) {
        String unit = RuntimeUtils.isMacOS() ? "px" : "pt";
        int height = mainFont.length > 0 ? mainFont[0].getHeight() : 13;
        return height + unit;
    }

    @NotNull
    private static String getFontFamily(@NotNull FontData[] mainFont, @NotNull String defaultFamily) {
        String fontFamily = defaultFamily;
        if (mainFont.length > 0) {
            fontFamily = mainFont[0].getName();
            if (fontFamily.equals(".AppleSystemUIFont")) {
                fontFamily = "system-ui";
            }
        }
        return fontFamily;
    }

    @NotNull
    private String colorToHex(@Nullable Color color) {
        if (color == null) {
            return "#000000";
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
