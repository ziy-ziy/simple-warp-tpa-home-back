<details>
<summary>中文</summary>

# Simple Warp TPA Home Back

一个轻量级 **Fabric** 模组，提供 **TPA**、**Home**、**Warp**、**Back** 四大功能。
没有弹窗，无需前置，完整翻译！装上就能用！


---

## 功能总览

| 分类 | 命令 | 权限 | 说明 |
|------|------|------|------|
| **TPA** | `/tpa <玩家>` | 所有人 | 向目标玩家发送传送请求 |
| | `/tpay` | 所有人 | 接受传送请求 |
| | `/tpan` | 所有人 | 拒绝传送请求 |
| **Home** | `/sethome <名称>` | 所有人 | 在当前位置设置家（支持中文名） |
| | `/home <名称>` | 所有人 | 传送到指定的家 |
| | `/homes` | 所有人 | 列出所有家（可点击传送） |
| | `/delhome <名称>` | 所有人 | 删除一个家 |
| **Warp** | `/warp <名称>` | 所有人 | 传送到指定传送点 |
| | `/warps` | 所有人 | 列出所有传送点（可点击传送） |
| | `/setwarp <名称>` | **OP 专用** | 设置全局传送点（支持中文名） |
| | `/delwarp <名称>` | **OP 专用** | 删除传送点 |
| **Back** | `/back` | 所有人 | 返回到传送前的位置，如没有则传送到上一次死亡地点 |
| | `/swthbconfig backEnabled true` | **OP 专用** | 开启Back功能 |
| | `/swthbconfig backEnabled false` | **OP 专用** | 关闭Back功能（默认） |
| **配置** | `/swthbconfig maxHomes <数量>` | **OP 专用** | 修改每名玩家最大家的数量（默认5个） |
| | `/swthbconfig maxWarps <数量>` | **OP 专用** | 修改 Warp 最大数量（默认5个） |
| | `/swthbconfig reload` | **OP 专用** | 从磁盘重新加载配置 |
| | `/swthbconfig save` | **OP 专用** | 手动保存配置到磁盘 |
| | `/swthbconfig teleportDelay <秒>` | **OP 专用** | 修改传送倒计时（默认0秒） |

---

## 功能详情

### 传送倒计时
- 默认关闭

### TPA 传送请求

玩家 A 执行 `/tpa 玩家B` 后：
- 玩家 A 收到："**[玩家B] 的传送请求已发送，等待回应...**"
- 玩家 B 收到："**[玩家A] 请求传送到你的位置 [接受] [拒绝]**"
- **接受 / 拒绝** 按钮可在聊天栏直接点击执行
- 请求超时时间：**60 秒**
- 支持跨维度传送

### Home 家系统

- 每位玩家默认最多 **5 个家**（可通过 `/swthbconfig` 修改）
- 家名称支持中文（如 `/sethome 工业区`）
- 家名相同自动更新坐标（不增加数量）

### Warp 全局传送点

- Warp 是管理员设置的全局公共传送点，所有玩家可用
- `/warps` 列出所有公共传送点，每条可点击传送

### Back 返回

- 使用 `/back` 返回到传送前的位置，如没有则传送到上一次死亡地点
- 功能默认关闭

---

## 配置文件

所有数据保存在 `saves/<存档>/data/simple-warp-tpa-home-back/data.json`，

```json
{
  "maxHomes": 5,
  "maxWarps": 5,
  "teleportDelay": 3,
  "backEnabled": false,
  "homes": {
    "玩家UUID": [
      { "name": "home1", "world": "minecraft:overworld", "x": 100, "y": 64, "z": 200, "yaw": 0, "pitch": 0 }
    ]
  },
  "warps": {
    "spawn": { "world": "minecraft:overworld", "x": 0, "y": 70, "z": 0, "yaw": 0, "pitch": 0 }
  }
}
```

- **修改自动保存**：每次 使用`sethome``delhome``setwarp``delwarp``swthbconfig`自动写入文件
- **服务器关闭自动保存**
- **服务器启动自动加载**

---

## 多语言支持

内置简体中文、繁體中文、英文、西班牙文、印地語、阿拉伯語、法文、日文、俄文语言文件。

- 客户装有 mod → 根据客户端语言显示翻译
- 客户端未装 mod → 显示中文

