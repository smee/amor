/*
 * Copyright 2008-2009 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.store.readonly.fetcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import voldemort.store.readonly.FileFetcher;
import voldemort.utils.EventThrottler;
import voldemort.utils.Props;
import voldemort.utils.Time;
import voldemort.utils.Utils;

/**
 * A fetcher that fetches the store files from HDFS
 * 
 * @author jay
 * 
 */
public class HdfsFetcher implements FileFetcher {

    private static final Logger logger = Logger.getLogger(HdfsFetcher.class);
    private static final String DEFAULT_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"),
                                                            "hdfs-fetcher").getAbsolutePath();
    private static final int REPORTING_INTERVAL_BYTES = 100 * 1024 * 1024;
    private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    private File tempDir;
    private final Long maxBytesPerSecond;
    private final int bufferSize;

    public HdfsFetcher(Props props) {
        this(props.containsKey("fetcher.max.bytes.per.sec") ? props.getBytes("fetcher.max.bytes.per.sec")
                                                           : null,
             new File(props.getString("hdfs.fetcher.tmp.dir", DEFAULT_TEMP_DIR)),
             (int) props.getBytes("hdfs.fetcher.buffer.size", DEFAULT_BUFFER_SIZE));
        logger.info("Created hdfs fetcher with temp dir = " + tempDir.getAbsolutePath()
                    + " and throttle rate " + maxBytesPerSecond + " and buffer size " + bufferSize);
    }

    public HdfsFetcher() {
        this((Long) null, null, DEFAULT_BUFFER_SIZE);
    }

    public HdfsFetcher(Long maxBytesPerSecond, File tempDir, int bufferSize) {
        if(tempDir == null)
            this.tempDir = new File(DEFAULT_TEMP_DIR);
        else
            this.tempDir = Utils.notNull(new File(tempDir, "hdfs-fetcher"));
        this.maxBytesPerSecond = maxBytesPerSecond;
        this.bufferSize = bufferSize;
        this.tempDir.mkdirs();
    }

    public File fetch(String fileUrl) throws IOException {
        Path path = new Path(fileUrl);
        Configuration config = new Configuration();
        config.setInt("io.file.buffer.size", bufferSize);
        FileSystem fs = path.getFileSystem(config);
        EventThrottler throttler = null;
        if(maxBytesPerSecond != null)
            throttler = new EventThrottler(maxBytesPerSecond);

        // copy file
        CopyStats stats = new CopyStats();
        File destination = new File(this.tempDir, path.getName());
        fetch(fs, path, destination, throttler, stats);
        return destination;
    }

    private void fetch(FileSystem fs, Path source, File dest, EventThrottler throttler, CopyStats stats)
            throws IOException {
        if(fs.isFile(source)) {
            copyFile(fs, source, dest, throttler, stats);
        } else {
            dest.mkdirs();
            FileStatus[] statuses = fs.listStatus(source);
            if(statuses != null) {
                // sort the files so that index files come last. Maybe
                // this will help keep them cached until the swap
                Arrays.sort(statuses, new IndexFileLastComparator());
                for(FileStatus status: statuses) {
                    if(!status.getPath().getName().startsWith(".")) {
                        fetch(fs,
                              status.getPath(),
                              new File(dest, status.getPath().getName()),
                              throttler,
                              stats);
                    }
                }
            }
        }
    }

    private void copyFile(FileSystem fs,
                          Path source,
                          File dest,
                          EventThrottler throttler,
                          CopyStats stats) throws IOException {
        logger.info("Starting copy of " + source + " to " + dest);
        FSDataInputStream input = null;
        OutputStream output = null;
        try {
            input = fs.open(source);
            output = new FileOutputStream(dest);
            byte[] buffer = new byte[bufferSize];
            while(true) {
                int read = input.read(buffer);
                if(read < 0)
                    break;
                output.write(buffer, 0, read);
                if(throttler != null)
                    throttler.maybeThrottle(read);
                stats.recordBytes(read);
                if(stats.getBytesSinceLastReport() > REPORTING_INTERVAL_BYTES) {
                    NumberFormat format = NumberFormat.getNumberInstance();
                    format.setMaximumFractionDigits(2);
                    logger.info(stats.getTotalBytesCopied() / (1024 * 1024) + " MB copied at "
                                + format.format(stats.getBytesPerSecond() / (1024 * 1024))
                                + " MB/sec");
                    stats.reset();
                }
            }
            logger.info("Completed copy of " + source + " to " + dest);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }

    private static class CopyStats {

        private long bytesSinceLastReport;
        private long totalBytesCopied;
        private long startNs;

        public CopyStats() {
            this.totalBytesCopied = 0L;
            this.bytesSinceLastReport = 0L;
            this.startNs = System.nanoTime();
        }

        public void recordBytes(long bytes) {
            this.totalBytesCopied += bytes;
            this.bytesSinceLastReport += bytes;
        }

        public void reset() {
            this.bytesSinceLastReport = 0;
            this.startNs = System.nanoTime();
        }

        public long getBytesSinceLastReport() {
            return bytesSinceLastReport;
        }

        public long getTotalBytesCopied() {
            return totalBytesCopied;
        }

        public double getBytesPerSecond() {
            double ellapsedSecs = System.nanoTime() - startNs / (double) Time.NS_PER_SECOND;
            return bytesSinceLastReport / ellapsedSecs;
        }
    }

    /**
     * A comparator that sorts index files last. This is a heuristic for
     * retaining the index file in page cache until the swap occurs
     * 
     */
    static class IndexFileLastComparator implements Comparator<FileStatus> {

        public int compare(FileStatus fs1, FileStatus fs2) {
            // directories before files
            if(fs1.isDir())
                return fs2.isDir() ? 0 : -1;
            // index files after all other files
            else if(fs1.getPath().getName().endsWith(".index"))
                return fs2.getPath().getName().endsWith(".index") ? 0 : 1;
            // everything else is equivalent
            else
                return 0;
        }

    }

    /*
     * Main method for testing fetching
     */
    public static void main(String[] args) throws Exception {
        if(args.length != 2)
            Utils.croak("USAGE: java " + HdfsFetcher.class.getName() + " url maxBytesPerSec");
        String url = args[0];
        long maxBytesPerSec = Long.parseLong(args[1]);
        Path p = new Path(url);
        FileStatus status = p.getFileSystem(new Configuration()).getFileStatus(p);
        long size = status.getLen();
        HdfsFetcher fetcher = new HdfsFetcher(maxBytesPerSec, null, DEFAULT_BUFFER_SIZE);
        long start = System.currentTimeMillis();
        File location = fetcher.fetch(url);
        double rate = size * Time.MS_PER_SECOND / (double) (System.currentTimeMillis() - start);
        System.out.println("Fetch to " + location + " completed: " + rate + " bytes/sec.");
    }
}
