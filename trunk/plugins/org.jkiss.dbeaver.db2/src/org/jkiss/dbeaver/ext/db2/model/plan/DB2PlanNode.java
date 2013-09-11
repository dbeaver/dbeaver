/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.plan;

import java.util.Collection;
import java.util.Collections;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DB2 Plan Node
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanNode implements DBCPlanNode {

   private DB2PlanOperator planOperator;
   private DB2PlanNode     parent;

   public DB2PlanNode(DB2PlanOperator planOperator, DB2PlanNode parent) {
      this.planOperator = planOperator;
      this.parent = parent;
   }

   @Override
   public DB2PlanNode getParent() {
      return parent;
   }

   @Override
   public Collection<DB2PlanNode> getNested() {
      return Collections.emptyList();
   }

   // ----------
   // Properties
   // ----------

   @Property(editable = false, viewable = true, order = 1)
   public Integer getId() {
      return planOperator.getOperatorId();
   }

   @Property(editable = false, viewable = true, order = 2)
   public String getType() {
      return planOperator.getOperatorType();
   }

   @Property(editable = false, viewable = true, order = 3)
   public Double getTotalCost() {
      return planOperator.getTotalCost();
   }

}
