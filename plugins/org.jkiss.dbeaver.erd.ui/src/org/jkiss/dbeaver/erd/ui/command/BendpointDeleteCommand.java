/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.command;

import org.jkiss.dbeaver.erd.ui.part.AssociationPart;

public class BendpointDeleteCommand extends BendpointCommand {

    private int bendpointIndex;

    public BendpointDeleteCommand(AssociationPart association, int bendpointIndex) {
        super(association);
        this.bendpointIndex = bendpointIndex;
    }

    @Override
    public void execute() {
        association.removeBendpoint(bendpointIndex);
    }

    @Override
    public void undo() {
/*
        super.undo();
        getWire().insertBendpoint(getIndex(), bendpoint);
*/
    }

}


