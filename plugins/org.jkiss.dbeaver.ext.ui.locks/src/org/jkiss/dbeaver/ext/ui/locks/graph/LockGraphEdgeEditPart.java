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

import org.eclipse.draw2dl.IFigure;
import org.eclipse.draw2dl.PolygonDecoration;
import org.eclipse.draw2dl.PolylineConnection;
import org.eclipse.gef3.EditPolicy;
import org.eclipse.gef3.editparts.AbstractConnectionEditPart;
import org.eclipse.gef3.editpolicies.ConnectionEndpointEditPolicy;

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
