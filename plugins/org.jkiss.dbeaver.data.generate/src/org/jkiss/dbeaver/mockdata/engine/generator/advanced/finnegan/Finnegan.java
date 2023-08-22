// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.finnegan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.io.Serializable;

public class Finnegan implements Serializable
{
    private static final long serialVersionUID = -2578460257281186353L;
    public final String[] openingVowels;
    public final String[] midVowels;
    public final String[] openingConsonants;
    public final String[] midConsonants;
    public final String[] closingConsonants;
    public final String[] vowelSplitters;
    public final String[] closingSyllables;
    public boolean clean;
    public final LinkedHashMap<Integer, Double> syllableFrequencies;
    protected double totalSyllableFrequency;
    public final double vowelStartFrequency;
    public final double vowelEndFrequency;
    public final double vowelSplitFrequency;
    public final double syllableEndFrequency;
    protected final Pattern[] sanityChecks;
    public ArrayList<Modifier> modifiers;
    protected static final Pattern repeats;
    protected static final Pattern diacritics;
    public static final Pattern[] vulgarChecks;
    public static final Pattern[] englishSanityChecks;
    public static final Pattern[] japaneseSanityChecks;
    public RNG rng;
    public static final char[][] accentedVowels;
    public static final char[][] accentedConsonants;
    public static final Finnegan LOVECRAFT;
    public static final Finnegan ENGLISH;
    public static final Finnegan GREEK_ROMANIZED;
    public static final Finnegan GREEK_AUTHENTIC;
    public static final Finnegan FRENCH;
    public static final Finnegan RUSSIAN_ROMANIZED;
    public static final Finnegan RUSSIAN_AUTHENTIC;
    public static final Finnegan JAPANESE_ROMANIZED;
    public static final Finnegan SWAHILI;
    public static final Finnegan SOMALI;
    public static final Finnegan HINDI_ROMANIZED;
    public static final Finnegan FANTASY_NAME;
    public static final Finnegan FANCY_FANTASY_NAME;
    
