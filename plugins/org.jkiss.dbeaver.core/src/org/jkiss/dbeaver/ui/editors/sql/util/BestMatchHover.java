/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.DBeaverConstants;

import java.util.ArrayList;
import java.util.List;

public class BestMatchHover extends AbstractSQLEditorTextHover
    implements ITextHover, ITextHoverExtension, ITextHoverExtension2, IInformationProviderExtension2
{
    static final Log log = LogFactory.getLog(BestMatchHover.class);

    private List<ITextHover> instantiatedTextHovers = new ArrayList<ITextHover>(2);
    private ITextHover bestHover;
    private IEditorPart editor;

    public BestMatchHover(IEditorPart editor)
    {
        this.editor = editor;
        installTextHovers();
    }

    /*
     * @see AbstractSQLEditorTextHover#setEditor(IEditorPart)
     */
    @Override
    public void setEditor(IEditorPart editor)
    {
        this.editor = editor;
        for (ITextHover hover : instantiatedTextHovers)
        {
            if (hover instanceof AbstractSQLEditorTextHover)
            {
                ((AbstractSQLEditorTextHover) hover).setEditor(editor);
            }
        }
    }

    /**
     * Installs all text hovers.
     */
    private void installTextHovers()
    {

        // initialize lists - indicates that the initialization happened

        // populate list
        instantiatedTextHovers.add(new SQLAnnotationHover(editor));

        IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = pluginRegistry.getExtensionPoint(
            DBeaverConstants.PLUGIN_ID, "texthover"); //$NON-NLS-1$ //$NON-NLS-2$
        if (extensionPoint != null) {
            IExtension[] extensions = extensionPoint.getExtensions();
            for (IExtension extension : extensions) {
                IConfigurationElement[] configElements = extension
                    .getConfigurationElements();
                for (IConfigurationElement configElement : configElements) {
                    if (configElement.getName().equals("hover")) {
                        //$NON-NLS-1$
                        //String className = configElement.getAttribute("class");
                        try {
                            AbstractSQLEditorTextHover h = (AbstractSQLEditorTextHover) configElement
                                .createExecutableExtension("class"); //$NON-NLS-1$
                            h.setEditor(editor);
                            instantiatedTextHovers.add(h);
                        }
                        catch (CoreException e) {
                            log.error(e);
                        }

                    }
                }
            }
        }
    }

    private void checkTextHovers()
    {
    }

    protected void addTextHover(ITextHover hover)
    {
        if (!instantiatedTextHovers.contains(hover))
        {
            instantiatedTextHovers.add(hover);
        }
    }

    /*
     * @deprecated
     * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
     */
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion)
    {
        Object info = getHoverInfo2(textViewer, hoverRegion);
        return info == null ? null : info.toString();
    }

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion)
    {
        checkTextHovers();
        bestHover = null;

        if (instantiatedTextHovers == null)
        {
            return null;
        }

        for (ITextHover hover : instantiatedTextHovers) {
            Object info = null;
            if (hover instanceof ITextHoverExtension2) {
                info =  ((ITextHoverExtension2)hover).getHoverInfo2(textViewer, hoverRegion);
            } else {
                //info = hover.getHoverInfo(textViewer, hoverRegion);
            }
            if (info != null) {
                bestHover = hover;
                return info;
            }
        }

        return null;
    }

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     * @since 3.0
     */
    @Override
    public IInformationControlCreator getHoverControlCreator()
    {
        if (bestHover instanceof ITextHoverExtension)
        {
            return ((ITextHoverExtension) bestHover).getHoverControlCreator();
        }

        return null;
    }

    /*
     * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
     * @since 3.0
     */
    @Override
    public IInformationControlCreator getInformationPresenterControlCreator()
    {
        if (bestHover instanceof IInformationProviderExtension2)
        {
            return ((IInformationProviderExtension2) bestHover).getInformationPresenterControlCreator();
        }

        return null;
    }

}