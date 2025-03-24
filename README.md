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

进一步地，根据已知的每句歌词的时间范围，合并歌曲音频，此时的歌词分析图片，输出视频。

此用法的实现是通过程序调用翻译和释义接口。

已知问题：单词拆分和注音有小概率不正确；单词释义未联系上下文，有较大概率不正确；Google翻译未联系上下文，较生硬；

### 用法3：生成歌词分析Markdown文件

> DbServiceTest.renderSongJson()

询问大模型AI，prompt暂略，得到歌词分析json，存为文件放入指定位置。

输入片段：

```json
{
    "translation": "闪闪发光的天空中的星星啊",
    "lineNotes": [
      {
        "lyric": "きらきら光る",
        "wordNotes": [
          {
            "text": "きらきら",
            "category": ["状态副词"],
            "explain": "形容闪烁发亮的样子",
            "level": "N3"
          },
          {
            "text": "光る",
            "hurikana": "ひかる",
            "category": ["动词原形"],
            "explain": "发光，闪耀",
            "level": "N4"
          }
        ]
      },
      {
        "lyric": "お空の星よ",
        "wordNotes": [
          {
            "text": "お空",
            "hurikana": "おそら",
            "category": ["普通名词"],
            "explain": "天空（お为美化前缀）",
            "level": "N4"
          },
          {
            "text": "の",
            "category": ["格助词"],
            "explain": "表示所属关系",
            "level": "N5"
          },
          {
            "text": "星",
            "hurikana": "ほし",
            "category": ["普通名词"],
            "explain": "星星",
            "level": "N5"
          },
          {
            "text": "よ",
            "category": ["终助词"],
            "explain": "表示呼唤、感叹",
            "level": "N4"
          }
        ]
      }
    ]
  }
```

运行后生成Markdown文件。

输出片段：

| 原文/中文          | 解释            | 原形  | 分类            | 等级  | 更多解释        |
| -------------- | ------------- | --- | ------------- | --- | ----------- |
| _闪闪发光的天空中的星星啊_ |               ||     |               |     |             
| **きらきら光る**     ||               |     |               |     |             
| きらきら           | 形容闪烁发亮的样子     |     | 状态副词          | N3  |             |
| 光る(ひかる)        | 发光，闪耀         |     | 动词原形          | N4  |             |
| **お空の星よ**      ||               |     |               |     |             
| お空(おそら)        | 天空（お为美化前缀）    |     | 普通名词          | N4  |             |
| の              | 表示所属关系        |     | 格助词           | N5  |             |
| 星(ほし)          | 星星            |     | 普通名词          | N5  |             |
| よ              | 表示呼唤、感叹       |     | 终助词           | N4  |             |

此用法的实现是手工询问大模型AI（因为由程序询问大模型AI需要api付费）。

prompt中要求ai联系上下文解读，能一定程度缓解上述用法2的问题。

#### 依赖

- 单词拆分和注音：[kuromoji](https://github.com/atilika/kuromoji)  
- 单词释义：Moji api  
- 整句翻译：Google翻译
- 手工询问大模型AI
- 输出格式用途：[NicokaraMaker2](https://shinta.coresv.com/old-logs/nicokaramaker2-jpn/)