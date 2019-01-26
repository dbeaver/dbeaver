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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * TextEditorUtils
 */
public class TextEditorUtils {

    private static final Log log = Log.getLog(TextEditorUtils.class);

    private static Map<String, Integer> ACTION_TRANSLATE_MAP;

    public static Map<String, Integer> getTextEditorActionMap()
    {
        if (ACTION_TRANSLATE_MAP == null) {
            ACTION_TRANSLATE_MAP = new HashMap<>();
            FakeTextEditor.fillActionMap(ACTION_TRANSLATE_MAP );
        }
        return ACTION_TRANSLATE_MAP;
    }

    /**
     * Eclipse hack. Disables/enabled all key bindings in specified site's part. Works only if host editor is extender of
     * AbstractTextEditor Uses reflection because setActionActivation is private method
     * TODO: find better way to disable key bindings or prioritize event handling to widgets
     *
     * @param partSite workbench part site
     * @param enable enable or disable
     */
    @Deprecated
    public static void enableHostEditorKeyBindings(IWorkbenchPartSite partSite, boolean enable)
    {
        IWorkbenchPart part = partSite.getPart();
        if (part instanceof AbstractTextEditor) {
            AbstractTextEditor hostEditor = (AbstractTextEditor) part;
            Control widget = hostEditor.getAdapter(Control.class);
            if (widget == null || widget.isDisposed()) {
                return;
            }
            try {
                Method activatorMethod = AbstractTextEditor.class.getDeclaredMethod("setActionActivation", Boolean.TYPE);
                activatorMethod.setAccessible(true);
                activatorMethod.invoke(hostEditor, enable);
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = ((InvocationTargetException) e).getTargetException();
                }
                log.warn("Can't disable text editor action activations", e);
            }
            //hostEditor.getEditorSite().getActionBarContributor().setActiveEditor(hostEditor);
        }
    }

    public static void enableHostEditorKeyBindingsSupport(final IWorkbenchPartSite partSite, Control control)
    {
        if (!(partSite.getPart() instanceof AbstractTextEditor)) {
            return;
        }

        final boolean[] activated = new boolean[] {false};
        control.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!activated[0]) {
                    enableHostEditorKeyBindings(partSite, false);
                    activated[0] = true;
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (activated[0]) {
                    enableHostEditorKeyBindings(partSite, true);
                    activated[0] = false;
                }
            }
        });
        control.addDisposeListener(e -> {
            if (activated[0]) {
                if (!DBWorkbench.getPlatform().isShuttingDown()) {
                    enableHostEditorKeyBindings(partSite, true);
                }
                activated[0] = false;
            }
        });
    }

    public static IAction createFindReplaceAction(Shell shell, IFindReplaceTarget target) {
        return new FindReplaceAction(
            ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages"),
            "Editor.FindReplace.",
            shell,
            target);
    }

    public static boolean isDarkThemeEnabled() {
        boolean isDark = false;
        IThemeEngine engine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
        if (engine != null) {
            ITheme activeTheme = engine.getActiveTheme();
            if (activeTheme != null) {
                isDark = activeTheme.getId().contains("dark");
            }
        }
        return isDark;
    }

    private static class FakeTextEditor extends AbstractTextEditor {
        static void fillActionMap(Map<String, Integer> map) {
            for (AbstractTextEditor.IdMapEntry entry : AbstractTextEditor.ACTION_MAP) {
                map.put(entry.getActionId(), entry.getAction());
            }
        }
    }



}
