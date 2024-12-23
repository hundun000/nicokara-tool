package com.github.dnbn.submerge.api.subtitle.common;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public abstract class SubtitleLine<T extends TimedObject, T_TEXT> implements TimedLine {

    /**
     * Serial Id
     */
    private static final long serialVersionUID = 288560648398584309L;

    /**
     * Subtitle Text. This is the actual text which will be displayed as a subtitle
     * onscreen.
     */
    protected List<T_TEXT> textRawLines = new ArrayList<>();

    /**
     * Timecodes
     */
    protected T time;

    /**
     * Comparator that only compare timings
     *
     * @return the comparator
     */
    public static Comparator<TimedLine> timeComparator = new Comparator<TimedLine>() {

        @Override
        public int compare(TimedLine o1, TimedLine o2) {
            return o1.getTime().compareTo(o2.getTime());
        }
    };

    /**
     * Constructor
     */
    public SubtitleLine() {
        super();
    }

    /**
     * Constructor
     */
    public SubtitleLine(T time) {

        super();
        this.time = time;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TimedLine other = (TimedLine) obj;
        return compareTo(other) == 0;
    }

    @Override
    public int compare(TimedLine o1, TimedLine o2) {

        return o1.compareTo(o2);
    }

    @Override
    public int compareTo(TimedLine o) {

        int compare = this.time.compareTo(o.getTime());
        if (compare == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (T_TEXT textLine : textRawLines) {
                stringBuilder.append(textLine)
                        .append(",");
            }

            StringBuilder stringBuilderO = new StringBuilder();
            List<String> textLines = o.getTextLines();
            for (String textLine : textLines) {
                stringBuilderO.append(textLine)
                        .append(",");
            }
            String thisText = stringBuilder.toString();
            String otherText = stringBuilderO.toString();
            compare = thisText.compareTo(otherText);
        }

        return compare;
    }

}
