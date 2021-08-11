/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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
package org.jkiss.dbeaver.ext.ui.locks.graph;

import org.eclipse.draw2dl.*;
import org.eclipse.draw2dl.geometry.Rectangle;


public class LockGraphNodeFigure extends Figure {

	private Label label;

	private RectangleFigure rectangleFigure;

	public LockGraphNodeFigure(String  title,boolean selected) {
		setLayoutManager(new XYLayout());
		this.rectangleFigure = new RectangleFigure();
		this.rectangleFigure.setBackgroundColor(selected ? ColorConstants.orange : ColorConstants.lightGray);
		add(this.rectangleFigure);
		this.label = new Label();
		this.label.setText(title); //$NON-NLS-1$		
		add(this.label);
	}

	public Label getLabel() {
		return this.label;
	}

	public RectangleFigure getRectangleFigure() {
		return this.rectangleFigure;
	}

	@Override
	public void paintFigure(Graphics g) {
		Rectangle r = getBounds().getCopy();
		setConstraint(getRectangleFigure(), new Rectangle(0, 0, r.width,r.height));
		setConstraint(getLabel(), new Rectangle(0, 0, r.width, r.height));
		getRectangleFigure().invalidate();
		getLabel().invalidate();
	}
}