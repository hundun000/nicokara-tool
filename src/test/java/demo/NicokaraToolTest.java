package demo;

import static org.junit.Assert.*;

import org.junit.Test;

import demo.NicokaraTool.MyToken;

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
        result = MyToken.toLyric(NicokaraTool.toMyTokenList(text, "USELESS"));
        assertEquals("すれ違(ちが)って\n", result);
        
        
    }

    @Test
    public void test2() {
        String text;
        String result;
        
        text = "抱きしめ";
        result = MyToken.toLyric(NicokaraTool.toMyTokenList(text, "USELESS"));
        assertEquals("抱(だ)きしめ\n", result);

        
        
    }
    
    @Test
    public void test3() {
        String text;
        String result;
        
        text = "向こう";
        result = MyToken.toLyric(NicokaraTool.toMyTokenList(text, "USELESS"));
        assertEquals("向(む)こう\n", result);
        //assertEquals("向(む)こー\n", result);
        
        
    }
    
    @Test
    public void test4() {
        String text;
        String result;
        
        text = "登った";
        result = MyToken.toLyric(NicokaraTool.toMyTokenList(text, "USELESS"));
        assertEquals("登(のぼ)った\n", result);

        
        
    }
}
