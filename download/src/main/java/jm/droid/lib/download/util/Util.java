/*
 * Copyright (C) 2016 The Android Open Source Project
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
package jm.droid.lib.download.util;

import static android.content.Context.UI_MODE_SERVICE;
import static jm.droid.lib.download.util.Assertions.checkNotNull;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.security.NetworkSecurityPolicy;
import android.text.TextUtils;
import android.util.SparseLongArray;
import android.view.Display;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import jm.droid.lib.download.C;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;


/** Miscellaneous utility methods. */
public final class Util {

  /**
   * Like {@link Build.VERSION#SDK_INT}, but in a place where it can be conveniently overridden for
   * local testing.
   */
  public static final int SDK_INT = Build.VERSION.SDK_INT;

  /**
   * Like {@link Build#DEVICE}, but in a place where it can be conveniently overridden for local
   * testing.
   */
  public static final String DEVICE = Build.DEVICE;

  /**
   * Like {@link Build#MANUFACTURER}, but in a place where it can be conveniently overridden for
   * local testing.
   */
  public static final String MANUFACTURER = Build.MANUFACTURER;

  /**
   * Like {@link Build#MODEL}, but in a place where it can be conveniently overridden for local
   * testing.
   */
  public static final String MODEL = Build.MODEL;

  /** A concise description of the device that it can be useful to log for debugging purposes. */
  public static final String DEVICE_DEBUG_INFO =
      DEVICE + ", " + MODEL + ", " + MANUFACTURER + ", " + SDK_INT;

  /** An empty byte array. */
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private static final String TAG = "Util";
  private static final Pattern XS_DATE_TIME_PATTERN =
      Pattern.compile(
          "(\\d\\d\\d\\d)\\-(\\d\\d)\\-(\\d\\d)[Tt]"
              + "(\\d\\d):(\\d\\d):(\\d\\d)([\\.,](\\d+))?"
              + "([Zz]|((\\+|\\-)(\\d?\\d):?(\\d\\d)))?");
  private static final Pattern XS_DURATION_PATTERN =
      Pattern.compile(
          "^(-)?P(([0-9]*)Y)?(([0-9]*)M)?(([0-9]*)D)?"
              + "(T(([0-9]*)H)?(([0-9]*)M)?(([0-9.]*)S)?)?$");
  private static final Pattern ESCAPED_CHARACTER_PATTERN = Pattern.compile("%([A-Fa-f0-9]{2})");

  // https://docs.microsoft.com/en-us/azure/media-services/previous/media-services-deliver-content-overview#URLs
  private static final Pattern ISM_PATH_PATTERN =
      Pattern.compile("(?:.*\\.)?isml?(?:/(manifest(.*))?)?", Pattern.CASE_INSENSITIVE);
  private static final String ISM_HLS_FORMAT_EXTENSION = "format=m3u8-aapl";
  private static final String ISM_DASH_FORMAT_EXTENSION = "format=mpd-time-csf";

  // Replacement map of ISO language codes used for normalization.
  @Nullable private static HashMap<String, String> languageTagReplacementMap;

  private Util() {}

  /**
   * Converts the entirety of an {@link InputStream} to a byte array.
   *
   * @param inputStream the {@link InputStream} to be read. The input stream is not closed by this
   *     method.
   * @return a byte array containing all of the inputStream's bytes.
   * @throws IOException if an error occurs reading from the stream.
   */
  public static byte[] toByteArray(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[1024 * 4];
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toByteArray();
  }

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

  /**
   * Calls {@link Context#startForegroundService(Intent)} if {@link #SDK_INT} is 26 or higher, or
   * {@link Context#startService(Intent)} otherwise.
   *
   * @param context The context to call.
   * @param intent The intent to pass to the called method.
   * @return The result of the called method.
   */
  @Nullable
  public static ComponentName startForegroundService(Context context, Intent intent) {
    if (SDK_INT >= 26) {
      return context.startForegroundService(intent);
    } else {
      return context.startService(intent);
    }
  }

  /**
   * Checks whether it's necessary to request the {@link permission#READ_EXTERNAL_STORAGE}
   * permission read the specified {@link Uri}s, requesting the permission if necessary.
   *
   * @param activity The host activity for checking and requesting the permission.
   * @param uris {@link Uri}s that may require {@link permission#READ_EXTERNAL_STORAGE} to read.
   * @return Whether a permission request was made.
   */
  public static boolean maybeRequestReadExternalStoragePermission(Activity activity, Uri... uris) {
    if (SDK_INT < 23) {
      return false;
    }
    for (Uri uri : uris) {
      if (maybeRequestReadExternalStoragePermission(activity, uri)) {
        return true;
      }
    }
    return false;
  }

  private static boolean maybeRequestReadExternalStoragePermission(Activity activity, Uri uri) {
    return SDK_INT >= 23
        && (isLocalFileUri(uri) || isMediaStoreExternalContentUri(uri))
        && requestExternalStoragePermission(activity);
  }

  private static boolean isMediaStoreExternalContentUri(Uri uri) {
    if (!"content".equals(uri.getScheme()) || !MediaStore.AUTHORITY.equals(uri.getAuthority())) {
      return false;
    }
    List<String> pathSegments = uri.getPathSegments();
    if (pathSegments.isEmpty()) {
      return false;
    }
    String firstPathSegment = pathSegments.get(0);
    return MediaStore.VOLUME_EXTERNAL.equals(firstPathSegment)
        || MediaStore.VOLUME_EXTERNAL_PRIMARY.equals(firstPathSegment);
  }

  /**
   * Returns true if the URI is a path to a local file or a reference to a local file.
   *
   * @param uri The uri to test.
   */
  public static boolean isLocalFileUri(Uri uri) {
    String scheme = uri.getScheme();
    return TextUtils.isEmpty(scheme) || "file".equals(scheme);
  }

