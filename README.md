# 手表背单词（OPPO 手机端 App + vivo WATCH GT 通知镜像）

每天在固定时间推送一个英语单词通知，通知经蓝牙自动出现在你的 **vivo WATCH GT** 上，
抬腕即看；手机 App 内可点「已背 ✓」打卡，记录**连续天数**和累计词数。

> 为什么不做成手表里的 App？
> vivo WATCH GT 跑的是**蓝河操作系统 BlueOS**，不是安卓/Wear OS，不向个人开放 APK 侧载，
> 手表应用需注册企业开发者账号上架。最稳的路是「手机 App 定时通知 → 手表镜像」，
> 本工程就是这条路线。

---

## 一、把工程跑起来（只需做一次）

1. 在电脑（Mac/Windows 均可）装 **Android Studio**（免费）：https://developer.android.com/studio
2. Android Studio 里选 **New Project → Empty Views Activity**，语言选 **Kotlin**，
   Minimum SDK 选 **26 (Android 8.0)**，包名随意（后面会被覆盖）。
3. 把本仓库里的文件**覆盖**进新项目：
   - `app/build.gradle` → 覆盖新项目的 `app/build.gradle`
   - `app/src/main/AndroidManifest.xml` → 覆盖
   - `app/src/main/java/com/chenxin/wordreminder/` 下 6 个 `.kt` → 复制进对应包目录
   - `app/src/main/res/` 下文件 → 覆盖/加入
   - `app/src/main/assets/words.json` → 放进 `app/src/main/assets/`（没有就新建）
4. 顶部菜单 **Build → Make Project**（或点锤子），等待同步通过。
5. 手机（OPPO）开启 **开发者选项 → USB 调试**，用数据线连电脑。
6. 点 **Run ▶**（绿三角），选你的 OPPO 手机，App 会装好并自动启动。

> 进阶：也可直接 `git clone` 本仓库后用 Android Studio 打开根目录（需要本地有 Gradle 8.5+）。
> 新手用上面的「New Project 覆盖法」最不容易踩坑。

---

## 二、让提醒出现在手表上（关键一步，必须做）

vivo 手表默认不显示所有手机通知，需要手动放行本应用：

1. 打开手机上的 **vivo 健康 / 运动健康** App（与手表配对那个）。
2. 进入 **设备 → 通知管理**（或「消息通知」）。
3. 打开「通知显示」总开关，并在应用列表里**勾选「手表背单词」**。
4. 以后每天到点，手表会震动并弹出单词通知；点通知可回到手机 App 打卡。

---

## 三、怎么用

- 首次打开 App 会按默认 **09:00** 排好每日提醒；点「设置提醒时间」改时刻。
- 到点手表弹通知 → 手机打开 App 看释义例句 → 点「已背 ✓」打卡。
- 连续每天都背，连续天数 +1；哪天断了，重新从 1 算起（专治「计划容易中断」）。
- 重启手机后，BootReceiver 会自动把提醒重新排上，不用手动开 App。

---

## 四、换词库 / 加词

编辑 `app/src/main/assets/words.json`，保持格式即可（每条 `word / phonetic / meaning / example`）：

```json
{"word":"diligent","phonetic":"/ˈdɪlɪdʒənt/","meaning":"adj. 勤奋的","example":"She is diligent in study."}
```

单词按「距 2026-01-01 的天数」循环，词库越多、重复周期越长。
想从你自己的背诵计划开始，把起始日 `WordRepository.kt` 里的 `LocalDate.of(2026,1,1)` 改成你启动那天即可。

---

## 五、可继续优化的点（按需让我加）

- 每天推 **多个** 单词，或随机/按遗忘曲线复习旧词。
- 通知里直接带「已背」按钮，不用点开手机。
- 接入在线词库（如自有 Anki/Excel 导出）。
- 手表端做成蓝河「智慧卡片」（负一屏常驻今日单词，需走企业开发者上架）。

包名：`com.chenxin.wordreminder`　|　minSdk 26 · targetSdk 34 · Kotlin 1.9
