package hundun.nicokaratool.db.ai;

import hundun.nicokaratool.db.DbService.AiStep1ResultNode;
import hundun.nicokaratool.db.dto.LyricLineDTO;
import hundun.nicokaratool.util.JsonUtils;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ManualDeepseekService extends AiService {



    public ManualDeepseekService() {
        this.step1TaskSplitMinSize = 20;
        this.step1TaskSplitMaxSize = 25;
        this.step2TaskSplitMinSize = 10;
        this.step2TaskSplitMaxSize = 15;
    }

    @Override
    public @Nullable List<LyricLineDTO> aiStep2Group(List<String> askLines, String step2AskTemplate) {
        try {
            String hint = askLines.stream().collect(Collectors.joining("\n"));
            String content = readBigStringIn(hint);
            content = formatAiResult(content);
            List<LyricLineDTO> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, LyricLineDTO.class));
            List<String> resultLines = nodes.stream().map(node -> node.getLyric()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                logNotEquals(askLines, resultLines);
                throw new Exception("resultLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable List<AiStep1ResultNode> aiStep1Group(List<String> askLines, String step1AskTemplate) {
        try {
            String hint = askLines.stream().collect(Collectors.joining("\n"));
            String content = readBigStringIn(hint);
            content = formatAiResult(content);
            List<AiStep1ResultNode> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, AiStep1ResultNode.class));
            List<String> resultLines = nodes.stream().flatMap(node -> node.getLyrics().stream()).collect(Collectors.toList());
            if (!askLines.equals(resultLines)) {
                logNotEquals(askLines, resultLines);
                throw new Exception("resultLines not equals.");
            }
            return nodes;
        } catch (Exception e) {
            return null;
        }
    }

    public static String readBigStringIn(String askLines) throws IOException {
        // 创建多行文本区域
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setLineWrap(true); // 自动换行
        textArea.setWrapStyleWord(true); // 以单词为单位换行

        // 将文本区域放入滚动面板中，以便内容超出时显示滚动条
        JScrollPane scrollPane = new JScrollPane(textArea);

        // 创建一个面板，将提示标签和文本区域添加到面板中
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,  BoxLayout.Y_AXIS));
        panel.add(new JLabel("askLines: "));
        panel.add(new JTextArea(askLines));
        panel.add(new JLabel("请粘贴ai回复json: "));
        panel.add(scrollPane);

        // 显示输入对话框
        int result = JOptionPane.showConfirmDialog(null, panel, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // 处理用户的输入
        if (result == JOptionPane.OK_OPTION) {
            String inputText = textArea.getText();
            return inputText;
        } else {
            return "";
        }
    }

}
