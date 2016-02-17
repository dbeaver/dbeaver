/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.jface.text.rules.*;



public class XMLPartitionScanner extends RuleBasedPartitionScanner {
	public static final String XML_PARTITIONING= "__xml_partitioning"; //$NON-NLS-1$
	public final static String XML_DEFAULT = "__xml_default"; //$NON-NLS-1$
	public final static String XML_COMMENT = "__xml_comment"; //$NON-NLS-1$
	public final static String XML_TAG = "__xml_tag"; //$NON-NLS-1$

	public XMLPartitionScanner() {

		IToken xmlComment = new Token(XMLPartitionScanner.XML_COMMENT);
		IToken tag = new Token(XMLPartitionScanner.XML_TAG);

		IPredicateRule[] rules = new IPredicateRule[2];

		rules[0] = new MultiLineRule("<!--", "-->", xmlComment);  //$NON-NLS-1$//$NON-NLS-2$
		rules[1] = new XMLTagRule(tag);

		setPredicateRules(rules);
	}
}
