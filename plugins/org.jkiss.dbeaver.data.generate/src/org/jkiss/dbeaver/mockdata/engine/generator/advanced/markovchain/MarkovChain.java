// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.markovchain;

import java.util.Deque;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.Map;

public final class MarkovChain
{
    private final int wordsPerState;
    private final String[] words;
    private final Map<List<String>, Map<List<String>, Integer>> map;
    private final Map<List<String>, Integer> totalCountMap;
    private final List<List<String>> vocabulary;
    private final Random random;
    
    public MarkovChain(final String[] words, final int wordsPerState, final Random random) {
        this.map = new HashMap<List<String>, Map<List<String>, Integer>>();
        this.totalCountMap = new HashMap<List<String>, Integer>();
        this.vocabulary = new ArrayList<List<String>>();
        this.words = Objects.requireNonNull(words, "Word array is null.");
        this.wordsPerState = this.checkPositive(wordsPerState);
        if (words.length < wordsPerState) {
            throw new IllegalArgumentException("number of words < k");
        }
        this.random = Objects.requireNonNull(random, "The random is null.");
        this.build();
    }
    
    public MarkovChain(final String[] words, final int wordsPerState) {
        this(words, wordsPerState, new Random());
    }
    
    public Random getRandom() {
        return this.random;
    }
    
    public String[] compose(int numberOfWords) {
        this.checkRequestedNumberOfWords(numberOfWords);
        List<String> startState = this.vocabulary.get(this.random.nextInt(this.vocabulary.size()));
        final String[] outputWords = new String[numberOfWords];
        numberOfWords -= this.wordsPerState;
        for (int i = 0; i < startState.size(); ++i) {
            outputWords[i] = startState.get(i);
        }
        int index = this.wordsPerState;
        while (numberOfWords-- > 0) {
            final List<String> nextState = this.randomTransition(startState);
            outputWords[index++] = lastOf(nextState);
            startState = nextState;
        }
        return outputWords;
    }
    
    private static <T> T lastOf(final List<T> list) {
        return list.get(list.size() - 1);
    }
    
    private List<String> randomTransition(final List<String> startState) {
        final Map<List<String>, Integer> localMap = this.map.get(startState);
        if (localMap == null) {
            return this.vocabulary.get(this.random.nextInt(this.vocabulary.size()));
        }
        final int choices = this.totalCountMap.get(startState);
        int coin = this.random.nextInt(choices);
        for (final Map.Entry<List<String>, Integer> entry : localMap.entrySet()) {
            if (coin < entry.getValue()) {
                return entry.getKey();
            }
            coin -= entry.getValue();
        }
        throw new IllegalStateException("Should not get here");
    }
    
    private void build() {
        final Set<List<String>> filter = new HashSet<List<String>>();
        final Deque<String> wordDeque = new ArrayDeque<String>();
        for (int i = 0; i < this.wordsPerState; ++i) {
            wordDeque.addLast(this.words[i]);
        }
        for (int i = this.wordsPerState; i < this.words.length; ++i) {
            final List<String> startSentence = new ArrayList<String>(wordDeque);
            filter.add(startSentence);
            wordDeque.removeFirst();
            wordDeque.addLast(this.words[i]);
            final List<String> nextSentence = new ArrayList<String>(wordDeque);
            Map<List<String>, Integer> localMap = this.map.get(startSentence);
            if (localMap == null) {
                this.map.put(startSentence, localMap = new HashMap<List<String>, Integer>());
            }
            localMap.put(nextSentence, localMap.getOrDefault(nextSentence, 0) + 1);
            this.totalCountMap.put(startSentence, this.totalCountMap.getOrDefault(startSentence, 0) + 1);
        }
        this.vocabulary.addAll(filter);
    }
    
    private int checkPositive(final int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k < 1");
        }
        return k;
    }
    
    private void checkRequestedNumberOfWords(final int numberOfWords) {
        if (numberOfWords < this.wordsPerState) {
            throw new IllegalArgumentException("The minimum number of words for composition should be " + this.wordsPerState + ". Received " + numberOfWords);
        }
    }
}