  /**
   * Tests two objects for {@link Object#equals(Object)} equality, handling the case where one or
   * both may be null.
   *
   * @param o1 The first object.
   * @param o2 The second object.
   * @return {@code o1 == null ? o2 == null : o1.equals(o2)}.
   */
  public static boolean areEqual(@Nullable Object o1, @Nullable Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  /**
   * Tests whether an {@code items} array contains an object equal to {@code item}, according to
   * {@link Object#equals(Object)}.
   *
   * <p>If {@code item} is null then true is returned if and only if {@code items} contains null.
   *
   * @param items The array of items to search.
   * @param item The item to search for.
   * @return True if the array contains an object equal to the item being searched for.
   */
  public static boolean contains(Object[] items, @Nullable Object item) {
    for (Object arrayItem : items) {
      if (areEqual(arrayItem, item)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes an indexed range from a List.
   *
   * <p>Does nothing if the provided range is valid and {@code fromIndex == toIndex}.
   *
   * @param list The List to remove the range from.
   * @param fromIndex The first index to be removed (inclusive).
   * @param toIndex The last index to be removed (exclusive).
   * @throws IllegalArgumentException If {@code fromIndex} &lt; 0, {@code toIndex} &gt; {@code
   *     list.size()}, or {@code fromIndex} &gt; {@code toIndex}.
   */
  public static <T> void removeRange(List<T> list, int fromIndex, int toIndex) {
    if (fromIndex < 0 || toIndex > list.size() || fromIndex > toIndex) {
      throw new IllegalArgumentException();
    } else if (fromIndex != toIndex) {
      // Checking index inequality prevents an unnecessary allocation.
      list.subList(fromIndex, toIndex).clear();
    }
  }

  /**
   * Casts a nullable variable to a non-null variable without runtime null check.
   *
   * <p>Use {@link Assertions#checkNotNull(Object)} to throw if the value is null.
   */
  @SuppressWarnings({"nullness:contracts.postcondition", "nullness:return"})
  public static <T> T castNonNull(@Nullable T value) {
    return value;
  }

  /** Casts a nullable type array to a non-null type array without runtime null check. */
  @SuppressWarnings({"nullness:contracts.postcondition", "nullness:return"})
  public static <T> T[] castNonNullTypeArray(T[] value) {
    return value;
  }

  /**
   * Copies and optionally truncates an array. Prevents null array elements created by {@link
   * Arrays#copyOf(Object[], int)} by ensuring the new length does not exceed the current length.
   *
   * @param input The input array.
   * @param length The output array length. Must be less or equal to the length of the input array.
   * @return The copied array.
   */
  @SuppressWarnings({"nullness:argument", "nullness:return"})
  public static <T> T[] nullSafeArrayCopy(T[] input, int length) {
    Assertions.checkArgument(length <= input.length);
    return Arrays.copyOf(input, length);
  }

  /**
   * Copies a subset of an array.
   *
   * @param input The input array.
   * @param from The start the range to be copied, inclusive
   * @param to The end of the range to be copied, exclusive.
   * @return The copied array.
   */
  @SuppressWarnings({"nullness:argument", "nullness:return"})
  public static <T> T[] nullSafeArrayCopyOfRange(T[] input, int from, int to) {
    Assertions.checkArgument(0 <= from);
    Assertions.checkArgument(to <= input.length);
    return Arrays.copyOfRange(input, from, to);
  }

  /**
   * Creates a new array containing {@code original} with {@code newElement} appended.
   *
   * @param original The input array.
   * @param newElement The element to append.
   * @return The new array.
   */
  public static <T> T[] nullSafeArrayAppend(T[] original, T newElement) {
    T[] result = Arrays.copyOf(original, original.length + 1);
    result[original.length] = newElement;
    return castNonNullTypeArray(result);
  }

  /**
   * Creates a new array containing the concatenation of two non-null type arrays.
   *
   * @param first The first array.
   * @param second The second array.
   * @return The concatenated result.
   */
  @SuppressWarnings("nullness:assignment")
  public static <T> T[] nullSafeArrayConcatenation(T[] first, T[] second) {
    T[] concatenation = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(
        /* src= */ second,
        /* srcPos= */ 0,
        /* dest= */ concatenation,
        /* destPos= */ first.length,
        /* length= */ second.length);
    return concatenation;
  }

  /**
   * Copies the contents of {@code list} into {@code array}.
   *
   * <p>{@code list.size()} must be the same as {@code array.length} to ensure the contents can be
   * copied into {@code array} without leaving any nulls at the end.
   *
   * @param list The list to copy items from.
   * @param array The array to copy items to.
   */
  @SuppressWarnings("nullness:toArray.nullable.elements.not.newarray")
  public static <T> void nullSafeListToArray(List<T> list, T[] array) {
    Assertions.checkState(list.size() == array.length);
    list.toArray(array);
  }

  /**
   * Creates a {@link Handler} on the current {@link Looper} thread.
   *
   * @throws IllegalStateException If the current thread doesn't have a {@link Looper}.
   */
  public static Handler createHandlerForCurrentLooper() {
    return createHandlerForCurrentLooper(/* callback= */ null);
  }

  /**
   * Creates a {@link Handler} with the specified {@link Handler.Callback} on the current {@link
   * Looper} thread.
   *
   * <p>The method accepts partially initialized objects as callback under the assumption that the
   * Handler won't be used to send messages until the callback is fully initialized.
   *
   * @param callback A {@link Handler.Callback}. May be a partially initialized class, or null if no
   *     callback is required.
   * @return A {@link Handler} with the specified callback on the current {@link Looper} thread.
   * @throws IllegalStateException If the current thread doesn't have a {@link Looper}.
   */
  public static Handler createHandlerForCurrentLooper(
      @Nullable Handler.Callback callback) {
    return createHandler(Assertions.checkStateNotNull(Looper.myLooper()), callback);
  }

  /**
   * Creates a {@link Handler} on the current {@link Looper} thread.
   *
   * <p>If the current thread doesn't have a {@link Looper}, the application's main thread {@link
   * Looper} is used.
   */
  public static Handler createHandlerForCurrentOrMainLooper() {
    return createHandlerForCurrentOrMainLooper(/* callback= */ null);
  }

  /**
   * Creates a {@link Handler} with the specified {@link Handler.Callback} on the current {@link
   * Looper} thread.
   *
   * <p>The method accepts partially initialized objects as callback under the assumption that the
   * Handler won't be used to send messages until the callback is fully initialized.
   *
   * <p>If the current thread doesn't have a {@link Looper}, the application's main thread {@link
   * Looper} is used.
   *
   * @param callback A {@link Handler.Callback}. May be a partially initialized class, or null if no
   *     callback is required.
   * @return A {@link Handler} with the specified callback on the current {@link Looper} thread.
   */
  public static Handler createHandlerForCurrentOrMainLooper(
      @Nullable Handler.Callback callback) {
    return createHandler(getCurrentOrMainLooper(), callback);
  }

  /**
   * Creates a {@link Handler} with the specified {@link Handler.Callback} on the specified {@link
   * Looper} thread.
   *
   * <p>The method accepts partially initialized objects as callback under the assumption that the
   * Handler won't be used to send messages until the callback is fully initialized.
   *
   * @param looper A {@link Looper} to run the callback on.
   * @param callback A {@link Handler.Callback}. May be a partially initialized class, or null if no
   *     callback is required.
   * @return A {@link Handler} with the specified callback on the current {@link Looper} thread.
   */
  @SuppressWarnings({"nullness:argument", "nullness:return"})
  public static Handler createHandler(
      Looper looper, @Nullable Handler.Callback callback) {
    return new Handler(looper, callback);
  }

  /**
   * Posts the {@link Runnable} if the calling thread differs with the {@link Looper} of the {@link
   * Handler}. Otherwise, runs the {@link Runnable} directly.
   *
   * @param handler The handler to which the {@link Runnable} will be posted.
   * @param runnable The runnable to either post or run.
   * @return {@code true} if the {@link Runnable} was successfully posted to the {@link Handler} or
   *     run. {@code false} otherwise.
   */
  public static boolean postOrRun(Handler handler, Runnable runnable) {
    Looper looper = handler.getLooper();
    if (!looper.getThread().isAlive()) {
      return false;
    }
    if (handler.getLooper() == Looper.myLooper()) {
      runnable.run();
      return true;
    } else {
      return handler.post(runnable);
    }
  }

  /**
   * Returns the {@link Looper} associated with the current thread, or the {@link Looper} of the
   * application's main thread if the current thread doesn't have a {@link Looper}.
   */
  public static Looper getCurrentOrMainLooper() {
    @Nullable Looper myLooper = Looper.myLooper();
    return myLooper != null ? myLooper : Looper.getMainLooper();
  }

  /**
   * Instantiates a new single threaded executor whose thread has the specified name.
   *
   * @param threadName The name of the thread.
   * @return The executor.
   */
  public static ExecutorService newSingleThreadExecutor(String threadName) {
    return Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, threadName));
  }

  /**
   * Closes a {@link Closeable}, suppressing any {@link IOException} that may occur. Both {@link
   * java.io.OutputStream} and {@link InputStream} are {@code Closeable}.
   *
   * @param closeable The {@link Closeable} to close.
   */
  public static void closeQuietly(@Nullable Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException e) {
      // Ignore.
    }
  }

  /**
   * Reads an integer from a {@link Parcel} and interprets it as a boolean, with 0 mapping to false
   * and all other values mapping to true.
   *
   * @param parcel The {@link Parcel} to read from.
   * @return The read value.
   */
  public static boolean readBoolean(Parcel parcel) {
    return parcel.readInt() != 0;
  }

  /**
   * Writes a boolean to a {@link Parcel}. The boolean is written as an integer with value 1 (true)
   * or 0 (false).
   *
   * @param parcel The {@link Parcel} to write to.
   * @param value The value to write.
   */
  public static void writeBoolean(Parcel parcel, boolean value) {
    parcel.writeInt(value ? 1 : 0);
  }

  /**
   * Returns the language tag for a {@link Locale}.
   *
   * @param locale A {@link Locale}.
   * @return The language tag.
   */
  public static String getLocaleLanguageTag(Locale locale) {
    return SDK_INT >= 21 ? getLocaleLanguageTagV21(locale) : locale.toString();
  }


  /**
   * Returns a new {@link String} constructed by decoding UTF-8 encoded bytes.
   *
   * @param bytes The UTF-8 encoded bytes to decode.
   * @return The string.
   */
  public static String fromUtf8Bytes(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Returns a new {@link String} constructed by decoding UTF-8 encoded bytes in a subarray.
   *
   * @param bytes The UTF-8 encoded bytes to decode.
   * @param offset The index of the first byte to decode.
   * @param length The number of bytes to decode.
   * @return The string.
   */
  public static String fromUtf8Bytes(byte[] bytes, int offset, int length) {
    return new String(bytes, offset, length, StandardCharsets.UTF_8);
  }

  /**
   * Returns a new byte array containing the code points of a {@link String} encoded using UTF-8.
   *
   * @param value The {@link String} whose bytes should be obtained.
   * @return The code points encoding using UTF-8.
   */
  public static byte[] getUtf8Bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Splits a string using {@code value.split(regex, -1}). Note: this is is similar to {@link
   * String#split(String)} but empty matches at the end of the string will not be omitted from the
   * returned array.
   *
   * @param value The string to split.
   * @param regex A delimiting regular expression.
   * @return The array of strings resulting from splitting the string.
   */
  public static String[] split(String value, String regex) {
    return value.split(regex, /* limit= */ -1);
  }

  /**
   * Splits the string at the first occurrence of the delimiter {@code regex}. If the delimiter does
   * not match, returns an array with one element which is the input string. If the delimiter does
   * match, returns an array with the portion of the string before the delimiter and the rest of the
   * string.
   *
   * @param value The string.
   * @param regex A delimiting regular expression.
   * @return The string split by the first occurrence of the delimiter.
   */
  public static String[] splitAtFirst(String value, String regex) {
    return value.split(regex, /* limit= */ 2);
  }

  /**
   * Returns whether the given character is a carriage return ('\r') or a line feed ('\n').
   *
   * @param c The character.
   * @return Whether the given character is a linebreak.
   */
  public static boolean isLinebreak(int c) {
    return c == '\n' || c == '\r';
  }

  /**
   * Formats a string using {@link Locale#US}.
   *
   * @see String#format(String, Object...)
   */
  public static String formatInvariant(String format, Object... args) {
    return String.format(Locale.US, format, args);
  }

  /**
   * Divides a {@code numerator} by a {@code denominator}, returning the ceiled result.
   *
   * @param numerator The numerator to divide.
   * @param denominator The denominator to divide by.
   * @return The ceiled result of the division.
   */
  public static int ceilDivide(int numerator, int denominator) {
    return (numerator + denominator - 1) / denominator;
  }

  /**
   * Divides a {@code numerator} by a {@code denominator}, returning the ceiled result.
   *
   * @param numerator The numerator to divide.
   * @param denominator The denominator to divide by.
   * @return The ceiled result of the division.
   */
  public static long ceilDivide(long numerator, long denominator) {
    return (numerator + denominator - 1) / denominator;
  }

  /**
   * Constrains a value to the specified bounds.
   *
   * @param value The value to constrain.
   * @param min The lower bound.
   * @param max The upper bound.
   * @return The constrained value {@code Math.max(min, Math.min(value, max))}.
   */
  public static int constrainValue(int value, int min, int max) {
    return max(min, min(value, max));
  }

  /**
   * Constrains a value to the specified bounds.
   *
   * @param value The value to constrain.
   * @param min The lower bound.
   * @param max The upper bound.
   * @return The constrained value {@code Math.max(min, Math.min(value, max))}.
   */
  public static long constrainValue(long value, long min, long max) {
    return max(min, min(value, max));
  }

  /**
   * Constrains a value to the specified bounds.
   *
   * @param value The value to constrain.
   * @param min The lower bound.
   * @param max The upper bound.
   * @return The constrained value {@code Math.max(min, Math.min(value, max))}.
   */
  public static float constrainValue(float value, float min, float max) {
    return max(min, min(value, max));
  }

  /**
   * Returns the sum of two arguments, or a third argument if the result overflows.
   *
   * @param x The first value.
   * @param y The second value.
   * @param overflowResult The return value if {@code x + y} overflows.
   * @return {@code x + y}, or {@code overflowResult} if the result overflows.
   */
  public static long addWithOverflowDefault(long x, long y, long overflowResult) {
    long result = x + y;
    // See Hacker's Delight 2-13 (H. Warren Jr).
    if (((x ^ result) & (y ^ result)) < 0) {
      return overflowResult;
    }
    return result;
  }

  /**
   * Returns the difference between two arguments, or a third argument if the result overflows.
   *
   * @param x The first value.
   * @param y The second value.
   * @param overflowResult The return value if {@code x - y} overflows.
   * @return {@code x - y}, or {@code overflowResult} if the result overflows.
   */
  public static long subtractWithOverflowDefault(long x, long y, long overflowResult) {
    long result = x - y;
    // See Hacker's Delight 2-13 (H. Warren Jr).
    if (((x ^ y) & (x ^ result)) < 0) {
      return overflowResult;
    }
    return result;
  }

  /**
   * Returns the index of the first occurrence of {@code value} in {@code array}, or {@link
   * C#INDEX_UNSET} if {@code value} is not contained in {@code array}.
   *
   * @param array The array to search.
   * @param value The value to search for.
   * @return The index of the first occurrence of value in {@code array}, or {@link C#INDEX_UNSET}
   *     if {@code value} is not contained in {@code array}.
   */
  public static int linearSearch(int[] array, int value) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] == value) {
        return i;
      }
    }
    return C.INDEX_UNSET;
  }

  /**
   * Returns the index of the first occurrence of {@code value} in {@code array}, or {@link
   * C#INDEX_UNSET} if {@code value} is not contained in {@code array}.
   *
   * @param array The array to search.
   * @param value The value to search for.
   * @return The index of the first occurrence of value in {@code array}, or {@link C#INDEX_UNSET}
   *     if {@code value} is not contained in {@code array}.
   */
  public static int linearSearch(long[] array, long value) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] == value) {
        return i;
      }
    }
    return C.INDEX_UNSET;
  }

  /**
   * Returns the index of the largest element in {@code array} that is less than (or optionally
   * equal to) a specified {@code value}.
   *
   * <p>The search is performed using a binary search algorithm, so the array must be sorted. If the
   * array contains multiple elements equal to {@code value} and {@code inclusive} is true, the
   * index of the first one will be returned.
   *
   * @param array The array to search.
   * @param value The value being searched for.
   * @param inclusive If the value is present in the array, whether to return the corresponding
   *     index. If false then the returned index corresponds to the largest element strictly less
   *     than the value.
   * @param stayInBounds If true, then 0 will be returned in the case that the value is smaller than
   *     the smallest element in the array. If false then -1 will be returned.
   * @return The index of the largest element in {@code array} that is less than (or optionally
   *     equal to) {@code value}.
   */
  public static int binarySearchFloor(
      int[] array, int value, boolean inclusive, boolean stayInBounds) {
    int index = Arrays.binarySearch(array, value);
    if (index < 0) {
      index = -(index + 2);
    } else {
      while (--index >= 0 && array[index] == value) {}
      if (inclusive) {
        index++;
      }
    }
    return stayInBounds ? max(0, index) : index;
  }

  /**
   * Returns the index of the largest element in {@code array} that is less than (or optionally
   * equal to) a specified {@code value}.
   *
   * <p>The search is performed using a binary search algorithm, so the array must be sorted. If the
   * array contains multiple elements equal to {@code value} and {@code inclusive} is true, the
   * index of the first one will be returned.
   *
   * @param array The array to search.
   * @param value The value being searched for.
   * @param inclusive If the value is present in the array, whether to return the corresponding
   *     index. If false then the returned index corresponds to the largest element strictly less
   *     than the value.
   * @param stayInBounds If true, then 0 will be returned in the case that the value is smaller than
   *     the smallest element in the array. If false then -1 will be returned.
   * @return The index of the largest element in {@code array} that is less than (or optionally
   *     equal to) {@code value}.
   */
  public static int binarySearchFloor(
      long[] array, long value, boolean inclusive, boolean stayInBounds) {
    int index = Arrays.binarySearch(array, value);
    if (index < 0) {
      index = -(index + 2);
    } else {
      while (--index >= 0 && array[index] == value) {}
      if (inclusive) {
        index++;
      }
    }
    return stayInBounds ? max(0, index) : index;
  }

  /**
   * Returns the index of the largest element in {@code list} that is less than (or optionally equal
   * to) a specified {@code value}.
   *
   * <p>The search is performed using a binary search algorithm, so the list must be sorted. If the
   * list contains multiple elements equal to {@code value} and {@code inclusive} is true, the index
   * of the first one will be returned.
   *
   * @param <T> The type of values being searched.
   * @param list The list to search.
   * @param value The value being searched for.
   * @param inclusive If the value is present in the list, whether to return the corresponding
   *     index. If false then the returned index corresponds to the largest element strictly less
   *     than the value.
   * @param stayInBounds If true, then 0 will be returned in the case that the value is smaller than
   *     the smallest element in the list. If false then -1 will be returned.
   * @return The index of the largest element in {@code list} that is less than (or optionally equal
   *     to) {@code value}.
   */
  public static <T extends Comparable<? super T>> int binarySearchFloor(
      List<? extends Comparable<? super T>> list,
      T value,
      boolean inclusive,
      boolean stayInBounds) {
    int index = Collections.binarySearch(list, value);
    if (index < 0) {
      index = -(index + 2);
    } else {
      while (--index >= 0 && list.get(index).compareTo(value) == 0) {}
      if (inclusive) {
        index++;
      }
    }
    return stayInBounds ? max(0, index) : index;
  }


  /**
   * Returns the index of the smallest element in {@code array} that is greater than (or optionally
   * equal to) a specified {@code value}.
   *
   * <p>The search is performed using a binary search algorithm, so the array must be sorted. If the
   * array contains multiple elements equal to {@code value} and {@code inclusive} is true, the
   * index of the last one will be returned.
   *
   * @param array The array to search.
   * @param value The value being searched for.
   * @param inclusive If the value is present in the array, whether to return the corresponding
   *     index. If false then the returned index corresponds to the smallest element strictly
   *     greater than the value.
   * @param stayInBounds If true, then {@code (a.length - 1)} will be returned in the case that the
   *     value is greater than the largest element in the array. If false then {@code a.length} will
   *     be returned.
   * @return The index of the smallest element in {@code array} that is greater than (or optionally
   *     equal to) {@code value}.
   */
  public static int binarySearchCeil(
      int[] array, int value, boolean inclusive, boolean stayInBounds) {
    int index = Arrays.binarySearch(array, value);
    if (index < 0) {
      index = ~index;
    } else {
      while (++index < array.length && array[index] == value) {}
      if (inclusive) {
        index--;
      }
    }
    return stayInBounds ? min(array.length - 1, index) : index;
  }

  /**
   * Returns the index of the smallest element in {@code array} that is greater than (or optionally
   * equal to) a specified {@code value}.
   *
   * <p>The search is performed using a binary search algorithm, so the array must be sorted. If the
   * array contains multiple elements equal to {@code value} and {@code inclusive} is true, the
   * index of the last one will be returned.
   *
   * @param array The array to search.
   * @param value The value being searched for.
   * @param inclusive If the value is present in the array, whether to return the corresponding
   *     index. If false then the returned index corresponds to the smallest element strictly
   *     greater than the value.
   * @param stayInBounds If true, then {@code (a.length - 1)} will be returned in the case that the
   *     value is greater than the largest element in the array. If false then {@code a.length} will
   *     be returned.
   * @return The index of the smallest element in {@code array} that is greater than (or optionally
   *     equal to) {@code value}.
   */
  public static int binarySearchCeil(
      long[] array, long value, boolean inclusive, boolean stayInBounds) {
    int index = Arrays.binarySearch(array, value);
    if (index < 0) {
      index = ~index;
    } else {
      while (++index < array.length && array[index] == value) {}
      if (inclusive) {
        index--;
      }
    }
    return stayInBounds ? min(array.length - 1, index) : index;
  }

  /**
   * Returns the index of the smallest element in {@code list} that is greater than (or optionally
   * equal to) a specified value.
   *
   * <p>The search is performed using a binary search algorithm, so the list must be sorted. If the
   * list contains multiple elements equal to {@code value} and {@code inclusive} is true, the index
   * of the last one will be returned.
   *
   * @param <T> The type of values being searched.
   * @param list The list to search.
   * @param value The value being searched for.
   * @param inclusive If the value is present in the list, whether to return the corresponding
   *     index. If false then the returned index corresponds to the smallest element strictly
   *     greater than the value.
   * @param stayInBounds If true, then {@code (list.size() - 1)} will be returned in the case that
   *     the value is greater than the largest element in the list. If false then {@code
   *     list.size()} will be returned.
   * @return The index of the smallest element in {@code list} that is greater than (or optionally
   *     equal to) {@code value}.
   */
  public static <T extends Comparable<? super T>> int binarySearchCeil(
      List<? extends Comparable<? super T>> list,
      T value,
      boolean inclusive,
      boolean stayInBounds) {
    int index = Collections.binarySearch(list, value);
    if (index < 0) {
      index = ~index;
    } else {
      int listSize = list.size();
      while (++index < listSize && list.get(index).compareTo(value) == 0) {}
      if (inclusive) {
        index--;
      }
    }
    return stayInBounds ? min(list.size() - 1, index) : index;
  }

  /**
   * Compares two long values and returns the same value as {@code Long.compare(long, long)}.
   *
   * @param left The left operand.
   * @param right The right operand.
   * @return 0, if left == right, a negative value if left &lt; right, or a positive value if left
   *     &gt; right.
   */
  public static int compareLong(long left, long right) {
    return left < right ? -1 : left == right ? 0 : 1;
  }

  /**
   * Returns the minimum value in the given {@link SparseLongArray}.
   *
   * @param sparseLongArray The {@link SparseLongArray}.
   * @return The minimum value.
   * @throws NoSuchElementException If the array is empty.
   */
  @RequiresApi(18)
  public static long minValue(SparseLongArray sparseLongArray) {
    if (sparseLongArray.size() == 0) {
      throw new NoSuchElementException();
    }
    long min = Long.MAX_VALUE;
    for (int i = 0; i < sparseLongArray.size(); i++) {
      min = min(min, sparseLongArray.valueAt(i));
    }
    return min;
  }

  /**
   * Returns the maximum value in the given {@link SparseLongArray}.
   *
   * @param sparseLongArray The {@link SparseLongArray}.
   * @return The maximum value.
   * @throws NoSuchElementException If the array is empty.
   */
  @RequiresApi(18)
  public static long maxValue(SparseLongArray sparseLongArray) {
    if (sparseLongArray.size() == 0) {
      throw new NoSuchElementException();
    }
    long max = Long.MIN_VALUE;
    for (int i = 0; i < sparseLongArray.size(); i++) {
      max = max(max, sparseLongArray.valueAt(i));
    }
    return max;
  }

  /**
   * Converts a time in microseconds to the corresponding time in milliseconds, preserving {@link
   * C#TIME_UNSET} and {@link C#TIME_END_OF_SOURCE} values.
   *
   * @param timeUs The time in microseconds.
   * @return The corresponding time in milliseconds.
   */
  public static long usToMs(long timeUs) {
    return (timeUs == C.TIME_UNSET || timeUs == C.TIME_END_OF_SOURCE) ? timeUs : (timeUs / 1000);
  }

  /**
   * Converts a time in milliseconds to the corresponding time in microseconds, preserving {@link
   * C#TIME_UNSET} values and {@link C#TIME_END_OF_SOURCE} values.
   *
   * @param timeMs The time in milliseconds.
   * @return The corresponding time in microseconds.
   */
  public static long msToUs(long timeMs) {
    return (timeMs == C.TIME_UNSET || timeMs == C.TIME_END_OF_SOURCE) ? timeMs : (timeMs * 1000);
  }

  /**
   * Returns a user agent string based on the given application name and the library version.
   *
   * @param context A valid context of the calling application.
   * @param applicationName String that will be prefix'ed to the generated user agent.
   * @return A user agent string generated using the applicationName and the library version.
   */
  public static String getUserAgent(Context context, String applicationName) {
    String versionName;
    try {
      String packageName = context.getPackageName();
      PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
      versionName = info.versionName;
    } catch (NameNotFoundException e) {
      versionName = "?";
    }
    return applicationName
        + "/"
        + versionName
        + " (Linux;Android "
        + Build.VERSION.RELEASE
        + ") ";
  }
  /**
   * Returns the specified millisecond time formatted as a string.
   *
   * @param builder The builder that {@code formatter} will write to.
   * @param formatter The formatter.
   * @param timeMs The time to format as a string, in milliseconds.
   * @return The time formatted as a string.
   */
  public static String getStringForTime(StringBuilder builder, Formatter formatter, long timeMs) {
    if (timeMs == C.TIME_UNSET) {
      timeMs = 0;
    }
    String prefix = timeMs < 0 ? "-" : "";
    timeMs = abs(timeMs);
    long totalSeconds = (timeMs + 500) / 1000;
    long seconds = totalSeconds % 60;
    long minutes = (totalSeconds / 60) % 60;
    long hours = totalSeconds / 3600;
    builder.setLength(0);
    return hours > 0
        ? formatter.format("%s%d:%02d:%02d", prefix, hours, minutes, seconds).toString()
        : formatter.format("%s%02d:%02d", prefix, minutes, seconds).toString();
  }

  /**
   * Escapes a string so that it's safe for use as a file or directory name on at least FAT32
   * filesystems. FAT32 is the most restrictive of all filesystems still commonly used today.
   *
   * <p>For simplicity, this only handles common characters known to be illegal on FAT32: &lt;,
   * &gt;, :, ", /, \, |, ?, and *. % is also escaped since it is used as the escape character.
   * Escaping is performed in a consistent way so that no collisions occur and {@link
   * #unescapeFileName(String)} can be used to retrieve the original file name.
   *
   * @param fileName File name to be escaped.
   * @return An escaped file name which will be safe for use on at least FAT32 filesystems.
   */
  public static String escapeFileName(String fileName) {
    int length = fileName.length();
    int charactersToEscapeCount = 0;
    for (int i = 0; i < length; i++) {
      if (shouldEscapeCharacter(fileName.charAt(i))) {
        charactersToEscapeCount++;
      }
    }
    if (charactersToEscapeCount == 0) {
      return fileName;
    }

    int i = 0;
    StringBuilder builder = new StringBuilder(length + charactersToEscapeCount * 2);
    while (charactersToEscapeCount > 0) {
      char c = fileName.charAt(i++);
      if (shouldEscapeCharacter(c)) {
        builder.append('%').append(Integer.toHexString(c));
        charactersToEscapeCount--;
      } else {
        builder.append(c);
      }
    }
    if (i < length) {
      builder.append(fileName, i, length);
    }
    return builder.toString();
  }

  private static boolean shouldEscapeCharacter(char c) {
    switch (c) {
      case '<':
      case '>':
      case ':':
      case '"':
      case '/':
      case '\\':
      case '|':
      case '?':
      case '*':
      case '%':
        return true;
      default:
        return false;
    }
  }

  /**
   * Unescapes an escaped file or directory name back to its original value.
   *
   * <p>See {@link #escapeFileName(String)} for more information.
   *
   * @param fileName File name to be unescaped.
   * @return The original value of the file name before it was escaped, or null if the escaped
   *     fileName seems invalid.
   */
  @Nullable
  public static String unescapeFileName(String fileName) {
    int length = fileName.length();
    int percentCharacterCount = 0;
    for (int i = 0; i < length; i++) {
      if (fileName.charAt(i) == '%') {
        percentCharacterCount++;
      }
    }
    if (percentCharacterCount == 0) {
      return fileName;
    }

    int expectedLength = length - percentCharacterCount * 2;
    StringBuilder builder = new StringBuilder(expectedLength);
    Matcher matcher = ESCAPED_CHARACTER_PATTERN.matcher(fileName);
    int startOfNotEscaped = 0;
    while (percentCharacterCount > 0 && matcher.find()) {
      char unescapedCharacter = (char) Integer.parseInt(checkNotNull(matcher.group(1)), 16);
      builder.append(fileName, startOfNotEscaped, matcher.start()).append(unescapedCharacter);
      startOfNotEscaped = matcher.end();
      percentCharacterCount--;
    }
    if (startOfNotEscaped < length) {
      builder.append(fileName, startOfNotEscaped, length);
    }
    if (builder.length() != expectedLength) {
      return null;
    }
    return builder.toString();
  }

  /**
   * A hacky method that always throws {@code t} even if {@code t} is a checked exception, and is
   * not declared to be thrown.
   */
  public static void sneakyThrow(Throwable t) {
    sneakyThrowInternal(t);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrowInternal(Throwable t) throws T {
    throw (T) t;
  }

  /** Recursively deletes a directory and its content. */
  public static void recursiveDelete(File fileOrDirectory) {
    File[] directoryFiles = fileOrDirectory.listFiles();
    if (directoryFiles != null) {
      for (File child : directoryFiles) {
        recursiveDelete(child);
      }
    }
    fileOrDirectory.delete();
  }

  /** Creates an empty directory in the directory returned by {@link Context#getCacheDir()}. */
  public static File createTempDirectory(Context context, String prefix) throws IOException {
    File tempFile = createTempFile(context, prefix);
    tempFile.delete(); // Delete the temp file.
    tempFile.mkdir(); // Create a directory with the same name.
    return tempFile;
  }

  /**
   * Converts an integer to a long by unsigned conversion.
   *
   * <p>This method is equivalent to {@link Integer#toUnsignedLong(int)} for API 26+.
   */
  public static long toUnsignedLong(int x) {
    // x is implicitly casted to a long before the bit operation is executed but this does not
    // impact the method correctness.
    return x & 0xFFFFFFFFL;
  }

  /**
   * Returns the long that is composed of the bits of the 2 specified integers.
   *
   * @param mostSignificantBits The 32 most significant bits of the long to return.
   * @param leastSignificantBits The 32 least significant bits of the long to return.
   * @return a long where its 32 most significant bits are {@code mostSignificantBits} bits and its
   *   32 least significant bits are {@code leastSignificantBits}.
   */
  public static long toLong(int mostSignificantBits, int leastSignificantBits) {
    return (toUnsignedLong(mostSignificantBits) << 32) | toUnsignedLong(leastSignificantBits);
  }
  /** Creates a new empty file in the directory returned by {@link Context#getCacheDir()}. */
  public static File createTempFile(Context context, String prefix) throws IOException {
    return File.createTempFile(prefix, null, checkNotNull(context.getCacheDir()));
  }

  /**
   * Returns the result of updating a CRC-32 with the specified bytes in a "most significant bit
   * first" order.
   *
   * @param bytes Array containing the bytes to update the crc value with.
   * @param start The index to the first byte in the byte range to update the crc with.
   * @param end The index after the last byte in the byte range to update the crc with.
   * @param initialValue The initial value for the crc calculation.
   * @return The result of updating the initial value with the specified bytes.
   */
  public static int crc32(byte[] bytes, int start, int end, int initialValue) {
    for (int i = start; i < end; i++) {
      initialValue =
          (initialValue << 8)
              ^ CRC32_BYTES_MSBF[((initialValue >>> 24) ^ (bytes[i] & 0xFF)) & 0xFF];
    }
    return initialValue;
  }

  /**
   * Returns the result of updating a CRC-8 with the specified bytes in a "most significant bit
   * first" order.
   *
   * @param bytes Array containing the bytes to update the crc value with.
   * @param start The index to the first byte in the byte range to update the crc with.
   * @param end The index after the last byte in the byte range to update the crc with.
   * @param initialValue The initial value for the crc calculation.
   * @return The result of updating the initial value with the specified bytes.
   */
  public static int crc8(byte[] bytes, int start, int end, int initialValue) {
    for (int i = start; i < end; i++) {
      initialValue = CRC8_BYTES_MSBF[initialValue ^ (bytes[i] & 0xFF)];
    }
    return initialValue;
  }

  /** Compresses {@code input} using gzip and returns the result in a newly allocated byte array. */
  public static byte[] gzip(byte[] input) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (GZIPOutputStream os = new GZIPOutputStream(output)) {
      os.write(input);
    } catch (IOException e) {
      // A ByteArrayOutputStream wrapped in a GZipOutputStream should never throw IOException since
      // no I/O is happening.
      throw new IllegalStateException(e);
    }
    return output.toByteArray();
  }

  /**
   * Absolute <i>get</i> method for reading an int value in {@link ByteOrder#BIG_ENDIAN} in a {@link
   * ByteBuffer}. Same as {@link ByteBuffer#getInt(int)} except the buffer's order as returned by
   * {@link ByteBuffer#order()} is ignored and {@link ByteOrder#BIG_ENDIAN} is used instead.
   *
   * @param buffer The buffer from which to read an int in big endian.
   * @param index The index from which the bytes will be read.
   * @return The int value at the given index with the buffer bytes ordered most significant to
   *     least significant.
   */
  public static int getBigEndianInt(ByteBuffer buffer, int index) {
    int value = buffer.getInt(index);
    return buffer.order() == ByteOrder.BIG_ENDIAN ? value : Integer.reverseBytes(value);
  }

  /** Returns the default {@link Locale.Category#DISPLAY DISPLAY} {@link Locale}. */
  public static Locale getDefaultDisplayLocale() {
    return SDK_INT >= 24 ? Locale.getDefault(Locale.Category.DISPLAY) : Locale.getDefault();
  }

  /**
   * Returns whether the app is running on a TV device.
   *
   * @param context Any context.
   * @return Whether the app is running on a TV device.
   */
  public static boolean isTv(Context context) {
    // See https://developer.android.com/training/tv/start/hardware.html#runtime-check.
    @Nullable
    UiModeManager uiModeManager =
        (UiModeManager) context.getApplicationContext().getSystemService(UI_MODE_SERVICE);
    return uiModeManager != null
        && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
  }

  /**
   * Returns whether the app is running on an automotive device.
   *
   * @param context Any context.
   * @return Whether the app is running on an automotive device.
   */
  public static boolean isAutomotive(Context context) {
    return SDK_INT >= 23
        && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
  }

  /**
   * Gets the size of the current mode of the default display, in pixels.
   *
   * <p>Note that due to application UI scaling, the number of pixels made available to applications
   * (as reported by {@link Display#getSize(Point)} may differ from the mode's actual resolution (as
   * reported by this function). For example, applications running on a display configured with a 4K
   * mode may have their UI laid out and rendered in 1080p and then scaled up. Applications can take
   * advantage of the full mode resolution through a {@link SurfaceView} using full size buffers.
   *
   * @param context Any context.
   * @return The size of the current mode, in pixels.
   */
  public static Point getCurrentDisplayModeSize(Context context) {
    @Nullable Display defaultDisplay = null;
    if (SDK_INT >= 17) {
      @Nullable
      DisplayManager displayManager =
          (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
      // We don't expect displayManager to ever be null, so this check is just precautionary.
      // Consider removing it when the library minSdkVersion is increased to 17 or higher.
      if (displayManager != null) {
        defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
      }
    }
    if (defaultDisplay == null) {
      WindowManager windowManager =
          checkNotNull((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
      defaultDisplay = windowManager.getDefaultDisplay();
    }
    return getCurrentDisplayModeSize(context, defaultDisplay);
  }

  /**
   * Gets the size of the current mode of the specified display, in pixels.
   *
   * <p>Note that due to application UI scaling, the number of pixels made available to applications
   * (as reported by {@link Display#getSize(Point)} may differ from the mode's actual resolution (as
   * reported by this function). For example, applications running on a display configured with a 4K
   * mode may have their UI laid out and rendered in 1080p and then scaled up. Applications can take
   * advantage of the full mode resolution through a {@link SurfaceView} using full size buffers.
   *
   * @param context Any context.
   * @param display The display whose size is to be returned.
   * @return The size of the current mode, in pixels.
   */
  public static Point getCurrentDisplayModeSize(Context context, Display display) {
    if (display.getDisplayId() == Display.DEFAULT_DISPLAY && isTv(context)) {
      // On Android TVs it's common for the UI to be driven at a lower resolution than the physical
      // resolution of the display (e.g., driving the UI at 1080p when the display is 4K).
      // SurfaceView outputs are still able to use the full physical resolution on such devices.
      //
      // Prior to API level 26, the Display object did not provide a way to obtain the true physical
      // resolution of the display. From API level 26, Display.getMode().getPhysical[Width|Height]
      // is expected to return the display's true physical resolution, but we still see devices
      // setting their hardware compositor output size incorrectly, which makes this unreliable.
      // Hence for TV devices, we try and read the display's true physical resolution from system
      // properties.
      //
      // From API level 28, Treble may prevent the system from writing sys.display-size, so we check
      // vendor.display-size instead.
      @Nullable
      String displaySize =
          SDK_INT < 28
              ? getSystemProperty("sys.display-size")
              : getSystemProperty("vendor.display-size");
      // If we managed to read the display size, attempt to parse it.
      if (!TextUtils.isEmpty(displaySize)) {
        try {
          String[] displaySizeParts = split(displaySize.trim(), "x");
          if (displaySizeParts.length == 2) {
            int width = Integer.parseInt(displaySizeParts[0]);
            int height = Integer.parseInt(displaySizeParts[1]);
            if (width > 0 && height > 0) {
              return new Point(width, height);
            }
          }
        } catch (NumberFormatException e) {
          // Do nothing.
        }
        Log.e(TAG, "Invalid display size: " + displaySize);
      }

      // Sony Android TVs advertise support for 4k output via a system feature.
      if ("Sony".equals(MANUFACTURER)
          && MODEL.startsWith("BRAVIA")
          && context.getPackageManager().hasSystemFeature("com.sony.dtv.hardware.panel.qfhd")) {
        return new Point(3840, 2160);
      }
    }

    Point displaySize = new Point();
    if (SDK_INT >= 23) {
      getDisplaySizeV23(display, displaySize);
    } else if (SDK_INT >= 17) {
      getDisplaySizeV17(display, displaySize);
    } else {
      getDisplaySizeV16(display, displaySize);
    }
    return displaySize;
  }

  /**
   * Returns the current time in milliseconds since the epoch.
   *
   * @param elapsedRealtimeEpochOffsetMs The offset between {@link SystemClock#elapsedRealtime()}
   *     and the time since the Unix epoch, or {@link C#TIME_UNSET} if unknown.
   * @return The Unix time in milliseconds since the epoch.
   */
  public static long getNowUnixTimeMs(long elapsedRealtimeEpochOffsetMs) {
    return elapsedRealtimeEpochOffsetMs == C.TIME_UNSET
        ? System.currentTimeMillis()
        : SystemClock.elapsedRealtime() + elapsedRealtimeEpochOffsetMs;
  }

  /**
   * Moves the elements starting at {@code fromIndex} to {@code newFromIndex}.
   *
   * @param items The list of which to move elements.
   * @param fromIndex The index at which the items to move start.
   * @param toIndex The index up to which elements should be moved (exclusive).
   * @param newFromIndex The new from index.
   */
  @SuppressWarnings("ExtendsObject") // See go/lsc-extends-object
  public static <T extends Object> void moveItems(
      List<T> items, int fromIndex, int toIndex, int newFromIndex) {
    ArrayDeque<T> removedItems = new ArrayDeque<>();
    int removedItemsLength = toIndex - fromIndex;
    for (int i = removedItemsLength - 1; i >= 0; i--) {
      removedItems.addFirst(items.remove(fromIndex + i));
    }
    items.addAll(min(newFromIndex, items.size()), removedItems);
  }

  /** Returns whether the table exists in the database. */
  public static boolean tableExists(SQLiteDatabase database, String tableName) {
    long count =
        DatabaseUtils.queryNumEntries(
            database, "sqlite_master", "tbl_name = ?", new String[] {tableName});
    return count > 0;
  }

  /**
   * Returns the sum of all summands of the given array.
   *
   * @param summands The summands to calculate the sum from.
   * @return The sum of all summands.
   */
  public static long sum(long... summands) {
    long sum = 0;
    for (long summand : summands) {
      sum += summand;
    }
    return sum;
  }

  @Nullable
  private static String getSystemProperty(String name) {
    try {
      @SuppressLint("PrivateApi")
      Class<?> systemProperties = Class.forName("android.os.SystemProperties");
      Method getMethod = systemProperties.getMethod("get", String.class);
      return (String) getMethod.invoke(systemProperties, name);
    } catch (Exception e) {
      Log.e(TAG, "Failed to read system property " + name, e);
      return null;
    }
  }

  @RequiresApi(23)
  private static void getDisplaySizeV23(Display display, Point outSize) {
    Display.Mode mode = display.getMode();
    outSize.x = mode.getPhysicalWidth();
    outSize.y = mode.getPhysicalHeight();
  }

  @RequiresApi(17)
  private static void getDisplaySizeV17(Display display, Point outSize) {
    display.getRealSize(outSize);
  }

  private static void getDisplaySizeV16(Display display, Point outSize) {
    display.getSize(outSize);
  }

  private static String[] getSystemLocales() {
    Configuration config = Resources.getSystem().getConfiguration();
    return SDK_INT >= 24
        ? getSystemLocalesV24(config)
        : new String[] {getLocaleLanguageTag(config.locale)};
  }

  @RequiresApi(24)
  private static String[] getSystemLocalesV24(Configuration config) {
    return split(config.getLocales().toLanguageTags(), ",");
  }

  @RequiresApi(21)
  private static String getLocaleLanguageTagV21(Locale locale) {
    return locale.toLanguageTag();
  }

  private static HashMap<String, String> createIsoLanguageReplacementMap() {
    String[] iso2Languages = Locale.getISOLanguages();
    HashMap<String, String> replacedLanguages =
        new HashMap<>(
            /* initialCapacity= */ iso2Languages.length + additionalIsoLanguageReplacements.length);
    for (String iso2 : iso2Languages) {
      try {
        // This returns the ISO 639-2/T code for the language.
        String iso3 = new Locale(iso2).getISO3Language();
        if (!TextUtils.isEmpty(iso3)) {
          replacedLanguages.put(iso3, iso2);
        }
      } catch (MissingResourceException e) {
        // Shouldn't happen for list of known languages, but we don't want to throw either.
      }
    }
    // Add additional replacement mappings.
    for (int i = 0; i < additionalIsoLanguageReplacements.length; i += 2) {
      replacedLanguages.put(
          additionalIsoLanguageReplacements[i], additionalIsoLanguageReplacements[i + 1]);
    }
    return replacedLanguages;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private static boolean requestExternalStoragePermission(Activity activity) {
    if (activity.checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      activity.requestPermissions(
          new String[] {permission.READ_EXTERNAL_STORAGE}, /* requestCode= */ 0);
      return true;
    }
    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private static boolean isTrafficRestricted(Uri uri) {
    return "http".equals(uri.getScheme())
        && !NetworkSecurityPolicy.getInstance()
            .isCleartextTrafficPermitted(checkNotNull(uri.getHost()));
  }

  private static String maybeReplaceLegacyLanguageTags(String languageTag) {
    for (int i = 0; i < isoLegacyTagReplacements.length; i += 2) {
      if (languageTag.startsWith(isoLegacyTagReplacements[i])) {
        return isoLegacyTagReplacements[i + 1]
            + languageTag.substring(/* beginIndex= */ isoLegacyTagReplacements[i].length());
      }
    }
    return languageTag;
  }

  // Additional mapping from ISO3 to ISO2 language codes.
  private static final String[] additionalIsoLanguageReplacements =
      new String[] {
        // Bibliographical codes defined in ISO 639-2/B, replaced by terminological code defined in
        // ISO 639-2/T. See https://en.wikipedia.org/wiki/List_of_ISO_639-2_codes.
        "alb", "sq",
        "arm", "hy",
        "baq", "eu",
        "bur", "my",
        "tib", "bo",
        "chi", "zh",
        "cze", "cs",
        "dut", "nl",
        "ger", "de",
        "gre", "el",
        "fre", "fr",
        "geo", "ka",
        "ice", "is",
        "mac", "mk",
        "mao", "mi",
        "may", "ms",
        "per", "fa",
        "rum", "ro",
        "scc", "hbs-srp",
        "slo", "sk",
        "wel", "cy",
        // Deprecated 2-letter codes, replaced by modern equivalent (including macrolanguage)
        // See https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes, "ISO 639:1988"
        "id", "ms-ind",
        "iw", "he",
        "heb", "he",
        "ji", "yi",
        // Individual macrolanguage codes mapped back to full macrolanguage code.
        // See https://en.wikipedia.org/wiki/ISO_639_macrolanguage
        "arb", "ar-arb",
        "in", "ms-ind",
        "ind", "ms-ind",
        "nb", "no-nob",
        "nob", "no-nob",
        "nn", "no-nno",
        "nno", "no-nno",
        "tw", "ak-twi",
        "twi", "ak-twi",
        "bs", "hbs-bos",
        "bos", "hbs-bos",
        "hr", "hbs-hrv",
        "hrv", "hbs-hrv",
        "sr", "hbs-srp",
        "srp", "hbs-srp",
        "cmn", "zh-cmn",
        "hak", "zh-hak",
        "nan", "zh-nan",
        "hsn", "zh-hsn"
      };

  // Legacy tags that have been replaced by modern equivalents (including macrolanguage)
  // See https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry.
  private static final String[] isoLegacyTagReplacements =
      new String[] {
        "i-lux", "lb",
        "i-hak", "zh-hak",
        "i-navajo", "nv",
        "no-bok", "no-nob",
        "no-nyn", "no-nno",
        "zh-guoyu", "zh-cmn",
        "zh-hakka", "zh-hak",
        "zh-min-nan", "zh-nan",
        "zh-xiang", "zh-hsn"
      };

  /**
   * Allows the CRC-32 calculation to be done byte by byte instead of bit per bit in the order "most
   * significant bit first".
   */
  private static final int[] CRC32_BYTES_MSBF = {
    0X00000000, 0X04C11DB7, 0X09823B6E, 0X0D4326D9, 0X130476DC, 0X17C56B6B, 0X1A864DB2,
    0X1E475005, 0X2608EDB8, 0X22C9F00F, 0X2F8AD6D6, 0X2B4BCB61, 0X350C9B64, 0X31CD86D3,
    0X3C8EA00A, 0X384FBDBD, 0X4C11DB70, 0X48D0C6C7, 0X4593E01E, 0X4152FDA9, 0X5F15ADAC,
    0X5BD4B01B, 0X569796C2, 0X52568B75, 0X6A1936C8, 0X6ED82B7F, 0X639B0DA6, 0X675A1011,
    0X791D4014, 0X7DDC5DA3, 0X709F7B7A, 0X745E66CD, 0X9823B6E0, 0X9CE2AB57, 0X91A18D8E,
    0X95609039, 0X8B27C03C, 0X8FE6DD8B, 0X82A5FB52, 0X8664E6E5, 0XBE2B5B58, 0XBAEA46EF,
    0XB7A96036, 0XB3687D81, 0XAD2F2D84, 0XA9EE3033, 0XA4AD16EA, 0XA06C0B5D, 0XD4326D90,
    0XD0F37027, 0XDDB056FE, 0XD9714B49, 0XC7361B4C, 0XC3F706FB, 0XCEB42022, 0XCA753D95,
    0XF23A8028, 0XF6FB9D9F, 0XFBB8BB46, 0XFF79A6F1, 0XE13EF6F4, 0XE5FFEB43, 0XE8BCCD9A,
    0XEC7DD02D, 0X34867077, 0X30476DC0, 0X3D044B19, 0X39C556AE, 0X278206AB, 0X23431B1C,
    0X2E003DC5, 0X2AC12072, 0X128E9DCF, 0X164F8078, 0X1B0CA6A1, 0X1FCDBB16, 0X018AEB13,
    0X054BF6A4, 0X0808D07D, 0X0CC9CDCA, 0X7897AB07, 0X7C56B6B0, 0X71159069, 0X75D48DDE,
    0X6B93DDDB, 0X6F52C06C, 0X6211E6B5, 0X66D0FB02, 0X5E9F46BF, 0X5A5E5B08, 0X571D7DD1,
    0X53DC6066, 0X4D9B3063, 0X495A2DD4, 0X44190B0D, 0X40D816BA, 0XACA5C697, 0XA864DB20,
    0XA527FDF9, 0XA1E6E04E, 0XBFA1B04B, 0XBB60ADFC, 0XB6238B25, 0XB2E29692, 0X8AAD2B2F,
    0X8E6C3698, 0X832F1041, 0X87EE0DF6, 0X99A95DF3, 0X9D684044, 0X902B669D, 0X94EA7B2A,
    0XE0B41DE7, 0XE4750050, 0XE9362689, 0XEDF73B3E, 0XF3B06B3B, 0XF771768C, 0XFA325055,
    0XFEF34DE2, 0XC6BCF05F, 0XC27DEDE8, 0XCF3ECB31, 0XCBFFD686, 0XD5B88683, 0XD1799B34,
    0XDC3ABDED, 0XD8FBA05A, 0X690CE0EE, 0X6DCDFD59, 0X608EDB80, 0X644FC637, 0X7A089632,
    0X7EC98B85, 0X738AAD5C, 0X774BB0EB, 0X4F040D56, 0X4BC510E1, 0X46863638, 0X42472B8F,
    0X5C007B8A, 0X58C1663D, 0X558240E4, 0X51435D53, 0X251D3B9E, 0X21DC2629, 0X2C9F00F0,
    0X285E1D47, 0X36194D42, 0X32D850F5, 0X3F9B762C, 0X3B5A6B9B, 0X0315D626, 0X07D4CB91,
    0X0A97ED48, 0X0E56F0FF, 0X1011A0FA, 0X14D0BD4D, 0X19939B94, 0X1D528623, 0XF12F560E,
    0XF5EE4BB9, 0XF8AD6D60, 0XFC6C70D7, 0XE22B20D2, 0XE6EA3D65, 0XEBA91BBC, 0XEF68060B,
    0XD727BBB6, 0XD3E6A601, 0XDEA580D8, 0XDA649D6F, 0XC423CD6A, 0XC0E2D0DD, 0XCDA1F604,
    0XC960EBB3, 0XBD3E8D7E, 0XB9FF90C9, 0XB4BCB610, 0XB07DABA7, 0XAE3AFBA2, 0XAAFBE615,
    0XA7B8C0CC, 0XA379DD7B, 0X9B3660C6, 0X9FF77D71, 0X92B45BA8, 0X9675461F, 0X8832161A,
    0X8CF30BAD, 0X81B02D74, 0X857130C3, 0X5D8A9099, 0X594B8D2E, 0X5408ABF7, 0X50C9B640,
    0X4E8EE645, 0X4A4FFBF2, 0X470CDD2B, 0X43CDC09C, 0X7B827D21, 0X7F436096, 0X7200464F,
    0X76C15BF8, 0X68860BFD, 0X6C47164A, 0X61043093, 0X65C52D24, 0X119B4BE9, 0X155A565E,
    0X18197087, 0X1CD86D30, 0X029F3D35, 0X065E2082, 0X0B1D065B, 0X0FDC1BEC, 0X3793A651,
    0X3352BBE6, 0X3E119D3F, 0X3AD08088, 0X2497D08D, 0X2056CD3A, 0X2D15EBE3, 0X29D4F654,
    0XC5A92679, 0XC1683BCE, 0XCC2B1D17, 0XC8EA00A0, 0XD6AD50A5, 0XD26C4D12, 0XDF2F6BCB,
    0XDBEE767C, 0XE3A1CBC1, 0XE760D676, 0XEA23F0AF, 0XEEE2ED18, 0XF0A5BD1D, 0XF464A0AA,
    0XF9278673, 0XFDE69BC4, 0X89B8FD09, 0X8D79E0BE, 0X803AC667, 0X84FBDBD0, 0X9ABC8BD5,
    0X9E7D9662, 0X933EB0BB, 0X97FFAD0C, 0XAFB010B1, 0XAB710D06, 0XA6322BDF, 0XA2F33668,
    0XBCB4666D, 0XB8757BDA, 0XB5365D03, 0XB1F740B4
  };

  /**
   * Allows the CRC-8 calculation to be done byte by byte instead of bit per bit in the order "most
   * significant bit first".
   */
  private static final int[] CRC8_BYTES_MSBF = {
    0x00, 0x07, 0x0E, 0x09, 0x1C, 0x1B, 0x12, 0x15, 0x38, 0x3F, 0x36, 0x31, 0x24, 0x23, 0x2A,
    0x2D, 0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, 0x48, 0x4F, 0x46, 0x41, 0x54, 0x53,
    0x5A, 0x5D, 0xE0, 0xE7, 0xEE, 0xE9, 0xFC, 0xFB, 0xF2, 0xF5, 0xD8, 0xDF, 0xD6, 0xD1, 0xC4,
    0xC3, 0xCA, 0xCD, 0x90, 0x97, 0x9E, 0x99, 0x8C, 0x8B, 0x82, 0x85, 0xA8, 0xAF, 0xA6, 0xA1,
    0xB4, 0xB3, 0xBA, 0xBD, 0xC7, 0xC0, 0xC9, 0xCE, 0xDB, 0xDC, 0xD5, 0xD2, 0xFF, 0xF8, 0xF1,
    0xF6, 0xE3, 0xE4, 0xED, 0xEA, 0xB7, 0xB0, 0xB9, 0xBE, 0xAB, 0xAC, 0xA5, 0xA2, 0x8F, 0x88,
    0x81, 0x86, 0x93, 0x94, 0x9D, 0x9A, 0x27, 0x20, 0x29, 0x2E, 0x3B, 0x3C, 0x35, 0x32, 0x1F,
    0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A, 0x57, 0x50, 0x59, 0x5E, 0x4B, 0x4C, 0x45, 0x42,
    0x6F, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7D, 0x7A, 0x89, 0x8E, 0x87, 0x80, 0x95, 0x92, 0x9B,
    0x9C, 0xB1, 0xB6, 0xBF, 0xB8, 0xAD, 0xAA, 0xA3, 0xA4, 0xF9, 0xFE, 0xF7, 0xF0, 0xE5, 0xE2,
    0xEB, 0xEC, 0xC1, 0xC6, 0xCF, 0xC8, 0xDD, 0xDA, 0xD3, 0xD4, 0x69, 0x6E, 0x67, 0x60, 0x75,
    0x72, 0x7B, 0x7C, 0x51, 0x56, 0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44, 0x19, 0x1E, 0x17, 0x10,
    0x05, 0x02, 0x0B, 0x0C, 0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34, 0x4E, 0x49, 0x40,
    0x47, 0x52, 0x55, 0x5C, 0x5B, 0x76, 0x71, 0x78, 0x7F, 0x6A, 0x6D, 0x64, 0x63, 0x3E, 0x39,
    0x30, 0x37, 0x22, 0x25, 0x2C, 0x2B, 0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13, 0xAE,
    0xA9, 0xA0, 0xA7, 0xB2, 0xB5, 0xBC, 0xBB, 0x96, 0x91, 0x98, 0x9F, 0x8A, 0x8D, 0x84, 0x83,
    0xDE, 0xD9, 0xD0, 0xD7, 0xC2, 0xC5, 0xCC, 0xCB, 0xE6, 0xE1, 0xE8, 0xEF, 0xFA, 0xFD, 0xF4,
    0xF3
  };
}
