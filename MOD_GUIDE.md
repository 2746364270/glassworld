# 玻璃剑模组 · 全部改动清单（可当作写 mod 的参考）

> 工程：`D:\minecraft_mods_development\glassworld`（基于 NeoForge MDK 26.2 / NeoForge 26.2.0.90 / Gradle 9.2.1 / Java 25）
> 目标：一把玻璃剑——耐久 3、攻击伤害 +50、木棍 + 玻璃合成、配方排布与其它剑一致。

---

## 0. 一句话总览

整个 mod 只做了 4 件事：

1. 把模板的 `examplemod` 身份改成 `glassworld`（mod id / 包名 / 显示名）。
2. 定义一个**工具材质**（`ToolMaterial`）：耐久 3、伤害加成 50。
3. 用 `DeferredRegister.Items.registerItem(...)` 注册一个**普通 `Item`**，属性用 `properties.sword(材质, 伤害基准, 攻速基准)` 组装。
4. 补齐 4 类资源文件：**物品模型定义 / 模型 / 贴图 / 语言**，再加 1 个 **配方 JSON**。

---

## 1. 改动总表

### 1.1 glassworld 工程（mod 本体）

| # | 文件 | 类型 | 作用 |
|---|---|---|---|
| 1 | `gradle.properties` | 修改 | mod_id / mod_name / mod_group_id 改为 glassworld |
| 2 | `src/main/java/com/example/glassworld/GlassWorld.java` | **新增** | 主类：材质、物品注册、创造页、配置、事件 |
| 3 | `src/main/java/com/example/glassworld/GlassWorldClient.java` | **新增** | 客户端专用入口（配置界面） |
| 4 | `src/main/java/com/example/glassworld/Config.java` | **新增**（内容来自模板） | 示例配置 |
| 5 | `src/main/java/com/example/examplemod/`（3 个 .java） | **删除** | 旧包，被 2/3/4 取代 |
| 6 | `src/main/resources/assets/examplemod/` | **删除** | 旧命名空间资源 |
| 7 | `src/main/resources/assets/glassworld/items/glass_sword.json` | **新增** | 物品模型定义（26.x 新格式） |
| 8 | `src/main/resources/assets/glassworld/models/item/glass_sword.json` | **新增** | 物品模型（handheld） |
| 9 | `src/main/resources/assets/glassworld/textures/item/glass_sword.png` | **新增** | 16×16 贴图 |
| 10 | `src/main/resources/assets/glassworld/lang/en_us.json` | **新增** | 英文名 |
| 11 | `src/main/resources/assets/glassworld/lang/zh_cn.json` | **新增** | 中文名 |
| 12 | `src/main/resources/data/glassworld/recipe/glass_sword.json` | **新增** | 合成配方 |
| 13 | `src/main/resources/META-INF/neoforge.mods.toml` | 修改 | 描述文本、注释里的 logo 文件名 |
| 14 | `build/libs/examplemod-1.0.0.jar` | **删除** | 改名前的旧构建产物 |

### 1.2 MDK 模板工程（`MDK-26.2-NeoGradle-main`）

| # | 文件 | 类型 | 作用 |
|---|---|---|---|
| 15 | `gradle.properties` | 修改 | `org.gradle.configuration-cache=true` → `false`（IDEA 委托运行与配置缓存冲突，见 §6.1） |

### 1.3 机器环境（不在工程里，但构建必需）

| # | 路径 | 类型 | 作用 |
|---|---|---|---|
| 16 | `C:\Users\27463\.gradle\gradle.properties` | **新增** | 给 Gradle 守护进程配 HTTP/HTTPS 代理（`127.0.0.1:17890`），否则 `maven.neoforged.net` 拉不到插件 |
| 17 | `D:\minecraft_mods_development\glass_sword_preview.png` | 新增（可删） | 只是贴图的 8 倍放大预览图，不参与构建 |
| 18 | `glassworld/run/server/` | 新增后已删除 | 我做运行时验证时起的临时开发服务器（含 eula.txt、world） |

> 说明：`.idea/`、`build.gradle`、`settings.gradle`、`gradle/wrapper/*`、`gradlew*`、`src/.../README.md` 全部**没有改动**。

---

## 2. 逐文件详解

### 2.1 `gradle.properties`（mod 身份）

```properties
mod_id=glassworld              # 改：examplemod → glassworld
mod_name=Glass World           # 改：Example Mod → Glass World
mod_group_id=com.example.glassworld   # 改：com.example.examplemod
mod_version=1.0.0              # 不变
```

要点：

