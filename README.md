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

## 三、怎么用（含遗忘曲线复习）

- 首次打开 App 会按默认 **09:00** 排好每日提醒；点「设置提醒时间」改时刻。
- 到点手表弹通知，标题显示 **「今日复习（N 词）」**，N 是今天该复习的单词数。
- 手机打开 App → 看释义例句 → 点「已背 ✓」：**该词升一级、下次复习自动推后**（间隔 1→2→4→7→15→30 天，越熟隔越久，即艾宾浩斯遗忘曲线）。
- 顶部显示「连续 X 天 · 累计复习 Y 次」：连续每天都复习，天数 +1；断了从 1 重算。
- 新词按词库顺序**每天新增 1 个**进入复习池，不会一上来砸一堆。
- 重启手机后，BootReceiver 自动把提醒重新排上，不用手动开 App。

> 复习状态（每个词的等级/到期日）存在手机本地 `SharedPreferences`，换手机或重装 App 会清空、需从头复习。

---

## 四、怎么修改单词表（自己就能改）

单词表是 `app/src/main/assets/words.json`，**纯文本、照格式改就行**，不需要懂编程：

1. 用任意文本编辑器（电脑记事本/VS Code，或手机上的文本编辑器 App）打开 `words.json`。
2. 每条单词是一个 `{...}`，逗号隔开，保持 4 个字段：

```json
{"word":"diligent","phonetic":"/ˈdɪlɪdʒənt/","meaning":"adj. 勤奋的","example":"She is diligent in study."}
```

- `word` 英文词（必填）　`phonetic` 音标（可空，写 `""`）　`meaning` 中文释义（必填）　`example` 例句（可空）
- 想加词：复制一行、改内容、末尾加逗号（最后一行不加逗号）。
- 想删词：整行删掉（注意逗号别留错）。
- **改完保存**，重新构建安装即可（见下方「重新构建」）。

> 嫌手改 JSON 麻烦？把你的单词列表（一行一个，格式 `英文 | 中文`）发我，我帮你转成 words.json 并直接重新构建。

---

## 五、重新构建（改完代码/词库后）

- 本工程走 **GitHub Actions 云端构建**（`.github/workflows/build.yml`）：把改动 `git push` 到 `main` 分支，
  GitHub 自动编译，**Actions 页下载 `app-debug.apk`** 重新安装即可。
- 第一次构建需删掉旧版 App 再装（或覆盖安装）。

---

## 六、可继续优化的点（按需让我加）

- 通知里直接带「已背」按钮，不用点开手机。
- 接入在线词库（如自有 Anki/Excel 导出）。
- 单词发音（TTS 朗读）。
- 手表端做成蓝河「智慧卡片」（负一屏常驻今日单词，需走企业开发者上架）。

包名：`com.chenxin.wordreminder`　|　minSdk 26 · targetSdk 34 · Kotlin 1.9
