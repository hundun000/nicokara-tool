package hundun.nicokaratool.db.ai;

import javax.swing.*;
import java.awt.*;

public class ManualDeepseekTempPanel extends JPanel {
    final boolean lastFail;
    final JTextArea textArea;
    public ManualDeepseekTempPanel(boolean lastFail, String... hints) {
        this.lastFail = lastFail;
        this.setLayout(new BoxLayout(this,  BoxLayout.Y_AXIS));
        this.add(new JLabel("askLines: "));
        for (String hint : hints) {
            JTextArea hintArea = new JTextArea(hint);
            hintArea.setLineWrap(true);
            hintArea.setWrapStyleWord(true);
            hintArea.setEditable(false);

            JScrollPane hintPane = new JScrollPane(hintArea);
            hintPane.setPreferredSize(new Dimension(400, 100)); // 设置最大可见高度
            hintPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            hintPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            this.add(hintPane);
        }
        this.textArea = new JTextArea(10, 30);
        textArea.setLineWrap(true); // 自动换行
        textArea.setWrapStyleWord(true); // 以单词为单位换行
        // 将文本区域放入滚动面板中，以便内容超出时显示滚动条
        JScrollPane scrollPane = new JScrollPane(textArea);

        this.add(new JLabel("请粘贴ai回复json: "));
        this.add(scrollPane);
    }

    public String getInputText() {
        // 显示输入对话框
        int result = JOptionPane.showConfirmDialog(null, this, lastFail ? "上次结果含有错误！" : "正常", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // 处理用户的输入
        if (result == JOptionPane.OK_OPTION) {
            String inputText = textArea.getText();
            return inputText;
        } else {
            return "";
        }
    }
}
