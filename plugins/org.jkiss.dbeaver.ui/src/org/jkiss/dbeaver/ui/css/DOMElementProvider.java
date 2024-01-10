/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.jkiss.dbeaver.ui.controls.VerticalFolder;
import org.w3c.dom.Element;

/**
 * CSS DOM element provider
 */
public class DOMElementProvider implements IElementProvider {

	public static final IElementProvider INSTANCE = new DOMElementProvider();

	@Override
	public Element getElement(Object element, CSSEngine engine) {
		if (element instanceof VerticalFolder) {
			return new VerticalFolderElement((VerticalFolder) element, engine);
		}
		return null;
	}
}
