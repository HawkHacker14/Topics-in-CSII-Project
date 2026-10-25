import java.util.Arrays;

/** Implementation of the classic Porter (1980) stemming algorithm. */
public class PorterStemmer {

    private char[] b;
    private int j, k;

    private static final String[][] STEP2 = {
        {"ational","ate"},{"tional","tion"},{"enci","ence"},{"anci","ance"},{"izer","ize"},
        {"bli","ble"},{"alli","al"},{"entli","ent"},{"eli","e"},{"ousli","ous"},
        {"ization","ize"},{"ation","ate"},{"ator","ate"},{"alism","al"},{"iveness","ive"},
        {"fulness","ful"},{"ousness","ous"},{"aliti","al"},{"iviti","ive"},{"biliti","ble"},
        {"logi","log"}
    };
    private static final String[][] STEP3 = {
        {"icate","ic"},{"ative",""},{"alize","al"},{"iciti","ic"},{"ical","ic"},{"ful",""},{"ness",""}
    };
    private static final String[] STEP4 = {
        "al","ance","ence","er","ic","able","ible","ant","ement","ment","ent",
        "ion","ou","ism","ate","iti","ous","ive","ize"
    };

    public String stem(String word) {
        if (word.length() <= 2) return word;
        b = word.toCharArray();
        k = b.length - 1;
        step1ab();
        step1c();
        applyRules(STEP2);
        applyRules(STEP3);
        step4();
        step5();
        return new String(b, 0, k + 1);
    }

    private boolean cons(int i) {
        switch (b[i]) {
            case 'a': case 'e': case 'i': case 'o': case 'u': return false;
            case 'y': return i == 0 || !cons(i - 1);
            default:  return true;
        }
    }

    /** Number of consonant-vowel sequences in b[0..j]. */
    private int m() {
        int n = 0, i = 0;
        while (true) { if (i > j) return n; if (!cons(i)) break; i++; }
        i++;
        while (true) {
            while (true) { if (i > j) return n; if (cons(i)) break; i++; }
            i++; n++;
            while (true) { if (i > j) return n; if (!cons(i)) break; i++; }
            i++;
        }
    }

    private boolean vowelInStem() {
        for (int i = 0; i <= j; i++) if (!cons(i)) return true;
        return false;
    }

    private boolean doublec(int p) {
        return p >= 1 && b[p] == b[p - 1] && cons(p);
    }

    private boolean cvc(int i) {
        if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
        char ch = b[i];
        return ch != 'w' && ch != 'x' && ch != 'y';
    }

    private boolean ends(String s) {
        int len = s.length(), o = k - len + 1;
        if (o < 0) return false;
        for (int i = 0; i < len; i++) if (b[o + i] != s.charAt(i)) return false;
        j = k - len;
        return true;
    }

    private void setTo(String s) {
        int len = s.length(), o = j + 1;
        if (o + len > b.length) b = Arrays.copyOf(b, o + len);
        for (int i = 0; i < len; i++) b[o + i] = s.charAt(i);
        k = j + len;
    }

    /** Removes plurals and -ed / -ing. */
    private void step1ab() {
        if (b[k] == 's') {
            if (ends("sses")) k -= 2;
            else if (ends("ies")) setTo("i");
            else if (b[k - 1] != 's') k--;
        }
        if (ends("eed")) {
            if (m() > 0) k--;
        } else if ((ends("ed") || ends("ing")) && vowelInStem()) {
            k = j;
            if (ends("at")) setTo("ate");
            else if (ends("bl")) setTo("ble");
            else if (ends("iz")) setTo("ize");
            else if (doublec(k)) {
                k--;
                char ch = b[k];
                if (ch == 'l' || ch == 's' || ch == 'z') k++;
            } else if (m() == 1 && cvc(k)) setTo("e");
        }
    }

    /** Terminal y -> i when there is another vowel in the stem. */
    private void step1c() {
        if (ends("y") && vowelInStem()) b[k] = 'i';
    }

    private void applyRules(String[][] rules) {
        for (String[] r : rules) {
            if (ends(r[0])) {
                if (m() > 0) setTo(r[1]);
                return;
            }
        }
    }

    /** Removes -ant, -ence, -ment, etc. when the measure m() > 1. */
    private void step4() {
        for (String s : STEP4) {
            if (ends(s)) {
                if (s.equals("ion") && !(j >= 0 && (b[j] == 's' || b[j] == 't'))) return;
                if (m() > 1) k = j;
                return;
            }
        }
    }

    /** Removes a final -e and reduces -ll to -l. */
    private void step5() {
        j = k;
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || (a == 1 && !cvc(k - 1))) k--;
        }
        if (b[k] == 'l' && doublec(k) && m() > 1) k--;
    }
}

