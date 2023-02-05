package jm.droid.lib.download.util;

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
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.net.ConnectivityManagerCompat;

public class NetworkState {
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

    public NetworkState(boolean isConnected, boolean isValidated, boolean isMetered, boolean isNotRoaming) {
        this.isConnected = isConnected;
        this.isValidated = isValidated;
        this.isMetered = isMetered;
        this.isNotRoaming = isNotRoaming;
    }


    public static NetworkState activeNetworkState(ConnectivityManager connectivityManager) {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        boolean isValidated = isActiveNetworkValidated(connectivityManager);
        boolean isMetered = isActiveNetworkMetered(connectivityManager);
        boolean isNotRoaming = activeNetworkInfo != null && !activeNetworkInfo.isRoaming();
        return new NetworkState(isConnected, isValidated, isMetered, isNotRoaming);
    }

    @Override
    public String toString() {
        return "NetworkState{" +
            "isConnected=" + isConnected +
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
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities == null) return false;
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (SecurityException ex) {
            return false;
        }
    }

}
