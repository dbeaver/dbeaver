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

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_PREDICATE table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanOperatorPredicate {

   private DB2PlanOperator db2Operator;

   private Integer         operatorId;
   private Integer         predicateId;
   private String          howApplied;
   private String          whenApplied;
   private String          predicateText;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanOperatorPredicate(JDBCResultSet dbResult, DB2PlanOperator db2Operator) {
      this.db2Operator = db2Operator;

      this.operatorId = JDBCUtils.safeGetInteger(dbResult, "OPERATOR_ID");
      this.predicateId = JDBCUtils.safeGetInteger(dbResult, "PREDICATE_ID");
      this.howApplied = JDBCUtils.safeGetString(dbResult, "HOW_APPLIED");
      this.whenApplied = JDBCUtils.safeGetString(dbResult, "WHEN_APPLIED");

      // TODO DF: bad Clob..
      this.predicateText = JDBCUtils.safeGetString(dbResult, "PREDICATE_TEXT");
   }

   @Override
   public String toString() {
      return predicateId.toString();

   }

   // ----------------
   // Standard Getters
   // ----------------

   public Integer getOperatorId() {
      return operatorId;
   }

   public Integer getPredicateId() {
      return predicateId;
   }

   public String getHowApplied() {
      return howApplied;
   }

   public String getWhenApplied() {
      return whenApplied;
   }

   public String getPredicateText() {
      return predicateText;
   }

   public DB2PlanOperator getDb2Operator() {
      return db2Operator;
   }

}
