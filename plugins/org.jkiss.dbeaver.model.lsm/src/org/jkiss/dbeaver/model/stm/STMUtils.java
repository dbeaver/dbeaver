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
package org.jkiss.dbeaver.model.stm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class STMUtils {

    @NotNull
    public static List<STMTreeNode> expandSubtree(
        @NotNull STMTreeNode root,
        @Nullable Set<String> toExpand,
        @NotNull Set<String> toCollect
    ) {
        List<STMTreeNode> result = new ArrayList<>();
        Stack<STMTreeNode> stack = new Stack<>();
        stack.add(root);

        while (stack.size() > 0) {
            STMTreeNode node = stack.pop();
            String nodeName = node.getNodeName();
            
            if (toCollect.contains(nodeName)) {
                result.add(node);
            } else if (toExpand == null || toExpand.contains(nodeName)) {
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    stack.push((STMTreeNode) node.getChild(i));
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<STMTreeTermNode> expandTerms(@NotNull STMTreeNode root) {
        List<STMTreeTermNode> result = new ArrayList<>();
        Stack<STMTreeNode> stack = new Stack<>();
        stack.add(root);

        while (stack.size() > 0) {
            STMTreeNode node = stack.pop();
            
            if (node instanceof STMTreeTermNode term) {
                result.add(term);
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    stack.push((STMTreeNode) node.getChild(i));
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<String> expandTermStrings(@NotNull STMTreeNode root) {
        List<String> result = new ArrayList<>();
        Stack<STMTreeNode> stack = new Stack<>();
        stack.add(root);

        while (stack.size() > 0) {
            STMTreeNode node = stack.pop();
            
            if (node instanceof STMTreeTermNode term) {
                result.add(term.getText());
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    stack.push((STMTreeNode) node.getChild(i));
                }
            }
        }
        return result;
    }


}
