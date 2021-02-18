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
