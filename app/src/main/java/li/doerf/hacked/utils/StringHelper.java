package li.doerf.hacked.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Created by moo on 23.02.18.
 */

public class StringHelper {

    public static String reverseString(String aString) {
        return new StringBuilder(aString).reverse().toString();
    }

    public static String addDigitSeperator(String aNumberString) {
        return reverseString(Joiner.on(",").join(Splitter.fixedLength(3).split(reverseString(aNumberString))));
    }
}
