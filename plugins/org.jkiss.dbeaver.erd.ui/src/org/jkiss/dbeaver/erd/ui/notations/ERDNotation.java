/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.erd.model.ERDAssociation;

/**
 * Interface of ER Diagram style notation
 */
public interface ERDNotation {
    /**
     * Method designed to display diagram relation notation
     * 
     * @param conn        - connection class specification
     * @param association - ERD association
     * @param bckColor    - back end color
     * @param frgColor    - front end color
     */
    public void applyNotationForArrows(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor);

    /**
     * Method designed to display diagram entity notation
     * 
     * @param conn        - connection class specification
     * @param association - ERD association
     * @param bckColor    - back end color
     * @param frgColor    - front end color
     */
    public void applyNotationForEntities(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor);
}
