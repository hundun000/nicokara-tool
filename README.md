## NicokaraTool

由程序为歌词生成注音（含NLP判断多音字的读音），并以NicokaraMaker的格式（@Ruby）输出。

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

### WIP

#### 输入输出歌词的格式

尝试同时支持NicokaraMaker格式和Aegisub格式(`.ass`)。

#### 歌词分析

进一步进行分析（单词词性，单词释义，整句翻译等），输出为图片。

<details open>
    <summary>输出</summary>
    <img src="./doc/1.png" alt="">
</details>

已知问题：单词拆分和注音有小概率不正确；单词释义未联系上下文，有较大概率不正确；Google翻译未联系上下文，较生硬；

#### 依赖

- 单词拆分和注音：[kuromoji](https://github.com/atilika/kuromoji)  
- 单词释义：Moji api  
- 整句翻译：Google翻译
- 输出格式用途：[NicokaraMaker2](https://shinta.coresv.com/old-logs/nicokaramaker2-jpn/)

### 日语使用例 （已过时）

在`data/example-japanese.txt`放入歌词文本。仅支持无时间戳的形式。

```
大切な思い出を
ギュっと抱いて進もう
「ありがとう」も「大好き」も
まだまだ言い足りないでしょ
坂道は続いてく
どのぐらい登った(きた)のかな？
あせらないで大丈夫
進む場所は間違ってないよ

重なる声
あと一歩が重くなるけど
```

假设不存在`data/example-japanese.rootHint.json`（以下简称rootHint.json），运行JapaneseServiceTest，程序输出为：

```
@Ruby1=言,い
@Ruby2=一,ひと
@Ruby3=場所,ばしょ
@Ruby4=歩,ほ
@Ruby5=大切,たいせつ
@Ruby6=重,かさ,[00:00:00],[99:99:99] // TODO
@Ruby7=重,おも,[00:00:00],[99:99:99] // TODO
...
@Ruby17=登,のぼ
...
```

当出现一个汉字多种读法时，将生成rootHint.json:

```json
{
  "kanjiHints" : [ {
    "kanji" : "重",
    "pronounceHints" : [ {
      "pronounce" : "かさ",
      "rubyLines" : [ "かさ,[00:00:00],[99:99:99] // from 重なる" ]
    }, {
      "pronounce" : "おも",
      "rubyLines" : [ "おも,[00:00:00],[99:99:99] // from 重く" ]
    } ]
  } ],
  "nluDisallowHints" : null
}
```

需要用户手工修改rootHint.json：

- 令"重"="かさ"作为默认注音，即不标注时间戳；令"重"="おも"在指定时间区间内生效；
- 新增"登"="き"的特殊读法

```json
{
  "kanjiHints" : [ {
    "kanji" : "重",
    "pronounceHints" : [ {
      "pronounce" : "かさ",
      "rubyLines" : [ "かさ" ]
    }, {
      "pronounce" : "おも",
      "rubyLines" : [ "おも,[02:14:37],[02:18:10]" ]
    } ]
  }, {
    "kanji" : "登",
    "pronounceHints" : [ {
      "pronounce" : "き",
      "rubyLines" : [ "き" ]
    } ]
  }],
  "nluDisallowHints" : null
}
```

再次运行运行JapaneseServiceTest，此时输出变为：

```
...
@Ruby6=重,かさ
@Ruby7=重,おも,[02:14:37],[02:18:10]
...
@Ruby17=登,き
...
```

### 其他WIP功能

#### 为粤语歌词注音

由[pycantonese-server](https://github.com/hundun000/pycantonese-server)提供粤语NLP。

在`data/example-cantonese.txt`放入歌词文本。支持行首行尾时间戳的形式。

```
[00:21.961]攔路雨偏似雪花 飲泣的你凍嗎
[00:26.464]這風褸我給你磨到有襟花[00:28.464]
```

运行CantoneseServiceTest，程序输出为：

```
@Ruby1=你,néih
@Ruby2=飲泣,yám yāp
@Ruby3=風褸,fūng lāu
...
@Ruby19=襟,kām
```

rootHint.json的作用和日语情况类似。对于粤语，可设置nluDisallowHints传给pycantonese，详见pycantonese文档。

此处nluDisallowHints使得“似雪花”在NLP中理解为“似/雪花”而非“似雪/花”

```json
{
  "kanjiHints" : [ ],
  "nluDisallowHints" : [ "似雪" ]
}
```