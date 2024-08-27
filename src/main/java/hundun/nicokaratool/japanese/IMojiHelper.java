package hundun.nicokaratool.japanese;

import java.util.stream.Collectors;

import com.moji4j.MojiConverter;
import com.moji4j.MojiDetector;

/**
 * @author hundun
 * Created on 2023/03/09
 */
public interface IMojiHelper {

    String katakanaToHiragana(String katakana);

    boolean hasKanji(String text);

    public static class Moji4jHelper implements IMojiHelper {

        static MojiConverter converter = new MojiConverter();
        static MojiDetector detector = new MojiDetector();
        
        
        @Override
        public String katakanaToHiragana(String katakana) {
            /*
            * kuromoji划分出token("登っ")，而Moji4j中需要的是"登った"来辅助决定"っ"的发音，此时会无法正常工作。
            */
            return converter.convertRomajiToHiragana(converter.convertKanaToRomaji(katakana));
        }

        @Override
        public boolean hasKanji(String text) {
            return detector.hasKanji(text);
        }
        
    }
    
    public static class SimpleMojiHelper implements IMojiHelper {

        @Override
        public String katakanaToHiragana(String katakana) {
            return katakana.chars()
                    .mapToObj(it -> (char)it)
                    .map(it -> String.valueOf(JapaneseCharacterTool.toHiragana(it)))
                    .collect(Collectors.joining())
                    ;
        }

        @Override
        public boolean hasKanji(String text) {
            for (int i = 0; i < text.length(); i++) {
                if (JapaneseCharacterTool.isKanji(text.charAt(i))) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
}