    static {
        repeats = Pattern.compile("(.)\\1+");
        diacritics = Pattern.compile("[\\u0300-\\u036F\\u1DC0-\\u1DFF]+");
        vulgarChecks = new Pattern[] { Pattern.compile("[Ss\u03beCc\u0441\u03c2\u0421][h\u043d\u041d].*[dt\u0442\u03c4\u0422\u03a4f]"), Pattern.compile("([Pp\u0440\u03c1\u0420\u03a1][h\u043d\u041d])|[Kk\u043a\u03ba\u041a\u039aFfDdCc\u0441\u03c2\u0421].{1,4}[Kk\u043a\u03ba\u041a\u039aCc\u0441\u03c2\u0421x\u0445\u0436\u03c7\u0425\u0416\u03a7]"), Pattern.compile("[Bb\u044a\u044b\u0431\u0432\u03b2\u042a\u042b\u0411\u0412\u0392]..?.?[c\u0441\u03c2\u0421][h\u043d\u041d]"), Pattern.compile("[Ww\u0448\u0449\u03c8\u0428\u0429Hh\u043d\u041d]..?[r\u044f\u042f]"), Pattern.compile("[Tt\u0442\u03c4\u0422\u03a4]..?[t\u0442\u03c4\u0422\u03a4]"), Pattern.compile("([Pp\u0440\u03c1\u0420\u03a1][h\u043d\u041d])|[Ff]..?[r\u044f\u042f][t\u0442\u03c4\u0422\u03a4]"), Pattern.compile("([Ss\u03be][h\u043d\u041d])|[j][i\u03c4\u03b9\u0399].?[s\u03bez\u0396]"), Pattern.compile("[Aa\u0430\u03b1\u0410\u0391\u039b][Nn\u0438\u0439\u0418\u0419\u039d]..?[Ss\u03bel\u03b9\u03b6z\u0396]"), Pattern.compile("[Aa\u0430\u03b1\u0410\u0391\u039b][s\u03be][s\u03be]"), Pattern.compile(".[u\u03bc\u03c5\u03bd][h\u043d\u041d]?[n\u0438\u0439\u0418\u0419\u039d]+[t\u0442\u03c4\u0422\u03a4]"), Pattern.compile("[NnFf]..?g"), Pattern.compile("[Pp\u0440\u03c1\u0420\u03a1][e\u0435\u0451\u0437\u03be\u03b5\u0415\u0401\u0417\u039e\u0395\u03a3io\u043e\u044e\u03c3\u03bf\u041e\u042e\u039fu\u03bc\u03c5\u03bd][e\u0435\u0451\u0437\u03be\u03b5\u0415\u0401\u0417\u039e\u0395\u03a3o\u043e\u044e\u03c3\u03bf\u041e\u042e\u039fs]"), Pattern.compile("[Mm\u043c\u041c\u039c]..?[r\u044f\u042f].?d"), Pattern.compile("[Gg][h\u043d\u041d]?[a\u0430\u03b1\u0410\u0391\u039be\u0435\u0451\u0437\u03be\u03b5\u0415\u0401\u0417\u039e\u0395\u03a3][y\u0443\u03bb\u03b3\u0423\u03a5e\u0435\u0451\u0437\u03be\u03b5\u0415\u0401\u0417\u039e\u0395\u03a3]") };
        englishSanityChecks = new Pattern[] { Pattern.compile("[AEIOUaeiou]{3}"), Pattern.compile("(\\w)\\1\\1"), Pattern.compile("(.)\\1(.)\\2"), Pattern.compile("[Aa][ae]"), Pattern.compile("[Uu][umlkj]"), Pattern.compile("[Ii][iyqkhrl]"), Pattern.compile("[Oo][c]"), Pattern.compile("[Yy][aeiou]{2}"), Pattern.compile("[Rr][aeiouy]+[xrhp]"), Pattern.compile("[Qq]u[yu]"), Pattern.compile("[^oai]uch"), Pattern.compile("[^tcsz]hh"), Pattern.compile("[Hh][tcszi]h"), Pattern.compile("[Tt]t[^aeiouy]{2}"), Pattern.compile("[IYiy]h[^aeiouy ]"), Pattern.compile("[szSZrlRL][^aeiou][rlsz]"), Pattern.compile("[UIuiYy][wy]"), Pattern.compile("^[UIui][ae]"), Pattern.compile("q$") };
        japaneseSanityChecks = new Pattern[] { Pattern.compile("[AEIOUaeiou]{3}"), Pattern.compile("(\\w)\\1\\1"), Pattern.compile("[Tt]s[^u]"), Pattern.compile("[Ff][^u]"), Pattern.compile("[Yy][^auo]"), Pattern.compile("[Tt][ui]"), Pattern.compile("[SsZzDd]i"), Pattern.compile("[Hh]u") };
        accentedVowels = new char[][] { { '\u00e0', '\u00e1', '\u00e2', '\u00e3', '\u00e4', '\u00e5', '\u00e6', '\u0101', '\u0103', '\u0105', '\u01fb', '\u01fd' }, { '\u00e8', '\u00e9', '\u00ea', '\u00eb', '\u0113', '\u0115', '\u0117', '\u0119', '\u011b' }, { '\u00ec', '\u00ed', '\u00ee', '\u00ef', '\u0129', '\u012b', '\u012d', '\u012f', '\u0131' }, { '\u00f2', '\u00f3', '\u00f4', '\u00f5', '\u00f6', '\u00f8', '\u014d', '\u014f', '\u0151', '\u0153', '\u01ff' }, { '\u00f9', '\u00fa', '\u00fb', '\u00fc', '\u0169', '\u016b', '\u016d', '\u016f', '\u0171', '\u0173' } };
        accentedConsonants = new char[][] { { 'b' }, { 'c', '\u00e7', '\u0107', '\u0109', '\u010b', '\u010d' }, { 'd', '\u00fe', '\u00f0', '\u010f', '\u0111' }, { 'f' }, { 'g', '\u011d', '\u011f', '\u0121', '\u0123' }, { 'h', '\u0125', '\u0127' }, { 'j', '\u0135', '\u0237' }, { 'k', '\u0137' }, { 'l', '\u013a', '\u013c', '\u013e', '\u0140', '\u0142' }, { 'm' }, { 'n', '\u00f1', '\u0144', '\u0146', '\u0148', '\u014b' }, { 'p' }, { 'q' }, { 'r', '\u0155', '\u0157', '\u0159' }, { 's', '\u015b', '\u015d', '\u015f', '\u0161', '\u0219' }, { 't', '\u0163', '\u0165', '\u021b' }, { 'v' }, { 'w', '\u0175', '\u1e81', '\u1e83', '\u1e85' }, { 'x' }, { 'y', '\u00fd', '\u00ff', '\u0177', '\u1ef3' }, { 'z', '\u017a', '\u017c', '\u017e' } };
        LOVECRAFT = new Finnegan(new String[] { "a", "i", "o", "e", "u", "a", "i", "o", "e", "u", "ia", "ai", "aa", "ei" }, new String[0], new String[] { "s", "t", "k", "n", "y", "p", "k", "l", "g", "gl", "th", "sh", "ny", "ft", "hm", "zvr", "cth" }, new String[] { "h", "gl", "gr", "nd", "mr", "vr", "kr" }, new String[] { "l", "p", "s", "t", "n", "k", "g", "x", "rl", "th", "gg", "gh", "ts", "lt", "rk", "kh", "sh", "ng", "shk" }, new String[] { "aghn", "ulhu", "urath", "oigor", "alos", "'yeh", "achtal", "urath", "ikhet", "adzek" }, new String[] { "'", "-" }, new int[] { 1, 2, 3 }, new double[] { 6.0, 7.0, 2.0 }, 0.4, 0.31, 0.07, 0.04, null, true);
        ENGLISH = new Finnegan(new String[] { "a", "a", "a", "a", "o", "o", "o", "e", "e", "e", "e", "e", "i", "i", "i", "i", "u", "a", "a", "a", "a", "o", "o", "o", "e", "e", "e", "e", "e", "i", "i", "i", "i", "u", "a", "a", "a", "o", "o", "e", "e", "e", "i", "i", "i", "u", "a", "a", "a", "o", "o", "e", "e", "e", "i", "i", "i", "u", "au", "ai", "ai", "ou", "ea", "ie", "io", "ei" }, new String[] { "u", "u", "oa", "oo", "oo", "oo", "ee", "ee", "ee", "ee" }, new String[] { "b", "bl", "br", "c", "cl", "cr", "ch", "d", "dr", "f", "fl", "fr", "g", "gl", "gr", "h", "j", "k", "l", "m", "n", "p", "pl", "pr", "qu", "r", "s", "sh", "sk", "st", "sp", "sl", "sm", "sn", "t", "tr", "th", "thr", "v", "w", "y", "z", "b", "bl", "br", "c", "cl", "cr", "ch", "d", "dr", "f", "fl", "fr", "g", "gr", "h", "j", "k", "l", "m", "n", "p", "pl", "pr", "r", "s", "sh", "st", "sp", "sl", "t", "tr", "th", "w", "y", "b", "br", "c", "ch", "d", "dr", "f", "g", "h", "j", "l", "m", "n", "p", "r", "s", "sh", "st", "sl", "t", "tr", "th", "b", "d", "f", "g", "h", "l", "m", "n", "p", "r", "s", "sh", "t", "th", "b", "d", "f", "g", "h", "l", "m", "n", "p", "r", "s", "sh", "t", "th", "r", "s", "t", "l", "n", "str", "spr", "spl", "wr", "kn", "kn", "gn" }, new String[] { "x", "cst", "bs", "ff", "lg", "g", "gs", "ll", "ltr", "mb", "mn", "mm", "ng", "ng", "ngl", "nt", "ns", "nn", "ps", "mbl", "mpr", "pp", "ppl", "ppr", "rr", "rr", "rr", "rl", "rtn", "ngr", "ss", "sc", "rst", "tt", "tt", "ts", "ltr", "zz" }, new String[] { "b", "rb", "bb", "c", "rc", "ld", "d", "ds", "dd", "f", "ff", "lf", "rf", "rg", "gs", "ch", "lch", "rch", "tch", "ck", "ck", "lk", "rk", "l", "ll", "lm", "m", "rm", "mp", "n", "nk", "nch", "nd", "ng", "ng", "nt", "ns", "lp", "rp", "p", "r", "rn", "rts", "s", "s", "s", "s", "ss", "ss", "st", "ls", "t", "t", "ts", "w", "wn", "x", "ly", "lly", "z", "b", "c", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "w" }, new String[] { "ate", "ite", "ism", "ist", "er", "er", "er", "ed", "ed", "ed", "es", "es", "ied", "y", "y", "y", "y", "ate", "ite", "ism", "ist", "er", "er", "er", "ed", "ed", "ed", "es", "es", "ied", "y", "y", "y", "y", "ate", "ite", "ism", "ist", "er", "er", "er", "ed", "ed", "ed", "es", "es", "ied", "y", "y", "y", "y", "ay", "ay", "ey", "oy", "ay", "ay", "ey", "oy", "ough", "aught", "ant", "ont", "oe", "ance", "ell", "eal", "oa", "urt", "ut", "iom", "ion", "ion", "ision", "ation", "ation", "ition", "ough", "aught", "ant", "ont", "oe", "ance", "ell", "eal", "oa", "urt", "ut", "iom", "ion", "ion", "ision", "ation", "ation", "ition", "ily", "ily", "ily", "adly", "owly", "oorly", "ardly", "iedly" }, new String[0], new int[] { 1, 2, 3, 4 }, new double[] { 7.0, 8.0, 4.0, 1.0 }, 0.22, 0.1, 0.0, 0.25, Finnegan.englishSanityChecks, true);
        GREEK_ROMANIZED = new Finnegan(new String[] { "a", "a", "a", "o", "o", "o", "e", "e", "i", "i", "i", "au", "ai", "ai", "oi", "oi", "ia", "io", "ou", "ou", "eo", "ei" }, new String[] { "ui", "ei" }, new String[] { "rh", "s", "z", "t", "t", "k", "ch", "n", "th", "kth", "m", "p", "ps", "b", "l", "kr", "g", "phth" }, new String[] { "lph", "pl", "l", "l", "kr", "nch", "nx", "ps" }, new String[] { "s", "p", "t", "ch", "n", "m", "s", "p", "t", "ch", "n", "m", "b", "g", "st", "rst", "rt", "sp", "rk", "ph", "x", "z", "nk", "ng", "th" }, new String[] { "os", "os", "is", "us", "um", "eum", "ium", "iam", "us", "um", "es", "anes", "eros", "or", "ophon", "on", "otron" }, new String[0], new int[] { 1, 2, 3 }, new double[] { 5.0, 7.0, 4.0 }, 0.45, 0.45, 0.0, 0.3, null, true);
        GREEK_AUTHENTIC = new Finnegan(new String[] { "\u03b1", "\u03b1", "\u03b1", "\u03bf", "\u03bf", "\u03bf", "\u03b5", "\u03b5", "\u03b9", "\u03b9", "\u03b9", "\u03b1\u03c5", "\u03b1\u03b9", "\u03b1\u03b9", "\u03bf\u03b9", "\u03bf\u03b9", "\u03b9\u03b1", "\u03b9\u03bf", "\u03bf\u03c5", "\u03bf\u03c5", "\u03b5\u03bf", "\u03b5\u03b9" }, new String[] { "\u03c5\u03b9", "\u03b5\u03b9" }, new String[] { "\u03c1", "\u03c3", "\u03b6", "\u03c4", "\u03c4", "\u03ba", "\u03c7", "\u03bd", "\u03b8", "\u03ba\u03b8", "\u03bc", "\u03c0", "\u03c8", "\u03b2", "\u03bb", "\u03ba\u03c1", "\u03b3", "\u03c6\u03b8" }, new String[] { "\u03bb\u03c6", "\u03c0\u03bb", "\u03bb", "\u03bb", "\u03ba\u03c1", "\u03b3\u03c7", "\u03b3\u03be", "\u03c8" }, new String[] { "\u03c3", "\u03c0", "\u03c4", "\u03c7", "\u03bd", "\u03bc", "\u03c3", "\u03c0", "\u03c4", "\u03c7", "\u03bd", "\u03bc", "\u03b2", "\u03b3", "\u03c3\u03c4", "\u03c1\u03c3\u03c4", "\u03c1\u03c4", "\u03c3\u03c0", "\u03c1\u03ba", "\u03c6", "\u03be", "\u03b6", "\u03b3\u03ba", "\u03b3\u03b3", "\u03b8" }, new String[] { "\u03bf\u03c2", "\u03bf\u03c2", "\u03b9\u03c2", "\u03c5\u03c2", "\u03c5\u03bc", "\u03b5\u03c5\u03bc", "\u03b9\u03c5\u03bc", "\u03b9\u03b1\u03bc", "\u03c5\u03c2", "\u03c5\u03bc", "\u03b5\u03c2", "\u03b1\u03bd\u03b5\u03c2", "\u03b5\u03c1\u03bf\u03c2", "\u03bf\u03c1", "\u03bf\u03c6\u03bf\u03bd", "\u03bf\u03bd", "\u03bf\u03c4\u03c1\u03bf\u03bd" }, new String[0], new int[] { 1, 2, 3 }, new double[] { 5.0, 7.0, 4.0 }, 0.45, 0.45, 0.0, 0.3, null, true);
        FRENCH = new Finnegan(new String[] { "a", "a", "a", "e", "e", "e", "i", "i", "o", "u", "a", "a", "a", "e", "e", "e", "i", "i", "o", "a", "a", "a", "e", "e", "e", "i", "i", "o", "u", "a", "a", "a", "e", "e", "e", "i", "i", "o", "a", "a", "e", "e", "i", "o", "a", "a", "a", "e", "e", "e", "i", "i", "o", "ai", "oi", "oui", "au", "\u0153u", "ou" }, new String[] { "ai", "aie", "aou", "eau", "oi", "oui", "oie", "eu", "eu", "\u00e0", "\u00e2", "ai", "a\u00ee", "a\u00ef", "aie", "aou", "ao\u00fb", "au", "ay", "e", "\u00e9", "\u00e9e", "\u00e8", "\u00ea", "eau", "ei", "e\u00ee", "eu", "e\u00fb", "i", "\u00ee", "\u00ef", "o", "\u00f4", "oe", "o\u00ea", "o\u00eb", "\u0153u", "oi", "oie", "o\u00ef", "ou", "o\u00fb", "oy", "u", "\u00fb", "ue", "a", "a", "a", "e", "e", "e", "i", "i", "o", "u", "a", "a", "a", "e", "e", "e", "i", "i", "o", "a", "a", "e", "e", "i", "o", "a", "a", "a", "e", "e", "e", "i", "i", "o" }, new String[] { "tr", "ch", "m", "b", "b", "br", "j", "j", "j", "j", "g", "t", "t", "t", "c", "d", "f", "f", "h", "n", "l", "l", "s", "s", "s", "r", "r", "r", "v", "v", "p", "pl", "pr", "bl", "br", "dr", "gl", "gr" }, new String[] { "cqu", "gu", "qu", "rqu", "nt", "ng", "ngu", "mb", "ll", "nd", "ndr", "nct", "st", "xt", "mbr", "pl", "g", "gg", "ggr", "gl", "m", "m", "mm", "v", "v", "f", "f", "f", "ff", "b", "b", "bb", "d", "d", "dd", "s", "s", "s", "ss", "ss", "ss", "cl", "cr", "ng", "\u00e7", "\u00e7", "r\u00e7" }, new String[0], new String[] { "e", "e", "e", "e", "e", "\u00e9", "\u00e9", "er", "er", "er", "er", "er", "es", "es", "es", "es", "es", "es", "e", "e", "e", "e", "e", "\u00e9", "\u00e9", "er", "er", "er", "er", "er", "er", "es", "es", "es", "es", "es", "e", "e", "e", "e", "e", "\u00e9", "\u00e9", "\u00e9", "er", "er", "er", "er", "er", "es", "es", "es", "es", "es", "ent", "em", "en", "en", "aim", "ain", "an", "oin", "ien", "iere", "ors", "anse", "ombs", "ommes", "ancs", "ends", "\u0153ufs", "erfs", "ongs", "aps", "ats", "ives", "ui", "illes", "aen", "aon", "am", "an", "eun", "ein", "age", "age", "uile", "uin", "um", "un", "un", "un", "aille", "ouille", "eille", "ille", "eur", "it", "ot", "oi", "oi", "oi", "aire", "om", "on", "on", "im", "in", "in", "ien", "ien", "ion", "il", "eil", "oin", "oint", "igu\u00eft\u00e9", "ience", "incte", "ang", "ong", "acr\u00e9", "eau", "ouche", "oux", "oux", "ect", "ecri", "agne", "uer", "aix", "eth", "ut", "ant", "anc", "anc", "anche", "ioche", "eaux", "ive", "eur", "ancois", "ecois" }, new String[0], new int[] { 1, 2, 3 }, new double[] { 18.0, 7.0, 2.0 }, 0.35, 1.0, 0.0, 0.55, null, true);
        RUSSIAN_ROMANIZED = new Finnegan(new String[] { "a", "e", "e", "i", "i", "o", "u", "ie", "y", "e", "iu", "ia", "y", "a", "a", "o", "u" }, new String[0], new String[] { "b", "v", "g", "d", "k", "l", "p", "r", "s", "t", "f", "kh", "ts", "b", "v", "g", "d", "k", "l", "p", "r", "s", "t", "f", "kh", "ts", "b", "v", "g", "d", "k", "l", "p", "r", "s", "t", "f", "zh", "m", "n", "z", "ch", "sh", "shch", "br", "sk", "tr", "bl", "gl", "kr", "gr" }, new String[] { "bl", "br", "pl", "dzh", "tr", "gl", "gr", "kr" }, new String[] { "b", "v", "g", "d", "zh", "z", "k", "l", "m", "n", "p", "r", "s", "t", "f", "kh", "ts", "ch", "sh", "v", "f", "sk", "sk", "sk", "s", "b", "d", "d", "n", "r", "r" }, new String[] { "odka", "odna", "usk", "ask", "usky", "ad", "ar", "ovich", "ev", "ov", "of", "agda", "etsky", "ich", "on", "akh", "iev", "ian" }, new String[0], new int[] { 1, 2, 3, 4, 5, 6 }, new double[] { 4.0, 5.0, 6.0, 5.0, 3.0, 1.0 }, 0.1, 0.2, 0.0, 0.12, Finnegan.englishSanityChecks, true);
        RUSSIAN_AUTHENTIC = new Finnegan(new String[] { "\u0430", "\u0435", "\u0451", "\u0438", "\u0439", "\u043e", "\u0443", "\u044a", "\u044b", "\u044d", "\u044e", "\u044f", "\u044b", "\u0430", "\u0430", "\u043e", "\u0443" }, new String[0], new String[] { "\u0431", "\u0432", "\u0433", "\u0434", "\u043a", "\u043b", "\u043f", "\u0440", "\u0441", "\u0442", "\u0444", "\u0445", "\u0446", "\u0431", "\u0432", "\u0433", "\u0434", "\u043a", "\u043b", "\u043f", "\u0440", "\u0441", "\u0442", "\u0444", "\u0445", "\u0446", "\u0431", "\u0432", "\u0433", "\u0434", "\u043a", "\u043b", "\u043f", "\u0440", "\u0441", "\u0442", "\u0444", "\u0436", "\u043c", "\u043d", "\u0437", "\u0447", "\u0448", "\u0449", "\u0431\u0440", "\u0441\u043a", "\u0442\u0440", "\u0431\u043b", "\u0433\u043b", "\u043a\u0440", "\u0433\u0440" }, new String[] { "\u0431\u043b", "\u0431\u0440", "\u043f\u043b", "\u0434\u0436", "\u0442\u0440", "\u0433\u043b", "\u0433\u0440", "\u043a\u0440" }, new String[] { "\u0431", "\u0432", "\u0433", "\u0434", "\u0436", "\u0437", "\u043a", "\u043b", "\u043c", "\u043d", "\u043f", "\u0440", "\u0441", "\u0442", "\u0444", "\u0445", "\u0446", "\u0447", "\u0448", "\u0432", "\u0444", "\u0441\u043a", "\u0441\u043a", "\u0441\u043a", "\u0441", "\u0431", "\u0434", "\u0434", "\u043d", "\u0440", "\u0440" }, new String[] { "\u043e\u0434\u043a\u0430", "\u043e\u0434\u043d\u0430", "\u0443\u0441\u043a", "\u0430\u0441\u043a", "\u0443\u0441\u043a\u044b", "\u0430\u0434", "\u0430\u0440", "\u043e\u0432\u0439\u0447", "\u0435\u0432", "\u043e\u0432", "\u043e\u0444", "\u0430\u0433\u0434\u0430", "\u0451\u0446\u043a\u044b", "\u0439\u0447", "\u043e\u043d", "\u0430\u0445", "\u044a\u0432", "\u044f\u043d" }, new String[0], new int[] { 1, 2, 3, 4, 5, 6 }, new double[] { 4.0, 5.0, 6.0, 5.0, 3.0, 1.0 }, 0.1, 0.2, 0.0, 0.12, null, true);
        JAPANESE_ROMANIZED = new Finnegan(new String[] { "a", "a", "a", "a", "e", "e", "i", "i", "i", "i", "o", "o", "o", "u", "ou", "u", "ai", "ai" }, new String[0], new String[] { "k", "ky", "s", "sh", "t", "ts", "ch", "n", "ny", "h", "f", "hy", "m", "my", "y", "r", "ry", "g", "gy", "z", "j", "d", "b", "by", "p", "py", "k", "t", "n", "s", "k", "t", "d", "s", "sh", "sh", "g", "r", "b", "k", "t", "n", "s", "k", "t", "b", "s", "sh", "sh", "g", "r", "b", "k", "t", "n", "s", "k", "t", "z", "s", "sh", "sh", "ch", "ry", "ts" }, new String[] { "k", "ky", "s", "sh", "t", "ts", "ch", "n", "ny", "h", "f", "hy", "m", "my", "y", "r", "ry", "g", "gy", "z", "j", "d", "b", "by", "p", "py", "k", "t", "d", "s", "k", "t", "d", "s", "sh", "sh", "y", "j", "p", "r", "d", "k", "t", "b", "s", "k", "t", "b", "s", "sh", "sh", "y", "j", "p", "r", "d", "k", "t", "z", "s", "f", "g", "z", "b", "d", "ts", "nn", "nn", "nn", "nd", "nz", "mm", "kk", "kk", "tt", "ss", "ssh", "tch" }, new String[] { "n" }, new String[0], new String[0], new int[] { 1, 2, 3, 4, 5 }, new double[] { 5.0, 4.0, 5.0, 4.0, 3.0 }, 0.3, 0.9, 0.0, 0.0, Finnegan.japaneseSanityChecks, true);
        SWAHILI = new Finnegan(new String[] { "a", "i", "o", "e", "u", "a", "a", "i", "o", "o", "e", "u", "a", "a", "i", "o", "o", "u", "a", "a", "i", "i", "o", "a", "a", "a", "a", "a", "a", "i", "o", "e", "u", "a", "a", "i", "o", "o", "e", "u", "a", "a", "i", "o", "o", "u", "a", "a", "i", "i", "o", "a", "a", "a", "a", "a", "aa", "aa", "ue", "uo", "ii", "ea" }, new String[0], new String[] { "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "nb", "nj", "ns", "nz", "nb", "nch", "nj", "ns", "ny", "nz", "nb", "nch", "nf", "ng", "nj", "nk", "np", "ns", "nz", "nb", "nch", "nd", "nf", "ng", "nj", "nk", "np", "ns", "nt", "nz", "nb", "nch", "nd", "nf", "ng", "nj", "nk", "np", "ns", "nt", "nv", "nw", "nz", "mb", "ms", "my", "mz", "mb", "mch", "ms", "my", "mz", "mb", "mch", "mk", "mp", "ms", "my", "mz", "mb", "mch", "md", "mk", "mp", "ms", "mt", "my", "mz", "mb", "mch", "md", "mf", "mg", "mj", "mk", "mp", "ms", "mt", "mv", "mw", "my", "mz", "sh", "sh", "sh", "ny", "kw", "dh", "th", "sh", "ny", "dh", "th", "sh", "gh", "r", "ny", "dh", "th", "sh", "gh", "r", "ny" }, new String[] { "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "b", "h", "j", "l", "s", "y", "m", "n", "b", "ch", "h", "j", "l", "s", "y", "z", "m", "n", "b", "ch", "f", "g", "h", "j", "k", "l", "p", "s", "y", "z", "m", "n", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "y", "z", "m", "n", "kw", "b", "ch", "d", "f", "g", "h", "j", "k", "l", "p", "s", "t", "v", "w", "y", "z", "m", "n", "kw", "nb", "nj", "ns", "nz", "nb", "nch", "nj", "ns", "ny", "nz", "nb", "nch", "nf", "ng", "nj", "nk", "np", "ns", "nz", "nb", "nch", "nd", "nf", "ng", "nj", "nk", "np", "ns", "nt", "nz", "nb", "nch", "nd", "nf", "ng", "nj", "nk", "np", "ns", "nt", "nw", "nz", "mb", "ms", "my", "mz", "mb", "mch", "ms", "my", "mz", "mb", "mch", "mk", "mp", "ms", "my", "mz", "mb", "mch", "md", "mk", "mp", "ms", "mt", "my", "mz", "mb", "mch", "md", "mf", "mg", "mj", "mk", "mp", "ms", "mt", "mw", "my", "mz", "sh", "sh", "sh", "ny", "kw", "dh", "th", "sh", "ny", "dh", "th", "sh", "gh", "r", "ny", "dh", "th", "sh", "gh", "r", "ny", "ng", "ng", "ng", "ng", "ng" }, new String[] { "" }, new String[] { "a-@2a", "a-@2a", "a-@3a", "a-@2a", "a-@2a", "a-@3a", "i-@2i", "i-@2i", "i-@3i", "e-@2e", "e-@2e", "e-@3e", "u-@2u", "u-@2u", "u-@3u" }, new String[0], new int[] { 1, 2, 3, 4, 5 }, new double[] { 1.0, 7.0, 6.0, 4.0, 2.0 }, 0.2, 1.0, 0.0, 0.25, null, true);
        SOMALI = new Finnegan(new String[] { "a", "a", "a", "a", "a", "a", "a", "aa", "aa", "aa", "e", "e", "ee", "i", "i", "i", "i", "ii", "o", "o", "o", "oo", "u", "u", "u", "uu", "uu" }, new String[0], new String[] { "b", "t", "j", "x", "kh", "d", "r", "s", "sh", "dh", "c", "g", "f", "q", "k", "l", "m", "n", "w", "h", "y", "x", "g", "b", "d", "s", "m", "dh", "n", "r", "g", "b", "s", "dh" }, new String[] { "bb", "gg", "dd", "bb", "dd", "rr", "ddh", "cc", "gg", "ff", "ll", "mm", "nn", "bb", "gg", "dd", "bb", "dd", "gg", "bb", "gg", "dd", "bb", "dd", "gg", "cy", "fk", "ft", "nt", "rt", "lt", "qm", "rdh", "rsh", "lq", "my", "gy", "by", "lkh", "rx", "md", "bd", "dg", "fd", "mf", "dh", "dh", "dh", "dh" }, new String[] { "b", "t", "j", "x", "kh", "d", "r", "s", "sh", "c", "g", "f", "q", "k", "l", "m", "n", "h", "x", "g", "b", "d", "s", "m", "q", "n", "r", "b", "t", "j", "x", "kh", "d", "r", "s", "sh", "c", "g", "f", "q", "k", "l", "m", "n", "h", "x", "g", "b", "d", "s", "m", "q", "n", "r", "b", "t", "j", "x", "kh", "d", "r", "s", "sh", "c", "g", "f", "q", "k", "l", "m", "n", "g", "b", "d", "s", "q", "n", "r", "b", "t", "x", "kh", "d", "r", "s", "sh", "g", "f", "q", "k", "l", "m", "n", "g", "b", "d", "s", "r", "n", "b", "t", "kh", "d", "r", "s", "sh", "g", "f", "q", "k", "l", "m", "n", "g", "b", "d", "s", "r", "n", "b", "t", "d", "r", "s", "sh", "g", "f", "q", "k", "l", "m", "n", "g", "b", "d", "s", "r", "n" }, new String[] { "aw", "ow", "ay", "ey", "oy", "ay", "ay" }, new String[0], new int[] { 1, 2, 3, 4, 5 }, new double[] { 5.0, 4.0, 5.0, 4.0, 1.0 }, 0.25, 0.3, 0.0, 0.08, null, true);
        HINDI_ROMANIZED = new Finnegan(new String[] { "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "u", "\u016b", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "u", "\u016b", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "u", "\u016b", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a", "a", "a", "a", "a", "a", "\u0101", "\u0101", "i", "i", "i", "i", "\u012b", "i", "i", "\u012b", "\u012b", "u", "u", "u", "\u016b", "u", "\u016b", "u", "\u016b", "e", "ai", "ai", "o", "o", "o", "au", "a\u1e43", "a\u1e43", "a\u1e43", "a\u1e43", "a\u1e43", "\u0101\u1e43", "\u0101\u1e43", "i\u1e43", "i\u1e43", "i\u1e43", "\u012b\u1e43", "\u012b\u1e43", "u\u1e43", "u\u1e43", "\u016b\u1e43", "ai\u1e43", "ai\u1e43", "o\u1e43", "o\u1e43", "o\u1e43", "au\u1e43" }, new String[] { "a'", "i'", "u'", "o'", "a'", "i'", "u'", "o'" }, new String[] { "k", "k", "k", "k", "k", "k", "k", "k", "k\u1e5b", "k\u1e5d", "k\u1e37", "c", "c", "c", "c", "c", "c", "c\u1e5b", "c\u1e5d", "c\u1e37", "\u1e6d", "t", "t", "t", "t", "t", "t", "t", "t", "t", "t\u1e5b", "t\u1e5d", "t\u1e5b", "t\u1e5d", "p", "p", "p", "p", "p", "p", "p", "p", "p", "p", "p\u1e5b", "p\u1e5d", "p\u1e37", "p\u1e39", "p\u1e5b", "p\u1e5d", "p", "p", "kh", "kh", "kh", "kh", "kh", "kh", "kh", "kh", "kh", "kh", "kh\u1e5b", "kh\u1e5d", "kh\u1e37", "kh\u1e39", "ch", "ch", "ch", "ch", "ch", "ch", "ch", "ch", "ch", "ch\u1e5b", "ch\u1e5d", "ch\u1e37", "ch\u1e39", "\u1e6dh", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th\u1e5b", "th\u1e5d", "th\u1e37", "th\u1e39", "ph", "ph", "ph", "ph", "ph", "ph", "ph", "ph\u1e5b", "ph\u1e5d", "ph\u1e37", "ph\u1e39", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "jh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "\u1e0dh", "dh", "bh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u015b", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "\u1e0dh", "dh", "bh", "\u1e45", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u1e63", "s", "g", "j", "\u1e0d", "d", "b", "gh", "\u1e0dh", "dh", "bh", "\u1e45", "\u1e47", "n", "m", "h", "y", "r", "l", "v", "\u1e63", "s", "g", "\u1e0d", "d", "b", "gh", "\u1e0dh", "dh", "bh", "n", "m", "v", "s", "g", "\u1e0d", "d", "b", "g", "d", "b", "dh", "bh", "n", "m", "v", "g", "\u1e0d", "d", "b", "g", "d", "b", "dh", "bh", "n", "m", "v" }, new String[] { "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k", "k", "k", "k", "k", "nk", "rk", "k\u1e5b", "k\u1e5b", "k\u1e5b", "k\u1e5b", "k\u1e5b", "nk\u1e5b", "rk\u1e5b", "k\u1e5d", "k\u1e5d", "k\u1e5d", "k\u1e5d", "k\u1e5d", "nk\u1e5d", "rk\u1e5d", "k\u1e37", "k\u1e37", "k\u1e37", "k\u1e37", "k\u1e37", "nk\u1e37", "rk\u1e37", "c", "c", "c", "c", "c", "c", "c\u1e5b", "c\u1e5d", "c\u1e37", "\u1e6d", "t", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "\u1e6d", "t", "t", "t", "t", "nt", "rt", "t\u1e5b", "t\u1e5b", "t\u1e5b", "t\u1e5b", "t\u1e5b", "nt\u1e5b", "rt\u1e5b", "t\u1e5d", "t\u1e5d", "t\u1e5d", "t\u1e5d", "t\u1e5d", "nt\u1e5d", "rt\u1e5d", "t\u1e5b", "t\u1e5b", "t\u1e5b", "t\u1e5b", "t\u1e5b", "nt\u1e5b", "rt\u1e5b", "t\u1e5d", "t\u1e5d", "t\u1e5d", "t\u1e5d", "t\u1e5d", "nt\u1e5d", "rt\u1e5d", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "p\u1e5b", "p\u1e5b", "p\u1e5b", "p\u1e5b", "p\u1e5b", "np\u1e5b", "rp\u1e5b", "p\u1e5d", "p\u1e5d", "p\u1e5d", "p\u1e5d", "p\u1e5d", "np\u1e5d", "rp\u1e5d", "p\u1e37", "p\u1e37", "p\u1e37", "p\u1e37", "p\u1e37", "np\u1e37", "rp\u1e37", "p\u1e39", "p\u1e39", "p\u1e39", "p\u1e39", "p\u1e39", "np\u1e39", "rp\u1e39", "p\u1e5b", "p\u1e5b", "p\u1e5b", "p\u1e5b", "p\u1e5b", "np\u1e5b", "rp\u1e5b", "p\u1e5d", "p\u1e5d", "p\u1e5d", "p\u1e5d", "p\u1e5d", "np\u1e5d", "rp\u1e5d", "p", "p", "p", "p", "p", "np", "rp", "p", "p", "p", "p", "p", "np", "rp", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh", "kh", "kh", "kh", "kh", "nkh", "rkh", "kh\u1e5b", "kh\u1e5b", "kh\u1e5b", "kh\u1e5b", "kh\u1e5b", "nkh\u1e5b", "rkh\u1e5b", "kh\u1e5d", "kh\u1e5d", "kh\u1e5d", "kh\u1e5d", "kh\u1e5d", "nkh\u1e5d", "rkh\u1e5d", "kh\u1e37", "kh\u1e37", "kh\u1e37", "kh\u1e37", "kh\u1e37", "nkh\u1e37", "rkh\u1e37", "kh\u1e39", "kh\u1e39", "kh\u1e39", "kh\u1e39", "kh\u1e39", "nkh\u1e39", "rkh\u1e39", "ch", "ch", "ch", "ch", "ch", "ch", "ch", "ch", "ch", "ch\u1e5b", "ch\u1e5d", "ch\u1e37", "ch\u1e39", "\u1e6dh", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th", "th", "th", "th", "th", "nth", "rth", "th\u1e5b", "th\u1e5b", "th\u1e5b", "th\u1e5b", "th\u1e5b", "nth\u1e5b", "rth\u1e5b", "th\u1e5d", "th\u1e5d", "th\u1e5d", "th\u1e5d", "th\u1e5d", "nth\u1e5d", "rth\u1e5d", "th\u1e37", "th\u1e37", "th\u1e37", "th\u1e37", "th\u1e37", "nth\u1e37", "rth\u1e37", "th\u1e39", "th\u1e39", "th\u1e39", "th\u1e39", "th\u1e39", "nth\u1e39", "rth\u1e39", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph", "ph", "ph", "ph", "ph", "nph", "rph", "ph\u1e5b", "ph\u1e5b", "ph\u1e5b", "ph\u1e5b", "ph\u1e5b", "nph\u1e5b", "rph\u1e5b", "ph\u1e5d", "ph\u1e5d", "ph\u1e5d", "ph\u1e5d", "ph\u1e5d", "nph\u1e5d", "rph\u1e5d", "ph\u1e37", "ph\u1e37", "ph\u1e37", "ph\u1e37", "ph\u1e37", "nph\u1e37", "rph\u1e37", "ph\u1e39", "ph\u1e39", "ph\u1e39", "ph\u1e39", "ph\u1e39", "nph\u1e39", "rph\u1e39", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "jh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u00f1", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u015b", "\u015b", "\u015b", "\u015b", "\u015b", "n\u015b", "r\u015b", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "j", "j", "j", "j", "j", "nj", "rj", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "\u1e45", "\u1e47", "n", "m", "m", "m", "m", "m", "nm", "rm", "h", "y", "y", "y", "y", "y", "ny", "ry", "r", "l", "v", "v", "v", "v", "v", "nv", "rv", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "\u1e63", "n\u1e63", "r\u1e63", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "gh", "gh", "gh", "gh", "gh", "ngh", "rgh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "\u1e0dh", "n\u1e0dh", "r\u1e0dh", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "n", "m", "m", "m", "m", "m", "nm", "rm", "v", "v", "v", "v", "v", "nv", "rv", "s", "s", "s", "s", "s", "ns", "rs", "g", "g", "g", "g", "g", "ng", "rg", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "g", "g", "g", "g", "g", "ng", "rg", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "n", "m", "m", "m", "m", "m", "nm", "rm", "v", "v", "v", "v", "v", "nv", "rv", "g", "g", "g", "g", "g", "ng", "rg", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "\u1e0d", "n\u1e0d", "r\u1e0d", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "g", "g", "g", "g", "g", "ng", "rg", "d", "d", "d", "d", "d", "nd", "rd", "b", "b", "b", "b", "b", "nb", "rb", "dh", "dh", "dh", "dh", "dh", "ndh", "rdh", "bh", "bh", "bh", "bh", "bh", "nbh", "rbh", "n", "m", "m", "m", "m", "m", "nm", "rm", "v", "v", "v", "v", "v", "nv", "rv" }, new String[] { "t", "d", "m", "r", "dh", "b", "t", "d", "m", "r", "dh", "bh", "nt", "nt", "nk", "\u1e63" }, new String[] { "it", "it", "ati", "adva", "a\u1e63", "arma", "ardha", "abi", "ab", "aya" }, new String[0], new int[] { 1, 2, 3, 4, 5 }, new double[] { 1.0, 2.0, 3.0, 3.0, 1.0 }, 0.15, 0.75, 0.0, 0.12, null, true).addModifiers(Modifier.replacementTable("\u1e5b\u1e5d\u1e37\u1e39\u1e0d\u1e6d\u1e45\u1e47\u1e63\u1e43\u1e25", "\u0157\u0155\u013c\u013a\u0111\u0163\u0144\u0146\u015f\u0115\u012d"));
        FANTASY_NAME = Finnegan.GREEK_ROMANIZED.mix(Finnegan.RUSSIAN_ROMANIZED.mix(Finnegan.FRENCH.removeAccents().mix(Finnegan.JAPANESE_ROMANIZED, 0.5), 0.85), 0.925);
        FANCY_FANTASY_NAME = Finnegan.FANTASY_NAME.addAccents(0.47, 0.07);
    }
    
