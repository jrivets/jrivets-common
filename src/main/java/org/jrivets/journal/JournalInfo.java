package org.jrivets.journal;

import org.jrivets.util.Objects;
import org.jrivets.util.container.Pair;

final class JournalInfo {

    static final JournalInfo NULL_INFO = new JournalInfo(new Pair<Integer, Long>(0, 0L),
            new Pair<Integer, Long>(0, 0L), new Pair<Integer, Long>(0, 0L), 0);

    private final Pair<Integer, Long> marker;

    private final Pair<Integer, Long> reader;

    private final Pair<Integer, Long> writer;

    /**
     * The maximal distance that could be between mark and read position
     */
    private final int readLimit;

    JournalInfo(Pair<Integer, Long> marker, Pair<Integer, Long> reader, Pair<Integer, Long> writer, int readLimit) {
        this.marker = marker;
        this.reader = reader;
        this.writer = writer;
        this.readLimit = readLimit;
    }

    public Pair<Integer, Long> getMarker() {
        return marker;
    }

    public Pair<Integer, Long> getReader() {
        return reader;
    }

    public Pair<Integer, Long> getWriter() {
        return writer;
    }

    public int getReadLimit() {
        return readLimit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = readLimit;
        result = prime * result + ((marker == null) ? 0 : marker.hashCode());
        result = prime * result + ((reader == null) ? 0 : reader.hashCode());
        result = prime * result + ((writer == null) ? 0 : writer.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }
        JournalInfo oj = (JournalInfo) obj;
        return Objects.equal(marker, oj.marker) && Objects.equal(reader, oj.reader) && Objects.equal(writer, oj.writer)
                && readLimit == oj.readLimit;
    }

    @Override
    public String toString() {
        return "{marker=" + marker + ", reader=" + reader + ", writer=" + writer + ", readLimit=" + readLimit + "}";
    }
}
