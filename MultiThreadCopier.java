package ir.sharif.math.ap2023.hw5;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiThreadCopier {
    public static final long SAFE_MARGIN = 6;
    private final List<Segment> responsibilities;
    private final String dest;
    private final SourceProvider sourceProvider;
    private final long size;
    private final int workerCount;

    public MultiThreadCopier(SourceProvider sourceProvider, String dest, int workerCount) {
        this.workerCount = workerCount;

        this.dest = dest;

        this.sourceProvider = sourceProvider;
        this.size = sourceProvider.size();

        responsibilities = Collections.synchronizedList(new ArrayList<>());
    }

    private synchronized SourceReader createSourceReader(long offset) {
        return sourceProvider.connect(offset);
    }

    private void initializeResponsibilities(int workerCount) {
        for (int i = 0; i < workerCount; i++) {
            Segment segment = new Segment(i * (size / workerCount), (i + 1) * (size / workerCount));
            if (i == workerCount - 1) segment.setEnd(size);

            responsibilities.add(segment);
        }
    }

    public void allocateFile() {
        try {
            RandomAccessFile file = new RandomAccessFile(dest, "rw");
            file.setLength(size);
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        allocateFile();
        initializeResponsibilities(workerCount);

        MyWriter[] myWriters = new MyWriter[workerCount];
        for (int i = 0; i < workerCount; i++)
            myWriters[i] = new MyWriter(i);

        for (int i = 0; i < workerCount; i++)
            myWriters[i].start();
    }

    static class Segment {
        private long start, end;

        public Segment(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }
    }

    private class MyWriter extends Thread {
        private final int workerIndex;
        private final RandomAccessFile fileWriter;

        public MyWriter(int workerIndex) {
            this.workerIndex = workerIndex;
            try {
                fileWriter = new RandomAccessFile(dest, "rw");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean updateResponsibility() {
            synchronized (responsibilities) {
                long max = -1;
                int index = 0;

                for (int i = 0; i < responsibilities.size(); i++) {
                    Segment segment = responsibilities.get(i);
                    if (segment.getEnd() - segment.getStart() > max) {
                        max = segment.getEnd() - segment.getStart();
                        index = i;
                    }
                }

                if (max >= SAFE_MARGIN) {
                    Segment segment = responsibilities.get(index);
                    long mid = (segment.getEnd() + segment.getStart()) / 2;
                    responsibilities.set(workerIndex, new Segment(mid, segment.getEnd()));
                    segment.setEnd(mid);
                    return true;
                }
                return false;
            }
        }


        @Override
        public void run() {
            super.run();

            do {
                SourceReader sourceReader = createSourceReader(responsibilities.get(workerIndex).getStart());
                try {
                    fileWriter.seek(responsibilities.get(workerIndex).getStart());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                while (responsibilities.get(workerIndex).getStart() < responsibilities.get(workerIndex).getEnd()) {
                    try {
                        synchronized (responsibilities) {
                            responsibilities.get(workerIndex).setStart(responsibilities.get(workerIndex).getStart() + 1);
                        }
                        fileWriter.write(sourceReader.read());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            } while (updateResponsibility());

            try {
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}