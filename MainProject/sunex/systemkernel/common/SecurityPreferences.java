package sunex.systemkernel.common;

import com.pcmsolutions.system.preferences.Impl_ZIntPref;
import com.pcmsolutions.system.preferences.ZIntPref;
import com.pcmsolutions.system.Zoeos;

import java.util.prefs.Preferences;

/**
 * User: paulmeehan
 * Date: 17-Jan-2004
 * Time: 06:42:04
 */
public class SecurityPreferences {
    private static int base = 243;
    private static int maxUses = 15;

    private static final ZIntPref pref = new Impl_ZIntPref(Preferences.systemNodeForPackage(SecurityPreferences.class), "security2tag", encodeTag(base));

    static {
        try {
            if (Zoeos.isUnlicensed())
                pref.putValue(encodeTag(decodeTag(pref.getValue()) + 1));
        } catch (Exception e) {
        }
    }

    public static boolean expired() {
        /*
        try {
            int tag = decodeTag(pref.getValue());
            System.out.println(tag);
            if ((tag < base || tag > base + maxUses) && Zoeos.isUnlicensed())
                return true;
        } catch (Exception e) {
            if (Zoeos.isUnlicensed())
                return true;
        }
        */
        return false;
    }

    private static int encodeTag(int n) {
        return (((n * 20) + 17) * 3) + 42;
    }

    private static int decodeTag(int n) {
        return (((n - 42) / 3) - 17) / 20;
    }
}
