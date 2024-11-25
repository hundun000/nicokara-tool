package hundun.nicokaratool.japanese;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


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

    /**
     * 送假名（日语：送り仮名／おくりがな okurigana），是指一个日语辞汇之中，汉字后面跟随（即所谓“送”）的假名，用来指示前面汉字的词性或读音。
     * <p>
     * 另外有些词汇省略或者不省略送假名的情况都存在，但意义不同，而常常是从动词派生出来的词汇。例如：
     * <p>
     * 話す：はなす，hanasu，谈话，动词。由此产生出了：
     * 話し：はなし，hanashi，谈话，其连用形
     * 話：はなし，hanashi，谈话，名词
     */
    @Test
    public void test6() {
        String text;
        String result;
        text = "話す";
        KuromojiTool.allFutures(text, splict);
        text = "話し";
        KuromojiTool.allFutures(text, splict);
        text = "話";
        KuromojiTool.allFutures(text, splict);
    }

}
