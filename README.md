# 户外跑模拟器

最近学校有个咪咕善跑活动可以抵消晨跑次数，为此写的作弊APP

## 使用方法

启动软件，如果没有开启模拟位置选项，先在开发者选项中打开，然后在地图中选取起点和终点，并且可以添加最多4个途径点，最后点击开始模拟。

进入咪咕善跑（其它同类软件未测试），开始户外跑，等一会就可以看到效果。

默认初始速度为1-7m/s，最高速度会随着时间慢慢减小，海拔22加减5米，没做设置界面，全都写死在LocationUtil类里面。

## 原理

利用高德地图SDK实现选取坐标点并根据这些坐标点规划路径，规划路径成功后可以得到一系列坐标点（依次连接这些坐标点就是规划出来的路径）。

得到这些坐标后如何实现GPS轨迹效果呢？

高德地图API中有个虚拟导航功能可以很方便地实现，但一旦开始导航后修改虚拟导航速度就失效了，导致从头到尾都是一个速度，个人觉得太假了，最后用了下面的方法

模拟定位时依次选取相邻两个坐标点，先算出直线距路，再生成随机速度，t=s/v得到所需时间，得到这些参数后就可以在时间s内每隔200毫秒更新下当前坐标（具体在LocationUtil.navigationLine方法）

如何虚拟GPS坐标等这边不说了，代码不多很好找

    由于签名不同的原因，编译后可能无法正常使用高德地图SDK，请自行申请高德地图KEY并配置

[下载成品APK](https://github.com/L-JINBIN/RunningSimulator/raw/master/app/app-release.apk)

## 截图

![image](https://github.com/L-JINBIN/RunningSimulator/raw/master/screenshots/Screenshot_1.png)

![image](https://github.com/L-JINBIN/RunningSimulator/raw/master/screenshots/Screenshot_2.png)

![image](https://github.com/L-JINBIN/RunningSimulator/raw/master/screenshots/Screenshot_3.png)