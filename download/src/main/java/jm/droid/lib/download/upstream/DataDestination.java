package jm.droid.lib.download.upstream;

import java.io.IOException;

public interface DataDestination extends DataWriter {

    interface Factory {
        /**
         * Creates a {@link DataDestination} instance.
         */
        DataDestination createDataDestination();
    }

    /**
     * 打开一个dataSpec 也即是将源数据保存的地方
     * @param dataSpec 描述目的文件的地址
     * @return 返回C.LENGTH_UNSET 表示重新写入，大于0的表示前面的数据已经写了文件，从后面的数据开始写入
     */
    long open(DataSpec dataSpec) throws IOException;

    /**
     * 关闭流
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * 成功下载成功的回调
     */
    void done();
}
