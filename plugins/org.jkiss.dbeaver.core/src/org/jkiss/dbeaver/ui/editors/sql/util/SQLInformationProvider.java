/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class SQLInformationProvider implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {

    class EditorWatcher implements IPartListener {

        /**
         * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
         */
        @Override
        public void partOpened(IWorkbenchPart part)
        {
        }

        /**
         * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
         */
        @Override
        public void partDeactivated(IWorkbenchPart part)
        {
        }

        /**
         * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
         */
        @Override
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
        @Override
        public void partActivated(IWorkbenchPart part)
        {
            update();
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part)
        {
            update();
        }
    }

    protected IEditorPart editor;
    protected IPartListener partListener;

    protected String currentPerspective;
    protected SQLAnnotationHover implementation;
    protected IInformationControlCreator informationControlCreator;

    public SQLInformationProvider(SQLEditorBase editor)
    {
        this.editor = editor;
        implementation = new SQLAnnotationHover(editor);

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

    @Override
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
    @Override
    public String getInformation(ITextViewer textViewer, IRegion subject)
    {
        Object information = getInformation2(textViewer, subject);
        return information == null ? null : information.toString();
    }

    @Override
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
    @Override
    public IInformationControlCreator getInformationPresenterControlCreator()
    {
/*
        IInformationControlCreator controlCreator = null;
        if (implementation != null) {
            controlCreator = implementation.getInformationPresenterControlCreator();
        }
        if (controlCreator != null) {
            return controlCreator;
        }

*/
        if (informationControlCreator == null) {
            informationControlCreator = new IInformationControlCreator() {
                @Override
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