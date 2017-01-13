package org.jkiss.dbeaver.ui.views.lock;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;

public class LockGraphEdgeEditPart extends AbstractConnectionEditPart {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ROLE,
				new LockGraphConnectionEditPolicy());
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
	}

	@Override
	protected IFigure createFigure() {
		
		PolylineConnection connection = (PolylineConnection) super.createFigure();
		
        connection.setLineWidth(1);
        
        PolygonDecoration decoration = new PolygonDecoration();
        
        decoration.setTemplate(PolygonDecoration.TRIANGLE_TIP);
        connection.setSourceDecoration(decoration);

        
        return connection;
		

	}

}
