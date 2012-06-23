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