    public String removeAccents(final CharSequence str) {
        String alteredString = Normalizer.normalize(str, Normalizer.Form.NFD);
        alteredString = Finnegan.diacritics.matcher(alteredString).replaceAll("").replace('\u00e6', 'a').replace('\u0153', 'o').replace('\u00f8', 'o').replace('\u00c6', 'A').replace('\u0152', 'O').replace('\u00d8', 'O');
        return alteredString;
    }
    
    public Finnegan() {
        this(new String[] { "a", "a", "a", "a", "o", "o", "o", "e", "e", "e", "e", "e", "i", "i", "i", "i", "u", "a", "a", "a", "a", "o", "o", "o", "e", "e", "e", "e", "e", "i", "i", "i", "i", "u", "a", "a", "a", "o", "o", "e", "e", "e", "i", "i", "i", "u", "a", "a", "a", "o", "o", "e", "e", "e", "i", "i", "i", "u", "au", "ai", "ai", "ou", "ea", "ie", "io", "ei" }, new String[] { "u", "u", "oa", "oo", "oo", "oo", "ee", "ee", "ee", "ee" }, new String[] { "b", "bl", "br", "c", "cl", "cr", "ch", "d", "dr", "f", "fl", "fr", "g", "gl", "gr", "h", "j", "k", "l", "m", "n", "p", "pl", "pr", "qu", "r", "s", "sh", "sk", "st", "sp", "sl", "sm", "sn", "t", "tr", "th", "thr", "v", "w", "y", "z", "b", "bl", "br", "c", "cl", "cr", "ch", "d", "dr", "f", "fl", "fr", "g", "gr", "h", "j", "k", "l", "m", "n", "p", "pl", "pr", "r", "s", "sh", "st", "sp", "sl", "t", "tr", "th", "w", "y", "b", "br", "c", "ch", "d", "dr", "f", "g", "h", "j", "l", "m", "n", "p", "r", "s", "sh", "st", "sl", "t", "tr", "th", "b", "d", "f", "g", "h", "l", "m", "n", "p", "r", "s", "sh", "t", "th", "b", "d", "f", "g", "h", "l", "m", "n", "p", "r", "s", "sh", "t", "th", "r", "s", "t", "l", "n", "str", "spr", "spl", "wr", "kn", "kn", "gn" }, new String[] { "x", "cst", "bs", "ff", "lg", "g", "gs", "ll", "ltr", "mb", "mn", "mm", "ng", "ng", "ngl", "nt", "ns", "nn", "ps", "mbl", "mpr", "pp", "ppl", "ppr", "rr", "rr", "rr", "rl", "rtn", "ngr", "ss", "sc", "rst", "tt", "tt", "ts", "ltr", "zz" }, new String[] { "b", "rb", "bb", "c", "rc", "ld", "d", "ds", "dd", "f", "ff", "lf", "rf", "rg", "gs", "ch", "lch", "rch", "tch", "ck", "ck", "lk", "rk", "l", "ll", "lm", "m", "rm", "mp", "n", "nk", "nch", "nd", "ng", "ng", "nt", "ns", "lp", "rp", "p", "r", "rn", "rts", "s", "s", "s", "s", "ss", "ss", "st", "ls", "t", "t", "ts", "w", "wn", "x", "ly", "lly", "z", "b", "c", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "w" }, new String[] { "ate", "ite", "ism", "ist", "er", "er", "er", "ed", "ed", "ed", "es", "es", "ied", "y", "y", "y", "y", "ate", "ite", "ism", "ist", "er", "er", "er", "ed", "ed", "ed", "es", "es", "ied", "y", "y", "y", "y", "ate", "ite", "ism", "ist", "er", "er", "er", "ed", "ed", "ed", "es", "es", "ied", "y", "y", "y", "y", "ay", "ay", "ey", "oy", "ay", "ay", "ey", "oy", "ough", "aught", "ant", "ont", "oe", "ance", "ell", "eal", "oa", "urt", "ut", "iom", "ion", "ion", "ision", "ation", "ation", "ition", "ough", "aught", "ant", "ont", "oe", "ance", "ell", "eal", "oa", "urt", "ut", "iom", "ion", "ion", "ision", "ation", "ation", "ition", "ily", "ily", "ily", "adly", "owly", "oorly", "ardly", "iedly" }, new String[0], new int[] { 1, 2, 3, 4 }, new double[] { 7.0, 8.0, 4.0, 1.0 }, 0.22, 0.1, 0.0, 0.25, Finnegan.englishSanityChecks, true);
    }
    
