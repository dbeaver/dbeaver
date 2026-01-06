/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

public class DirectedGraph {
    private final List<Node> nodes = new LinkedList<>();
    private final List<Edge> edges = new LinkedList<>();

    class Edge {
        public final Node from, to;
        public String label;
        public String color;

        public Edge(Node from, Node to, String label, String color) {
            this.from = from;
            this.to = to;
            this.label = label;
            this.color = color;
        }
    }

    class Node {
        public final int id;
        public String label;
        public String color;

        public Node(int id, String label, String color) {
            this.id = id;
            this.label = label;
            this.color = color;
        }
    }

    
    public Node createNode(String label, String color) {
        Node node = new Node(nodes.size(), label, color);
        this.nodes.add(node);
        return node;
    }

    public Edge createEdge(Node from, Node to, String label, String color) {
        Edge edge = new Edge(from, to, label, color);
        this.edges.add(edge);
        return edge;
    }

    public void saveToFile(String fileName) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName), Charset.forName("utf-8") , StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
            writer.write("<DirectedGraph xmlns=\"http://schemas.microsoft.com/vs/2009/dgml\">\r\n");
            writer.write("   <Nodes>\r\n");
            for (Node node: nodes) {
                writer.write("      <Node Id=\"" + node.id + "\" Label=\"" + node.label + "\" Background=\"" + node.color + "\" />\r\n");
            }
            writer.write("   </Nodes>\r\n");
            writer.write("   <Links>\r\n");
            for (Edge edge: edges) {
                writer.write("      <Link Source=\"" + edge.from.id + "\" Target=\"" + edge.to.id + "\" Label=\"" + edge.label + "\" Background=\"" + edge.color + "\" />\r\n");
            }
            writer.write("   </Links>\r\n");
            writer.write("</DirectedGraph>\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
