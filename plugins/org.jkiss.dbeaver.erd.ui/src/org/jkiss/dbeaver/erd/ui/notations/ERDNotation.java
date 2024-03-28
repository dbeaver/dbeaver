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
package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

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
    public void applyNotationForArrows(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PolylineConnection conn,
        @NotNull ERDAssociation association,
        @NotNull Color bckColor,
        @NotNull Color frgColor);

    /**
     * Method designed to display diagram entity notation
     *
     * @param conn        - connection class specification
     * @param association - ERD association
     * @param bckColor    - back end color
     * @param frgColor    - front end color
     */
    public void applyNotationForEntities(
        @NotNull PolylineConnection conn,
        @NotNull ERDAssociation association,
        @NotNull Color bckColor,
        @NotNull Color frgColor);

    /**
     * Indentation value is a length of orthogonal line for source and target
     * decorator. Each notation keeps own value to handle require behavior.
     *
     * @return - value of indentation
     */
    public double getIndentation();
}