- `mod_id` 是**全局命名空间**：决定物品 ID（`glassworld:glass_sword`）、资源目录名（`assets/glassworld/`、`data/glassworld/`）、jar 名（`glassworld-1.0.0.jar`）。
- `mod_id` 必须与 `@Mod(GlassWorld.MODID)` 里的字符串**完全一致**，否则启动直接崩。
- `mod_group_id` 只是 Maven group / Java 包名前缀，和 mod id 没有强绑定，但建议一致。
- 这几个值会被 `build.gradle` 里的 `processResources` 注入到 `neoforge.mods.toml` 的 `${mod_id}` 等占位符里——所以 toml 里不用手写。

### 2.2 `GlassWorld.java`（主类，最重要的一个文件）

完整代码：

```java
package com.example.glassworld;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GlassWorld.MODID)
public class GlassWorld {
    public static final String MODID = "glassworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 物品注册器：命名空间 = MODID
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /**
     * 工具材质。record 字段顺序：
     * (不能正确挖掘的方块, 耐久, 挖掘速度, 攻击伤害加成, 附魔能力, 修复材料标签)
     */
    public static final ToolMaterial GLASS_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, // 剑不挖方块，沿用木头即可
            3,                                   // 耐久 3
            2.0F,                                // 挖掘速度（剑不用）
            50.0F,                               // 攻击伤害加成 +50
            15,                                  // 附魔能力
            ItemTags.WOODEN_TOOL_MATERIALS       // 默认修复标签（下面会被玻璃覆盖）
    );

    public static final DeferredItem<Item> GLASS_SWORD = ITEMS.registerItem("glass_sword",
            properties -> new Item(properties
                    .sword(GLASS_TOOL_MATERIAL, 0.0F, -2.4F) // 伤害 = 基准 0 + 加成 50；攻速基准 -2.4（同原版剑）
                    .repairable(Items.GLASS)));              // 修复材料改成玻璃

    public GlassWorld(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);                        // 把注册器挂到 mod 总线

        modEventBus.addListener(this::addCreative);         // 加入创造模式物品栏
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);                  // 需要收游戏事件时才需要这行
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {  // 放进「战斗」页，和其它剑一起
            event.accept(GLASS_SWORD);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Glass World loaded");
    }
}
```

**逐段讲解：**

1. `@Mod(GlassWorld.MODID)` + 构造参数 `(IEventBus, ModContainer)` —— FML 会自动注入这两个对象，不需要自己 new。
2. `DeferredRegister.createItems(MODID)` —— 26.x 里物品用「延迟注册器」，注册动作统一在 `ITEMS.register(modEventBus)` 时发生，这样做的好处是**注册顺序安全**、可以在静态字段里互相引用。
3. `ToolMaterial` 是 26.x 的材质模型（旧版的 `Tier` + `SwordItem` 已经没了），它是一份纯数据：耐久 / 挖掘速度 / 伤害加成 / 附魔能力 / 修复标签。
4. `properties.sword(material, 伤害基准, 攻速基准)` 一次调用做了 4 件事（源码 `ToolMaterial.applySwordProperties`）：
   - `durability(material.durability)` → 耐久 = 3，同时最大堆叠 = 1
   - `repairable(material.repairItems)` + `enchantable(material.enchantmentValue)`
   - 写入 `TOOL` 组件（对蜘蛛网/剑可秒挖方块生效）和 `WEAPON` 组件（横扫、剑类判定）
   - 写入属性修饰符：`ATTACK_DAMAGE = 伤害基准 + 材质加成`，`ATTACK_SPEED = 攻速基准`
5. 伤害为什么是 `0.0F` 基准：属性值 = 基准 + 加成 = 0 + 50 = **+50 修饰符**；游戏内 tooltip 会把玩家基础 1 点算进去，所以显示 **51**。想让面板正好 50，把 `50.0F` 改成 `49.0F`。
6. `.repairable(Items.GLASS)` 写在 `.sword(...)` **之后**才会覆盖材质自带的修复标签（后写覆盖先写）。
7. `BuildCreativeModeTabContentsEvent` 是 NeoForge 的事件，用来往**原版**创造页里塞物品；想自建创造页才需要 `CreativeModeTab.builder()`（模板里原有，这次删掉了）。

### 2.3 `GlassWorldClient.java`（客户端入口）

```java
@Mod(value = GlassWorld.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GlassWorld.MODID, value = Dist.CLIENT)
public class GlassWorldClient {
    public GlassWorldClient(ModContainer container) {
        // 让 NeoForge 用通用配置界面渲染本 mod 的配置（Mods 界面 → 你的 mod → Config）
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        GlassWorld.LOGGER.info("HELLO FROM CLIENT SETUP");
        GlassWorld.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
```

