# TheRouter Android 组件化框架 - 完整项目分析文档

## 📋 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术栈](#2-技术栈)
- [3. 核心架构](#3-核心架构)
- [4. 模块结构详解](#4-模块结构详解)
- [5. 四大核心功能](#5-四大核心功能)
- [6. Gradle 插件系统](#6-gradle-插件系统)
- [7. 注解处理器 (APT/KSP)](#7-注解处理器-aptksp)
- [8. 构建配置](#8-构建配置)
- [9. 使用示例](#9-使用示例)
- [10. 高级特性](#10-高级特性)

---

## 1. 项目概述

### 1.1 项目简介

**TheRouter** 是一个强大的 Android 组件化解决方案，由货拉拉移动技术团队开发并开源。该项目旨在解决大型 Android 应用在模块化、组件化过程中面临的页面跳转、跨模块通信、模块初始化等核心问题。

### 1.2 核心定位

- **平台**: Android
- **语言**: Kotlin + Java
- **版本**: 1.3.2 (稳定版) / 1.3.3-rc1 (Compose 扩展版)
- **许可证**: Apache License 2.0
- **仓库**: Maven Central

### 1.3 核心价值

1. **解耦业务模块**: 通过路由机制实现模块间的零依赖通信
2. **简化开发流程**: 注解驱动的开发模式，减少样板代码
3. **提升性能**: 编译期处理 + 增量编译，运行时开销极小
4. **灵活扩展**: 支持拦截器、参数转换、动态加载等高级特性

---

## 2. 技术栈

### 2.1 开发语言

- **Kotlin**: 2.2.10 (主要开发语言)
- **Java**: 17 (兼容 Java 8+)

### 2.2 核心框架

| 框架/库 | 版本 | 用途 |
|---------|------|------|
| Android Gradle Plugin | 9.1.0 | 构建工具 |
| Kotlin Compose Plugin | 2.2.10 | Compose 支持 |
| KSP (Kotlin Symbol Processing) | 2.3.6 | 注解处理 |
| Gson | 2.9.1 | JSON 序列化 |
| ASM | 9.5 | 字节码操作 |
| Auto Service | 1.0 | SPI 服务注册 |

### 2.3 Android 生态

- **AndroidX**: 
  - Core KTX: 1.18.0
  - AppCompat: 1.7.1
  - Lifecycle: 2.10.0
  - Navigation Compose: 2.9.3
  - Material3: 1.10.0
  - RecyclerView: 1.3.0
  
- **Jetpack Compose**: 
  - Compose BOM: 2024.09.00
  - Glance (Widget): 1.1.1

### 2.4 编译目标

- **Compile SDK**: 36 (Android 16 Preview)
- **Min SDK**: 17 (基础库) / 23 (应用层)
- **Target SDK**: 36

---

## 3. 核心架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ business │  │ business │  │ business │              │
│  │    -a    │  │    -b    │  │   -base  │              │
│  └──────────┘  └──────────┘  └──────────┘              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   Router Core Layer                      │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Navigator  │  │ ServiceProv. │  │ FlowTaskExec │    │
│  │  (路由)    │  │  (依赖注入)   │  │  (自动初始化) │    │
│  └────────────┘  └──────────────┘  └──────────────┘    │
│  ┌────────────┐                                         │
│  │ActionManager│                                        │
│  │ (动态加载)  │                                         │
│  └────────────┘                                         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                  Infrastructure Layer                    │
│  ┌────────────┐  ┌──────────────┐                       │
│  │   APT/KSP  │  │ Gradle Plugin│                       │
│  │(注解处理器) │  │  (编译增强)   │                       │
│  └────────────┘  └──────────────┘                       │
└─────────────────────────────────────────────────────────┘
```

### 3.2 数据流向

```
开发者编写注解代码
       ↓
KSP/APT 编译期扫描
       ↓
生成路由表/服务注册表
       ↓
Gradle Plugin 字节码增强
       ↓
生成 routeMap.json + .therouter 文件
       ↓
运行时自动加载初始化
       ↓
提供路由跳转/依赖注入能力
```

---

## 4. 模块结构详解

### 4.1 模块划分

项目采用多模块架构，共包含 8 个主要模块：

#### 4.1.1 router (核心库)

**路径**: `router/`  
**类型**: Android Library  
**包名**: `cn.therouter`

**核心职责**:
- 提供路由跳转核心逻辑
- 实现导航器 (Navigator)
- 管理路由表 (RouteMap)
- 支持依赖注入 (ServiceProvider)
- 实现 FlowTask 自动初始化
- ActionManager 动态方法调用

**关键类**:
- `Navigator.kt`: 路由导航器，支持 Activity/Fragment 跳转
- `RouteItem.kt`: 路由项描述
- `PendingNavigator.kt`: 延迟跳转管理
- `ActionManager.kt`: Action 事件管理
- `ServiceProvider.kt`: 跨模块服务提供者
- `TheRouter.kt`: 框架入口类

**依赖**:
```kotlin
implementation(androidx.core.ktx)
implementation(androidx.appcompat)
implementation(gson)
```

#### 4.1.2 apt (注解处理器)

**路径**: `apt/`  
**类型**: Java Library (Kotlin)  
**包名**: `com.therouter.apt`

**核心职责**:
- 编译期扫描注解 (`@Route`, `@Autowired`, `@ServiceProvider`)
- 生成路由表和辅助代码
- 支持 KSP 和传统 APT 两种方式

**关键类**:
- `RouteProcessor.kt`: 路由注解处理器
- `AutowiredProcessor.kt`: 参数注入处理器
- `ServiceProviderProcessor.kt`: 服务提供者处理器
- `FlowTaskProcessor.kt`: 流程任务处理器

**生成的文件**:
- `route.therouter`: 路由表数据
- `serviceProvide.therouter`: 服务注册表
- `autowired.therouter`: 参数注入映射
- `flow.therouter`: 流程任务定义

**依赖**:
```kotlin
implementation(gson)
implementation(kotlin-stdlib)
implementation(ksp)
implementation(google-auto-service)
```

#### 4.1.3 buildSrc (Gradle 插件)

**路径**: `buildSrc/`  
**类型**: Gradle Plugin  
**包名**: `com.therouter.plugin`

**核心职责**:
- 字节码增强 (ASM)
- 路由表合并与优化
- 增量编译支持
- 代码注入

**关键类**:
- `TheRouterPlugin.kt`: 插件入口
- `TheRouterTask.kt`: 全量转换任务
- `TheRouterGetAllTask.kt`: 增量获取任务
- `TheRouterASM.kt`: ASM 字节码转换
- `AddCodeVisitor.java`: 类访问器

**工作机制**:
```kotlin
// AGP 8+ 集成
variant.instrumentation.transformClassesWith(
    TheRouterASM::class.java,
    InstrumentationScope.ALL
) { params ->
    params.theRouterBuildFolder.set(buildFolder)
}
```

**依赖**:
```kotlin
implementation(gradleApi)
implementation(com.android.tools.build:gradle-api:9.1.0)
implementation(asm:9.5)
implementation(asm-commons:9.5)
implementation(gson:2.9.1)
```

#### 4.1.4 app (示例应用)

**路径**: `app/`  
**类型**: Android Application  
**包名**: `com.therouter.app`

**核心职责**:
- 演示 TheRouter 各项功能
- 集成所有业务模块
- 提供完整的 UI 测试场景

**功能模块**:
- `navigator/`: 路由跳转演示
- `serviceprovider/`: 依赖注入演示
- `flowtask/`: 自动初始化演示
- `action/`: Action 动态调用演示
- `compose/`: Compose 集成演示
- `test/`: 单元测试

**依赖关系**:
```kotlin
api(project(":business-a"))
api(project(":business-b"))
api(project(":business-base"))
sourceKsp(libs.therouter.apt)
source(libs.therouter.router)
source(libs.therouter.compose)
```

#### 4.1.5 business-base (基础业务层)

**路径**: `business-base/`  
**类型**: Android Library  
**包名**: `com.therouter.demo.base`

**核心职责**:
- 提供基础 BaseActivity
- 定义跨模块接口 (IServiceProvider)
- 共享资源和工具类

**关键内容**:
- `BaseActivity.kt`: 基类 Activity，统一注入逻辑
- `di/`: 依赖注入接口定义
  - `ITest.kt`
  - `IJavaServiceProvider.kt`
  - `IJavaServiceProvider2.kt`

#### 4.1.6 business-a (业务模块 A)

**路径**: `business-a/`  
**类型**: Android Library  
**包名**: `com.therouter.businessa`

**核心职责**:
- 演示业务模块开发
- 展示模块间通信
- 提供测试页面

**依赖**:
```kotlin
api(project(":business-base"))
sourceKsp(libs.therouter.apt)
source(libs.therouter.router)
```

#### 4.1.7 business-b (业务模块 B)

**路径**: `business-b/`  
**类型**: Android Library  
**包名**: `com.therouter.businessb`

**核心职责**: 同 business-a，用于演示多模块场景

#### 4.1.8 compose (Compose 扩展)

**路径**: `compose/`  
**类型**: Android Library  
**包名**: `cn.therouter.compose`

**核心职责**:
- 为 Jetpack Compose 提供路由支持
- Compose 页面的路由注解处理

**版本**: 1.3.3-rc1 (独立于主版本)

---

## 5. 四大核心功能

### 5.1 页面导航 (Navigator)

#### 5.1.1 功能概述

Navigator 是 TheRouter 的路由导航核心，支持 Activity、Fragment 的跳转，并提供丰富的参数传递和拦截器功能。

#### 5.1.2 核心特性

1. **多 Path 映射**: 一个 URL 可映射到多个路由项
2. **参数自动拼接**: 智能处理 URL 参数
3. **Fragment 支持**: 可直接创建 Fragment 实例
4. **动画支持**: 自定义转场动画
5. **拦截器链**: Path 修改器、页面替换器、路由替换器、AOP 拦截器
6. **延迟跳转**: 路由表未初始化时的跳转暂存
7. **Action 兼容**: 自动识别并处理 Action 事件

#### 5.1.3 使用示例

```kotlin
// 基本跳转
TheRouter.build("http://therouter.com/home")
    .withString("key", "value")
    .withInt("count", 10)
    .navigation()

// Fragment 创建
val fragment = TheRouter.build("http://therouter.com/fragment")
    .createFragment<MyFragment>()

// 带回调的跳转
TheRouter.build("http://therouter.com/page")
    .navigation(context, object : NavigationCallback {
        override fun onFound(navigator: Navigator) {
            // 找到路由
        }
        override fun onLost(navigator: Navigator, requestCode: Int) {
            // 未找到路由
        }
        override fun onArrival(navigator: Navigator) {
            // 跳转完成
        }
    })
```

#### 5.1.4 拦截器体系

**Path 修改器** (`NavigatorPathFixHandle`):
```kotlin
addNavigatorPathFixHandle(object : NavigatorPathFixHandle {
    override fun watch(path: String?): Boolean {
        return path?.contains("http:") == true
    }
    override fun fix(path: String?): String {
        return path?.replace("http:", "https:") ?: ""
    }
})
```

**页面替换器** (`PathReplaceInterceptor`):
```kotlin
addPathReplaceInterceptor(object : PathReplaceInterceptor {
    override fun watch(path: String?): Boolean {
        return path == "https://kymjs.com/splash/to/home"
    }
    override fun replace(path: String?): String {
        return "https://business.com/home"
    }
})
```

**路由替换器** (`RouterReplaceInterceptor`):
```kotlin
addRouterReplaceInterceptor(object : RouterReplaceInterceptor {
    override fun watch(route: RouteItem?): Boolean {
        return route?.path?.contains("/wallet") == true
    }
    override fun replace(route: RouteItem?): RouteItem? {
        // 未登录时替换到登录页
        return if (!isLogin) {
            RouteItem("/login", LoginActivity::class.java.name)
        } else route
    }
})
```

**AOP 拦截器** (`RouterInterceptor`):
```kotlin
setRouterInterceptor { routeItem, callback ->
    if (routeItem.path.contains("/debug") && isProduction) {
        // 生产环境拦截 debug 页面
        return@setRouterInterceptor
    }
    callback.invoke(routeItem) // 继续跳转
}
```

#### 5.1.5 参数传递机制

```kotlin
// URL 参数解析
TheRouter.build("http://therouter.cn/page?a=b&k=v")
    .withString("c", "d")
    .withString("k", "x")  // 优先使用 with 的值
    .getUrlWithParams()
// 结果: http://therouter.cn/page?c=d&a=b&k=x

// Hash 参数解析
TheRouter.build("https://therouter.cn/page?code=123#/abc&k=v")
// 会解析 hash 部分的参数 k=v
```

#### 5.1.6 核心代码位置

**Navigator.kt** (868 行):
- 构造函数处理 URL 解析和 Path 修改
- `navigation()`: Activity 跳转
- `createFragment()`: 创建 Fragment
- `createIntent()`: 生成 Intent
- `getUrlWithParams()`: 智能参数拼接

**关键逻辑**:
```kotlin
init {
    // 1. 应用 Path 修改器
    for (handle in fixHandles) {
        if (it.watch(url)) {
            url = it.fix(url)
        }
    }
    
    // 2. 解析 URL 参数 (query + fragment)
    parserString(uri.encodedQuery)
    parserString(uri.encodedFragment)
}
```

---

### 5.2 跨模块依赖注入 (ServiceProvider)

#### 5.2.1 功能概述

ServiceProvider 实现了跨模块的服务注册与发现机制，无需直接依赖即可调用其他模块的功能。

#### 5.2.2 核心特性

1. **接口隔离**: 调用方只依赖接口，不依赖实现
2. **懒加载**: 服务在首次使用时创建
3. **参数化创建**: 支持带参数的服务构造
4. **编译期检查**: 注解处理器验证服务合法性

#### 5.2.3 使用示例

**定义服务接口** (business-base):
```java
public interface IJavaServiceProvider {
    String getString();
}
```

**提供服务实现** (business-a):
```java
public class JavaServiceProvider {
    @ServiceProvider
    public static IJavaServiceProvider create() {
        return new IJavaServiceProvider() {
            @Override
            public String getString() {
                return "Hello from Module A";
            }
        };
    }
}
```

**使用服务** (business-b):
```java
IJavaServiceProvider service = TheRouter.get(IJavaServiceProvider.class);
if (service != null) {
    String result = service.getString();
}
```

#### 5.2.4 带参数的服务

```kotlin
interface IUserService {
    fun getUserInfo(userId: String): UserInfo
}

class UserServiceImpl : IUserService {
    constructor(context: Context, token: String) {
        // 初始化逻辑
    }
}

@ServiceProvider(params = [Context::class, String::class])
fun createUserService(context: Context, token: String): IUserService {
    return UserServiceImpl(context, token)
}

// 使用
val service = TheRouter.get(IUserService::class, context, "token123")
```

#### 5.2.5 实现原理

1. **编译期**: APT 扫描 `@ServiceProvider` 注解，生成 `serviceProvide.therouter` 文件
2. **运行期**: 
   - `ServiceProvider` 类加载时读取注册表
   - `TheRouter.get()` 通过反射或工厂方法创建实例
   - 支持单例和多例模式

---

### 5.3 单模块自动初始化 (FlowTaskExecutor)

#### 5.3.1 功能概述

FlowTaskExecutor 提供了模块化的自动初始化机制，每个模块可以声明自己的初始化任务，框架会按依赖顺序自动执行。

#### 5.3.2 核心特性

1. **声明式初始化**: 通过接口声明任务依赖
2. **自动排序**: 根据依赖关系确定执行顺序
3. **异步支持**: 支持同步/异步任务
4. **生命周期感知**: 可在 Application 不同阶段执行

#### 5.3.3 使用示例

**定义任务常量** (business-base):
```java
public interface BusinessBaseFlowTask extends TheRouterFlowTask {
    String BASE_INIT = "base_init";
}
```

**声明任务依赖** (business-a):
```java
public interface BusinessAFlowTask extends TheRouterFlowTask, BusinessBaseFlowTask {
    String BIZA_INTERCEPTOR = "businessA_interceptor";
    String BIZA_INIT1 = "businessA_init1";
}
```

**实现初始化任务**:
```java
@FlowTask(taskName = BusinessAFlowTask.BIZA_INIT1, dependsOn = BusinessBaseFlowTask.BASE_INIT)
public class BusinessAInitTask implements FlowTaskExecutor {
    @Override
    public void init(Context context) {
        // 同步初始化逻辑
    }
    
    @Override
    public boolean isAsync() {
        return false;
    }
}
```

**异步任务示例**:
```java
@FlowTask(taskName = BusinessAFlowTask.BIZA_INIT2)
public class AsyncInitTask implements FlowTaskExecutor {
    @Override
    public void init(Context context) {
        // 异步初始化
        new Thread(() -> {
            // 耗时操作
            finish(); // 完成后必须调用
        }).start();
    }
    
    @Override
    public boolean isAsync() {
        return true;
    }
}
```

#### 5.3.4 任务依赖图

```
Application Start
      ↓
base_init (基础模块)
      ↓
businessA_init1 (依赖 base_init)
      ↓
businessA_init2 (依赖 businessA_init1)
      ↓
businessB_init1 (依赖 businessA_init2)
```

#### 5.3.5 实现原理

1. **编译期**: 扫描 `@FlowTask` 注解，生成 `flow.therouter` 文件
2. **运行期**: 
   - `FlowTaskExecutor` 读取所有任务
   - 拓扑排序确定执行顺序
   - 依次执行任务并监听完成状态

---

### 5.4 动态方法调用 (ActionManager)

#### 5.4.1 功能概述

ActionManager 允许客户端在运行时动态注册和执行方法，支持远程配置下发，实现动态化能力。

#### 5.4.2 核心特性

1. **动态注册**: 运行时注册 Action 处理器
2. **链式调用**: 支持多个拦截器串联
3. **参数传递**: 通过 Bundle 在拦截器间传递状态
4. **远程配置**: 可通过服务端下发 Action 配置
5. **线程安全**: 主线程执行，支持并发注册

#### 5.4.3 使用示例

**注册 Action**:
```kotlin
TheRouter.addActionInterceptor("action://logout") { context, navigator ->
    // 执行登出逻辑
    UserManager.logout()
    Toast.makeText(context, "已登出", Toast.LENGTH_SHORT).show()
    true // 终止后续拦截器
}
```

**触发 Action**:
```kotlin
// 方式1: 直接调用
TheRouter.build("action://logout").action()

// 方式2: 导航器兼容 (自动识别 Action)
TheRouter.build("action://logout").navigation()

// 方式3: 带参数
TheRouter.build("action://showToast")
    .withString("message", "Hello")
    .action(context)
```

**多拦截器链**:
```kotlin
// 拦截器1: 日志记录
TheRouter.addActionInterceptor("action://purchase") { _, navigator ->
    Log.d("Purchase", "开始购买流程")
    false // 继续下一个拦截器
}

// 拦截器2: 权限检查
TheRouter.addActionInterceptor("action://purchase") { context, navigator ->
    if (!UserManager.isLogin()) {
        // 跳转登录
        TheRouter.build("/login").navigation(context)
        true // 终止
    }
    false // 继续
}

// 拦截器3: 业务逻辑
TheRouter.addActionInterceptor("action://purchase") { context, navigator ->
    val productId = navigator.extras.getString("productId")
    PurchaseManager.buy(productId)
    true
}
```

**移除拦截器**:
```kotlin
// 移除指定 Action 的所有拦截器
TheRouter.removeAllInterceptorForKey("action://logout")

// 移除特定拦截器实例
TheRouter.removeActionInterceptor("action://purchase", interceptor)
```

#### 5.4.4 实战案例

**全局 Toast 工具**:
```kotlin
@Route(path = HomePathIndex.ACTION_TOAST)
class ToastAction : ActionInterceptor() {
    override fun handle(context: Context, navigator: Navigator): Boolean {
        val message = navigator.extras.getString("message") ?: ""
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        return true
    }
}

// 任意模块调用
TheRouter.build(HomePathIndex.ACTION_TOAST)
    .withString("message", "操作成功")
    .action()
```

**埋点统计**:
```kotlin
TheRouter.addActionInterceptor("action://page_view") { _, navigator ->
    val pageName = navigator.extras.getString("page")
    Analytics.track("page_view", mapOf("page" to pageName))
    false // 不阻断
}
```

#### 5.4.5 实现原理

**数据结构**:
```kotlin
// simpleUrl -> List<ActionInterceptor>
private val actionHandleMap = ConcurrentHashMap<String, MutableList<ActionInterceptor>>()
```

**执行流程**:
1. 查找 Action 对应的拦截器列表
2. 按优先级排序
3. 依次调用 `handle()`，传入 Bundle
4. 拦截器可修改 Bundle 传递给下一个
5. 全部执行完后调用 `onFinish()`

**线程模型**:
```kotlin
internal fun handleAction(navigator: Navigator, context: Context?) {
    executeInMainThread {
        // 确保在主线程执行
        val interceptorList = actionHandleMap[navigator.simpleUrl]
        // ... 执行逻辑
    }
}
```

---

## 6. Gradle 插件系统

### 6.1 插件架构

TheRouter 使用自定义 Gradle 插件实现编译期增强，支持 AGP 8+ 的新 API。

**插件 ID**: `therouter`  
**实现类**: `com.therouter.plugin.TheRouterPlugin`

### 6.2 核心功能

#### 6.2.1 字节码增强

使用 ASM 库在编译期修改类文件：
- 注入路由表加载代码
- 优化类初始化逻辑
- 添加性能监控点

#### 6.2.2 增量编译支持

**两种模式**:

1. **增量模式** (`isIncremental = true`):
   ```kotlin
   variant.instrumentation.transformClassesWith(
       TheRouterASM::class.java,
       InstrumentationScope.ALL
   )
   ```
   - 仅处理变更的类
   - 使用缓存加速构建
   - 适合开发阶段

2. **全量模式** (`isIncremental = false`):
   ```kotlin
   val theRouterTask = project.tasks.register(variantName, TheRouterTask::class)
   variant.artifacts.use(theRouterTask).toTransform(...)
   ```
   - 完整转换所有类
   - 生成完整路由表
   - 适合发布构建

#### 6.2.3 路由表合并

插件会收集所有模块的路由信息，生成统一的 `routeMap.json`:

```json
[
  {
    "path": "http://therouter.com/home",
    "className": "com.therouter.app.navigator.HomeActivity",
    "action": "action://scheme.com",
    "description": "路由测试首页",
    "params": {
      "strFromAnnotation": "来自注解设置的默认值"
    }
  }
]
```

### 6.3 配置选项

```kotlin
// build.gradle.kts
TheRouter {
    // 调试模式 (打印详细日志)
    debug = true
    
    // 强制增量编译
    forceIncremental = false
    
    // 增量缓存路径
    incrementalCachePath = "build/therouter"
    
    // 路由表路径 (默认 src/main/assets/therouter/routeMap.json)
    assetRouteMapPath = null
    
    // 检查路由表完整性
    checkRouteMap = true
    
    // 检查流程依赖
    checkFlowDepend = true
}
```

### 6.4 插件工作流程

```
1. 读取扩展配置 (TheRouterExtension)
         ↓
2. 注册 Transform/Instrumentation
         ↓
3. 收集所有 Jar/Directories
         ↓
4. ASM 扫描类文件
         ↓
5. 提取路由信息
         ↓
6. 生成 .therouter 文件
         ↓
7. 合并 routeMap.json
         ↓
8. 注入初始化代码
```

### 6.5 调试技巧

**插件源码调试**:
1. 将 `buildSrc` 文件夹重命名为其他名称
2. 移除根 `build.gradle` 中的 `classpath` 引用
3. 直接在项目中修改插件代码
4. Sync Gradle 查看效果

---

## 7. 注解处理器 (APT/KSP)

### 7.1 支持的注解

#### 7.1.1 @Route

标记 Activity/Fragment 为路由页面。

```kotlin
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Route(
    val path: String,           // 路由路径 (URL)
    val action: String = "",    // 关联的 Action
    val description: String = "", // 描述信息
    val params: Array<String> = [] // 默认参数 (kv 交替)
)
```

**使用示例**:
```kotlin
@Route(
    path = "http://therouter.com/home",
    action = "action://home_action",
    description = "首页",
    params = ["title", "默认标题"]
)
class HomeActivity : AppCompatActivity() {
    // ...
}
```

#### 7.1.2 @Autowired

标记字段为自动注入参数。

```kotlin
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class Autowired(
    val name: String = "",      // 参数名 (默认字段名)
    val required: Boolean = true, // 是否必需
    val defaultValue: String = "" // 默认值
)
```

**使用示例**:
```kotlin
class DetailActivity : AppCompatActivity() {
    @Autowired(name = "user_id", required = true)
    var userId: String? = null
    
    @Autowired(defaultValue = "18")
    var age: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TheRouter.inject(this) // 必须在 onCreate 中调用
        // userId 和 age 已自动赋值
    }
}
```

#### 7.1.3 @ServiceProvider

标记服务提供者方法。

```kotlin
@Retention(RetentionPolicy.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class ServiceProvider(
    val returnType: KClass<*> = ServiceProvider::class, // 返回类型
    val params: Array<KClass<*>> = [] // 构造函数参数
)
```

**使用示例**:
```kotlin
interface ILogger {
    fun log(message: String)
}

class LoggerImpl : ILogger {
    override fun log(message: String) {
        Log.d("Logger", message)
    }
}

@ServiceProvider
fun provideLogger(): ILogger {
    return LoggerImpl()
}
```

#### 7.1.4 @FlowTask

标记初始化任务。

```kotlin
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class FlowTask(
    val taskName: String,        // 任务名称
    val dependsOn: String = ""   // 依赖的任务
)
```

**使用示例**:
```kotlin
@FlowTask(
    taskName = BusinessAFlowTask.BIZA_INIT,
    dependsOn = BusinessBaseFlowTask.BASE_INIT
)
class InitTask : FlowTaskExecutor {
    override fun init(context: Context) {
        // 初始化逻辑
    }
    
    override fun isAsync(): Boolean = false
}
```

### 7.2 处理器实现

#### 7.2.1 RouteProcessor

扫描 `@Route` 注解，生成路由表。

**处理流程**:
1. 扫描所有标注 `@Route` 的类
2. 验证类是否为 Activity/Fragment
3. 提取注解参数
4. 生成 `RouteItem` 记录
5. 写入 `route.therouter` 文件

**生成文件格式**:
```
# route.therouter
http://therouter.com/home=com.therouter.app.HomeActivity|action://home|首页|title=默认标题
```

#### 7.2.2 AutowiredProcessor

扫描 `@Autowired` 注解，生成参数注入代码。

**生成代码示例**:
```java
// DetailActivity$$AutowiredInjector.java
public class DetailActivity$$AutowiredInjector implements AutowiredInjector<DetailActivity> {
    @Override
    public void inject(DetailActivity target) {
        Bundle bundle = target.getIntent().getExtras();
        if (bundle != null) {
            target.userId = bundle.getString("user_id");
            target.age = bundle.getInt("age", 18);
        }
    }
}
```

#### 7.2.3 ServiceProviderProcessor

扫描 `@ServiceProvider` 注解，生成服务注册表。

**生成文件格式**:
```
# serviceProvide.therouter
com.therouter.demo.di.ILogger=com.therouter.app.ServiceProvider_ProvideLogger
```

### 7.3 KSP vs APT

TheRouter 同时支持两种注解处理方式：

| 特性 | APT | KSP |
|------|-----|-----|
| 速度 | 较慢 | 快 2-3 倍 |
| Kotlin 支持 | 有限 | 原生支持 |
| 增量编译 | 支持 | 更好支持 |
| 配置 | `kapt` | `ksp` |

**推荐配置**:
```kotlin
// 使用 KSP (推荐)
plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    ksp(libs.therouter.apt)
}

// 或使用传统 APT
dependencies {
    kapt(libs.therouter.apt)
}
```

---

## 8. 构建配置

### 8.1 根目录配置

**build.gradle.kts**:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}
```

**settings.gradle.kts**:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.therouter.cn:8443/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.therouter.cn:8443/repository/maven-public/")
    }
}

// 动态包含模块
getLocalProperties().entries.forEach { entry ->
    if (entry.value.toString().toBoolean()) {
        include(":${entry.key}")
    }
}
```

### 8.2 版本目录 (libs.versions.toml)

```toml
[versions]
agp = "9.1.0"
kotlin = "2.2.10"
therouter = "1.3.2"
therouter-compose = "1.3.3-rc1"
gson = "2.9.1"
ksp = "2.3.6"

[libraries]
therouter-router = { group = "cn.therouter", name = "router", version.ref = "therouter" }
therouter-apt = { group = "cn.therouter", name = "apt", version.ref = "therouter" }
therouter-compose = { group = "cn.therouter", name = "compose", version.ref = "therouter-compose" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
therouter-plugin = { id = "cn.therouter", version.ref = "therouter" }
```

### 8.3 应用模块配置

**app/build.gradle.kts**:
```kotlin
plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("therouter")
}

android {
    namespace = "com.therouter.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.therouter.app"
        minSdk = 23
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding {
            enable = true
        }
    }
}

dependencies {
    api(project(":business-a"))
    api(project(":business-b"))
    api(project(":business-base"))
    
    sourceKsp(libs.therouter.apt)
    source(libs.therouter.compose)
    source(libs.therouter.router)
}
```

### 8.4 库模块配置

**business-a/build.gradle.kts**:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.therouter.businessa"
    compileSdk = 36
    defaultConfig {
        minSdk = 17
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    api(project(":business-base"))
    
    sourceKsp(libs.therouter.apt)
    source(libs.therouter.router)
}
```

### 8.5 ProGuard 配置

**proguard-rules.pro**:
```proguard
# Fragment 路由支持 (如需要)
# -keep public class * extends android.app.Fragment
# -keep public class * extends androidx.fragment.app.Fragment

# 保持 Keep 注解的类
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}

# 保持 Keep 注解的成员
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 保持 Keep 注解的方法/构造函数/字段
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# 保持 Autowired 注解的字段
-keepclasseswithmembers class * {
    @com.therouter.router.Autowired <fields>;
}
```

### 8.6 local.properties 调试配置

```properties
# 启用需要调试的模块
apt=true
router=true
buildSrc=true
```

---

## 9. 使用示例

### 9.1 快速开始

#### Step 1: 添加依赖

**根 build.gradle**:
```groovy
buildscript {
    dependencies {
        classpath 'cn.therouter:plugin:1.3.2'
    }
}
```

**模块 build.gradle**:
```groovy
apply plugin: 'therouter'

dependencies {
    kapt "cn.therouter:apt:1.3.2"
    implementation "cn.therouter:router:1.3.2"
}
```

#### Step 2: 初始化 (可选)

```java
public class App extends MultiDexApplication {
    @Override
    protected void attachBaseContext(Context base) {
        TheRouter.setDebug(BuildConfig.DEBUG);
        super.attachBaseContext(base);
    }
}
```

#### Step 3: 在 BaseActivity 中注入

```java
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TheRouter.inject(this);
    }
}
```

#### Step 4: 声明路由页面

```kotlin
@Route(path = "http://therouter.com/detail")
class DetailActivity : BaseActivity() {
    @Autowired
    var id: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // id 已自动注入
    }
}
```

#### Step 5: 跳转页面

```kotlin
TheRouter.build("http://therouter.com/detail")
    .withString("id", "12345")
    .navigation()
```

### 9.2 完整业务场景

#### 场景1: 电商首页跳转商品详情

```kotlin
// 商品详情页
@Route(
    path = "http://mall.com/product/detail",
    description = "商品详情页"
)
class ProductDetailActivity : BaseActivity() {
    @Autowired(name = "product_id")
    var productId: String? = null
    
    @Autowired(defaultValue = "false")
    var fromShare: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadProduct(productId)
    }
}

// 首页跳转
recyclerView.setOnItemClickListener { position ->
    val product = productList[position]
    TheRouter.build("http://mall.com/product/detail")
        .withString("product_id", product.id)
        .withBoolean("fromShare", false)
        .withInAnimation(R.anim.slide_in_right)
        .withOutAnimation(R.anim.slide_out_left)
        .navigation()
}
```

#### 场景2: 登录拦截

```kotlin
// 路由替换器实现登录拦截
addRouterReplaceInterceptor(object : RouterReplaceInterceptor {
    override fun watch(route: RouteItem?): Boolean {
        // 需要登录的页面
        return route?.path?.matches(Regex(".*/(wallet|order|cart).*")) == true
    }
    
    override fun replace(route: RouteItem?): RouteItem? {
        return if (!UserManager.isLogin() && route != null) {
            // 替换到登录页，并携带原路径
            RouteItem(
                path = "/user/login",
                className = LoginActivity::class.java.name,
                params = mapOf("redirect" to route.path)
            )
        } else route
    }
})

// 登录成功后继续原跳转
class LoginActivity : BaseActivity() {
    @Autowired
    var redirect: String? = null
    
    fun onLoginSuccess() {
        if (!redirect.isNullOrEmpty()) {
            TheRouter.build(redirect).navigation()
        }
        finish()
    }
}
```

#### 场景3: 跨模块用户服务

```kotlin
// business-base: 定义接口
interface IUserService {
    fun getUserId(): String
    fun getUserName(): String
    fun isLogin(): Boolean
    fun logout()
}

// business-user: 提供实现
@ServiceProvider
fun provideUserService(context: Context): IUserService {
    return UserServiceImpl(context)
}

class UserServiceImpl(private val context: Context) : IUserService {
    override fun getUserId() = SharedPreferencesUtils.getUserId(context)
    override fun getUserName() = SharedPreferencesUtils.getUserName(context)
    override fun isLogin() = getUserId().isNotEmpty()
    override fun logout() {
        SharedPreferencesUtils.clearUser(context)
        TheRouter.build("action://logout_success").action(context)
    }
}

// business-order: 使用服务
class OrderFragment : Fragment() {
    private val userService = TheRouter.get(IUserService::class)
    
    fun showOrderInfo() {
        if (userService?.isLogin() == true) {
            tvUserName.text = userService.getUserName()
        } else {
            TheRouter.build("/user/login").navigation(requireContext())
        }
    }
}
```

#### 场景4: 模块初始化依赖

```kotlin
// business-base: 基础初始化
@FlowTask(taskName = BaseFlowTask.INIT_LOG)
class LogInitTask : FlowTaskExecutor {
    override fun init(context: Context) {
        // 初始化日志框架
        Logger.init(BuildConfig.DEBUG)
    }
}

// business-network: 网络模块 (依赖日志)
@FlowTask(
    taskName = NetworkFlowTask.INIT_NETWORK,
    dependsOn = BaseFlowTask.INIT_LOG
)
class NetworkInitTask : FlowTaskExecutor {
    override fun init(context: Context) {
        // 初始化网络库
        RetrofitClient.init(context)
        Logger.d("Network", "网络初始化完成")
    }
}

// business-user: 用户模块 (依赖网络)
@FlowTask(
    taskName = UserFlowTask.INIT_USER,
    dependsOn = NetworkFlowTask.INIT_NETWORK
)
class UserInitTask : FlowTaskExecutor {
    override fun init(context: Context) {
        // 恢复用户会话
        UserManager.restoreSession()
    }
}
```

#### 场景5: 动态化配置

```kotlin
// 服务端下发配置
{
  "actions": [
    {
      "actionUrl": "action://spring_festival_red_packet",
      "trigger": "home_banner_click"
    }
  ]
}

// 客户端注册
RemoteConfigManager.onConfigUpdate { config ->
    config.actions.forEach { action ->
        TheRouter.addActionInterceptor(action.actionUrl) { ctx, _ ->
            when (action.actionUrl) {
                "action://spring_festival_red_packet" -> {
                    RedPacketDialog.show(ctx)
                    true
                }
                else -> false
            }
        }
    }
}

// 触发
findViewById<View>(R.id.homeBanner).setOnClickListener {
    TheRouter.build("action://spring_festival_red_packet").action()
}
```

### 9.3 Compose 集成

**添加依赖**:
```kotlin
dependencies {
    implementation("cn.therouter:compose:1.3.3-rc1")
}
```

**Compose 页面路由**:
```kotlin
@Composable
@Route(path = "https://kymjs.com/shell_screen/home")
fun HomePage(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("首页") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Button(onClick = {
                TheRouter.build("https://kymjs.com/shell_screen/detail")
                    .withString("id", "123")
                    .navigation()
            }) {
                Text("跳转到详情")
            }
        }
    }
}
```

**NavHost 集成**:
```kotlin
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "https://kymjs.com/shell_screen/home") {
        // TheRouter 会自动注册路由
        composable("https://kymjs.com/shell_screen/home") {
            HomePage(navController)
        }
    }
}
```

---

## 10. 高级特性

### 10.1 路由表预加载

**自定义路由表加载**:
```kotlin
RouteMapKt.setRouteMapInitTask(object : RouterMapInitTask {
    override fun asyncInitRouteMap() {
        // 从服务器下载最新路由表
        val remoteRouteMap = downloadRouteMap()
        // 合并本地和远程路由
        mergeRouteMaps(remoteRouteMap)
    }
})
```

**路由表格式**:
```json
[
  {
    "path": "http://therouter.com/home",
    "className": "com.therouter.app.HomeActivity",
    "action": "",
    "description": "首页",
    "params": {}
  }
]
```

### 10.2 历史记录

TheRouter 自动记录路由跳转历史：

```kotlin
// 获取历史记录
val history = TheRouter.getHistory()
history.forEach {
    println("${it.timestamp}: ${it.path}")
}

// 清除历史
TheRouter.clearHistory()

// 回退
TheRouter.back()
```

**历史类型**:
- `ActivityNavigatorHistory`: Activity 跳转历史
- `FragmentNavigatorHistory`: Fragment 创建历史
- `ActionNavigatorHistory`: Action 执行历史

### 10.3 自定义参数解析器

**实现自定义类型解析**:
```kotlin
class DateParser : AutowiredParser<Date> {
    override fun parse(value: String?): Date? {
        return value?.let { SimpleDateFormat("yyyy-MM-dd").parse(it) }
    }
}

// 注册
addAutowiredParser(Date::class.java, DateParser())

// 使用
@Autowired
var createDate: Date? = null
```

### 10.4 路由分组

**按模块分组路由**:
```kotlin
@RouteGroup("user")
object UserRoutes {
    const val LOGIN = "http://user.com/login"
    const val PROFILE = "http://user.com/profile"
    const val SETTINGS = "http://user.com/settings"
}

@RouteGroup("product")
object ProductRoutes {
    const val LIST = "http://product.com/list"
    const val DETAIL = "http://product.com/detail"
    const val CART = "http://product.com/cart"
}

// 使用
TheRouter.build(UserRoutes.LOGIN).navigation()
```

### 10.5 路由动画

**自定义转场动画**:
```kotlin
// res/anim/activity_slide_in.xml
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate
        android:fromXDelta="100%"
        android:toXDelta="0%"
        android:duration="300"/>
</set>

// res/anim/activity_slide_out.xml
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate
        android:fromXDelta="0%"
        android:toXDelta="-100%"
        android:duration="300"/>
</set>

// 使用
TheRouter.build("http://therouter.com/page")
    .withInAnimation(R.anim.activity_slide_in)
    .withOutAnimation(R.anim.activity_slide_out)
    .navigation()
```

### 10.6 路由元数据

**获取路由元数据**:
```kotlin
val navigator = TheRouter.build("http://therouter.com/home")
println("原始URL: ${navigator.originalUrl}")
println("简单URL: ${navigator.simpleUrl}")
println("完整URL: ${navigator.getUrlWithParams()}")
println("参数: ${navigator.extras}")
println("键值对: ${navigator.kvPair}")
```

### 10.7 性能优化

#### 10.7.1 增量编译

```kotlin
// build.gradle.kts
TheRouter {
    debug = false
    forceIncremental = true
    incrementalCachePath = "build/therouter-cache"
}
```

#### 10.7.2 路由表优化

- 避免重复路由定义
- 使用短路径减少内存占用
- 定期清理无用路由

#### 10.7.3 懒加载服务

```kotlin
// 仅在首次使用时创建
@ServiceProvider
fun provideExpensiveService(): ExpensiveService {
    return ExpensiveService() // 耗时操作
}

// 使用
val service = TheRouter.get(ExpensiveService::class) // 首次调用时才创建
```

### 10.8 调试技巧

#### 10.8.1 开启调试日志

```kotlin
TheRouter.setDebug(true)
```

**输出示例**:
```
D/TheRouter: Navigator::navigation begin navigate http://therouter.com/home
D/TheRouter: Navigator::navigation match route RouteItem{path=..., className=...}
D/TheRouter: Navigator::navigation startActivity HomeActivity
```

#### 10.8.2 路由表检查

```kotlin
// 检查路由是否存在
val route = matchRouteMap("http://therouter.com/home")
if (route == null) {
    Log.e("TheRouter", "路由不存在!")
}

// 打印所有路由
RouteMapKt.getAllRoutes().forEach {
    Log.d("TheRouter", "${it.path} -> ${it.className}")
}
```

#### 10.8.3 依赖关系可视化

```kotlin
// 打印 FlowTask 依赖图
FlowTaskExecutor.printDependencyGraph()
```

### 10.9 常见问题

#### Q1: 路由跳转无响应

**原因**:
1. 路由表未初始化完成
2. Path 拼写错误
3. 缺少 `TheRouter.inject(this)`

**解决**:
```kotlin
// 确保在 BaseActivity 中调用
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    TheRouter.inject(this) // 必须
}

// 使用 pending 机制
TheRouter.build(path).pending().navigation()
```

#### Q2: Autowired 注入为 null

**原因**:
1. 参数名不匹配
2. 未在 `onCreate` 中调用 `inject`
3. 类型不匹配

**解决**:
```kotlin
@Autowired(name = "user_id") // 明确指定参数名
var userId: String? = null

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    TheRouter.inject(this)
    Log.d("Test", "userId = $userId") // 此时已注入
}
```

#### Q3: 跨模块服务获取为 null

**原因**:
1. 服务提供方未正确注册
2. 服务实现类找不到

**解决**:
```kotlin
// 检查服务是否注册
val service = TheRouter.get(IService::class)
if (service == null) {
    Log.e("TheRouter", "服务未注册，检查 @ServiceProvider 注解")
}

// 确保服务提供方模块被依赖
// app/build.gradle
api(project(":business-module"))
```

### 10.10 最佳实践

#### 10.10.1 Path 命名规范

```kotlin
// 推荐: 使用 HTTPS + 域名 + 路径
@Route(path = "https://mall.com/product/detail")

// 推荐: 模块前缀清晰
const val USER_LOGIN = "https://user.com/login"
const val ORDER_DETAIL = "https://order.com/detail"

// 避免: 相对路径
@Route(path = "/detail") // 不推荐

// 避免: 硬编码字符串
TheRouter.build("https://mall.com/product/detail") // 不推荐

// 推荐: 常量管理
object ProductRoutes {
    const val DETAIL = "https://mall.com/product/detail"
}
TheRouter.build(ProductRoutes.DETAIL)
```

#### 10.10.2 服务接口设计

```kotlin
// 推荐: 接口独立
interface IUserService {
    fun getUserId(): String
}

interface IOrderService {
    fun createOrder(productId: String)
}

// 避免: 接口臃肿
interface IBigService {
    fun getUserId(): String
    fun createOrder(productId: String)
    fun getProducts(): List<Product>
    // ... 太多方法
}

// 推荐: 单一职责
@ServiceProvider
fun provideUserService(context: Context): IUserService {
    return UserServiceImpl(context)
}
```

#### 10.10.3 模块化边界

```
app (主应用)
  ├── business-home (首页模块)
  ├── business-user (用户模块)
  ├── business-order (订单模块)
  └── business-base (基础模块)
        ├── 定义所有接口
        ├── 提供 BaseActivity
        └── 共享工具类
```

**依赖方向**:
```
business-home → business-base
business-user → business-base
business-order → business-base
app → 所有 business-* 模块
```

**禁止**:
```
❌ business-home → business-user (横向依赖)
✅ 通过接口 + ServiceProvider 通信
```

---

## 附录

### A. 版本历史

| 版本 | 发布日期 | 主要更新 |
|------|---------|---------|
| 1.3.2 | 2024 | 稳定版，支持 AGP 9 |
| 1.3.3-rc1 | 2024 | Compose 支持 |
| 1.3.0 | 2023 | KSP 支持 |
| 1.2.x | 2022 | 初始版本 |

### B. 相关资源

- **官方网站**: https://therouter.cn
- **GitHub**: https://github.com/HuolalaTech/hll-wp-therouter-android
- **Wiki**: https://github.com/HuolalaTech/hll-wp-therouter-android/wiki
- **HarmonyOS 版本**: https://github.com/HuolalaTech/hll-wp-therouter-harmony
- **iOS 版本**: https://github.com/HuolalaTech/hll-wp-therouter-ios

### C. 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### D. 许可证

TheRouter 采用 Apache License 2.0 许可证。详见 [LICENSE](LICENSE) 文件。

### E. 致谢

感谢货拉拉移动技术团队的开发与维护！

---

**文档版本**: 1.0  
**最后更新**: 2026-07-03  
**基于版本**: TheRouter 1.3.2
