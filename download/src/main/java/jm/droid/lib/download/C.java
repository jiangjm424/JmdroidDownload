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
package jm.droid.lib.download;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.net.Uri;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import jm.droid.lib.download.util.Util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

/** Defines constants used by the library. */
@SuppressWarnings("InlinedApi")
public final class C {

  private C() {}

  /**
   * Special constant representing a time corresponding to the end of a source. Suitable for use in
   * any time base.
   */
  public static final long TIME_END_OF_SOURCE = Long.MIN_VALUE;

  /**
   * Special constant representing an unset or unknown time or duration. Suitable for use in any
   * time base.
   */
  public static final long TIME_UNSET = Long.MIN_VALUE + 1;

  /** Represents an unset or unknown index. */
  public static final int INDEX_UNSET = -1;

  /** Represents an unset or unknown position. */
  public static final int POSITION_UNSET = -1;

  /** Represents an unset or unknown rate. */
  public static final float RATE_UNSET = -Float.MAX_VALUE;

  /** Represents an unset or unknown integer rate. */
  public static final int RATE_UNSET_INT = Integer.MIN_VALUE + 1;

  /** Represents an unset or unknown length. */
  public static final int LENGTH_UNSET = -1;

  /** Represents an unset or unknown percentage. */
  public static final int PERCENTAGE_UNSET = -1;

  /** The number of milliseconds in one second. */
  public static final long MILLIS_PER_SECOND = 1000L;

  /** The number of microseconds in one second. */
  public static final long MICROS_PER_SECOND = 1000000L;

  /** The number of nanoseconds in one second. */
  public static final long NANOS_PER_SECOND = 1000000000L;

  /** The number of bits per byte. */
  public static final int BITS_PER_BYTE = 8;

  /** The number of bytes per float. */
  public static final int BYTES_PER_FLOAT = 4;

  /** A return value for methods where the end of an input was encountered. */
  public static final int RESULT_END_OF_INPUT = -1;
  /**
   * A return value for methods where the length of parsed data exceeds the maximum length allowed.
   */
  public static final int RESULT_MAX_LENGTH_EXCEEDED = -2;
  /** A return value for methods where nothing was read. */
  public static final int RESULT_NOTHING_READ = -3;
  /** A return value for methods where a buffer was read. */
  public static final int RESULT_BUFFER_READ = -4;
  /** A return value for methods where a format was read. */
  public static final int RESULT_FORMAT_READ = -5;

  /**
   * Network connection type. One of {@link #NETWORK_TYPE_UNKNOWN}, {@link #NETWORK_TYPE_OFFLINE},
   * {@link #NETWORK_TYPE_WIFI}, {@link #NETWORK_TYPE_2G}, {@link #NETWORK_TYPE_3G}, {@link
   * #NETWORK_TYPE_4G}, {@link #NETWORK_TYPE_5G_SA}, {@link #NETWORK_TYPE_5G_NSA}, {@link
   * #NETWORK_TYPE_CELLULAR_UNKNOWN}, {@link #NETWORK_TYPE_ETHERNET} or {@link #NETWORK_TYPE_OTHER}.
   */
  // @Target list includes both 'default' targets and TYPE_USE, to ensure backwards compatibility
  // with Kotlin usages from before TYPE_USE was added.
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    NETWORK_TYPE_UNKNOWN,
    NETWORK_TYPE_OFFLINE,
    NETWORK_TYPE_WIFI,
    NETWORK_TYPE_2G,
    NETWORK_TYPE_3G,
    NETWORK_TYPE_4G,
    NETWORK_TYPE_5G_SA,
    NETWORK_TYPE_5G_NSA,
    NETWORK_TYPE_CELLULAR_UNKNOWN,
    NETWORK_TYPE_ETHERNET,
    NETWORK_TYPE_OTHER
  })
  public @interface NetworkType {}
  /** Unknown network type. */
  public static final int NETWORK_TYPE_UNKNOWN = 0;
  /** No network connection. */
  public static final int NETWORK_TYPE_OFFLINE = 1;
  /** Network type for a Wifi connection. */
  public static final int NETWORK_TYPE_WIFI = 2;
  /** Network type for a 2G cellular connection. */
  public static final int NETWORK_TYPE_2G = 3;
  /** Network type for a 3G cellular connection. */
  public static final int NETWORK_TYPE_3G = 4;
  /** Network type for a 4G cellular connection. */
  public static final int NETWORK_TYPE_4G = 5;
  /** Network type for a 5G stand-alone (SA) cellular connection. */
  public static final int NETWORK_TYPE_5G_SA = 9;
  /** Network type for a 5G non-stand-alone (NSA) cellular connection. */
  public static final int NETWORK_TYPE_5G_NSA = 10;
  /**
   * Network type for cellular connections which cannot be mapped to one of {@link
   * #NETWORK_TYPE_2G}, {@link #NETWORK_TYPE_3G}, or {@link #NETWORK_TYPE_4G}.
   */
  public static final int NETWORK_TYPE_CELLULAR_UNKNOWN = 6;
  /** Network type for an Ethernet connection. */
  public static final int NETWORK_TYPE_ETHERNET = 7;
  /** Network type for other connections which are not Wifi or cellular (e.g. VPN, Bluetooth). */
  public static final int NETWORK_TYPE_OTHER = 8;

}
