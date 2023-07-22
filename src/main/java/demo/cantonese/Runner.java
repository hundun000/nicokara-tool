package demo.cantonese;

import demo.base.LyricLine;
import demo.base.LyricLine.LyricLineNode;
import demo.cantonese.PycantoneseFeignClient.YaleRequest;
import demo.cantonese.PycantoneseFeignClient.YaleResponse;
import demo.util.Utils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class Runner {

    public static void main(String[] args) {
        PycantoneseFeignClient client = PycantoneseFeignClient.instance();
        YaleRequest request = YaleRequest.builder()
                .text("你好世界")
                .build();
        YaleResponse response = client.jyutping_to_yale(request);
        System.out.println(response);


        String name = "富士山下";
        File kanjiHintsFile = new File("data/" + name + ".kanjiHints.json");
        List<String> list = Utils.readAllLines("data/" + name + ".txt");
        var myTokenList = toMyTokenList(list);
        var rubyCollector = new CantoneseRubyCollector();
        String ruby = rubyCollector.collectRuby(myTokenList, kanjiHintsFile);
        System.out.println("Ruby: ");
        System.out.println(ruby);
    }

    private static List<LyricLineNode> toMyTokenList(List<String> list) {
        return list.stream()
                .map(it -> LyricLine.parseOneNodeLine(it))
                .map(it -> it.getNodes())
                .flatMap(it -> it.stream())
                .collect(Collectors.toList());
    }


}
