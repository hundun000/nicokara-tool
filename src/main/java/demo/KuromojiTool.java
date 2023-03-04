package demo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

/**
 * @author hundun
 * Created on 2020/09/07
 */
public class KuromojiTool {
    
    public static void allFutures(String text, String splict) {
        StringBuilder result = new StringBuilder();
        Tokenizer tokenizer = new Tokenizer() ;
        String[] lines = text.split(splict);
        for (String line : lines) {
            List<Token> tokens = tokenizer.tokenize(line);
            for (Token token : tokens) {
                result.append(token.getSurface());
                if (!(isAllKana(token.getSurface()) || text.equals("*"))) {
                    result.append("(").append(token.getAllFeatures()).append(")");
                }
            }
            result.append("\n");
        }
        System.out.println(result.toString());
    }
    
    public static void addPronunciation(String text, String splict) {
        StringBuilder result = new StringBuilder();
        Tokenizer tokenizer = new Tokenizer() ;
        String[] lines = text.split(splict);
        for (String line : lines) {
            List<Token> tokens = tokenizer.tokenize(line);
            for (Token token : tokens) {
                result.append(token.getSurface());
                if (!isAllKana(token.getSurface()) && !token.getReading().equals("*") && !token.getReading().equals(token.getSurface())) {
                    result.append("(").append(
                            token.getPronunciation().chars()
                                    .mapToObj(c -> (char) c)
                                    .map(it -> String.valueOf(it))  
                                    .collect(Collectors.joining())
                            ).append(")");
                }
            }
            result.append("\n");
        }
        System.out.println(result.toString());
    }
    
    /**
     * 平假名（3040 - 309f）
     * 片假名（30a0 - 30ff）
     */
    //static Pattern kanaPattern = Pattern.compile("^[\u3040-\u309f\u30a0-\u30ff]+&");
    
    static Pattern kanaPattern = Pattern.compile("^["
            + "ぁあぃいぅうぇえぉおかがきぎくぐけげこごさざしじすずせぜそぞただちぢっつづてでとどなにぬねのはばぱひびぴふぶぷへべぺほぼぽまみむめもゃやゅゆょよらりるれろゎわゐゑをんゔゕゖ゙゚゛゜ゝゞゟ"
            + "゠ァアィイゥウェエォオカガキギクグケゲコゴサザシジスズセゼソゾタダチヂッツヅテデトドナニヌネノハバパヒビピフブプヘベペホボポマミムメモャヤュユョヨラリルレロヮワヰヱヲンヴヵヶヷヸヹヺ・ーヽヾヿ"
            + "]+$");
    
    
    public static boolean isAllKana(String text) {
        Matcher matcher = kanaPattern.matcher(text);
        return matcher.matches();
    }

}
