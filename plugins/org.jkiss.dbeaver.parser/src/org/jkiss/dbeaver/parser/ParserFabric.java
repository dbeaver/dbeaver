package org.jkiss.dbeaver.parser;

import java.util.HashMap;
import java.util.List;

import org.jkiss.dbeaver.parser.grammar.GrammarInfo;
import org.jkiss.dbeaver.parser.grammar.GrammarRule;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfa;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaBuilder;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaOperation;
import org.jkiss.dbeaver.parser.grammar.nfa.GrammarNfaTransition;
import org.jkiss.dbeaver.parser.grammar.nfa.ParseOperationKind;

public class ParserFabric {
    
    private final ParseFsm parseFsm;

    private ParserFabric(ParseFsm parseFsm) {
        this.parseFsm = parseFsm;
    }
    
    public Parser createParser() {
        return new Parser(parseFsm); 
    }
    
    public static ParserFabric getFabric(GrammarInfo grammar) {
        GrammarNfa nfa = new GrammarNfa();
        GrammarNfaBuilder builder = new GrammarNfaBuilder(nfa);
        HashMap<String, GrammarNfaBuilder.NfaFragment> ruleFragments = new HashMap<>();
        for(GrammarRule rule : grammar.rules.values()) {
            ruleFragments.put(rule.name, builder.traverseRule(rule));
        }

        for (GrammarNfaTransition n : nfa.getTransitions().toArray(new GrammarNfaTransition[0])) {
            if(n.getOperation().getKind() == ParseOperationKind.CALL) {
                nfa.removeTransition(n);
                nfa.createTransition(n.getFrom(), ruleFragments.get(n.getOperation().getRuleName()).getFrom(), GrammarNfaOperation.makeEmpty(n.getOperation().getExprId()));
            }
            if(n.getOperation().getKind() == ParseOperationKind.RESUME) {
                nfa.removeTransition(n);
                nfa.createTransition(ruleFragments.get(n.getOperation().getRuleName()).getTo(), n.getTo(), GrammarNfaOperation.makeEmpty(n.getOperation().getExprId()));
            }
        }

        List<GrammarNfaTransition> terminalTransitions = builder.getTerminalTransitions();
        GrammarAnalyzer analyzer = new GrammarAnalyzer(terminalTransitions, ruleFragments.get(grammar.startRule));
        ParseFsm parseFsm = analyzer.buildTerminalsGraph();
        parseFsm.prepare();

        return new ParserFabric(parseFsm);
    }
}