    public Finnegan(final String[] openingVowels, final String[] midVowels, final String[] openingConsonants, final String[] midConsonants, final String[] closingConsonants, final String[] closingSyllables, final String[] vowelSplitters, final int[] syllableLengths, final double[] syllableFrequencies, final double vowelStartFrequency, final double vowelEndFrequency, final double vowelSplitFrequency, final double syllableEndFrequency) {
        this(openingVowels, midVowels, openingConsonants, midConsonants, closingConsonants, closingSyllables, vowelSplitters, syllableLengths, syllableFrequencies, vowelStartFrequency, vowelEndFrequency, vowelSplitFrequency, syllableEndFrequency, Finnegan.englishSanityChecks, true);
    }
    
    public Finnegan(final String[] openingVowels, final String[] midVowels, final String[] openingConsonants, final String[] midConsonants, final String[] closingConsonants, final String[] closingSyllables, final String[] vowelSplitters, final int[] syllableLengths, final double[] syllableFrequencies, final double vowelStartFrequency, final double vowelEndFrequency, final double vowelSplitFrequency, final double syllableEndFrequency, final Pattern[] sane, final boolean clean) {
        this.totalSyllableFrequency = 0.0;
        this.rng = new RNG(hash64(new String[][] { openingVowels, midVowels, openingConsonants, midConsonants, closingConsonants, closingSyllables, vowelSplitters }) ^ hash64(syllableLengths) ^ hash64(syllableFrequencies) << 31 ^ Double.doubleToLongBits(vowelStartFrequency + 19.0 * (vowelEndFrequency + 19.0 * (vowelSplitFrequency + 19.0 * syllableEndFrequency))));
        this.openingVowels = openingVowels;
        System.arraycopy(midVowels, 0, this.midVowels = new String[openingVowels.length + midVowels.length], 0, midVowels.length);
        System.arraycopy(openingVowels, 0, this.midVowels, midVowels.length, openingVowels.length);
        this.openingConsonants = openingConsonants;
        System.arraycopy(midConsonants, 0, this.midConsonants = new String[midConsonants.length + closingConsonants.length], 0, midConsonants.length);
        System.arraycopy(closingConsonants, 0, this.midConsonants, midConsonants.length, closingConsonants.length);
        this.closingConsonants = closingConsonants;
        this.vowelSplitters = vowelSplitters;
        this.closingSyllables = closingSyllables;
        this.syllableFrequencies = new LinkedHashMap<Integer, Double>(syllableLengths.length);
        for (int i = 0; i < syllableLengths.length && i < syllableFrequencies.length; ++i) {
            this.syllableFrequencies.put(syllableLengths[i], syllableFrequencies[i]);
        }
        for (final Double freq : this.syllableFrequencies.values()) {
            this.totalSyllableFrequency += freq;
        }
        if (vowelStartFrequency > 1.0) {
            this.vowelStartFrequency = 1.0 / vowelStartFrequency;
        }
        else {
            this.vowelStartFrequency = vowelStartFrequency;
        }
        if (vowelEndFrequency > 1.0) {
            this.vowelEndFrequency = 1.0 / vowelEndFrequency;
        }
        else {
            this.vowelEndFrequency = vowelEndFrequency;
        }
        if (vowelSplitters.length == 0) {
            this.vowelSplitFrequency = 0.0;
        }
        else if (vowelSplitFrequency > 1.0) {
            this.vowelSplitFrequency = 1.0 / vowelSplitFrequency;
        }
        else {
            this.vowelSplitFrequency = vowelSplitFrequency;
        }
        if (closingSyllables.length == 0) {
            this.syllableEndFrequency = 0.0;
        }
        else if (syllableEndFrequency > 1.0) {
            this.syllableEndFrequency = 1.0 / syllableEndFrequency;
        }
        else {
            this.syllableEndFrequency = syllableEndFrequency;
        }
        this.clean = clean;
        this.sanityChecks = sane;
        this.modifiers = new ArrayList<Modifier>(16);
    }
    
