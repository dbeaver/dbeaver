/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.command;

import org.jkiss.dbeaver.ext.erd.part.AssociationPart;

public class BendpointDeleteCommand extends BendpointCommand {

    private int bendpointIndex;

    public BendpointDeleteCommand(AssociationPart association, int bendpointIndex) {
        super(association);
        this.bendpointIndex = bendpointIndex;
    }

    public void execute() {
        association.removeBendpoint(bendpointIndex);
    }

    public void undo() {
/*
        super.undo();
        getWire().insertBendpoint(getIndex(), bendpoint);
*/
    }

}


