/*******************************************************************************
 * Copyright (c) 2015 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;

public class E4ThemeColor {

    private static final Log log = Log.getLog(E4ThemeColor.class);
    private static boolean loggedError = false;

    public static RGB getRGBFromCssString(String cssValue) {
        try {
            if (cssValue.startsWith("rgb(")) { //$NON-NLS-1$
                String rest = cssValue.substring(4, cssValue.length());
                int idx = rest.indexOf("rgb("); //$NON-NLS-1$
                if (idx != -1) {
                    rest = rest.substring(idx + 4, rest.length());
                }
                idx = rest.indexOf(")"); //$NON-NLS-1$
                if (idx != -1) {
                    rest = rest.substring(0, idx);
                }
                String[] rgbValues = rest.split(","); //$NON-NLS-1$
                if (rgbValues.length == 3) {
                    return new RGB(Integer.parseInt(rgbValues[0].trim()), Integer.parseInt(rgbValues[1].trim()),
                        Integer.parseInt(rgbValues[2].trim()));
                }
            } else if (cssValue.startsWith("#")) { //$NON-NLS-1$
                String rest = cssValue.substring(1, cssValue.length());
                int idx = rest.indexOf("#"); //$NON-NLS-1$
                if (idx != -1) {
                    rest = rest.substring(idx + 1, rest.length());
                }
                if (rest.length() > 5) {
                    return new RGB(Integer.parseInt(rest.substring(0, 2), 16),
                        Integer.parseInt(rest.substring(2, 4), 16), Integer.parseInt(rest.substring(4, 6), 16));
                }
            }
            throw new IllegalArgumentException("RGB: " + cssValue); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            logOnce(e);
            return null;
        }
    }

    public static String getCssValueFromTheme(Display display, String value) {
        // use reflection so that this can build against Eclipse 3.x
        BundleContext context = FrameworkUtil.getBundle(E4ThemeColor.class).getBundleContext();
        ServiceReference<IThemeManager> reference = context.getServiceReference(IThemeManager.class);
        if (reference != null) {
            IThemeManager iThemeManager = context.getService(reference);
            if (iThemeManager != null) {
                IThemeEngine themeEngine = iThemeManager.getEngineForDisplay(display);
                if (themeEngine != null) {
                    CSSStyleDeclaration shellStyle = getStyleDeclaration(themeEngine, display);
                    if (shellStyle != null) {
                        CSSValue cssValue = shellStyle.getPropertyCSSValue(value);
                        if (cssValue != null) {
                            return cssValue.getCssText();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static CSSStyleDeclaration getStyleDeclaration(IThemeEngine themeEngine, Display display) {
        Shell shell = display.getActiveShell();
        CSSStyleDeclaration shellStyle = null;
        if (shell != null) {
            shellStyle = themeEngine.getStyle(shell);
        } else {
            for (Shell input : display.getShells()) {
                shellStyle = themeEngine.getStyle(input);
                if (shellStyle != null) {
                    break;
                }
            }
        }
        return shellStyle;
    }

    private static void logOnce(Exception e) {
        if (!loggedError) {
            log.error(e);
            loggedError = true;
        }
    }
}