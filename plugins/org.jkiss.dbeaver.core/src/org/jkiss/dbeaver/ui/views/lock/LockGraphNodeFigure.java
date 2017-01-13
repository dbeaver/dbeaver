package org.jkiss.dbeaver.ui.views.lock;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;


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