要点：`dist = Dist.CLIENT` 保证这个类只在客户端加载，服务器上引用客户端类会崩；物品/配方这类**双端通用**的东西不要写在这里。

### 2.4 `Config.java`

模板自带的示例配置，我只改了包名。里面演示了 `BooleanValue / IntValue / ConfigValue<String> / 列表 + 校验器` 的写法，可直接当模板用。它通过 `modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)` 生效。

### 2.5 资源文件（4 个）

**(a) 物品模型定义** `assets/glassworld/items/glass_sword.json` —— 26.x 的「item model definition」层（1.21.4 起新增，和下面的 models 是两回事）：

```json
{
  "model": { "type": "minecraft:model", "model": "glassworld:item/glass_sword" }
}
```

**(b) 物品模型** `assets/glassworld/models/item/glass_sword.json` —— `handheld` 父模型让物品斜着拿、有挥动动画（和所有剑一致）：

```json
{
  "parent": "minecraft:item/handheld",
  "textures": { "layer0": "glassworld:item/glass_sword" }
}
```

> 记住这个对应关系：`items/<id>.json` → 指向 → `models/item/<id>.json` → 指向 → `textures/item/<id>.png`。少一个就显示紫黑格。

**(c) 贴图** `assets/glassworld/textures/item/glass_sword.png` —— 16×16 RGBA。做法：取原版 `diamond_sword.png` 的轮廓，按亮度分 4 档重新上色（高光近白、主体淡青、暗部深蓝绿），并把内部像素设为半透明（alpha 172~245），得到玻璃质感。

**(d) 语言** `assets/glassworld/lang/en_us.json` / `zh_cn.json`：

```json
{
  "item.glassworld.glass_sword": "Glass Sword",   // zh_cn: "玻璃剑"
  "glassworld.configuration.title": "Glass World Configs"
}
```

要点：`item.<命名空间>.<路径>` 是固定格式，写错就是显示 `item.glassworld.glass_sword` 原文。

### 2.6 配方 `data/glassworld/recipe/glass_sword.json`

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "#": "minecraft:stick",
    "X": "minecraft:glass"
  },
  "pattern": ["X", "X", "#"],
  "result": { "id": "glassworld:glass_sword" }
}
```

要点（都是 26.x 的写法，和 1.20 之前不同）：

- 目录是 **`recipe`**（单数，1.21 起从 `recipes` 改名）。
- `key` 的值直接写物品 ID 字符串，不再需要 `{"item": "..."}`；写 `#minecraft:xxx` 表示标签。
- `result` 用 `{"id": ..., "count": n}`，`count` 省略就是 1。
- `pattern` 是"剑"的经典排布：材料在上两格、木棍最下，与所有原版剑一致。
- 配方在**数据包**里（`data/`），所以要放在资源包的 `data` 目录而不是 `assets`。

### 2.7 `neoforge.mods.toml`

只改了描述文本和注释里的 logo 文件名：

```toml
description='''
Glass World - a mod about glass themed gear.

Currently added:
- Glass Sword: a sword made from glass. Durability 3, attack damage +50, crafted from glass and a stick.
'''
```

其余字段（`modId` / `version` / `displayName` / `license` / 依赖）全用 `${...}` 占位符，由 `gradle.properties` 注入，**不要手改**。

---

## 3. 删除了什么

| 删除项 | 原因 |
|---|---|
| `src/main/java/com/example/examplemod/{ExampleMod,ExampleModClient,Config}.java` | 包名改名，内容迁移到 `com.example.glassworld` |
| `assets/examplemod/lang/en_us.json` | 命名空间改名 |
| `GlassWorld.java` 里的 `EXAMPLE_BLOCK` / `EXAMPLE_BLOCK_ITEM` / `EXAMPLE_ITEM` / `EXAMPLE_TAB` | 模板示例内容，和玻璃剑无关；删掉后物品直接进原版「战斗」页 |
| `build/libs/examplemod-1.0.0.jar` | 改名前的旧产物，留着会混淆 |
| `run/server/`（含 eula.txt、world） | 我做验证时生成的临时服务器目录 |

---

## 4. 26.2 写 mod 的 API 要点（这次踩到的）

