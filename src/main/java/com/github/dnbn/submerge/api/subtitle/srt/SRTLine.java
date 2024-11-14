package com.github.dnbn.submerge.api.subtitle.srt;

import com.github.dnbn.submerge.api.subtitle.common.SubtitleLine;

import java.util.List;

/**
 * Class <SRTLine> represents an abstract line of SRT, meaning text, timecodes and index
 */
public class SRTLine extends SubtitleLine<SRTTime> {

    private static final long serialVersionUID = -1220593401999895814L;

    private static final String NEW_LINE = "\n";

    private int id;

    public SRTLine(int id, SRTTime time, List<String> textLines) {

        this.id = id;
        this.time = time;
        this.textLines = textLines;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(this.id).append(NEW_LINE);
        sb.append(this.time).append(NEW_LINE);
        for (String textLine : textLines) {
            sb.append(textLine).append(NEW_LINE);
        }
        return sb.append(NEW_LINE).toString();
    }

    // ===================== getter and setter start =====================

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
