<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.7--1.21-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Paper%20%2F%20Spigot-Supported-blue?style=for-the-badge" alt="Paper/Spigot">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License">
</p>

<h1 align="center">🎨 LockiPrefixes</h1>

<p align="center">
  <b>A beautiful chat & tablist formatter for LuckPerms</b><br>
  Supports Minecraft 1.7 - 1.21 • Animated gradients • Rank sorting
</p>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 💬 **Chat Formatting** | Custom chat format with prefix, colors, and styles |
| 📋 **TAB List** | Formatted player names in the TAB list |
| 🔢 **Rank Sorting** | Higher ranks appear at the top of TAB list |
| 🔄 **Auto-Update** | TAB list updates when rank changes in LuckPerms |
| 🌈 **Animated Gradients** | Rainbow/gradient animations on rank names |
| 🔌 **PlaceholderAPI** | Use placeholders in other plugins |
| 📦 **Multi-Version** | Supports Minecraft 1.7 to 1.21! |

---

## 📥 Downloads

Choose the right version for your server:

| Build | Minecraft | Server Software |
|-------|-----------|-----------------|
| 🟢 `lockiprefixes-latest` | **1.20 - 1.21** | Paper (recommended) |
| 🔵 `lockiprefixes-modern` | **1.17 - 1.19** | Paper / Spigot |
| 🟡 `lockiprefixes-mid` | **1.13 - 1.16** | Spigot |
| 🟠 `lockiprefixes-legacy` | **1.7 - 1.12** | Spigot / Bukkit |

> 💡 **Tip:** Use the `latest` build for Paper 1.20+ servers - it has the best features including animated gradients!

---

## 📸 Preview

```
Chat:    Owner | Steve » Hello everyone!
TAB:     Owner | Steve
         Admin | Alex
         VIP   | Bob
         Player
```

With animated gradients, the rank name smoothly shifts through colors! 🎨

---

## 📥 Installation

### Step 1: Download
Download the correct JAR for your Minecraft version from [Releases](../../releases)

### Step 2: Install
Put the JAR file in your `plugins/` folder

### Step 3: Important! ⚠️
**For Paper 1.19+ servers**, add this to your `server.properties`:
```properties
enforce-secure-profile=false
```

### Step 4: Restart
Restart your server and edit `plugins/LockiPrefixes/config.yml`

---

## 📝 Configuration

### Chat Format
```yaml
chat:
  format: "{prefix} &7| &f{name} &7» &f{message}"
```

**Result:** `Owner | Steve » Hello!`

### TAB List Format
```yaml
tablist:
  format: "{prefix} &7| &f{name}"
  sorting:
    enabled: true  # Sort by rank priority
```

### Rank Setup
```yaml
groups:
  owner:
    chat-format: "&4&lOwner &7| &f{name} &7» &f{message}"
    tablist-format: "&4&lOwner &7| &f{name}"
    rank-tag: "Owner"
    priority: 100  # Higher = top of TAB list

  admin:
    chat-format: "&c&lAdmin &7| &f{name} &7» &f{message}"
    tablist-format: "&c&lAdmin &7| &f{name}"
    rank-tag: "Admin"
    priority: 80

  vip:
    chat-format: "&a&lVIP &7| &f{name} &7» &f{message}"
    tablist-format: "&a&lVIP &7| &f{name}"
    rank-tag: "VIP"
    priority: 10
```

> 💡 **Tip:** The group names must match your LuckPerms groups exactly!

---

## 🌈 Animated Gradients

*Available in the `latest` build (1.20-1.21)*

Make your rank names shift through colors!

```yaml
tablist:
  animation:
    enabled: true
    speed: 5  # Lower = faster animation

    groups:
      # Rainbow effect
      owner: "#FF0000,#FF7F00,#FFFF00,#00FF00,#0000FF,#8B00FF,#FF0000"
      
      # Fire effect (red-orange)
      admin: "#FF0000,#FF5500,#FFAA00,#FF5500,#FF0000"
      
      # Ocean effect (blue-cyan)
      vip: "#0000FF,#0055FF,#00AAFF,#00FFFF,#00AAFF,#0055FF,#0000FF"
```

