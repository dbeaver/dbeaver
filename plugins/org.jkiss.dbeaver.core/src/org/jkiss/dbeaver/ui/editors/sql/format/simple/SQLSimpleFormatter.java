/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.jkiss.dbeaver.ui.editors.sql.format.simple;

import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatter;
import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatterConfiguration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Performs simple formatting of basic SQL statements (DML + query).
 * Initially based on hibernate SQL formatter.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Serge Rieder
 */
public class SQLSimpleFormatter implements SQLFormatter {

	private static final String[] BEGIN_CLAUSES = {
        "left",
        "right",
        "inner",
        "outer",
        "group",
        "order",
    };
    private static final String[] END_CLAUSES = {
        "where",
        "set",
        "having",
        "join",
        "from",
        "by",
        "join",
        "into",
        "union",
    };
    private static final String[] LOGICAL = {
        "and",
        "or",
        "when",
        "else",
        "end",
    };
    private static final String[] QUANTIFIERS = {
        "in",
        "all",
        "exists",
        "some",
        "any",
    };
    private static final String[] DML = {
        "insert",
        "update",
        "delete",
    };
    private static final String[] MISC = {
        "select",
        "on",
    };

    public static final String WHITESPACE = " \n\r\f\t";

	private static final String INDENT_STRING = "    ";

	@Override
	public String format(String source, SQLFormatterConfiguration configuration) {
		return new FormatProcess( source ).perform();
	}

	private static class FormatProcess {
		boolean beginLine = true;
		boolean afterBeginBeforeEnd;
		boolean afterByOrSetOrFromOrSelect;
		boolean afterValues;
		boolean afterOn;
		boolean afterBetween;
		boolean afterInsert;
		int inFunction;
		int parensSinceSelect;
		private LinkedList<Integer> parenCounts = new LinkedList<Integer>();
		private LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<Boolean>();

		int indent = 1;

		StringBuilder result = new StringBuilder();
		StringTokenizer tokens;
		String lastToken;
		String token;
		String lcToken;

		public FormatProcess(String sql) {
			tokens = new StringTokenizer(
					sql,
					"()+*/-=<>'`\"[]," + WHITESPACE,
					true
			);
		}

		public String perform() {

			while ( tokens.hasMoreTokens() ) {
				token = tokens.nextToken();
				lcToken = token.toLowerCase();

				if ( "'".equals( token ) ) {
					String t;
					do {
						t = tokens.nextToken();
						token += t;
					}
					// cannot handle single quotes
					while ( !"'".equals( t ) && tokens.hasMoreTokens() );
				}
				else if ( "\"".equals( token ) ) {
					String t;
					do {
						t = tokens.nextToken();
						token += t;
					}
					while ( !"\"".equals( t ) );
				}

				if ( afterByOrSetOrFromOrSelect && ",".equals( token ) ) {
					commaAfterByOrFromOrSelect();
				}
				else if ( afterOn && ",".equals( token ) ) {
					commaAfterOn();
				}

				else if ( "(".equals( token ) ) {
					openParen();
				}
				else if ( ")".equals( token ) ) {
					closeParen();
				}

				else if ( contains(BEGIN_CLAUSES, lcToken) ) {
					beginNewClause();
				}

				else if ( contains(END_CLAUSES, lcToken) ) {
					endNewClause();
				}

				else if ( "select".equals( lcToken ) ) {
					select();
				}

				else if ( contains(DML, lcToken) ) {
					updateOrInsertOrDelete();
				}

				else if ( "values".equals( lcToken ) ) {
					values();
				}

				else if ( "on".equals( lcToken ) ) {
					on();
				}

				else if ( afterBetween && lcToken.equals( "and" ) ) {
					misc();
					afterBetween = false;
				}

				else if ( contains(LOGICAL, lcToken) ) {
					logical();
				}

				else if ( isWhitespace( token ) ) {
					white();
				}

				else {
					misc();
				}

				if ( !isWhitespace( token ) ) {
					lastToken = lcToken;
				}

			}
			return result.toString();
		}

