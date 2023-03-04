package demo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;


/**
 * @author hundun
 * Created on 2020/08/10
 */
public class KuromojiToolTest {
    public static String splict = "\\r\\n";
    public static String text = "登った 向こう";
    @Test
    public void testFunAllFutures() {
        KuromojiTool.allFutures(text, splict);
    }
    
    @Test
    public void testFun() {
        KuromojiTool.addPronunciation(text, splict);
    }

    
    @Test
    public void testIsPlain() {
        String text = "を";
        assertEquals(true, KuromojiTool.isAllKana(text));
        
        text = "ハァーヨイショ";
        assertEquals(true, KuromojiTool.isAllKana(text));
        
        text = "分ける";
        assertEquals(false, KuromojiTool.isAllKana(text));
    }

}
