/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

public class SQLInformationProvider implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {

    class EditorWatcher implements IPartListener {

        /**
         * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
         */
        public void partOpened(IWorkbenchPart part)
        {
        }

        /**
         * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
         */
        public void partDeactivated(IWorkbenchPart part)
        {
        }

        /**
         * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
         */
        public void partClosed(IWorkbenchPart part)
        {
            if (part == editor) {
                editor.getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
                partListener = null;
            }
        }

        /**
         * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
         */
        public void partActivated(IWorkbenchPart part)
        {
            update();
        }

        public void partBroughtToTop(IWorkbenchPart part)
        {
            update();
        }
    }

    protected IEditorPart editor;
    protected IPartListener partListener;

    protected String currentPerspective;
    protected BestMatchHover implementation = new BestMatchHover(null);
    protected IInformationControlCreator informationControlCreator;

    public SQLInformationProvider(IEditorPart editor)
    {

        this.editor = editor;

        if (this.editor != null) {

            partListener = new EditorWatcher();
            IWorkbenchWindow window = this.editor.getSite().getWorkbenchWindow();
            window.getPartService().addPartListener(partListener);

            update();
        }
    }

    protected void update()
    {

        IWorkbenchWindow window = editor.getSite().getWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        if (page != null) {

            IPerspectiveDescriptor perspective = page.getPerspective();
            if (perspective != null) {
                String perspectiveId = perspective.getId();

                if (currentPerspective == null || !currentPerspective.equals(perspectiveId)) {
                    currentPerspective = perspectiveId;

                    implementation.setEditor(editor);
                }
            }
        }
    }

    public IRegion getSubject(ITextViewer textViewer, int offset)
    {

        if (implementation != null) {
            return implementation.getHoverRegion(textViewer, offset);
        }

        return null;
    }

    /*
     * @deprecated
     */
    public String getInformation(ITextViewer textViewer, IRegion subject)
    {
        Object information = getInformation2(textViewer, subject);
        return information == null ? null : information.toString();
    }

    public Object getInformation2(ITextViewer textViewer, IRegion subject)
    {
        if (implementation != null) {
            Object s = implementation.getHoverInfo2(textViewer, subject);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
     */
    public IInformationControlCreator getInformationPresenterControlCreator()
    {
        IInformationControlCreator controlCreator = null;
        if (implementation != null) {
            controlCreator = implementation.getInformationPresenterControlCreator();
        }
        if (controlCreator != null) {
            return controlCreator;
        }

        if (informationControlCreator == null) {
            informationControlCreator = new IInformationControlCreator() {
                public IInformationControl createInformationControl(Shell shell)
                {
                    //boolean cutDown = false;
                    //int style = cutDown ? SWT.NONE : (SWT.V_SCROLL | SWT.H_SCROLL);
                    return new DefaultInformationControl(shell, true);
                }
            };
        }
        return informationControlCreator;
    }

}