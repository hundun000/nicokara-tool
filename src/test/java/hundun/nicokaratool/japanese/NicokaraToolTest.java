package hundun.nicokaratool.japanese;

import static org.junit.Assert.*;

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
        result = JapaneseLine.toLyric(JapaneseService.toMyTokenList(text, "USELESS"));
        assertEquals("すれ違(ちが)って\n", result);
        
        
    }

    @Test
    public void test2() {
        String text;
        String result;
        
        text = "抱きしめ";
        result = JapaneseLine.toLyric(JapaneseService.toMyTokenList(text, "USELESS"));
        assertEquals("抱(だ)きしめ\n", result);

        
        
    }
    
    @Test
    public void test3() {
        String text;
        String result;
        
        text = "向こう";
        result = JapaneseLine.toLyric(JapaneseService.toMyTokenList(text, "USELESS"));
        assertEquals("向(む)こう\n", result);
        //assertEquals("向(む)こー\n", result);
        
        
    }
    
    @Test
    public void test4() {
        String text;
        String result;
        
        text = "登った";
        result = JapaneseLine.toLyric(JapaneseService.toMyTokenList(text, "USELESS"));
        assertEquals("登(のぼ)った\n", result);

        
        
    }
    
    @Test
    public void test5() {
        String text;
        String result;
        
        text = "言い";
        result = JapaneseLine.toLyric(JapaneseService.toMyTokenList(text, "USELESS"));
        assertEquals("言(い)い\n", result);
    }
}
