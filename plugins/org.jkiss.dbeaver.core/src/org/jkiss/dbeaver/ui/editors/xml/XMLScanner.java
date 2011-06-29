package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.jkiss.dbeaver.ui.editors.text.TextWhiteSpaceDetector;


public class XMLScanner extends RuleBasedScanner {

	public XMLScanner(ISharedTextColors manager) {
		IToken procInstr =
			new Token(
				new TextAttribute(
					manager.getColor(XMLConfiguration.COLOR_PROC_INSTR)));

		IRule[] rules = new IRule[2];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", procInstr); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[1] = new WhitespaceRule(new TextWhiteSpaceDetector());

		setRules(rules);
	}
}
