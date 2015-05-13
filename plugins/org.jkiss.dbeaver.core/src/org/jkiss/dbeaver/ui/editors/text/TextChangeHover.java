/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.swt.widgets.Shell;


/**
 * Change hover for text editors. Respects tab settings and text editor font.
 *
 * @since 3.0
 */
public class TextChangeHover extends LineChangeHover {

	/** The last created information control. */
	private int fLastScrollIndex= 0;

	/*
	 * @see org.eclipse.jface.text.source.LineChangeHover#getTabReplacement()
	 */
	@Override
    protected String getTabReplacement() {
		return Character.toString('\t');
	}

	/*
	 * @see org.eclipse.jface.text.source.LineChangeHover#getHoverInfo(org.eclipse.jface.text.source.ISourceViewer, org.eclipse.jface.text.source.ILineRange, int)
	 */
	@Override
    public Object getHoverInfo(ISourceViewer sourceViewer, ILineRange lineRange, int visibleLines) {
		fLastScrollIndex= sourceViewer.getTextWidget().getHorizontalPixel();
		return super.getHoverInfo(sourceViewer, lineRange, visibleLines);
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverControlCreator()
	 */
	@Override
    public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			@Override
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, true);
			}
		};
	}

	/*
	 * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * @since 3.3
	 */
	@Override
    public IInformationControlCreator getInformationPresenterControlCreator() {
		return new IInformationControlCreator() {
			@Override
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, true);
			}
		};
	}

}
