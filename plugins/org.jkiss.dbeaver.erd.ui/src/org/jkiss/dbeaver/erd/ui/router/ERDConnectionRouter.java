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
package org.jkiss.dbeaver.erd.ui.router;

import org.eclipse.draw2d.AutomaticRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.PointList;
import org.jkiss.dbeaver.erd.ui.connector.ERDConnection;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic interface designed to set container for particular router
 */
public abstract class ERDConnectionRouter extends AutomaticRouter {

    private final Map<Connection, PointList> connection2points = new HashMap<>();
    private IFigure container;

    /**
     * Set container
     *
     * @param figure - container
     */
    public void setContainer(IFigure figure) {
        this.container = figure;
        // here considering reset points if container reset
        getConnectionPoints().clear();
    }

    /**
     * Return a container
     */
    public IFigure getContainer() {
        return container;
    }

    /**
     * Connection to points
     *
     * @return - map of points related to connection
     */
    public Map<Connection, PointList> getConnectionPoints() {
        return connection2points;
    }

    /**
     * Get instance of connection
     *
     * @return
     */
    public PolylineConnection getConnectionInstance() {
        return new ERDConnection();
    }
    
    @Override
    protected void handleCollision(PointList list, int index) {
        // TODO Auto-generated method stub
    }
}
