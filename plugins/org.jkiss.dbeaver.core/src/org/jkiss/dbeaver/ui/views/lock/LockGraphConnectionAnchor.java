package org.jkiss.dbeaver.ui.views.lock;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

public class LockGraphConnectionAnchor extends AbstractConnectionAnchor {

	public LockGraphConnectionAnchor(IFigure owner) {
		super(owner);
	}

	public Point getLocation(Point reference) {		
		Point point = getOwner().getBounds().getCenter();
		getOwner().translateToAbsolute(point);
		if (reference.x < point.x)
			point = getOwner().getBounds().getTop();
		else
			point = getOwner().getBounds().getBottom();
		getOwner().translateToAbsolute(point);
		return point;
	}

}
