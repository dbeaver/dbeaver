// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.mockdata.engine.generator.AbstractStringValueGenerator;

public abstract class AdvancedStringValueGenerator extends AbstractStringValueGenerator
{
    protected List<String> readDict(final String fileName) throws IOException {
         List<String> list = new ArrayList<String>();
         BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("/resources/dictionaries/" + fileName)));
        String line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            list.add(line);
        }
        return list;
    }
}
