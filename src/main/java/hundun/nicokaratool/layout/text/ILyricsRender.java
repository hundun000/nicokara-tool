package hundun.nicokaratool.layout.text;

public interface ILyricsRender<T_PARSED_LINE> {
    String toLyricsLine(T_PARSED_LINE line);
}