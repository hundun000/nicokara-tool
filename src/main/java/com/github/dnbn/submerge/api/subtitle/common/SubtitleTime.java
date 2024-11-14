package com.github.dnbn.submerge.api.subtitle.common;

//import java.time.LocalTime;

public class SubtitleTime implements TimedObject {

    private static final long serialVersionUID = -2283115927128309201L;

    /**
     * Start Time of the Event, in 0:00:00:00 format ie. Hrs:Mins:Secs:hundredths. This is
     * the time elapsed during script playback at which the text will appear onscreen.
     */
    protected long start;

    /**
     * End Time of the Event, in 0:00:00:00 format ie. Hrs:Mins:Secs:hundredths. This is
     * the time elapsed during script playback at which the text will disappear offscreen.
     */
    protected long end;

    public SubtitleTime() {
    }

    public SubtitleTime(long start, long end) {

        super();
        this.start = start;
        this.end = end;
    }

    @Override
    public int compare(TimedObject o1, TimedObject o2) {

        return o1.compareTo(o2);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TimedObject other = (TimedObject) obj;
        return compareTo(other) == 0;
    }

    @Override
    public int compareTo(TimedObject other) {
        long start = this.start - other.getStart();
        if (start == 0)
            start = this.end - other.getEnd();
        if (start > 0) {
            return 1;
        } else if (start < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    // ===================== getter and setter start =====================

    @Override
    public long getStart() {
        return this.start;
    }

    @Override
    public void setStart(long start) {
        this.start = start;
    }

    @Override
    public long getEnd() {
        return this.end;
    }

    @Override
    public void setEnd(long end) {
        this.end = end;
    }
}