    private Finnegan(final String[] openingVowels, final String[] midVowels, final String[] openingConsonants, final String[] midConsonants, final String[] closingConsonants, final String[] closingSyllables, final String[] vowelSplitters, final LinkedHashMap<Integer, Double> syllableFrequencies, final double vowelStartFrequency, final double vowelEndFrequency, final double vowelSplitFrequency, final double syllableEndFrequency, final Pattern[] sanityChecks, final boolean clean, final RNG rng, final Collection<Modifier> modifiers) {
        this.totalSyllableFrequency = 0.0;
        this.openingVowels = copyStrings(openingVowels);
        this.midVowels = copyStrings(midVowels);
        this.openingConsonants = copyStrings(openingConsonants);
        this.midConsonants = copyStrings(midConsonants);
        this.closingConsonants = copyStrings(closingConsonants);
        this.closingSyllables = copyStrings(closingSyllables);
        this.vowelSplitters = copyStrings(vowelSplitters);
        this.syllableFrequencies = new LinkedHashMap<Integer, Double>(syllableFrequencies);
        this.vowelStartFrequency = vowelStartFrequency;
        this.vowelEndFrequency = vowelEndFrequency;
        this.vowelSplitFrequency = vowelSplitFrequency;
        this.syllableEndFrequency = syllableEndFrequency;
        for (final Double freq : this.syllableFrequencies.values()) {
            this.totalSyllableFrequency += freq;
        }
        if (sanityChecks == null) {
            this.sanityChecks = null;
        }
        else {
            System.arraycopy(sanityChecks, 0, this.sanityChecks = new Pattern[sanityChecks.length], 0, sanityChecks.length);
        }
        this.clean = clean;
        this.rng = new RNG(rng.state);
        this.modifiers = new ArrayList<Modifier>(modifiers);
    }
    
    protected boolean checkAll(final CharSequence testing, final Pattern[] checks) {
        final String fixed = this.removeAccents(testing);
        for (int i = 0; i < checks.length; ++i) {
            if (checks[i].matcher(fixed).find()) {
                return false;
            }
        }
        return true;
    }
    
    public String word(final boolean capitalize) {
        return this.word(this.rng.state, capitalize);
    }
    
    public String word(final long seed, final boolean capitalize) {
        this.rng.state = seed;
        StringBuffer sb;
        while (true) {
            sb = new StringBuffer(20);
            double syllableChance = this.rng.nextDouble(this.totalSyllableFrequency);
            int syllables = 1;
            int i = 0;
            for (final Map.Entry<Integer, Double> kv : this.syllableFrequencies.entrySet()) {
                if (syllableChance < kv.getValue()) {
                    syllables = kv.getKey();
                    break;
                }
                syllableChance -= kv.getValue();
            }
            if (this.rng.nextDouble() < this.vowelStartFrequency) {
                sb.append(this.rng.getRandomElement(this.openingVowels));
                sb.append(this.rng.getRandomElement(this.midConsonants));
                ++i;
            }
            else {
                sb.append(this.rng.getRandomElement(this.openingConsonants));
            }
            while (i < syllables - 1) {
                sb.append(this.rng.getRandomElement(this.midVowels));
                if (this.rng.nextDouble() < this.vowelSplitFrequency) {
                    sb.append(this.rng.getRandomElement(this.vowelSplitters));
                    sb.append(this.rng.getRandomElement(this.midVowels));
                }
                sb.append(this.rng.getRandomElement(this.midConsonants));
                ++i;
            }
            if (this.rng.nextDouble() < this.syllableEndFrequency) {
                final String close = this.rng.getRandomElement(this.closingSyllables);
                if ((close.contains("@1") && syllables == 1) || (close.contains("@2") && syllables == 2) || (close.contains("@3") && syllables == 3)) {
                    sb.append(close.replaceAll("@\\d", sb.toString()));
                }
                else if (!close.contains("@")) {
                    sb.append(close);
                }
                else if (this.rng.nextDouble() < this.vowelEndFrequency) {
                    sb.append(this.rng.getRandomElement(this.midVowels));
                    if (this.rng.nextDouble() < this.vowelSplitFrequency) {
                        sb.append(this.rng.getRandomElement(this.vowelSplitters));
                        sb.append(this.rng.getRandomElement(this.midVowels));
                    }
                }
            }
            else {
                sb.append(this.rng.getRandomElement(this.midVowels));
                if (this.rng.nextDouble() < this.vowelSplitFrequency) {
                    sb.append(this.rng.getRandomElement(this.vowelSplitters));
                    sb.append(this.rng.getRandomElement(this.midVowels));
                }
                if (this.rng.nextDouble() >= this.vowelEndFrequency) {
                    sb.append(this.rng.getRandomElement(this.closingConsonants));
                    if (this.rng.nextDouble() < this.syllableEndFrequency) {
                        final String close = this.rng.getRandomElement(this.closingSyllables);
                        if ((close.contains("@1") && syllables == 1) || (close.contains("@2") && syllables == 2) || (close.contains("@3") && syllables == 3)) {
                            sb.append(close.replaceAll("@\\d", sb.toString()));
                        }
                        else if (!close.contains("@")) {
                            sb.append(close);
                        }
                    }
                }
            }
            if (this.sanityChecks != null && !this.checkAll(sb, this.sanityChecks)) {
                continue;
            }
            for (final Modifier mod : this.modifiers) {
                sb = mod.modify(this.rng, sb);
            }
            if (capitalize) {
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
            }
            if (this.clean && !this.checkAll(sb, Finnegan.vulgarChecks)) {
                continue;
            }
            break;
        }
        return sb.toString();
    }
    
    public String word(final boolean capitalize, final int approxSyllables) {
        return this.word(this.rng.state, capitalize, approxSyllables);
    }
    
    public String word(final long seed, final boolean capitalize, final int approxSyllables) {
        this.rng.setState(seed);
        if (approxSyllables > 0) {
            StringBuffer sb;
            while (true) {
                sb = new StringBuffer(20);
                int i = 0;
                if (this.rng.nextDouble() < this.vowelStartFrequency) {
                    sb.append(this.rng.getRandomElement(this.openingVowels));
                    sb.append(this.rng.getRandomElement(this.midConsonants));
                    ++i;
                }
                else {
                    sb.append(this.rng.getRandomElement(this.openingConsonants));
                }
                while (i < approxSyllables - 1) {
                    sb.append(this.rng.getRandomElement(this.midVowels));
                    if (this.rng.nextDouble() < this.vowelSplitFrequency) {
                        sb.append(this.rng.getRandomElement(this.vowelSplitters));
                        sb.append(this.rng.getRandomElement(this.midVowels));
                    }
                    sb.append(this.rng.getRandomElement(this.midConsonants));
                    ++i;
                }
                if (this.rng.nextDouble() < this.syllableEndFrequency) {
                    final String close = this.rng.getRandomElement(this.closingSyllables);
                    if ((close.contains("@1") && approxSyllables == 1) || (close.contains("@2") && approxSyllables == 2) || (close.contains("@3") && approxSyllables == 3)) {
                        sb.append(close.replaceAll("@\\d", sb.toString()));
                    }
                    else if (!close.contains("@")) {
                        sb.append(close);
                    }
                    else if (this.rng.nextDouble() < this.vowelEndFrequency) {
                        sb.append(this.rng.getRandomElement(this.midVowels));
                        if (this.rng.nextDouble() < this.vowelSplitFrequency) {
                            sb.append(this.rng.getRandomElement(this.vowelSplitters));
                            sb.append(this.rng.getRandomElement(this.midVowels));
                        }
                    }
                }
                else {
                    sb.append(this.rng.getRandomElement(this.midVowels));
                    if (this.rng.nextDouble() < this.vowelSplitFrequency) {
                        sb.append(this.rng.getRandomElement(this.vowelSplitters));
                        sb.append(this.rng.getRandomElement(this.midVowels));
                    }
                    if (this.rng.nextDouble() >= this.vowelEndFrequency) {
                        sb.append(this.rng.getRandomElement(this.closingConsonants));
                        if (this.rng.nextDouble() < this.syllableEndFrequency) {
                            String close = this.rng.getRandomElement(this.closingSyllables);
                            if ((close.contains("@1") && approxSyllables == 1) || (close.contains("@2") && approxSyllables == 2) || (close.contains("@3") && approxSyllables == 3)) {
                                close = close.replaceAll("@\\d", sb.toString());
                                sb.append(close);
                            }
                            else if (!close.contains("@")) {
                                sb.append(close);
                            }
                        }
                    }
                }
                if (this.sanityChecks != null && !this.checkAll(sb, this.sanityChecks)) {
                    continue;
                }
                for (final Modifier mod : this.modifiers) {
                    sb = mod.modify(this.rng, sb);
                }
                if (capitalize) {
                    sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                }
                if (this.clean && !this.checkAll(sb, Finnegan.vulgarChecks)) {
                    continue;
                }
                break;
            }
            return sb.toString();
        }
        final String finished = this.rng.getRandomElement(this.openingVowels);
        if (capitalize) {
            return finished.substring(0, 1).toUpperCase();
        }
        return finished.substring(0, 1);
    }
    
    public String sentence(final int minWords, final int maxWords) {
        return this.sentence(this.rng.state, minWords, maxWords, new String[] { ",", ",", ",", ";" }, new String[] { ".", ".", ".", "!", "?", "..." }, 0.2);
    }
    
    public String sentence(final int minWords, final int maxWords, final String[] midPunctuation, final String[] endPunctuation, final double midPunctuationFrequency) {
        return this.sentence(this.rng.state, minWords, maxWords, midPunctuation, endPunctuation, midPunctuationFrequency);
    }
    
    public String sentence(final long seed, int minWords, int maxWords, final String[] midPunctuation, final String[] endPunctuation, double midPunctuationFrequency) {
        this.rng.state = seed;
        if (minWords < 1) {
            minWords = 1;
        }
        if (minWords > maxWords) {
            maxWords = minWords;
        }
        if (midPunctuationFrequency > 1.0) {
            midPunctuationFrequency = 1.0 / midPunctuationFrequency;
        }
        final StringBuilder sb = new StringBuilder(12 * maxWords);
        sb.append(this.word(true));
        for (int i = 1; i < minWords; ++i) {
            if (this.rng.nextDouble() < midPunctuationFrequency) {
                sb.append(this.rng.getRandomElement(midPunctuation));
            }
            sb.append(' ');
            sb.append(this.word(false));
        }
        for (int i = minWords; i < maxWords && this.rng.nextInt(2 * maxWords) > i; ++i) {
            if (this.rng.nextDouble() < midPunctuationFrequency) {
                sb.append(this.rng.getRandomElement(midPunctuation));
            }
            sb.append(' ');
            sb.append(this.word(false));
        }
        sb.append(this.rng.getRandomElement(endPunctuation));
        return sb.toString();
    }
    
