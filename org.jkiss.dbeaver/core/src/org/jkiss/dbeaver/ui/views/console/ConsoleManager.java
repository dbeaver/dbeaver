/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.console;

import org.eclipse.ui.*;

/**
 * ConsoleManager
 */
public class ConsoleManager
{
    public static void writeMessage(String text, ConsoleMessageType messageType)
    {
        IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for (IWorkbenchWindow window : workbenchWindows) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference ref : page.getViewReferences()) {
                    IViewPart view = ref.getView(true);
                    if (view instanceof ConsoleView) {
                        ((ConsoleView) view).writeMessage(text, messageType);
                    }
                }
            }
        }
    }


}
