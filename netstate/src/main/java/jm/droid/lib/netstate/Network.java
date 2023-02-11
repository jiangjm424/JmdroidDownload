package jm.droid.lib.netstate;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class Network {
    /**
     * Network connection type. One of {@link #TYPE_UNKNOWN}, {@link #TYPE_OFFLINE},
     * {@link #TYPE_WIFI}, {@link #TYPE_2G}, {@link #TYPE_3G}, {@link
     * #TYPE_4G}, {@link #TYPE_5G_SA}, {@link #TYPE_5G_NSA}, {@link
     * #TYPE_CELLULAR_UNKNOWN}, {@link #TYPE_ETHERNET} or {@link #TYPE_OTHER}.
     */
    // @Target list includes both 'default' targets and TYPE_USE, to ensure backwards compatibility
    // with Kotlin usages from before TYPE_USE was added.
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
    @IntDef({
        TYPE_UNKNOWN,
        TYPE_OFFLINE,
        TYPE_WIFI,
        TYPE_2G,
        TYPE_3G,
        TYPE_4G,
        TYPE_5G_SA,
        TYPE_5G_NSA,
        TYPE_CELLULAR_UNKNOWN,
        TYPE_ETHERNET,
        TYPE_OTHER
    })
    public @interface NetworkType {}
    /** Unknown network type. */
    public static final int TYPE_UNKNOWN = 0;
    /** No network connection. */
    public static final int TYPE_OFFLINE = 1;
    /** Network type for a Wifi connection. */
    public static final int TYPE_WIFI = 2;
    /** Network type for a 2G cellular connection. */
    public static final int TYPE_2G = 3;
    /** Network type for a 3G cellular connection. */
    public static final int TYPE_3G = 4;
    /** Network type for a 4G cellular connection. */
    public static final int TYPE_4G = 5;
    /** Network type for a 5G stand-alone (SA) cellular connection. */
    public static final int TYPE_5G_SA = 9;
    /** Network type for a 5G non-stand-alone (NSA) cellular connection. */
    public static final int TYPE_5G_NSA = 10;
    /**
     * Network type for cellular connections which cannot be mapped to one of {@link
     * #TYPE_2G}, {@link #TYPE_3G}, or {@link #TYPE_4G}.
     */
    public static final int TYPE_CELLULAR_UNKNOWN = 6;
    /** Network type for an Ethernet connection. */
    public static final int TYPE_ETHERNET = 7;
    /** Network type for other connections which are not Wifi or cellular (e.g. VPN, Bluetooth). */
    public static final int TYPE_OTHER = 8;

}
