# 百度语音合成集成

[![GitHub release](https://img.shields.io/badge/release-1.0.9-green.svg)]()

# 自己构建成工具类

## 1.初始化

```
YuYinHeChengUtils.init(this,
            10010,
            appId,
            appKey,
            secretKey)
```

## 2.权限在onRequestPermissionsResult调用

```
YuYinHeChengUtils.onRequestPermissionsResult(requestCode, permissions , grantResults)
```

## 3.播放

```
if (YuYinHeChengUtils.getInitSuccess()) {//判断是否初始化成功
       YuYinHeChengUtils.speak(mTv.text.toString())
}
```

## 4.回收资源

```
YuYinHeChengUtils.onDestroy()
```

## 5.设置是否打印日志

```
    YuYinHeChengUtils.debug//默认为true
```


# 集成过程
## 第一步 添加sdk和so文件

1. 到[官网sdk下载界面](http://ai.baidu.com/sdk)下载android语音合成中的离在线融合sdk。
2. 复制com.baidu.tts_xxx.jar到libs文件夹下
3. 复制其中的assets离线语音文件到本地assets中
  
    ```
    其中
    m15:离线男声
    f7:离线女声
    yyjw:度逍遥
    as:度丫丫
    ```

4. 复制其中的so库到armeabi中

## 第二步 复制deomo中的其他文件到自己demo中

