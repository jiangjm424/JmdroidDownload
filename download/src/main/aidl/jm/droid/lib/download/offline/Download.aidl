// Download.aidl
//通过aidl传输自定义的数据结构时，除了创建的自定义结构实现android.os.Parcelable
//还需要建立一个同包名下的aidl文件，并声明该数据结构为 parcelable
package jm.droid.lib.download.offline;

import jm.droid.lib.download.offline.Download;

parcelable Download;
