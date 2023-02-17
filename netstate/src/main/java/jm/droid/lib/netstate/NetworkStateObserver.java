/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jm.droid.lib.netstate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyCallback.DisplayInfoListener;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer for network type changes.
 *
 * <p>{@link #register Registered} listeners are informed at registration and whenever the network
 * type changes.
 *
 * <p>The current network type can also be {@link #getNetworkState() queried} without registration.
 */
public final class NetworkStateObserver {

    /**
     * A listener for network type changes.
     */
    public interface Listener {

        /**
         * Called when the network type changed or when the listener is first registered.
         *
         * <p>This method is always called on the main thread.
         */
        void onNetworkStateChanged(NetworkState ns);
    }

    @Nullable
    private static volatile NetworkStateObserver staticInstance;

    private final Handler mainHandler;
    // This class needs to hold weak references as it doesn't require listeners to unregister.
    private final CopyOnWriteArrayList<WeakReference<Listener>> listeners;
    private final Object networkStateLock;
    private final Context appContext;
    @GuardedBy("networkTypeLock")
    private NetworkState ns = NetworkState.EMPTY;

    /**
     * Returns a network type observer instance.
     *
     * @param context A {@link Context}.
     */
    public static NetworkStateObserver getInstance(Context context) {
        if (staticInstance == null) {
            synchronized (NetworkStateObserver.class) {
                if (staticInstance == null) staticInstance = new NetworkStateObserver(context);
            }
        }
        return staticInstance;
    }

    private NetworkStateObserver(Context context) {
        if (context.getApplicationContext() != null) {
            appContext = context.getApplicationContext();
        } else {
            appContext = context;
        }
        mainHandler = new Handler(Looper.getMainLooper());
        listeners = new CopyOnWriteArrayList<>();
        networkStateLock = new Object();
        updateNetworkState();
        if (Util.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkCallbackV24(appContext);
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            Util.registerReceiverNotExported(appContext, new Receiver(), filter);
        }
    }

    /**
     * Registers a listener.
     *
     * <p>The current network type will be reported to the listener after registration.
     *
     * @param listener The {@link Listener}.
     */
    public void register(Listener listener) {
        removeClearedReferences();
        listeners.add(new WeakReference<>(listener));
        // Simulate an initial update on the main thread (like the sticky broadcast we'd receive if
        // we were to register a separate broadcast receiver for each listener).
        mainHandler.post(() -> listener.onNetworkStateChanged(getNetworkState()));
    }

    /**
     * Returns the current network state.
     */
    public NetworkState getNetworkState() {
        synchronized (networkStateLock) {
            return ns;
        }
    }

    @RequiresApi(24)
    private void registerNetworkCallbackV24(Context context) {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }
        NetworkCallback networkCallback = new NetworkCallback();
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private void removeClearedReferences() {
        for (WeakReference<Listener> listenerReference : listeners) {
            if (listenerReference.get() == null) {
                listeners.remove(listenerReference);
            }
        }
    }


    private void updateNetworkState() {
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkState state = NetworkState.activeNetworkState(connectivityManager);
        updateNetworkState(state);
    }

    private void updateNetworkState(NetworkState ns) {
        synchronized (networkStateLock) {
            if (Objects.equals(this.ns, ns)) {
                return;
            }
            this.ns = ns;
        }
        for (WeakReference<Listener> listenerReference : listeners) {
            @Nullable Listener listener = listenerReference.get();
            if (listener != null) {
                listener.onNetworkStateChanged(ns);
            } else {
                listeners.remove(listenerReference);
            }
        }
    }

    @RequiresApi(24)
    private final class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private boolean receivedCapabilitiesChange;
        private boolean networkValidated;

        @Override
        public void onAvailable(android.net.Network network) {
            postUpdateNetState();
        }

        @Override
        public void onLost(android.net.Network network) {
            postUpdateNetState();
        }

        @Override
        public void onBlockedStatusChanged(android.net.Network network, boolean blocked) {
            if (!blocked) {
                postUpdateNetState();
            }
        }

        @Override
        public void onCapabilitiesChanged(android.net.Network network, NetworkCapabilities networkCapabilities) {
            boolean networkValidated =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            if (!receivedCapabilitiesChange || this.networkValidated != networkValidated) {
                receivedCapabilitiesChange = true;
                this.networkValidated = networkValidated;
                postUpdateNetState();
            }
        }

        private void postUpdateNetState() {
            mainHandler.post(() -> updateNetworkState());
        }
    }

    private final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            @Network.NetworkType int networkType = NetworkState.getNetworkTypeFromConnectivityManager(connectivityManager);
            if (Util.SDK_INT >= 31 && networkType == Network.TYPE_4G) {
                // Delay update of the network type to check whether this is actually 5G-NSA.
                Api31.disambiguate4gAnd5gNsa(context, /* instance= */ NetworkStateObserver.this);
            } else {
                NetworkState state = NetworkState.activeNetworkState(connectivityManager);
                updateNetworkState(state);
            }
        }
    }

    @RequiresApi(31)
    private static final class Api31 {

        public static void disambiguate4gAnd5gNsa(Context context, NetworkStateObserver instance) {
            try {
                TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                DisplayInfoCallback callback = new DisplayInfoCallback(instance);
                telephonyManager.registerTelephonyCallback(context.getMainExecutor(), callback);
                // We are only interested in the initial response with the current state, so unregister
                // the listener immediately.
                telephonyManager.unregisterTelephonyCallback(callback);
            } catch (RuntimeException e) {
                // Ignore problems with listener registration and keep reporting as 4G.
                instance.updateNetworkState(NetworkState.copy(Network.TYPE_4G, true, true, true, true));
            }
        }

        private static final class DisplayInfoCallback extends TelephonyCallback
            implements DisplayInfoListener {

            private final NetworkStateObserver instance;

            public DisplayInfoCallback(NetworkStateObserver instance) {
                this.instance = instance;
            }

            @Override
            public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
                int overrideNetworkType = telephonyDisplayInfo.getOverrideNetworkType();
                boolean is5gNsa =
                    overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
                        || overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE
                        || overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED;
                NetworkState ns;
                if (is5gNsa) {
                    ns = NetworkState.copy(Network.TYPE_5G_NSA, true, true, true, true);
                } else {
                    ns = NetworkState.copy(Network.TYPE_4G, true, true, true, true);
                }
                instance.updateNetworkState(ns);
            }
        }
    }
}