    public String sentence(final int minWords, final int maxWords, final String[] midPunctuation, final String[] endPunctuation, final double midPunctuationFrequency, final int maxChars) {
        return this.sentence(this.rng.state, minWords, maxWords, midPunctuation, endPunctuation, midPunctuationFrequency, maxChars);
    }
    
    public String sentence(final long seed, int minWords, int maxWords, final String[] midPunctuation, final String[] endPunctuation, double midPunctuationFrequency, final int maxChars) {
        this.rng.state = seed;
        if (minWords < 1) {
            minWords = 1;
        }
        if (minWords > maxWords) {
            maxWords = minWords;
        }
        if (midPunctuationFrequency > 1.0) {
            midPunctuationFrequency = 1.0 / midPunctuationFrequency;
        }
        if (maxChars < 4) {
            return "!";
        }
        if (maxChars <= 5 * minWords) {
            minWords = 1;
            maxWords = 1;
        }
        int frustration = 0;
        final StringBuilder sb = new StringBuilder(maxChars);
        String next;
        for (next = this.word(true); next.length() >= maxChars - 1 && frustration < 50; next = this.word(true), ++frustration) {}
        if (frustration >= 50) {
            return "!";
        }
        sb.append(next);
        for (int i = 1; i < minWords && frustration < 50 && sb.length() < maxChars - 7; ++i) {
            if (this.rng.nextDouble() < midPunctuationFrequency && sb.length() < maxChars - 3) {
                sb.append(this.rng.getRandomElement(midPunctuation));
            }
            for (next = this.word(false); sb.length() + next.length() >= maxChars - 2 && frustration < 50; next = this.word(false), ++frustration) {}
            if (frustration >= 50) {
                break;
            }
            sb.append(' ');
            sb.append(next);
        }
        for (int i = minWords; i < maxWords && sb.length() < maxChars - 7 && this.rng.nextInt(2 * maxWords) > i && frustration < 50; ++i) {
            if (this.rng.nextDouble() < midPunctuationFrequency && sb.length() < maxChars - 3) {
                sb.append(this.rng.getRandomElement(midPunctuation));
            }
            for (next = this.word(false); sb.length() + next.length() >= maxChars - 2 && frustration < 50; next = this.word(false), ++frustration) {}
            if (frustration >= 50) {
                break;
            }
            sb.append(' ');
            sb.append(next);
        }
        next = this.rng.getRandomElement(endPunctuation);
        if (sb.length() + next.length() >= maxChars) {
            next = ".";
        }
        sb.append(next);
        if (sb.length() > maxChars) {
            return "!";
        }
        return sb.toString();
    }
    
    protected String[] merge1000(final String[] me, final String[] other, final double otherInfluence) {
        if (other.length <= 0 && me.length <= 0) {
            return new String[0];
        }
        final String[] ret = new String[1000];
        final int otherCount = (int)(1000.0 * otherInfluence);
        int idx = 0;
        if (other.length > 0) {
            final String[] tmp = this.rng.shuffle(other);
            for (idx = 0; idx < otherCount; ++idx) {
                ret[idx] = tmp[idx % tmp.length];
            }
        }
        if (me.length > 0) {
            final String[] tmp = this.rng.shuffle(me);
            while (idx < 1000) {
                ret[idx] = tmp[idx % tmp.length];
                ++idx;
            }
        }
        else {
            while (idx < 1000) {
                ret[idx] = other[idx % other.length];
                ++idx;
            }
        }
        return ret;
    }
    
    protected String[] accentVowels(final String[] me, final double influence) {
        final String[] ret = new String[1000];
        final int otherCount = (int)(1000.0 * influence);
        int idx = 0;
        if (me.length > 0) {
            final String[] tmp = this.rng.shuffle(me);
            for (idx = 0; idx < otherCount; ++idx) {
                ret[idx] = tmp[idx % tmp.length].replace('a', Finnegan.accentedVowels[0][this.rng.nextInt(Finnegan.accentedVowels[0].length)]).replace('e', Finnegan.accentedVowels[1][this.rng.nextInt(Finnegan.accentedVowels[1].length)]).replace('i', Finnegan.accentedVowels[2][this.rng.nextInt(Finnegan.accentedVowels[2].length)]).replace('o', Finnegan.accentedVowels[3][this.rng.nextInt(Finnegan.accentedVowels[3].length)]).replace('u', Finnegan.accentedVowels[4][this.rng.nextInt(Finnegan.accentedVowels[4].length)]);
                final Matcher matcher = Finnegan.repeats.matcher(ret[idx]);
                if (matcher.find()) {
                    ret[idx] = matcher.replaceAll(this.rng.getRandomElement(me));
                }
            }
            while (idx < 1000) {
                ret[idx] = tmp[idx % tmp.length];
                ++idx;
            }
            return ret;
        }
        return new String[0];
    }
    
    protected String[] accentConsonants(final String[] me, final double influence) {
        final String[] ret = new String[1000];
        final int otherCount = (int)(1000.0 * influence);
        int idx = 0;
        if (me.length > 0) {
            final String[] tmp = this.rng.shuffle(me);
            for (idx = 0; idx < otherCount; ++idx) {
                ret[idx] = tmp[idx % tmp.length].replace('c', Finnegan.accentedConsonants[1][this.rng.nextInt(Finnegan.accentedConsonants[1].length)]).replace('d', Finnegan.accentedConsonants[2][this.rng.nextInt(Finnegan.accentedConsonants[2].length)]).replace('f', Finnegan.accentedConsonants[3][this.rng.nextInt(Finnegan.accentedConsonants[3].length)]).replace('g', Finnegan.accentedConsonants[4][this.rng.nextInt(Finnegan.accentedConsonants[4].length)]).replace('h', Finnegan.accentedConsonants[5][this.rng.nextInt(Finnegan.accentedConsonants[5].length)]).replace('j', Finnegan.accentedConsonants[6][this.rng.nextInt(Finnegan.accentedConsonants[6].length)]).replace('k', Finnegan.accentedConsonants[7][this.rng.nextInt(Finnegan.accentedConsonants[7].length)]).replace('l', Finnegan.accentedConsonants[8][this.rng.nextInt(Finnegan.accentedConsonants[8].length)]).replace('n', Finnegan.accentedConsonants[10][this.rng.nextInt(Finnegan.accentedConsonants[10].length)]).replace('r', Finnegan.accentedConsonants[13][this.rng.nextInt(Finnegan.accentedConsonants[13].length)]).replace('s', Finnegan.accentedConsonants[14][this.rng.nextInt(Finnegan.accentedConsonants[14].length)]).replace('t', Finnegan.accentedConsonants[15][this.rng.nextInt(Finnegan.accentedConsonants[15].length)]).replace('w', Finnegan.accentedConsonants[17][this.rng.nextInt(Finnegan.accentedConsonants[17].length)]).replace('y', Finnegan.accentedConsonants[19][this.rng.nextInt(Finnegan.accentedConsonants[19].length)]).replace('z', Finnegan.accentedConsonants[20][this.rng.nextInt(Finnegan.accentedConsonants[20].length)]);
                final Matcher matcher = Finnegan.repeats.matcher(ret[idx]);
                if (matcher.find()) {
                    ret[idx] = matcher.replaceAll(this.rng.getRandomElement(me));
                }
            }
            while (idx < 1000) {
                ret[idx] = tmp[idx % tmp.length];
                ++idx;
            }
            return ret;
        }
        return new String[0];
    }
    
    protected String[] accentBoth(final String[] me, final double vowelInfluence, final double consonantInfluence) {
        final String[] ret = new String[1000];
        int idx = 0;
        if (me.length > 0) {
            final String[] tmp = this.rng.shuffle(me);
            for (idx = 0; idx < 1000; ++idx) {
                final boolean subVowel = this.rng.nextDouble() < vowelInfluence;
                final boolean subCon = this.rng.nextDouble() < consonantInfluence;
                if (subVowel && subCon) {
                    ret[idx] = tmp[idx % tmp.length].replace('a', Finnegan.accentedVowels[0][this.rng.nextInt(Finnegan.accentedVowels[0].length)]).replace('e', Finnegan.accentedVowels[1][this.rng.nextInt(Finnegan.accentedVowels[1].length)]).replace('i', Finnegan.accentedVowels[2][this.rng.nextInt(Finnegan.accentedVowels[2].length)]).replace('o', Finnegan.accentedVowels[3][this.rng.nextInt(Finnegan.accentedVowels[3].length)]).replace('u', Finnegan.accentedVowels[4][this.rng.nextInt(Finnegan.accentedVowels[4].length)]).replace('c', Finnegan.accentedConsonants[1][this.rng.nextInt(Finnegan.accentedConsonants[1].length)]).replace('d', Finnegan.accentedConsonants[2][this.rng.nextInt(Finnegan.accentedConsonants[2].length)]).replace('f', Finnegan.accentedConsonants[3][this.rng.nextInt(Finnegan.accentedConsonants[3].length)]).replace('g', Finnegan.accentedConsonants[4][this.rng.nextInt(Finnegan.accentedConsonants[4].length)]).replace('h', Finnegan.accentedConsonants[5][this.rng.nextInt(Finnegan.accentedConsonants[5].length)]).replace('j', Finnegan.accentedConsonants[6][this.rng.nextInt(Finnegan.accentedConsonants[6].length)]).replace('k', Finnegan.accentedConsonants[7][this.rng.nextInt(Finnegan.accentedConsonants[7].length)]).replace('l', Finnegan.accentedConsonants[8][this.rng.nextInt(Finnegan.accentedConsonants[8].length)]).replace('n', Finnegan.accentedConsonants[10][this.rng.nextInt(Finnegan.accentedConsonants[10].length)]).replace('r', Finnegan.accentedConsonants[13][this.rng.nextInt(Finnegan.accentedConsonants[13].length)]).replace('s', Finnegan.accentedConsonants[14][this.rng.nextInt(Finnegan.accentedConsonants[14].length)]).replace('t', Finnegan.accentedConsonants[15][this.rng.nextInt(Finnegan.accentedConsonants[15].length)]).replace('w', Finnegan.accentedConsonants[17][this.rng.nextInt(Finnegan.accentedConsonants[17].length)]).replace('y', Finnegan.accentedConsonants[19][this.rng.nextInt(Finnegan.accentedConsonants[19].length)]).replace('z', Finnegan.accentedConsonants[20][this.rng.nextInt(Finnegan.accentedConsonants[20].length)]);
                    final Matcher matcher = Finnegan.repeats.matcher(ret[idx]);
                    if (matcher.find()) {
                        ret[idx] = matcher.replaceAll(this.rng.getRandomElement(me));
                    }
                }
                else if (subVowel) {
                    ret[idx] = tmp[idx % tmp.length].replace('a', Finnegan.accentedVowels[0][this.rng.nextInt(Finnegan.accentedVowels[0].length)]).replace('e', Finnegan.accentedVowels[1][this.rng.nextInt(Finnegan.accentedVowels[1].length)]).replace('i', Finnegan.accentedVowels[2][this.rng.nextInt(Finnegan.accentedVowels[2].length)]).replace('o', Finnegan.accentedVowels[3][this.rng.nextInt(Finnegan.accentedVowels[3].length)]).replace('u', Finnegan.accentedVowels[4][this.rng.nextInt(Finnegan.accentedVowels[4].length)]);
                    final Matcher matcher = Finnegan.repeats.matcher(ret[idx]);
                    if (matcher.find()) {
                        ret[idx] = matcher.replaceAll(this.rng.getRandomElement(me));
                    }
                }
                else if (subCon) {
                    ret[idx] = tmp[idx % tmp.length].replace('c', Finnegan.accentedConsonants[1][this.rng.nextInt(Finnegan.accentedConsonants[1].length)]).replace('d', Finnegan.accentedConsonants[2][this.rng.nextInt(Finnegan.accentedConsonants[2].length)]).replace('f', Finnegan.accentedConsonants[3][this.rng.nextInt(Finnegan.accentedConsonants[3].length)]).replace('g', Finnegan.accentedConsonants[4][this.rng.nextInt(Finnegan.accentedConsonants[4].length)]).replace('h', Finnegan.accentedConsonants[5][this.rng.nextInt(Finnegan.accentedConsonants[5].length)]).replace('j', Finnegan.accentedConsonants[6][this.rng.nextInt(Finnegan.accentedConsonants[6].length)]).replace('k', Finnegan.accentedConsonants[7][this.rng.nextInt(Finnegan.accentedConsonants[7].length)]).replace('l', Finnegan.accentedConsonants[8][this.rng.nextInt(Finnegan.accentedConsonants[8].length)]).replace('n', Finnegan.accentedConsonants[10][this.rng.nextInt(Finnegan.accentedConsonants[10].length)]).replace('r', Finnegan.accentedConsonants[13][this.rng.nextInt(Finnegan.accentedConsonants[13].length)]).replace('s', Finnegan.accentedConsonants[14][this.rng.nextInt(Finnegan.accentedConsonants[14].length)]).replace('t', Finnegan.accentedConsonants[15][this.rng.nextInt(Finnegan.accentedConsonants[15].length)]).replace('w', Finnegan.accentedConsonants[17][this.rng.nextInt(Finnegan.accentedConsonants[17].length)]).replace('y', Finnegan.accentedConsonants[19][this.rng.nextInt(Finnegan.accentedConsonants[19].length)]).replace('z', Finnegan.accentedConsonants[20][this.rng.nextInt(Finnegan.accentedConsonants[20].length)]);
                    final Matcher matcher = Finnegan.repeats.matcher(ret[idx]);
                    if (matcher.find()) {
                        ret[idx] = matcher.replaceAll(this.rng.getRandomElement(me));
                    }
                }
                else {
                    ret[idx] = tmp[idx % tmp.length];
                }
            }
            return ret;
        }
        return new String[0];
    }
    
