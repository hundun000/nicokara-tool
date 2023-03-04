### NicokaraTool.main()

在`data/example.txt`放入歌词文本。

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

运行后输入"example"。

```
Enter name: 
example
```

程序将输出`@Ruby`格式的汉字注音（歌词中的特殊发音（登った == きた）不会自动处理，如有需要人工后期处理，例如将它加入kanjiHints.json）。用于 [NicoKaraMaker2](https://shinta.coresv.com/help/NicoKaraMaker2_JPN.html)。

```
Ruby: 
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

当出现一个汉字多种读法时，将生成`data/example.kanjiHints.json`:

```json
[{
  "kanji" : "重",
  "hintMap" : {
    "かさ" : [ "かさ,[00:00:00],[99:99:99] // TODO" ],
    "おも" : [ "おも,[00:00:00],[99:99:99] // TODO" ]
  }
}]
```

需要用户手工修改:

```json
[{
  "kanji" : "重",
  "hintMap" : {
    "かさ" : [ "かさ" ],
    "おも" : [ "おも,[02:14:37],[02:18:10]" ]
  }
}, {
  "kanji" : "登",
  "hintMap" : {
    "き" : [ "き" ]
  }
}]
```

再次运行，此时输出变为：

```
...
@Ruby6=重,かさ
@Ruby7=重,おも,[02:14:37],[02:18:10]
...
@Ruby17=登,き
...
```