### Preset Gradients

| Name | Colors | Preview |
|------|--------|---------|
| 🌈 Rainbow | `#FF0000,#FF7F00,#FFFF00,#00FF00,#0000FF,#8B00FF,#FF0000` | Red→Orange→Yellow→Green→Blue→Purple |
| 🔥 Fire | `#FF0000,#FF5500,#FFAA00,#FF5500,#FF0000` | Red→Orange→Red |
| 🌊 Ocean | `#0000FF,#0055FF,#00AAFF,#00FFFF,#00AAFF,#0055FF` | Blue→Cyan→Blue |
| 💜 Galaxy | `#FF00FF,#AA00FF,#5500FF,#AA00FF,#FF00FF` | Pink→Purple→Pink |
| 💚 Nature | `#00FF00,#55FF00,#AAFF00,#55FF00,#00FF00` | Green→Lime→Green |
| ❄️ Ice | `#FFFFFF,#AAFFFF,#55FFFF,#AAFFFF,#FFFFFF` | White→Cyan→White |

---

## 🎨 Color Codes

### Legacy Colors
```
&0 Black       &8 Dark Gray
&1 Dark Blue   &9 Blue
&2 Dark Green  &a Green
&3 Dark Aqua   &b Aqua
&4 Dark Red    &c Red
&5 Purple      &d Pink
&6 Gold        &e Yellow
&7 Gray        &f White
```

### Formatting
```
&l Bold
&o Italic
&n Underline
&m Strikethrough
&r Reset
```

### Hex Colors (1.16+)
```
&#FF5555  = Light Red
&#55FF55  = Light Green
&#5555FF  = Light Blue
&#FFAA00  = Orange
```

---

## 📦 Placeholders

### Built-in Placeholders
Use these in your format strings:

| Placeholder | Description |
|-------------|-------------|
| `{name}` | Player name |
| `{displayname}` | Nickname |
| `{prefix}` | LuckPerms prefix |
| `{suffix}` | LuckPerms suffix |
| `{message}` | Chat message |

### PlaceholderAPI
Use in other plugins:

| Placeholder | Output |
|-------------|--------|
| `%lockiprefixes_prefix%` | Player's prefix |
| `%lockiprefixes_suffix%` | Player's suffix |
| `%lockiprefixes_group%` | Primary group |
| `%lockiprefixes_formatted%` | Full formatted name |

---

## 💻 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/lockiprefixes reload` | `lockiprefixes.reload` | Reload configuration |

---

## 📋 Requirements

- **Server:** Paper, Spigot, or Bukkit (1.7 - 1.21)
- **Required:** [LuckPerms](https://luckperms.net/)
- **Optional:** [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

---

## ❓ FAQ

### Chat not working? (Paper 1.19+)
Make sure you have `enforce-secure-profile=false` in your `server.properties` and restart the server.

### TAB list not updating?
1. Make sure LuckPerms is installed
2. Player must have a group assigned
3. Try `/lockiprefixes reload`

### How to disable animations?
Set `animation.enabled: false` in config.yml

### Can I use this with TAB plugin?
Yes! This plugin is compatible with TAB. You can either:
- Use LockiPrefixes for everything
- Or disable LockiPrefixes tablist and use TAB with `%lockiprefixes_formatted%`

### Which build should I use?
| Your Server | Use This Build |
|-------------|----------------|
| Paper 1.20+ | `lockiprefixes-latest` |
| Spigot 1.17-1.19 | `lockiprefixes-modern` |
| Spigot 1.13-1.16 | `lockiprefixes-mid` |
| Spigot 1.7-1.12 | `lockiprefixes-legacy` |

---

## 🔨 Building from Source

```bash
git clone https://github.com/leifiyoo/lockiprefixes.git
cd lockiprefixes

# Build all versions
./gradlew build

# Or build specific version
./gradlew :latest:build   # 1.20-1.21
./gradlew :modern:build   # 1.17-1.19
./gradlew :mid:build      # 1.13-1.16
./gradlew :legacy:build   # 1.7-1.12
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ for the Minecraft community<br>
  <b>Supports Minecraft 1.7 - 1.21</b>
</p>