    public Finnegan mix(final Finnegan other, double otherInfluence) {
        otherInfluence = Math.max(0.0, Math.min(otherInfluence, 1.0));
        final double myInfluence = 1.0 - otherInfluence;
        final long oldState = this.rng.state;
        this.rng.state = (((long)this.hashCode() & 0xFFFFFFFFL) | (((long)other.hashCode() & 0xFFFFFFFFL) << 32 ^ Double.doubleToLongBits(otherInfluence)));
        final String[] ov = this.merge1000(this.openingVowels, other.openingVowels, otherInfluence);
        final String[] mv = this.merge1000(this.midVowels, other.midVowels, otherInfluence);
        final String[] oc = this.merge1000(this.openingConsonants, other.openingConsonants, otherInfluence);
        final String[] mc = this.merge1000(this.midConsonants, other.midConsonants, otherInfluence);
        final String[] cc = this.merge1000(this.closingConsonants, other.closingConsonants, otherInfluence);
        final String[] cs = this.merge1000(this.closingSyllables, other.closingSyllables, otherInfluence);
        final String[] splitters = this.merge1000(this.vowelSplitters, other.vowelSplitters, otherInfluence);
        final LinkedHashMap<Integer, Double> freqs = new LinkedHashMap<Integer, Double>(this.syllableFrequencies);
        for (final Map.Entry<Integer, Double> kv : other.syllableFrequencies.entrySet()) {
            if (freqs.containsKey(kv.getKey())) {
                freqs.put(kv.getKey(), kv.getValue() + freqs.get(kv.getKey()));
            }
            else {
                freqs.put(kv.getKey(), kv.getValue());
            }
        }
        final List<Modifier> mods = new ArrayList<Modifier>((int)(Math.ceil(this.modifiers.size() * myInfluence) + Math.ceil(other.modifiers.size() * otherInfluence)));
        mods.addAll(this.rng.randomPortion(this.modifiers, (int)Math.ceil(this.modifiers.size() * myInfluence)));
        mods.addAll(this.rng.randomPortion(other.modifiers, (int)Math.ceil(other.modifiers.size() * otherInfluence)));
        final Finnegan finished = new Finnegan(ov, mv, oc, mc, cc, cs, splitters, freqs, this.vowelStartFrequency * myInfluence + other.vowelStartFrequency * otherInfluence, this.vowelEndFrequency * myInfluence + other.vowelEndFrequency * otherInfluence, this.vowelSplitFrequency * myInfluence + other.vowelSplitFrequency * otherInfluence, this.syllableEndFrequency * myInfluence + other.syllableEndFrequency * otherInfluence, (this.sanityChecks == null) ? other.sanityChecks : this.sanityChecks, true, new RNG(this.rng.state), mods);
        this.rng.state = oldState;
        return finished;
    }
    
    public Finnegan addAccents(double vowelInfluence, double consonantInfluence) {
        vowelInfluence = Math.max(0.0, Math.min(vowelInfluence, 1.0));
        consonantInfluence = Math.max(0.0, Math.min(consonantInfluence, 1.0));
        final long oldState = this.rng.state;
        this.rng.state = (((long)this.hashCode() & 0xFFFFFFFFL) ^ ((Double.doubleToLongBits(vowelInfluence) & 0xFFFFFFFFL) | Double.doubleToLongBits(consonantInfluence) << 32));
        final String[] ov = this.accentVowels(this.openingVowels, vowelInfluence);
        final String[] mv = this.accentVowels(this.midVowels, vowelInfluence);
        final String[] oc = this.accentConsonants(this.openingConsonants, consonantInfluence);
        final String[] mc = this.accentConsonants(this.midConsonants, consonantInfluence);
        final String[] cc = this.accentConsonants(this.closingConsonants, consonantInfluence);
        final String[] cs = this.accentBoth(this.closingSyllables, vowelInfluence, consonantInfluence);
        final int[] lens = new int[this.syllableFrequencies.size()];
        final double[] odds = new double[this.syllableFrequencies.size()];
        int i = 0;
        for (final Map.Entry<Integer, Double> kv : this.syllableFrequencies.entrySet()) {
            lens[i] = kv.getKey();
            odds[i++] = kv.getValue();
        }
        final Finnegan finished = new Finnegan(ov, mv, oc, mc, cc, cs, this.vowelSplitters, lens, odds, this.vowelStartFrequency, this.vowelEndFrequency, this.vowelSplitFrequency, this.syllableEndFrequency, this.sanityChecks, this.clean);
        finished.rng.state = this.rng.state;
        this.rng.state = oldState;
        return finished;
    }
    
    static String[] copyStrings(final String[] start) {
        final String[] next = new String[start.length];
        System.arraycopy(start, 0, next, 0, start.length);
        return next;
    }
    
    public Finnegan removeAccents() {
        final String[] ov = this.openingVowels.clone();
        final String[] mv = this.midVowels.clone();
        final String[] oc = this.openingConsonants.clone();
        final String[] mc = this.midConsonants.clone();
        final String[] cc = this.closingConsonants.clone();
        final String[] cs = this.closingSyllables.clone();
        for (int i = 0; i < ov.length; ++i) {
            ov[i] = this.removeAccents(this.openingVowels[i]);
        }
        for (int i = 0; i < mv.length; ++i) {
            mv[i] = this.removeAccents(this.midVowels[i]);
        }
        for (int i = 0; i < oc.length; ++i) {
            oc[i] = this.removeAccents(this.openingConsonants[i]);
        }
        for (int i = 0; i < mc.length; ++i) {
            mc[i] = this.removeAccents(this.midConsonants[i]);
        }
        for (int i = 0; i < cc.length; ++i) {
            cc[i] = this.removeAccents(this.closingConsonants[i]);
        }
        for (int i = 0; i < cs.length; ++i) {
            cs[i] = this.removeAccents(this.closingSyllables[i]);
        }
        final int[] lens = new int[this.syllableFrequencies.size()];
        final double[] odds = new double[this.syllableFrequencies.size()];
        int j = 0;
        for (final Map.Entry<Integer, Double> kv : this.syllableFrequencies.entrySet()) {
            lens[j] = kv.getKey();
            odds[j++] = kv.getValue();
        }
        final Finnegan finished = new Finnegan(ov, mv, oc, mc, cc, cs, this.vowelSplitters, lens, odds, this.vowelStartFrequency, this.vowelEndFrequency, this.vowelSplitFrequency, this.syllableEndFrequency, this.sanityChecks, this.clean);
        finished.rng.state = this.rng.state;
        return finished;
    }
    
    public Finnegan addModifiers(final Collection<Modifier> mods) {
        final Finnegan next = this.copy();
        next.modifiers.addAll(mods);
        return next;
    }
    
    public Finnegan addModifiers(final Modifier... mods) {
        final Finnegan next = this.copy();
        Collections.addAll(next.modifiers, mods);
        return next;
    }
    
    public Finnegan removeModifiers() {
        final Finnegan next = this.copy();
        next.modifiers.clear();
        return next;
    }
    
    public static Modifier modifier(final String pattern, final String replacement) {
        return new Modifier(pattern, replacement);
    }
    
