/*
 * Copyright (C) 2017 The Android Open Source Project
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
package jm.droid.lib.download.offline;

import static jm.droid.lib.download.util.Util.castNonNull;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import jm.droid.lib.download.util.Assertions;
import jm.droid.lib.download.util.DigestUtils;
import jm.droid.lib.download.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Defines content to be downloaded. */
public final class DownloadRequest implements Parcelable {

  /** Thrown when the encoded request data belongs to an unsupported request type. */
  public static class UnsupportedRequestException extends IOException {}

  /** A builder for download requests. */
  public static class Builder {
    private final String id;
    private final Uri uri;
    @Nullable private String displayName;
    @Nullable private List<StreamKey> streamKeys;
    @Nullable private String path;
    @Nullable private byte[] data;

    /** Creates a new instance with the specified id and uri. */
    /* package */ Builder(String id, Uri uri) {
      this.id = id;
      this.uri = uri;
    }
    /** Creates a new instance with the specified uri. */
    public Builder(Uri uri) {
        this.id = DigestUtils.md5Hex(uri.toString());
        this.uri = uri;
    }
    /** Sets the {@link DownloadRequest#displayName}. */
    public Builder setDisplayName(@Nullable String displayName) {
      this.displayName = displayName;
      return this;
    }

    /** Sets the {@link DownloadRequest#streamKeys}. */
    public Builder setStreamKeys(@Nullable List<StreamKey> streamKeys) {
      this.streamKeys = streamKeys;
      return this;
    }

    /** Sets the {@link DownloadRequest#path}. */
    public Builder setPath(@Nullable String path) {
      this.path = path;
      return this;
    }

    /** Sets the {@link DownloadRequest#data}. */
    public Builder setData(@Nullable byte[] data) {
      this.data = data;
      return this;
    }

    public DownloadRequest build() {
      return new DownloadRequest(
          id,
          uri,
          displayName,
          streamKeys != null ? streamKeys : new ArrayList<>(),
          path,
          data);
    }
  }

  /** The unique content id. */
  public final @NotNull String id;
  /** The uri being downloaded. */
  public final Uri uri;
  /**
   * The MIME type of this content. Used as a hint to infer the content's type (DASH, HLS,
   * SmoothStreaming). If null, a {@code DownloadService} will infer the content type from the
   * {@link #uri}.
   */
  @Nullable public final String displayName;
  /** Stream keys to be downloaded. If empty, all streams will be downloaded. */
  public final List<StreamKey> streamKeys;
  /**
   * Custom key for cache indexing, or null. Must be null for DASH, HLS and SmoothStreaming
   * downloads.
   */
  @Nullable public final String path;
  /** Application defined data associated with the download. May be empty. */
  public final byte[] data;

  /**
   * @param id See {@link #id}.
   * @param uri See {@link #uri}.
   * @param displayName See {@link #displayName}
   * @param streamKeys See {@link #streamKeys}.
   * @param path See {@link #path}.
   * @param data See {@link #data}.
   */
  private DownloadRequest(
      @NotNull String id,
      Uri uri,
      @Nullable String displayName,
      List<StreamKey> streamKeys,
      @Nullable String path,
      @Nullable byte[] data) {
    this.id = id;
    this.uri = uri;
    this.displayName = displayName;
    ArrayList<StreamKey> mutableKeys = new ArrayList<>(streamKeys);
    Collections.sort(mutableKeys);
    this.streamKeys = Collections.unmodifiableList(mutableKeys);
    this.path = path;
    this.data = data != null ? Arrays.copyOf(data, data.length) : Util.EMPTY_BYTE_ARRAY;
  }

  /* package */ DownloadRequest(Parcel in) {
    id = castNonNull(in.readString());
    uri = Uri.parse(castNonNull(in.readString()));
    displayName = in.readString();
    int streamKeyCount = in.readInt();
    ArrayList<StreamKey> mutableStreamKeys = new ArrayList<>(streamKeyCount);
    for (int i = 0; i < streamKeyCount; i++) {
      mutableStreamKeys.add(in.readParcelable(StreamKey.class.getClassLoader()));
    }
    streamKeys = Collections.unmodifiableList(mutableStreamKeys);
    path = in.readString();
    data = castNonNull(in.createByteArray());
  }

  public Builder buildUpon() {
    return new Builder(id, uri).setData(data).setStreamKeys(streamKeys).setPath(path).setDisplayName(displayName);
  }
  /**
   * Returns a copy with the specified ID.
   *
   * @param id The ID of the copy.
   * @return The copy with the specified ID.
   */
  public DownloadRequest copyWithId(String id) {
    return new DownloadRequest(id, uri, displayName, streamKeys, path, data);
  }

  /**
   * Returns the result of merging {@code newRequest} into this request. The requests must have the
   * same {@link #id}.
   *
   * <p>The resulting request contains the stream keys from both requests. For all other member
   * variables, those in {@code newRequest} are preferred.
   *
   * @param newRequest The request being merged.
   * @return The merged result.
   * @throws IllegalArgumentException If the requests do not have the same {@link #id}.
   */
  public DownloadRequest copyWithMergedRequest(DownloadRequest newRequest) {
    Assertions.checkArgument(id.equals(newRequest.id));
    List<StreamKey> mergedKeys;
    if (streamKeys.isEmpty() || newRequest.streamKeys.isEmpty()) {
      // If either streamKeys is empty then all streams should be downloaded.
      mergedKeys = Collections.emptyList();
    } else {
      mergedKeys = new ArrayList<>(streamKeys);
      for (int i = 0; i < newRequest.streamKeys.size(); i++) {
        StreamKey newKey = newRequest.streamKeys.get(i);
        if (!mergedKeys.contains(newKey)) {
          mergedKeys.add(newKey);
        }
      }
    }
    return new DownloadRequest(
        id,
        uri,
        displayName,
        mergedKeys,
        path,
        newRequest.data);
  }


  @Override
  public String toString() {
    return displayName + ":" + id;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof DownloadRequest)) {
      return false;
    }
    DownloadRequest that = (DownloadRequest) o;
    return id.equals(that.id)
        && uri.equals(that.uri)
        && Util.areEqual(displayName, that.displayName)
        && streamKeys.equals(that.streamKeys)
        && Util.areEqual(path, that.path)
        && Arrays.equals(data, that.data);
  }

  @Override
  public final int hashCode() {
    int result = 31 * id.hashCode();
    result = 31 * result + uri.hashCode();
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + streamKeys.hashCode();
    result = 31 * result + (path != null ? path.hashCode() : 0);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }

  // Parcelable implementation.

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(uri.toString());
    dest.writeString(displayName);
    dest.writeInt(streamKeys.size());
    for (int i = 0; i < streamKeys.size(); i++) {
      dest.writeParcelable(streamKeys.get(i), /* parcelableFlags= */ 0);
    }
    dest.writeString(path);
    dest.writeByteArray(data);
  }

  public static final Creator<DownloadRequest> CREATOR =
      new Creator<DownloadRequest>() {

        @Override
        public DownloadRequest createFromParcel(Parcel in) {
          return new DownloadRequest(in);
        }

        @Override
        public DownloadRequest[] newArray(int size) {
          return new DownloadRequest[size];
        }
      };
}
