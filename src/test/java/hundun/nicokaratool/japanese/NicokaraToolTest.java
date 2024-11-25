package hundun.nicokaratool.japanese;

import static org.junit.Assert.*;

import hundun.nicokaratool.layout.SimpleLyricsRender;
import org.junit.Test;

/**
 * @author hundun
 * Created on 2023/03/09
 */
public class NicokaraToolTest {

    JapaneseService japaneseService = new JapaneseService();
    
    @Test
    public void test() {
        String text;
        String result;
        
        text = "すれ違って";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(japaneseService.toParsedNoTagLine(text));
        assertEquals("すれ違(ちが)って", result);
        
        
    }

    @Test
    public void test2() {
        String text;
        String result;
        
        text = "抱きしめ";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(japaneseService.toParsedNoTagLine(text));
        assertEquals("抱(だ)きしめ", result);

        
        
    }
    
    @Test
    public void test3() {
        String text;
        String result;
        
        text = "向こう";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(japaneseService.toParsedNoTagLine(text));
        assertEquals("向(む)こう", result);
        //assertEquals("向(む)こー", result);
        
        
    }
    
    @Test
    public void test4() {
        String text;
        String result;
        
        text = "登った";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(japaneseService.toParsedNoTagLine(text));
        assertEquals("登(のぼ)った", result);

        
        
    }
    
    @Test
    public void test5() {
        String text;
        String result;
        
        text = "言い";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(japaneseService.toParsedNoTagLine(text));
        assertEquals("言(い)い", result);
    }

    @Test
    public void testTagTokenizer() {
        String text;

        text = "[00:22:90]大切な[00:24:03]思い[00:24:94]出を[00:25:51]";
        TagTokenizer tagTokenizer = new TagTokenizer();
        var result = tagTokenizer.parse(text);
        System.out.println(result);
    }
}