    public static Modifier modifier(final String pattern, final String replacement, final double chance) {
        return new Modifier(pattern, replacement, chance);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final Finnegan finnegan = (Finnegan)o;
        if (this.clean != finnegan.clean) {
            return false;
        }
        if (Double.compare(finnegan.totalSyllableFrequency, this.totalSyllableFrequency) != 0) {
            return false;
        }
        if (Double.compare(finnegan.vowelStartFrequency, this.vowelStartFrequency) != 0) {
            return false;
        }
        if (Double.compare(finnegan.vowelEndFrequency, this.vowelEndFrequency) != 0) {
            return false;
        }
        if (Double.compare(finnegan.vowelSplitFrequency, this.vowelSplitFrequency) != 0) {
            return false;
        }
        if (Double.compare(finnegan.syllableEndFrequency, this.syllableEndFrequency) != 0) {
            return false;
        }
        if (!Arrays.equals(this.openingVowels, finnegan.openingVowels)) {
            return false;
        }
        if (!Arrays.equals(this.midVowels, finnegan.midVowels)) {
            return false;
        }
        if (!Arrays.equals(this.openingConsonants, finnegan.openingConsonants)) {
            return false;
        }
        if (!Arrays.equals(this.midConsonants, finnegan.midConsonants)) {
            return false;
        }
        if (!Arrays.equals(this.closingConsonants, finnegan.closingConsonants)) {
            return false;
        }
        if (!Arrays.equals(this.vowelSplitters, finnegan.vowelSplitters)) {
            return false;
        }
        if (!Arrays.equals(this.closingSyllables, finnegan.closingSyllables)) {
            return false;
        }
        Label_0267: {
            if (this.syllableFrequencies != null) {
                if (this.syllableFrequencies.equals(finnegan.syllableFrequencies)) {
                    break Label_0267;
                }
            }
            else if (finnegan.syllableFrequencies == null) {
                break Label_0267;
            }
            return false;
        }
        if (!Arrays.equals(this.sanityChecks, finnegan.sanityChecks)) {
            return false;
        }
        if (this.rng != null) {
            if (this.rng.equals(finnegan.rng)) {
                return (this.modifiers != null) ? this.modifiers.equals(finnegan.modifiers) : (finnegan.modifiers == null);
            }
        }
        else if (finnegan.rng == null) {
            return (this.modifiers != null) ? this.modifiers.equals(finnegan.modifiers) : (finnegan.modifiers == null);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        long result = hash64(this.openingVowels);
        result = 31L * result + hash64(this.midVowels);
        result = 31L * result + hash64(this.openingConsonants);
        result = 31L * result + hash64(this.midConsonants);
        result = 31L * result + hash64(this.closingConsonants);
        result = 31L * result + hash64(this.vowelSplitters);
        result = 31L * result + hash64(this.closingSyllables);
        result = 31L * result + (this.clean ? 1 : 0);
        result = 31L * result + ((this.syllableFrequencies != null) ? this.syllableFrequencies.hashCode() : 0);
        long temp = Double.doubleToLongBits(this.totalSyllableFrequency);
        result = 31L * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.vowelStartFrequency);
        result = 31L * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.vowelEndFrequency);
        result = 31L * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.vowelSplitFrequency);
        result = 31L * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.syllableEndFrequency);
        result = 31L * result + (int)(temp ^ temp >>> 32);
        result = 31L * result + ((this.sanityChecks != null) ? (this.sanityChecks.length + 1) : 0);
        result = 31L * result + ((this.modifiers != null) ? this.modifiers.hashCode() : 0);
        result = 31L * result + ((this.rng != null) ? this.rng.hashCode() : 0);
        return (int)result;
    }
    
    @Override
    public String toString() {
        return "Finnegan{openingVowels=" + Arrays.toString(this.openingVowels) + ", midVowels=" + Arrays.toString(this.midVowels) + ", openingConsonants=" + Arrays.toString(this.openingConsonants) + ", midConsonants=" + Arrays.toString(this.midConsonants) + ", closingConsonants=" + Arrays.toString(this.closingConsonants) + ", vowelSplitters=" + Arrays.toString(this.vowelSplitters) + ", closingSyllables=" + Arrays.toString(this.closingSyllables) + ", syllableFrequencies=" + this.syllableFrequencies + ", totalSyllableFrequency=" + this.totalSyllableFrequency + ", vowelStartFrequency=" + this.vowelStartFrequency + ", vowelEndFrequency=" + this.vowelEndFrequency + ", vowelSplitFrequency=" + this.vowelSplitFrequency + ", syllableEndFrequency=" + this.syllableEndFrequency + ", sanityChecks=" + Arrays.toString(this.sanityChecks) + ", clean=" + this.clean + ", modifiers=" + this.modifiers + ", RNG=" + this.rng + '}';
    }
    
    public long getSeed() {
        return this.rng.state;
    }
    
    public void setSeed(final long seed) {
        this.rng.state = seed;
    }
    
    static long hash64(final char[] data) {
        if (data == null) {
            return 0L;
        }
        long h = -3750763034362895579L;
        final long len = data.length;
        for (int i = 0; i < len; ++i) {
            h ^= (data[i] & '\u00ff');
            h *= 1099511628211L;
            h ^= data[i] >>> 8;
            h *= 1099511628211L;
        }
        return h;
    }
    
    static long hash64(final int[] data) {
        if (data == null) {
            return 0L;
        }
        long h = -3750763034362895579L;
        final long len = data.length;
        for (int i = 0; i < len; ++i) {
            h ^= (data[i] & 0xFF);
            h *= 1099511628211L;
            h ^= (data[i] >>> 8 & 0xFF);
            h *= 1099511628211L;
            h ^= (data[i] >>> 16 & 0xFF);
            h *= 1099511628211L;
            h ^= data[i] >>> 24;
            h *= 1099511628211L;
        }
        return h;
    }
    
    static long hash64(final double[] data) {
        if (data == null) {
            return 0L;
        }
        long h = -3750763034362895579L;
        final long len = data.length;
        for (int i = 0; i < len; ++i) {
            final long t = Double.doubleToRawLongBits(data[i]);
            h ^= (t & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 8 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 16 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 24 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 32 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 40 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 48 & 0xFFL);
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }
    
    static long hash64(final String s) {
        if (s == null) {
            return 0L;
        }
        return hash64(s.toCharArray());
    }
    
    static long hash64(final String[] data) {
        if (data == null) {
            return 0L;
        }
        long h = -3750763034362895579L;
        final long len = data.length;
        for (int i = 0; i < len; ++i) {
            final long t = hash64(data[i]);
            h ^= (t & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 8 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 16 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 24 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 32 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 40 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 48 & 0xFFL);
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }
    
    static long hash64(final String[]... data) {
        if (data == null) {
            return 0L;
        }
        long h = -3750763034362895579L;
        final long len = data.length;
        for (int i = 0; i < len; ++i) {
            final long t = hash64(data[i]);
            h ^= (t & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 8 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 16 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 24 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 32 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 40 & 0xFFL);
            h *= 1099511628211L;
            h ^= (t >>> 48 & 0xFFL);
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }
    
    public Finnegan copy() {
        return new Finnegan(this.openingVowels, this.midVowels, this.openingConsonants, this.midConsonants, this.closingConsonants, this.closingSyllables, this.vowelSplitters, this.syllableFrequencies, this.vowelStartFrequency, this.vowelEndFrequency, this.vowelSplitFrequency, this.syllableEndFrequency, this.sanityChecks, this.clean, this.rng, this.modifiers);
    }
    
    public class RNG implements Serializable
    {
        private static final long serialVersionUID = 4378460257281186371L;
        private static final long DOUBLE_MASK = 9007199254740991L;
        private static final double NORM_53 = 1.1102230246251565E-16;
        public long state;
        
        public RNG() {
            this.state = Double.doubleToLongBits(Math.random());
        }
        
        public RNG(final long seed) {
            this.state = seed;
        }
        
        public long nextLong() {
            final long state = this.state - 7046029254386353131L;
            this.state = state;
            long z = state;
            z = (z ^ z >>> 30) * -4658895280553007687L;
            z = (z ^ z >>> 27) * -7723592293110705685L;
            return z ^ z >>> 31;
        }
        
        public int nextInt() {
            return (int)this.nextLong();
        }
        
        public int nextInt(final int n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            final int bits = this.nextInt() >>> 1;
            return bits % n;
        }
        
        public int nextInt(final int lower, final int upper) {
            if (upper - lower <= 0) {
                throw new IllegalArgumentException();
            }
            return lower + this.nextInt(upper - lower);
        }
        
        public long nextLong(final long n) {
            if (n <= 0L) {
                throw new IllegalArgumentException();
            }
            final long bits = this.nextLong() >>> 1;
            return bits % n;
        }
        
        public long nextLong(final long lower, final long upper) {
            if (upper - lower <= 0L) {
                throw new IllegalArgumentException();
            }
            return lower + this.nextLong(upper - lower);
        }
        
        public double nextDouble() {
            return (this.nextLong() & 0x1FFFFFFFFFFFFFL) * 1.1102230246251565E-16;
        }
        
        public double nextDouble(final double outer) {
            return this.nextDouble() * outer;
        }
        
        public <T> T getRandomElement(final T[] array) {
            if (array.length < 1) {
                return null;
            }
            return array[this.nextInt(array.length)];
        }
        
        public <T> T[] shuffle(final T[] elements) {
            final Object[] array = elements.clone();
            for (int n = array.length, i = 0; i < n; ++i) {
                final int r = i + this.nextInt(n - i);
                final T t = (T)array[r];
                array[r] = array[i];
                array[i] = t;
            }
            return (T[])array;
        }
        
        public <T> ArrayList<T> shuffle(final List<T> elements) {
            final ArrayList<T> al = new ArrayList<T>((Collection<? extends T>)elements);
            for (int n = al.size(), i = 0; i < n; ++i) {
                Collections.swap(al, i + this.nextInt(n - i), i);
            }
            return al;
        }
        
        public <T> List<T> randomPortion(final List<T> data, final int count) {
            return this.shuffle(data).subList(0, Math.min(count, data.size()));
        }
        
        public long getState() {
            return this.state;
        }
        
        public void setState(final long state) {
            this.state = state;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final RNG rng = (RNG)o;
            return this.state == rng.state;
        }
        
        @Override
        public int hashCode() {
            return (int)(this.state ^ this.state >>> 32);
        }
        
        @Override
        public String toString() {
            return "RNG{state=" + this.state + '}';
        }
    }
    
    public static class Modifier implements Serializable
    {
        private static final long serialVersionUID = 1734863678490422371L;
        public final Alteration[] alterations;
        public static final Modifier LISP;
        public static final Modifier HISS;
        public static final Modifier STUTTER;
        public static final Modifier DOUBLE_VOWELS;
        public static final Modifier DOUBLE_CONSONANTS;
        public static final Modifier NO_DOUBLES;
        
        static {
            LISP = new Modifier("[s\u015b\u015d\u015f\u0161\u0219]+h?", "th");
            HISS = new Modifier("(.)([s\u015b\u015d\u015f\u0161\u0219z\u017a\u017c\u017e])+", "$1$2$2$2");
            STUTTER = new Modifier(new Alteration[] { new Alteration("^([^a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011bi\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131o\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ffu\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173y\u00fd\u00ff\u0177\u1ef3\u03b1\u03bf\u03b5\u03b9\u03c5\u0430\u0435\u0451\u0438\u0439\u044a\u044b\u044d\u044e\u044f\u043e\u0443]+)", "$1-$1", 0.2), new Alteration("^([a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011bi\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131o\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ffu\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u03b1\u03bf\u03b5\u03b9\u03c5\u0430\u0435\u0451\u0438\u0439\u044a\u044b\u044d\u044e\u044f\u043e\u0443]+)", "$1-$1", 0.2) });
            DOUBLE_VOWELS = new Modifier("([a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ff])([^a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011bi\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131o\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ffu\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173y\u00fd\u00ff\u0177\u1ef3]|$)", "$1$1$2", 0.4);
            DOUBLE_CONSONANTS = new Modifier("([a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011bi\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131o\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ffu\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173y\u00fd\u00ff\u0177\u1ef3\u03b1\u03bf\u03b5\u03b9\u03c5\u0430\u0435\u0451\u0438\u0439\u044a\u044b\u044d\u044e\u044f\u043e\u0443]|^)([^a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011bi\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131o\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ffu\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173y\u00fd\u00ff\u0177\u1ef3\u03b1\u03bf\u03b5\u03b9\u03c5\u0430\u0435\u0451\u0438\u0439\u044a\u044b\u044d\u044e\u044f\u043e\u0443qwhjx])([a\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u0101\u0103\u0105\u01fb\u01fde\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011bi\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131o\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u0153\u01ffu\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173y\u00fd\u00ff\u0177\u1ef3\u03b1\u03bf\u03b5\u03b9\u03c5\u0430\u0435\u0451\u0438\u0439\u044a\u044b\u044d\u044e\u044f\u043e\u0443]|$)", "$1$2$2$3", 0.5);
            NO_DOUBLES = new Modifier("(.)\\1", "$1");
        }
        
        public Modifier() {
            this("sh?", "th");
        }
        
        public Modifier(final String pattern, final String replacement) {
            this.alterations = new Alteration[] { new Alteration(pattern, replacement) };
        }
        
        public Modifier(final String pattern, final String replacement, final double chance) {
            this.alterations = new Alteration[] { new Alteration(pattern, replacement, chance) };
        }
        
        public Modifier(final Alteration... alts) {
            this.alterations = ((alts == null) ? new Alteration[0] : alts);
        }
        
        public StringBuffer modify(final RNG rng, StringBuffer sb) {
            Alteration[] alterations;
            for (int length = (alterations = this.alterations).length, i = 0; i < length; ++i) {
                final Alteration alt = alterations[i];
                final Matcher m = alt.pattern.matcher(sb);
                final StringBuffer sb2 = new StringBuffer();
                while (m.find()) {
                    if (rng.nextDouble() < alt.chance) {
                        m.appendReplacement(sb2, alt.replacer);
                    }
                    else {
                        m.appendReplacement(sb2, m.group());
                    }
                }
                m.appendTail(sb2);
                sb = sb2;
            }
            return sb;
        }
        
        public static Modifier replacementTable(final String initial, final String change) {
            final Alteration[] alts = new Alteration[Math.min(initial.length(), change.length())];
            for (int i = 0; i < alts.length; ++i) {
                alts[i] = new Alteration("\\Q" + initial.charAt(i) + "\\E", change.substring(i, i + 1));
            }
            return new Modifier(alts);
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final Modifier modifier = (Modifier)o;
            return Arrays.equals(this.alterations, modifier.alterations);
        }
        
        @Override
        public int hashCode() {
            return Arrays.hashCode(this.alterations);
        }
        
        @Override
        public String toString() {
            return "Modifier{alterations=" + Arrays.toString(this.alterations) + '}';
        }
    }
    
    public static class Alteration implements Serializable
    {
        private static final long serialVersionUID = -2138854697837563188L;
        public Pattern pattern;
        public String replacer;
        public double chance;
        
        public Alteration() {
            this("[s\u015b\u015d\u015f\u0161\u0219]+h?", "th");
        }
        
        public Alteration(final String pattern, final String replacement) {
            this.pattern = Pattern.compile(pattern);
            this.replacer = replacement;
            this.chance = 1.0;
        }
        
        public Alteration(final String pattern, final String replacement, final double chance) {
            this.pattern = Pattern.compile(pattern);
            this.replacer = replacement;
            this.chance = chance;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final Alteration that = (Alteration)o;
            return Double.compare(that.chance, this.chance) == 0 && this.replacer.equals(that.replacer);
        }
        
        @Override
        public int hashCode() {
            long result = Finnegan.hash64(this.replacer);
            result = 31L * result + this.pattern.hashCode();
            final long temp = Double.doubleToLongBits(this.chance);
            result = 31L * result + (temp ^ temp >>> 32);
            return (int)result;
        }
        
        @Override
        public String toString() {
            return "Alteration{pattern=" + this.pattern + "replacer=" + this.replacer + ", chance=" + this.chance + '}';
        }
    }
}
