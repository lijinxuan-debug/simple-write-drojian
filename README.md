# Accounting 记账应用

一款功能完整的个人记账Android应用。

## 主要功能

- **账单记录**：支持手动记账
- **账单统计**：月度/年度统计，图表可视化展示
- **数据导出**：支持导出Excel账单
- **用户系统**：注册、登录、忘记密码
- **图片附件**：支持添加账单图片
- **分类管理**：自定义收支分类
- **我的发现**：可以同AI机器人进行交流

## 技术栈

- **语言**：Kotlin
- **数据库**：Room
- **UI组件**：Material Design
- **图片加载**：Glide
- **图表**：MPAndroidChart
- **Excel导出**：Apache POI

## 环境要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- Android SDK 31-36
- Gradle 8.0+

## 运行项目

1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接真机或启动模拟器
5. 点击运行按钮

## 项目结构

```
app/src/main/java/com/example/accounting/
├── data/              # 数据层
│   ├── dao/          # 数据访问对象
│   ├── database/     # Room数据库
│   └── model/        # 数据模型
├── ui/               # 界面层
│   ├── home/         # 首页
│   ├── statistics/   # 统计页
│   ├── discovery/    # 发现页
│   ├── profile/      # 个人中心
│   └── record/       # 记账页
├── viewmodel/        # 视图模型
├── adapter/          # 适配器
├── engine/           # 业务引擎
└── utils/            # 工具类
```