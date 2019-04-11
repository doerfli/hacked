package li.doerf.hacked.utils;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by moo on 23.02.18.
 */

public class StringHelper {

    public static String addDigitSeperator(String aNumberString) {
        return NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(aNumberString));
    }

    public static String join(String[] strings, String joinStr) {
        return join(Arrays.asList(strings), joinStr);
    }

    private static String join(Collection<String> strings, String joinStr) {
        if (strings.size() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        Iterator<String> iter = strings.iterator();
        b.append(iter.next());
        while(iter.hasNext()) {
            b.append(joinStr);
            b.append(iter.next());
        }
        return b.toString();
    }
}
