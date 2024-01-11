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

import org.eclipse.e4.ui.css.core.dom.IStreamingNodeList;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.jkiss.dbeaver.ui.controls.VerticalFolder;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * VerticalFolderElement
 */
public class VerticalFolderElement extends CompositeElement implements IStreamingNodeList {

	public VerticalFolderElement(VerticalFolder composite, CSSEngine engine) {
		super(composite, engine);
	}

	protected VerticalFolder getVerticalFolder() {
		return (VerticalFolder) getNativeWidget();
	}

	@Override
	public Stream<Node> stream() {
		return Arrays.stream(getVerticalFolder().getChildren()).map(this::getElement);
	}

}
