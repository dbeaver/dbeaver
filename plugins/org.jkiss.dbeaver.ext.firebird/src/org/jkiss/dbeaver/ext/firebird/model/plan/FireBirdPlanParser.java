/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;

/**
 * Firebird plan parser. It interpretes tokens returned by FireBirdPlanTokenMatcher
 * tokenizer and creates plan node tree. When index is found it's selectivity
 * is added to index plan node.
 *
 * @author tomashorak@post.cz
 */
class FireBirdPlanParser {

		private String plan;
		private JDBCSession session;
		private FireBirdPlanTokenMatcher tokenMatch;
		
		FireBirdPlanParser(String plan, JDBCSession session) {
			this.plan = plan;
			this.session = session;
			this.tokenMatch = new FireBirdPlanTokenMatcher(plan);
		}
		
/*
	PLAN <plan-expr>
	
	<plan-expr> ::=  (<plan-item> [, <plan-item> ...])
	                    | <sorted-item>
	                    | <joined-item>
	                    | <merged-item>
	                    
	<sorted-item> ::=  SORT (<plan-item>)
	
	<joined-item> ::=  JOIN (<plan-item>, <plan-item> [, <plan-item> ...])
	
	<merged-item> ::=  [SORT] MERGE (<sorted-item>, <sorted-item> [, <sorted-item> ...])
	
	<plan-item> ::= <basic-item> | <plan-expr>
	
	<basic-item> ::= <relation>
	                    {NATURAL
	                     | INDEX (<indexlist>)
	                     | ORDER index [INDEX (<indexlist>)]}
	                     
	<relation> ::=  table | view [table]
						
	<indexlist> ::= index [, index ...]
	    	
*/

