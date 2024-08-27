package hundun.nicokaratool.japanese;

import static org.junit.Assert.*;

import hundun.nicokaratool.japanese.JapaneseService.SimpleLyricsRender;
import org.junit.Test;

import hundun.nicokaratool.japanese.JapaneseService.JapaneseLine;

/**
 * @author hundun
 * Created on 2023/03/09
 */
public class NicokaraToolTest {

    @Test
    public void test() {
        String text;
        String result;
        
        text = "すれ違って";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(JapaneseService.toParsedLinesCore(text));
        assertEquals("すれ違(ちが)って", result);
        
        
    }

    @Test
    public void test2() {
        String text;
        String result;
        
        text = "抱きしめ";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(JapaneseService.toParsedLinesCore(text));
        assertEquals("抱(だ)きしめ", result);

        
        
    }
    
    @Test
    public void test3() {
        String text;
        String result;
        
        text = "向こう";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(JapaneseService.toParsedLinesCore(text));
        assertEquals("向(む)こう", result);
        //assertEquals("向(む)こー", result);
        
        
    }
    
    @Test
    public void test4() {
        String text;
        String result;
        
        text = "登った";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(JapaneseService.toParsedLinesCore(text));
        assertEquals("登(のぼ)った", result);

        
        
    }
    
    @Test
    public void test5() {
        String text;
        String result;
        
        text = "言い";
        result = SimpleLyricsRender.INSTANCE.toLyricsLine(JapaneseService.toParsedLinesCore(text));
        assertEquals("言(い)い", result);
    }
}
