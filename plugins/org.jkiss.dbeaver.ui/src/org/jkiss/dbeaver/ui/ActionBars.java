package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.part.EditorActionBarContributor;

public class ActionBars {

    public static IStatusLineManager extractStatusLineManager(IWorkbenchSite site) {
        IActionBars actionBars = extractActionBars(site);
        if (actionBars == null) {
            return null;
        }
        return actionBars.getStatusLineManager();
    }

    public static IActionBars extractActionBars(IWorkbenchSite site) {
        if (site == null) {
            return null;
        }
        IWorkbenchPage page= site.getPage();
        IWorkbenchPart activePart= page.getActivePart();

        if (activePart instanceof IViewPart) {
            IViewPart activeViewPart= (IViewPart)activePart;
            IViewSite activeViewSite= activeViewPart.getViewSite();
            return activeViewSite.getActionBars();
        }

        if (activePart instanceof IEditorPart) {
            IEditorPart activeEditorPart= (IEditorPart)activePart;
            IEditorActionBarContributor contributor= activeEditorPart.getEditorSite().getActionBarContributor();
            if (contributor instanceof EditorActionBarContributor) {
                return ((EditorActionBarContributor) contributor).getActionBars();
            }
        }
        if (site instanceof IViewSite) {
            IViewSite viewSite = (IViewSite) site;
            return viewSite.getActionBars();
        }
        if (site instanceof IEditorSite) {
            IEditorSite editorSite = (IEditorSite) site;
            return editorSite.getActionBars();
        }
        if (site instanceof IIntroSite) {
            IIntroSite introSite = (IIntroSite) site;
            return introSite.getActionBars();
        }
        //OMG, what is it?
        return null;
    }
}
