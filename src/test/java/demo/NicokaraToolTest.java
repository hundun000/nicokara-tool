package demo;

import static org.junit.Assert.*;

import demo.japanese.NicokaraRunner;
import org.junit.Test;

import demo.japanese.NicokaraRunner.JapaneseToken;

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
        result = JapaneseToken.toLyric(NicokaraRunner.toMyTokenList(text, "USELESS"));
        assertEquals("すれ違(ちが)って\n", result);
        
        
    }

    @Test
    public void test2() {
        String text;
        String result;
        
        text = "抱きしめ";
        result = JapaneseToken.toLyric(NicokaraRunner.toMyTokenList(text, "USELESS"));
        assertEquals("抱(だ)きしめ\n", result);

        
        
    }
    
    @Test
    public void test3() {
        String text;
        String result;
        
        text = "向こう";
        result = JapaneseToken.toLyric(NicokaraRunner.toMyTokenList(text, "USELESS"));
        assertEquals("向(む)こう\n", result);
        //assertEquals("向(む)こー\n", result);
        
        
    }
    
    @Test
    public void test4() {
        String text;
        String result;
        
        text = "登った";
        result = JapaneseToken.toLyric(NicokaraRunner.toMyTokenList(text, "USELESS"));
        assertEquals("登(のぼ)った\n", result);

        
        
    }
    
    @Test
    public void test5() {
        String text;
        String result;
        
        text = "言い";
        result = JapaneseToken.toLyric(NicokaraRunner.toMyTokenList(text, "USELESS"));
        assertEquals("言(い)い\n", result);
    }
}