		FireBirdPlanNode parse() throws FireBirdPlanException {
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.PLAN);
			FireBirdPlanNode node = addPlanNode(null, plan);
			tokenMatch.jump();
			planExpr(node);
			return node;
		}	
		
		private void planExpr(FireBirdPlanNode parent) throws FireBirdPlanException {
			switch (tokenMatch.token) {
				case LEFTPARENTHESE:
					do {
						tokenMatch.jump();
						planItem(parent);
					} while (tokenMatch.token == FireBirdPlanToken.COMMA);
					tokenMatch.checkToken(FireBirdPlanToken.RIGHTPARENTHESE);
					break;
				case SORT:
					sortedItem(parent);
					break;
				case JOIN:
					joinedItem(parent);
					break;
				case HASH:
					hashedItem(parent);
					break;
				case SORT_MERGE:
					mergedItem(parent, true);
					break;
				case MERGE:
					mergedItem(parent, false);
					break;					
				default:
					tokenMatch.raisePlanTokenException();
			}
		}
		
		private void planItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			switch (tokenMatch.token) {
				case IDENTIFICATOR:
					basicItem(parent);
					break;
				default:
					planExpr(parent);
					break;
			}
		}
		
		private void joinedItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			tokenMatch.checkToken(FireBirdPlanToken.JOIN);
			FireBirdPlanNode node = addPlanNode(parent, "JOIN");
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.LEFTPARENTHESE);
			do {
				tokenMatch.jump();
				planItem(node);
			} while (tokenMatch.getToken() == FireBirdPlanToken.COMMA);
			tokenMatch.checkToken(FireBirdPlanToken.RIGHTPARENTHESE);
		}
			
		private void hashedItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			tokenMatch.checkToken(FireBirdPlanToken.HASH);
			FireBirdPlanNode node = addPlanNode(parent, "HASH");
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.LEFTPARENTHESE);
			do {
				tokenMatch.jump();
				planItem(node);
			} while (tokenMatch.getToken() == FireBirdPlanToken.COMMA);
			tokenMatch.checkToken(FireBirdPlanToken.RIGHTPARENTHESE);
		}
			
		private void mergedItem(FireBirdPlanNode parent, Boolean sorted) throws FireBirdPlanException {
			if (sorted) {
				tokenMatch.checkToken(FireBirdPlanToken.SORT_MERGE);
			} else {
				tokenMatch.checkToken(FireBirdPlanToken.MERGE);
			}
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.LEFTPARENTHESE);
			FireBirdPlanNode node = null;
			if (sorted) {
				node = addPlanNode(parent, "SORT MERGE");
			} else {
				node = addPlanNode(parent, "MERGE");
			}
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.LEFTPARENTHESE);
			do {
				tokenMatch.jump();
				sortedItem(node);
				tokenMatch.jump();
			} while (tokenMatch.getToken() == FireBirdPlanToken.COMMA);
			tokenMatch.checkToken(FireBirdPlanToken.RIGHTPARENTHESE);
		}
		
		private void sortedItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			tokenMatch.checkToken(FireBirdPlanToken.SORT);
			FireBirdPlanNode node = addPlanNode(parent, "SORT");
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.LEFTPARENTHESE);
			tokenMatch.jump();
			planItem(node);
			tokenMatch.checkToken(FireBirdPlanToken.RIGHTPARENTHESE);			
		}
		
		private void basicItem(FireBirdPlanNode parent) throws FireBirdPlanException {
			String aliases = collectIdentifiers();
			switch (tokenMatch.token) {
				case NATURAL:
					addPlanNode(parent, aliases + " NATURAL");
					tokenMatch.jump();
					break;
				case INDEX:
					String indexes = collectIndexes();
					addPlanNode(parent, aliases + " INDEX (" + indexes + ")");
					break;
				case ORDER:
					tokenMatch.jump();
					tokenMatch.checkToken(FireBirdPlanToken.IDENTIFICATOR);
					String orderIndex = tokenMatch.getValue();
					tokenMatch.jump();
					String text = aliases + " ORDER " + orderIndex + indexInfo(orderIndex);
					if (tokenMatch.getToken() == FireBirdPlanToken.INDEX) {
						String orderIndexes = collectIndexes();
						text = text + " INDEX(" + orderIndexes + ")";
					}
					addPlanNode(parent, text);
					break;
			default:
				tokenMatch.raisePlanTokenException();
			}
			
		}

		private String collectIdentifiers() {
			String identifiers = "";
			while (tokenMatch.getToken() == FireBirdPlanToken.IDENTIFICATOR) {
				identifiers = identifiers + tokenMatch.getValue() + " ";
				tokenMatch.jump();
			};
			return identifiers;
		}
		
		private String collectIndexes() throws FireBirdPlanException {
			tokenMatch.jump();
			tokenMatch.checkToken(FireBirdPlanToken.LEFTPARENTHESE);
			String indexes = "";
			tokenMatch.jump();
			while (tokenMatch.getToken() != FireBirdPlanToken.RIGHTPARENTHESE) {
				indexes = indexes + tokenMatch.getValue() + indexInfo(tokenMatch.getValue());
				tokenMatch.jump();
				if(tokenMatch.getToken() == FireBirdPlanToken.COMMA) {
					indexes = indexes + ",";
					tokenMatch.jump();
				}
			};
			return indexes;
		}
		
		private FireBirdPlanNode addPlanNode(FireBirdPlanNode parent, String text) {
			FireBirdPlanNode node;
			node = new FireBirdPlanNode(text);
			node.parent = parent;
			if (parent != null) {
				parent.getNested().add(node);
			}
			return node;
		}
		
		private String indexInfo(String index) throws FireBirdPlanException {
			StringBuilder sb = new StringBuilder();
			sb.append("( ");
			try {
				JDBCPreparedStatement dbStat;
				dbStat = session.prepareStatement(
						"SELECT RDB$FIELD_NAME, RDB$STATISTICS FROM RDB$INDEX_SEGMENTS "
						+ "WHERE RDB$INDEX_NAME = ? ORDER BY RDB$FIELD_POSITION");
	            try {
	                dbStat.setString(1, index);
	                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
	                    while (dbResult.next()) {
	                    	sb.append(String.format("%1$s[%2$f]", dbResult.getString(1).trim(), dbResult.getDouble(2)));
	                    	sb.append(", ");
	                    }
	                    sb.delete(sb.length() - 2, sb.length());
	                }
	            } finally {
	                dbStat.close();
	            }
			} catch (SQLException e) {
				throw new FireBirdPlanException(index, e.getMessage());
			}
			sb.append(" )");
			return sb.toString();
		}
		
	}