1. **没有 `SwordItem`/`PickaxeItem` 这些类了**：物品就是 `Item`，工具/剑行为通过 `Item.Properties` 的 `sword()/pickaxe()/axe()/shovel()/hoe()/spear()` 配置。
2. **材质是 `ToolMaterial`（record）**，不是老的 `Tier`；原版实例：`ToolMaterial.WOOD/STONE/COPPER/IRON/GOLD/DIAMOND/NETHERITE`。自定义剑直接 `new ToolMaterial(...)`。
3. **`Identifier` 取代了 `ResourceLocation`**（`net.minecraft.resources.Identifier`）。
4. **物品模型是两层**：`assets/<ns>/items/<id>.json`（模型定义）+ `assets/<ns>/models/item/<id>.json`（几何/贴图）。
5. **配方目录单数 `recipe`**，`key` 直接写字符串，`result` 用 `{id, count}`。
6. `DeferredRegister.Items` 提供 `registerItem(name, properties -> item)` / `registerSimpleItem(...)` / `register(...)` 三种写法；自定义 `Item` 子类用 `registerItem` 最省事。
7. 创造页入口两种：**塞原版页**用 `BuildCreativeModeTabContentsEvent`（本次用法）；**自建页**用 `CreativeModeTab.builder()` + `DeferredRegister<CreativeModeTab>`。

---

## 5. 我是怎么验证的（你也可以照做）

```powershell
cd D:\minecraft_mods_development\glassworld
.\gradlew.bat build        # 编译 + 打包，产物 build\libs\glassworld-1.0.0.jar
```

- 想进游戏看效果：IDEA 里运行 `glassworld: Client`，或 `.\gradlew.bat runClient`。
- 想不开游戏验证**注册 + 配方**：`run\server\eula.txt` 写 `eula=true`，然后 `.\gradlew.bat runServer`，日志里应该出现：
  - `Glass World 1.0.0 (glassworld)`（mod 被识别）
  - `Loaded 1586 recipes`（原版 1585 + 我们的 1；数字不变说明配方没被解析）
  - `[com.example.glassworld.GlassWorld] Glass World loaded`
- 检查 jar 内容（确认资源都打进去了）：

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead('build\libs\glassworld-1.0.0.jar')
$z.Entries | Select-Object FullName
```

---

## 6. 环境侧的两处改动（重要，不然跑不起来）

### 6.1 关掉 Gradle 的 configuration cache

`glassworld/gradle.properties`（和 MDK 模板工程）里 `org.gradle.configuration-cache=false`。

原因：IDEA 用「Build and run using: Gradle」模式启动游戏时，会动态生成一个合成任务并注入初始化脚本，里面注册了 `Gradle.addListener`、并在执行期访问 `Task.project`；这两件事配置缓存都不允许，于是运行配置直接失败：

```
Invocation of 'Task.project' by task ':net.neoforged.fml.startup.Client.main()' at execution time
is unsupported with the configuration cache
```

只想保留配置缓存的话，把 IDEA 的 `Settings → Build Tools → Gradle → Build and run using` 改成 `IntelliJ IDEA`，再把这一行改回 `true`。

### 6.2 Gradle 走代理

`C:\Users\27463\.gradle\gradle.properties`：

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=17890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=17890
systemProp.http.nonProxyHosts=localhost|127.0.0.1|[::1]
```

原因：本机直连 `maven.neoforged.net` 被阻断（DNS 被污染），而 Gradle 守护进程不读 Windows 系统代理/IDEA 代理，只有 `systemProp.*` 生效。**端口跟你的代理客户端走，换了端口要同步改；关掉代理时构建会失败。**

---

## 7. 再加一个物品的最小步骤（复制模板）

以「玻璃镐」为例，共 6 步：

1. `GlassWorld.java`：复用 `GLASS_TOOL_MATERIAL`，注册
   ```java
   public static final DeferredItem<Item> GLASS_PICKAXE = ITEMS.registerItem("glass_pickaxe",
           properties -> new Item(properties.pickaxe(GLASS_TOOL_MATERIAL, 1.0F, -2.8F)));
   ```
2. `assets/glassworld/items/glass_pickaxe.json`：`{"model": {"type": "minecraft:model", "model": "glassworld:item/glass_pickaxe"}}`
3. `assets/glassworld/models/item/glass_pickaxe.json`：`{"parent": "minecraft:item/handheld", "textures": {"layer0": "glassworld:item/glass_pickaxe"}}`
4. `assets/glassworld/textures/item/glass_pickaxe.png`：16×16 贴图
5. `data/glassworld/recipe/glass_pickaxe.json`：三格材料 + 两格木棍的镐子排布
6. `lang/en_us.json` + `zh_cn.json` 各加一行 `"item.glassworld.glass_pickaxe": "..."`
7. （要进创造页）`addCreative` 里再 `event.accept(GLASS_PICKAXE)`；或者挂在别的原版页。

改完 `.\gradlew.bat build` 一把过，`runClient` 里 `/give @s glassworld:glass_pickaxe` 验证。
