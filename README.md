### NicokaraTool.main()

在`data/input.txt`放入歌词文本。

```
大切な思い出を
ギュっと抱いて進もう
「ありがとう」も「大好き」も
まだまだ言い足りないでしょ
坂道は続いてく
どのぐらい登った(きた)のかな？
あせらないで大丈夫
進む場所は間違ってないよ
...
```

运行后输出`@Ruby`格式的汉字注音（歌词中的特殊发音（登った == きた）不会处理，如有需要人工后期处理）。用于 [NicoKaraMaker2](https://shinta.coresv.com/help/NicoKaraMaker2_JPN.html)。

```
@Ruby0=言,い
@Ruby1=一,いち
@Ruby2=頂,いただき
@Ruby3=待,ま
@Ruby4=分,ぶん
@Ruby5=場所,ばしょ
@Ruby6=暇,ひま
@Ruby7=上手,うま
@Ruby8=越,こ
@Ruby9=見,み
@Ruby10=向,む
...
```