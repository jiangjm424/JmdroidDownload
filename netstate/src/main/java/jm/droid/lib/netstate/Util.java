package jm.droid.lib.netstate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.Nullable;

/* package */ final class Util {

    public static final int SDK_INT = Build.VERSION.SDK_INT;

    /**
     * Registers a {@link BroadcastReceiver} that's not intended to receive broadcasts from other
     * apps. This will be enforced by specifying {@link Context#} if {@link
     * #SDK_INT} is 33 or above.
     *
     * @param context The context on which {@link Context#registerReceiver} will be called.
     * @param receiver The {@link BroadcastReceiver} to register. This value may be null.
     * @param filter Selects the Intent broadcasts to be received.
     * @return The first sticky intent found that matches {@code filter}, or null if there are none.
     */
    @Nullable
    public static Intent registerReceiverNotExported(
        Context context, @Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (SDK_INT < 33) {
            return context.registerReceiver(receiver, filter);
        } else {
            return context.registerReceiver(receiver, filter);
        }
    }

    /**
     * Registers a {@link BroadcastReceiver} that's not intended to receive broadcasts from other
     * apps. This will be enforced by specifying {@link Context#} if {@link
     * #SDK_INT} is 33 or above.
     *
     * @param context The context on which {@link Context#registerReceiver} will be called.
     * @param receiver The {@link BroadcastReceiver} to register. This value may be null.
     * @param filter Selects the Intent broadcasts to be received.
     * @param handler Handler identifying the thread that will receive the Intent.
     * @return The first sticky intent found that matches {@code filter}, or null if there are none.
     */
    @Nullable
    public static Intent registerReceiverNotExported(
        Context context, BroadcastReceiver receiver, IntentFilter filter, Handler handler) {
        if (SDK_INT < 33) {
            return context.registerReceiver(receiver, filter, /* broadcastPermission= */ null, handler);
        } else {
            return context.registerReceiver(
                receiver,
                filter,
                /* broadcastPermission= */ null,
                handler);
        }
    }
}