        private void commaAfterOn() {
			out();
			indent--;
			newline();
			afterOn = false;
			afterByOrSetOrFromOrSelect = true;
		}

		private void commaAfterByOrFromOrSelect() {
			out();
			newline();
		}

		private void logical() {
			if ( "end".equals( lcToken ) ) {
				indent--;
			}
			newline();
			out();
			beginLine = false;
		}

		private void on() {
			indent++;
			afterOn = true;
			newline();
			out();
			beginLine = false;
		}

		private void misc() {
			out();
			if ( "between".equals( lcToken ) ) {
				afterBetween = true;
			}
			if ( afterInsert ) {
				newline();
				afterInsert = false;
			}
			else {
				beginLine = false;
				if ( "case".equals( lcToken ) ) {
					indent++;
				}
			}
		}

		private void white() {
			if ( !beginLine ) {
				result.append( " " );
			}
		}

		private void updateOrInsertOrDelete() {
			out();
			indent++;
			beginLine = false;
			if ( "update".equals( lcToken ) ) {
				newline();
			}
			if ( "insert".equals( lcToken ) ) {
				afterInsert = true;
			}
		}

		private void select() {
			out();
			indent++;
			newline();
			parenCounts.addLast( parensSinceSelect );
			afterByOrFromOrSelects.addLast( afterByOrSetOrFromOrSelect );
			parensSinceSelect = 0;
			afterByOrSetOrFromOrSelect = true;
		}

		private void out() {
			result.append( token );
		}

		private void endNewClause() {
			if ( !afterBeginBeforeEnd ) {
				indent--;
				if ( afterOn ) {
					indent--;
					afterOn = false;
				}
				newline();
			}
			out();
			if ( !"union".equals( lcToken ) ) {
				indent++;
			}
			newline();
			afterBeginBeforeEnd = false;
			afterByOrSetOrFromOrSelect = "by".equals( lcToken )
					|| "set".equals( lcToken )
					|| "from".equals( lcToken );
		}

		private void beginNewClause() {
			if ( !afterBeginBeforeEnd ) {
				if ( afterOn ) {
					indent--;
					afterOn = false;
				}
				indent--;
				newline();
			}
			out();
			beginLine = false;
			afterBeginBeforeEnd = true;
		}

		private void values() {
			indent--;
			newline();
			out();
			indent++;
			newline();
			afterValues = true;
		}

		private void closeParen() {
			parensSinceSelect--;
			if ( parensSinceSelect < 0 ) {
				indent--;
				parensSinceSelect = parenCounts.removeLast();
				afterByOrSetOrFromOrSelect = afterByOrFromOrSelects.removeLast();
			}
			if ( inFunction > 0 ) {
				inFunction--;
				out();
			}
			else {
				if ( !afterByOrSetOrFromOrSelect ) {
					indent--;
					newline();
				}
				out();
			}
			beginLine = false;
		}

		private void openParen() {
			if ( isFunctionName( lastToken ) || inFunction > 0 ) {
				inFunction++;
			}
			beginLine = false;
			if ( inFunction > 0 ) {
				out();
			}
			else {
				out();
				if ( !afterByOrSetOrFromOrSelect ) {
					indent++;
					newline();
					beginLine = true;
				}
			}
			parensSinceSelect++;
		}

		private static boolean isFunctionName(String tok) {
			final char begin = tok.charAt( 0 );
			final boolean isIdentifier = Character.isJavaIdentifierStart( begin ) || '"' == begin;
			return isIdentifier &&
					!contains(LOGICAL, tok ) &&
					!contains(END_CLAUSES, tok ) &&
					!contains(QUANTIFIERS, tok ) &&
					!contains(DML, tok) &&
					!contains(MISC, tok);
		}

		private static boolean isWhitespace(String token) {
			return WHITESPACE.contains( token );
		}

		private void newline() {
			result.append( "\n" );
			for ( int i = 0; i < indent; i++ ) {
				result.append( INDENT_STRING );
			}
			beginLine = true;
		}
	}

    private static boolean contains(String[] array, String lcToken)
    {
        return Arrays.binarySearch(array, lcToken) != -1;
    }

}
