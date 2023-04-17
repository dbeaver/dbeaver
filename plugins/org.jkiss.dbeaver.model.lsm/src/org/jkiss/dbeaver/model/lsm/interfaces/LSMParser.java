package org.jkiss.dbeaver.model.lsm.interfaces;

import org.antlr.v4.runtime.tree.Tree;

public interface LSMParser extends LSMObject<LSMParser> {
    
    Tree parse();
}
