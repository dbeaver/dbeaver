/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.parser.common;

import org.jkiss.dbeaver.parser.common.grammar.nfa.GrammarNfaOperation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parser finite state machine
 */
class ParserFsm {
    private final List<ParserFsmNode> initialStates;
    private final List<ParserFsmNode> allStates;

    public ParserFsm(List<ParserFsmNode> initialStates, List<ParserFsmNode> parseFsmStates) {
        this.initialStates = initialStates;
        this.allStates = parseFsmStates;
    }

    public Iterable<ParserFsmNode> getInitialStates() {
        return initialStates;
    }

    public void compact() {
        for (ParserFsmNode n : this.allStates) {
            n.compact();
        }
    }

    /**
     * Prepare common recognizing pattern for all possible terminals associated with presented parsing steps
     * for all finite state machine states
     */
    public void prepare() {
        for (ParserFsmNode state : this.allStates) {
            state.prepare();
        }
    }
    
    public String collectDebugString() {
        StringBuilder sb = new StringBuilder();
        for (var s : allStates) {
            sb.append("state").append(s).append(":");
            if (this.initialStates.contains(s)) {
                sb.append("<START>");
            }
            if (s.isEnd()) {
                sb.append(" <END>");
            }
            sb.append("\n");
            var transitionsByPattern = s.getTransitions().stream()
                .collect(Collectors.groupingBy(t -> t.getPattern() == null ? "" : t.getPattern()));

            for (var entry : transitionsByPattern.entrySet()) {
                sb.append("\ton ").append(entry.getKey()).append("\n");
                for (var transition : entry.getValue()) {
                    sb.append("\t\tto ").append(transition.getTo()).append(" { ");
                    sb.append(transition.getOperations().stream().map(GrammarNfaOperation::toString).collect(Collectors.joining(", ")));
                    sb.append(" }\n");
                }
                
            }
        } 
        return sb.toString();
    }

    public void collectDebugString(ITextPartConsumer consumer) throws IOException {
        StringBuilder sb;
        for (var s : allStates) {
            sb = new StringBuilder();
            sb.append("state").append(s).append(":");
            if (this.initialStates.contains(s)) {
                sb.append("<START>");
            }
            if (s.isEnd()) {
                sb.append(" <END>");
            }
            sb.append("\n");
            consumer.accept(sb.toString());

            var transitionsByPattern = s.getTransitions().stream()
                .collect(Collectors.groupingBy(t -> t.getPattern() == null ? "" : t.getPattern()));

            for (var entry : transitionsByPattern.entrySet()) {
                sb = new StringBuilder();
                sb.append("\ton ").append(entry.getKey()).append("\n");
                consumer.accept(sb.toString());
                for (var transition : entry.getValue()) {
                    sb = new StringBuilder();
                    sb.append("\t\tto ").append(transition.getTo()).append(" { ");
                    sb.append(transition.getOperations().stream().map(GrammarNfaOperation::toString).collect(Collectors.joining(", ")));
                    sb.append(" }\n");
                    consumer.accept(sb.toString());
                }
            }
        }
    }

    public interface ITextPartConsumer {
        void accept(String str) throws IOException;
    }

    /*
    public XmlGraph collectDebugView() {
        XmlGraph xg = new XmlGraph();
        Map<ParseState, XmlGraphNode> xnn = new HashMap<>();
        XmlGraphNode xend = xg.createNode();
        xend.setText("END");
        for (var s : allStates) {
            XmlGraphNode xn = xg.createNode();
            xn.setText("#" + s.getId());
            xnn.put(s, xn);
        }
        for (var s : allStates) {
            for (var t : s.getTransitions()) {
                XmlGraphNode xn = xg.createNode();
                xn.setText(t.pattern + "\n\n" + String.join("\n",
                        t.operations.stream().map(op -> op.toString()).collect(Collectors.toList())));
                xnn.get(s).connectTo(xn);
                XmlGraphNode xt = xnn.get(t.to);
                if (xt != null) {
                    xn.connectTo(xt);
                } else {
                    xn.connectTo(xend);
                }
            }
        } 
        xg.saveTo("d:\\temp\\out2.dgml");

        return xg;
    }*/
     
}
