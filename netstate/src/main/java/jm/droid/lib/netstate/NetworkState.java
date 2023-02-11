package jm.droid.lib.netstate;

import static android.net.ConnectivityManager.TYPE_BLUETOOTH;
import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_MOBILE_DUN;
import static android.net.ConnectivityManager.TYPE_MOBILE_HIPRI;
import static android.net.ConnectivityManager.TYPE_MOBILE_MMS;
import static android.net.ConnectivityManager.TYPE_MOBILE_SUPL;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.ConnectivityManager.TYPE_WIMAX;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NetworkState {
    /**
     * 网络类型
     */
    public final @Network.NetworkType int netType;
    /**
     * Determines if the network is connected.
     */
    public final boolean isConnected;
    /**
     * Determines if the network is validated - has a working Internet connection.
     */
    public final boolean isValidated;
    /**
     * Determines if the network is metered.(受限制的流量)
     * 在{@isConnedted} 为true时有效
     */
    public final boolean isMetered;
    /**
     * Determines if the network is not roaming.
     */
    public final boolean isNotRoaming;

    public final static NetworkState EMPTY = new NetworkState(Network.TYPE_UNKNOWN,false,false,false,false);

    private NetworkState(@Network.NetworkType int netType, boolean isConnected, boolean isValidated, boolean isMetered, boolean isNotRoaming) {
        this.netType = netType;
        this.isConnected = isConnected;
        this.isValidated = isValidated;
        this.isMetered = isMetered;
        this.isNotRoaming = isNotRoaming;
    }

    public static NetworkState copy(@Network.NetworkType int netType, boolean isConnected, boolean isValidated, boolean isMetered, boolean isNotRoaming) {
        return new NetworkState(netType, isConnected, isValidated,isMetered,isNotRoaming);
    }

    public static NetworkState activeNetworkState(ConnectivityManager connectivityManager) {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        int netType = getNetworkTypeFromConnectivityManager(connectivityManager);
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        boolean isValidated = isActiveNetworkValidated(connectivityManager);
        boolean isMetered = isActiveNetworkMetered(connectivityManager);
        boolean isNotRoaming = activeNetworkInfo != null && !activeNetworkInfo.isRoaming();
        return new NetworkState(netType, isConnected, isValidated, isMetered, isNotRoaming);
    }

    public static @Network.NetworkType int getNetworkTypeFromConnectivityManager(@Nullable ConnectivityManager connectivityManager) {
        if (connectivityManager == null) {
            return Network.TYPE_UNKNOWN;
        }
        NetworkInfo networkInfo;
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (SecurityException e) {
            // Expected if permission was revoked.
            return Network.TYPE_UNKNOWN;
        }
        if (networkInfo == null || !networkInfo.isConnected()) {
            return Network.TYPE_OFFLINE;
        }
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return Network.TYPE_WIFI;
            case ConnectivityManager.TYPE_WIMAX:
                return Network.TYPE_4G;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                return getMobileNetworkType(networkInfo);
            case ConnectivityManager.TYPE_ETHERNET:
                return Network.TYPE_ETHERNET;
            default:
                return Network.TYPE_OTHER;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkState that = (NetworkState) o;

        if (netType != that.netType) return false;
        if (isConnected != that.isConnected) return false;
        if (isValidated != that.isValidated) return false;
        if (isMetered != that.isMetered) return false;
        return isNotRoaming == that.isNotRoaming;
    }

    @Override
    public int hashCode() {
        int result = netType;
        result = 31 * result + (isConnected ? 1 : 0);
        result = 31 * result + (isValidated ? 1 : 0);
        result = 31 * result + (isMetered ? 1 : 0);
        result = 31 * result + (isNotRoaming ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NetworkState{" +
            "netType=" + netType +
            ", isConnected=" + isConnected +
            ", isValidated=" + isValidated +
            ", isMetered=" + isMetered +
            ", isNotRoaming=" + isNotRoaming +
            '}';
    }

    /**
     * ConnectivityManagerCompat.isActiveNetworkMetered
     * 在api > 16 时使用的{@ConnectivityManager.isActiveNetworkMetered} 一直返回true
     * 所以这里继续使用旧方法
     */
    private static boolean isActiveNetworkMetered(@NonNull ConnectivityManager cm) {
        final NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            // err on side of caution
            return true;
        }
        final int type = info.getType();
        switch (type) {
            case TYPE_MOBILE:
            case TYPE_MOBILE_DUN:
            case TYPE_MOBILE_HIPRI:
            case TYPE_MOBILE_MMS:
            case TYPE_MOBILE_SUPL:
            case TYPE_WIMAX:
                return true;
            case TYPE_WIFI:
            case TYPE_BLUETOOTH:
            case TYPE_ETHERNET:
                return false;
            default:
                // err on side of caution
                return true;
        }
    }

    private static boolean isActiveNetworkValidated(ConnectivityManager connectivityManager) {
        if (Build.VERSION.SDK_INT < 23) return false;
        try {
            android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities == null) return false;
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (SecurityException ex) {
            return false;
        }
    }


    private static @Network.NetworkType int getMobileNetworkType(NetworkInfo networkInfo) {
        switch (networkInfo.getSubtype()) {
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return Network.TYPE_2G;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return Network.TYPE_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return Network.TYPE_4G;
            case TelephonyManager.NETWORK_TYPE_NR:
                return Util.SDK_INT >= 29 ? Network.TYPE_5G_SA : Network.TYPE_UNKNOWN;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return Network.TYPE_WIFI;
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default: // Future mobile network types.
                return Network.TYPE_CELLULAR_UNKNOWN;
        }
    }

}
