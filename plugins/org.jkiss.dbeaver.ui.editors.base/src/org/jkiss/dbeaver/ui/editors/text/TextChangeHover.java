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
