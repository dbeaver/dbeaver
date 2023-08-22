// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.markovchain;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.generator.advanced.AdvancedStringValueGenerator;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public class MarkovChainGenerator extends AdvancedStringValueGenerator
{
    private static final String DEF_BOOK = "book_hamlet.txt";
    private MarkovChain mc;
    private int minWordCount;
    private int maxWordCount;
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        this.minWordCount = CommonUtils.toInt(properties.get("minWordCount"), 5);
        this.maxWordCount = CommonUtils.toInt(properties.get("maxWordCount"), 10);
    }
    
    @Override
    protected Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.mc == null) {
            this.initMC();
        }
        final int wordCount = (this.minWordCount == this.maxWordCount) ? this.minWordCount : (this.minWordCount + this.mc.getRandom().nextInt(this.maxWordCount - this.minWordCount));
        return concat(this.mc.compose(wordCount));
    }
    
    private void initMC() throws IOException {
        final List<String> textLines = super.readDict("book_hamlet.txt");
        final Set<String> wordSet = new LinkedHashSet<String>();
        for (final String line : textLines) {
            Collections.addAll(wordSet, line.split("(\\s|\\W)+"));
        }
        final String[] words = wordSet.toArray(new String[0]);
        setWordsToLowerCase(words);
        this.mc = new MarkovChain(words, 2);
    }
    
    private static String concat(final String... strings) {
        final StringBuilder sb = new StringBuilder();
        for (final String string : strings) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(string);
        }
        return sb.toString();
    }
    
    private static void setWordsToLowerCase(final String[] words) {
        for (int i = 0; i < words.length; ++i) {
            words[i] = words[i].toLowerCase();
        }
    }
}
