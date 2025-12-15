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
package org.eclipse.swt;

import org.eclipse.swt.internal.Converter;
import org.eclipse.swt.internal.gtk.GDK;
import org.eclipse.swt.internal.gtk.GTK;
import org.eclipse.swt.internal.gtk3.GTK3;
import org.eclipse.swt.internal.gtk4.GTK4;

public final class GtkCssHelper {
    private static final String SWT_THEMING_FIXES_GTK = """
        check, radio {
        	margin: 0 2px;
        }
        
        combobox button.combo box arrow {
        	margin: 0 2px;
        }""";

    /**
     * A copy of <a href="https://github.com/eclipse-platform/eclipse.platform.swt/blob/60de8f91c7c851fbfe562a1b21c18713a0528852/bundles/org.eclipse.swt/Eclipse%20SWT/gtk/org/eclipse/swt/graphics/Device.java#L752">org.eclipse.swt.graphics.Device#overrideThemeValues()</a>.
     * Prevents radio buttons and checkboxes from being truncated on KDE when using the Breeze theme.
     * <p>
     * See <a href="https://github.com/dbeaver/dbeaver/issues/39248">https://github.com/dbeaver/dbeaver/issues/39248</a>
     * <p>
     * See <a href="https://github.com/eclipse-platform/eclipse.platform.swt/issues/1758">https://github.com/eclipse-platform/eclipse.platform.swt/issues/1758</a>
     * <p>
     * This fix can be removed after we migrate to Eclipse 2025-12. Hopefully, the fix included in the SWT itself will land
     * with Eclipse.
     */
    public static void overrideThemeValues() {
        long provider = GTK.gtk_css_provider_new();

        if (GTK.GTK4) {
            long display = GDK.gdk_display_get_default();
            if (display == 0 || provider == 0) {
                System.out.println("SWT Warning: Override of theme values failed. Reason: could not acquire display or provider.");
                return;
            }
            GTK4.gtk_style_context_add_provider_for_display(display, provider, GTK.GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
        } else {
            long screen = GDK.gdk_screen_get_default();
            if (screen == 0 || provider == 0) {
                System.out.println("SWT Warning: Override of theme values failed. Reason: could not acquire screen or provider.");
                return;
            }
            GTK3.gtk_style_context_add_provider_for_screen(screen, provider, GTK.GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
        }

        if (GTK.GTK4) {
            GTK4.gtk_css_provider_load_from_data(provider, Converter.wcsToMbcs(SWT_THEMING_FIXES_GTK, true), -1);
        } else {
            GTK3.gtk_css_provider_load_from_data(provider, Converter.wcsToMbcs(SWT_THEMING_FIXES_GTK, true), -1, null);
        }
    }
}
