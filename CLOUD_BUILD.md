# 云端构建 APK（不用装 Android Studio，不占用你电脑）

原理：代码推到 GitHub 后，GitHub 的免费服务器会自动帮你把工程编译成安装包 `app-debug.apk`，
你只需要在网页上点几下下载，再传到 OPPO 安装即可。整个过程你电脑不装任何开发工具。

---

## 第一步：准备一个 GitHub 账号（免费）

- 打开 https://github.com ，点 **Sign up**（注册），按提示填邮箱、设密码、验证邮箱即可。
- **如果你已经有 GitHub 账号，直接用，不用新注册。**
- 完全免费，个人项目够用。

## 第二步：新建一个仓库（Repository）

1. 登录后，点页面右上角 **+** → **New repository**。
2. Repository name 填 `WordReminder`（随便起也行）。
3. 选 **Public**（公开，免费；代码只是背单词小程序，无隐私）。
4. 不要勾选 "Add a README"（保持空仓库，方便等会儿传文件）。
5. 点 **Create repository**。

## 第三步：把工程文件传到 GitHub（推荐用 GitHub Desktop，图形界面、不用敲命令）

> 直接网页拖拽对多层文件夹很麻烦，用官方桌面软件最省事。

1. 下载安装 **GitHub Desktop**：https://desktop.github.com （免费，和 Android Studio 不是一回事）。
2. 打开后用你的 GitHub 账号登录。
3. 点 **File → Clone repository**，选刚才建的 `WordReminder`，克隆到电脑某处。
4. 把 `WordReminder` 文件夹里的**所有内容**复制进克隆出来的那个文件夹
   （含 `.github` 文件夹、 `app` 文件夹、各个 `.gradle` 文件等，**不要漏掉隐藏的 `.github`**）。
5. 回到 GitHub Desktop，它会自动列出改动；在左下角写个说明（如 "init"），点 **Commit to main**。
6. 点右上角 **Push origin**（推送到 GitHub）。

> 推完之后，云端构建就已经自动开始跑了（详见下一步）。

## 第四步：等构建完成、下载 APK

1. 打开浏览器进你的仓库页面，点上方 **Actions** 标签。
2. 你会看到一条正在跑的构建记录（黄色的圈）。点进去，等它变成 **绿色对勾**（通常 3–6 分钟）。
   - 如果没自动开始：进 **Actions** → 左侧 **Build APK** → 右侧 **Run workflow** 手动点一次。
3. 构建成功后，页面下方 **Artifacts** 区会出现 `app-debug`，点它下载（得到 `app-debug.zip`）。
4. 解压得到 `app-debug.apk`。

## 第五步：安装到 OPPO 手机

1. OPPO 上：设置 → 密码与安全 → 系统安全 → **安装未知应用**，给「文件管理」或你用来传文件的 App 打开权限。
2. 把 `app-debug.apk` 传到手机（数据线 / 微信文件传输 / 网盘都行）。
3. 在手机上点开 `app-debug.apk` → 安装。
4. **关键一步（否则手表不提醒）**：手机打开「vivo 健康 / 运动健康」App → 设备 → 通知管理 →
   打开总开关并**勾选本应用「手表背单词」**，之后每日通知会经蓝牙镜像到手表。

---

## 常见问题

- **构建失败/红色叉？** 多半是文件没传全（漏了 `.github` 或 `app` 子目录）。重新核对第三步第 4 点。
- **不想注册 GitHub？** 那走另一条路：让我在这台机器上直接帮你编译出 APK 文件（见 README 里"本地构建"），
  你拿到文件直接装，零账号、零 git，只是会占用这台电脑几分钟编译时间。
- **想换单词表？** 改 `app/src/main/assets/words.json` 后重新推一次 GitHub，构建会自动出新包。