欢迎提交添加更多语言。


</details>


# Simple Warp TPA Home Back
- A lightweight Fabric mod that provides four core functions: TPA, Home, Warp and Back.
- No pop-up windows, no required dependencies, fully localized translations! Install and play instantly!

### Feature Overview
| Category | Command | Permission | Description |
|------|------|------|------|
| **TPA** | `/tpa <player>` | All Players | Send a teleport request to the target player |
| | `/tpay` | All Players | Accept an incoming teleport request |
| | `/tpan` | All Players | Decline an incoming teleport request |
| **Home** | `/sethome <name>` | All Players | 	Set a home at your current position |
| | `/home <name>` | All Players | Teleport to the specified home |
| | `/homes` | All Players | List all your homes (clickable for instant teleport) |
| | `/delhome <name>` | All Players | 	Delete a specific home |
| **Warp** | `/warp <name>` | All Players | 	Teleport to the specified public warp |
| | `/warps` | All Players | List all public warps (clickable for instant teleport) |
| | `/setwarp <name>` | **OP Only** | Create a global public warp point  |
| | `/delwarp <name>` | **OP Only** | Delete a public warp point |
| **Back** | `/back` | All Players | Return to your position before teleportation; if none exists, teleport to your last death location |
| | `/swthbconfig backEnabled true` | **OP Only** | Enable the Back feature |
| | `/swthbconfig backEnabled false` | **OP Only** | Disable the Back feature (disabled by default) |
| **Config** | `/swthbconfig maxHomes <amount>` | **OP Only** | Adjust the maximum number of homes per player (default: 5) |
| | `/swthbconfig maxWarps <amount>` | **OP Only** | Adjust the global maximum number of warps (default: 5) |
| | `/swthbconfig reload` | **OP Only** | Reload configuration data from disk |
| | `/swthbconfig save` | **OP Only** | Manually save all configuration to disk |
| | `/swthbconfig teleportDelay <seconds>` | **OP Only** | Change the teleport countdown (default 0 seconds) |

## Feature Details
### Teleport Countdown
- Off by default
### TPA Teleport Request System
#### When Player A runs /tpa PlayerB:
- Player A receives message: "Teleport request sent to [PlayerB], waiting for response..."
- Player B receives message: "[PlayerA] requests to teleport to your location [Accept] [Deny]"
- The [Accept] and [Deny] buttons are clickable directly in chat
- Request timeout duration: 60 seconds
- Cross-dimension teleportation supported
## Player Home System
- Each player has a default limit of 5 homes (adjustable via /swthbconfig)
- Home names support Chinese characters (e.g. /sethome Industrial Zone)
- Re-setting a home with an identical name overwrites coordinates without consuming an extra home slot
## Public Warp Points
- Warps are global public teleport locations created exclusively by server OPs, accessible to all players
- Run /warps to view all public warps; each entry is clickable for one-click teleport
### Back Command
- Run `/back` to return to your position before teleporting; if no prior teleport position exists, you will be sent to your last death location.
- This feature is disabled by default.
---
## Configuration File
All data is stored uniformly in saves/save/data/simple-warp-tpa-home-back/data.json:

```
{
  "maxHomes": 5,
  "maxWarps": 5,
  "teleportDelay": 3,
  "backEnabled": false,
  "homes": {
    "PlayerUUID": [
      { "name": "home1", "world": "minecraft:overworld", "x": 100, "y": 64, "z": 200, "yaw": 0, "pitch": 0 }
    ]
  },
  "warps": {
    "spawn": { "world": "minecraft:overworld", "x": 0, "y": 70, "z": 0, "yaw": 0, "pitch": 0 }
  }
}
```

- Auto-save on edits: Changes made via /sethome, /delhome, /setwarp, /delwarp and /swthbconfig are automatically written to the file.
- Auto-save on server shutdown
- Auto-load all data on server startup

# Multilingual Support
#### Built-in language files: Simplified Chinese, Traditional Chinese, English, Spanish, Hindi, Arabic, French, Japanese and Russian.
- If the client has this mod installed: text displays according to the client's game language setting
- If the client does not have this mod installed: text defaults to Simplified Chinese
- Contributions for additional translations are welcome.




