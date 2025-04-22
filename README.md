## NicokaraTool

由程序对歌词进行若干处理，包括：

- 生成注音（含NLP判断多音字的读音），并以NicokaraMaker的格式（@Ruby）输出。
- 生成歌词分析视频
- 生成歌词分析Markdown文件

### 用法1：生成注音

> JapaneseServiceTest.testAllFeaturesShort() / JapaneseServiceTest.testAllFeaturesLong()

<details open>
    <summary>输入</summary>
    <div style="border: 1px solid black">
        [00:22:90]大切な[00:24:03]思い[00:24:94]出を[00:25:51]<br>
        [00:25:86]ギュっと[00:26:36]抱いて[00:27:10]進もう[00:27:95]<br>
        [00:28:25]「ありがとう」も[00:29:73]「大好き」も[00:30:92]<br>
        まだまだ言い足りないでしょ<br>
        ……<br>
    </div>
</details>

支持输入含或不含timetag。

<details open>
    <summary>输出</summary>
    <div style="border: 1px solid black">
        @Ruby1=言,い<br>
        @Ruby2=一,ひと<br>
        @Ruby3=場所,ばしょ<br>
        @Ruby4=歩,ほ<br>
        @Ruby5=大切,たいせつ<br>
        ……<br>
    </div>
</details>

### 用法2：生成歌词分析视频

> JapaneseServiceTest.testAllFeaturesShort() / JapaneseServiceTest.testAllFeaturesLong()

进一步进行分析（单词词性，单词释义，整句翻译等），输出为图片。

<details open>
    <summary>输出</summary>
    <img src="./doc/1.png" alt="">
</details>

进一步地，合并歌曲音频+每句歌词的时间范围对应的分析图片，输出视频。

此用法的实现是通过程序调用翻译和释义接口。

已知问题：单词拆分和注音有小概率不正确；单词释义未联系上下文，有较大概率不正确；Google翻译未联系上下文，较生硬；

### 用法3：生成歌词分析Markdown文件

> DbServiceTest.renderSongJson()

询问大模型AI，prompt见`Step1AskTemplate.rxt`和`Step2AskTemplate.rxt`，prompt中要求ai联系上下文解读，能一定程度缓解上述用法2的问题。

得到歌词分析json，存为文件放入指定位置。

输入片段：

```json
{
  "translation" : "闪耀着光芒的天上的星星啊",
  "groupNote" : "这句是歌词的开头，直接呼唤或描述天空中闪耀的星星，是一个独立的感叹或引入句。「〜よ」表示呼唤或感叹。",
  "lineNotes" : [ {
    "lyric" : "きらきら光る お空の星よ",
    "wordNotes" : [ {
      "text" : "きらきら",
      "hurikana" : null,
      "origin" : null,
      "translation" : "闪闪发光地",
      "explain" : "副词，表示闪烁的状态，修饰动词「光る」"
    }, {
      "text" : "光る",
      "hurikana" : "ひかる",
      "origin" : "光る",
      "translation" : "闪耀",
      "explain" : "动词基本形，作连体修饰语，修饰后面的名词「星」"
    }, {
      "text" : "お",
      "hurikana" : null,
      "origin" : null,
      "translation" : null,
      "explain" : "接头词，用于名词前，表示美化或尊敬"
    }, {
      "text" : "空",
      "hurikana" : "そら",
      "origin" : null,
      "translation" : "天空",
      "explain" : "名词，表示天空"
    }, {
      "text" : "の",
      "hurikana" : null,
      "origin" : null,
      "translation" : null,
      "explain" : "格助词，连接「お空」和「星」，表示所属关系"
    }, {
      "text" : "星",
      "hurikana" : "ほし",
      "origin" : null,
      "translation" : "星星",
      "explain" : "名词，是句子描述的主体"
    }, {
      "text" : "よ",
      "hurikana" : null,
      "origin" : null,
      "translation" : null,
      "explain" : "终助词，用于句末，表示呼唤或引起注意"
    } ]
  } ]
}
```

再进一步生成Markdown文件。

输出片段：

| 原文/中文               | 解释                                                                       | 原形  | 更多解释                                                |
| ------------------- | ------------------------------------------------------------------------ | --- | --------------------------------------------------- |
| _闪耀着光芒的天上的星星啊_      | 这句是歌词的开头，直接呼唤或描述天空中闪耀的星星，是一个独立的感叹或引入句。「〜よ」表示呼唤或感叹。                       ||     |                                                     
| **きらきら光る お空の星よ**    ||                                                                          |     |                                                     
| きらきら                | 闪闪发光地                                                                    |     | 副词，表示闪烁的状态，修饰动词「光る」                                 |
| 光る(ひかる)             | 闪耀                                                                       | 光る  | 动词基本形，作连体修饰语，修饰后面的名词「星」                             |
| お                   |                                                                          |     | 接头词，用于名词前，表示美化或尊敬                                   |
| 空(そら)               | 天空                                                                       |     | 名词，表示天空                                             |
| の                   |                                                                          |     | 格助词，连接「お空」和「星」，表示所属关系                               |
| 星(ほし)               | 星星                                                                       |     | 名词，是句子描述的主体                                         |
| よ                   |                                                                          |     | 终助词，用于句末，表示呼唤或引起注意                                  |

支持多重方式询问大模型AI：

- Gemini接口（因为有免费额度）
- 本地Ollama接口
- GUI交互，显示prompt，用户人工询问AI后将回复粘贴到输入框交给程序。

#### 依赖

- 单词拆分和注音：[kuromoji](https://github.com/atilika/kuromoji)  
- 单词释义：Moji api  
- 整句翻译：Google翻译
- 大模型AI
- 视频合成：FFMpeg
- 输出格式用途：[NicokaraMaker2](https://shinta.coresv.com/old-logs/nicokaramaker2-jpn/)