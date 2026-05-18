<div align="center">

# LockiPrefixes

**Clean rank-based chat and tablist formatting for Paper/Spigot servers.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.8%20%E2%86%92%2026.1.x-2EA043?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net)
[![Paper](https://img.shields.io/badge/Paper-ready-5865F2?style=for-the-badge)](https://papermc.io)
[![LuckPerms](https://img.shields.io/badge/LuckPerms-required-F59E0B?style=for-the-badge)](https://luckperms.net)
[![License](https://img.shields.io/badge/License-MIT-111827?style=for-the-badge)](https://github.com/leifiyoo/lockiprefixes/blob/main/LICENSE)

</div>

---

## Features

- In-game rank editor with `/lpx menu`
- Automatic LuckPerms group creation
- Chat and tablist formatting from one template
- TAB plugin support through PlaceholderAPI
- Hot reload with `/lpx reload`
- Startup-only update checks with clean admin notifications

> For chat formatting on modern servers, set `enforce-secure-profile=false` in `server.properties`.

---

## Pick Your JAR

| File | Minecraft |
|---|---|
| `lockiprefixes-legacy-x.x.x.jar` | 1.8 - 1.12 |
| `lockiprefixes-mid-x.x.x.jar` | 1.13 - 1.18 |
| `lockiprefixes-modern-x.x.x.jar` | 1.19 - 1.19.4 |
| `lockiprefixes-latest-x.x.x.jar` | 1.20 - 1.21.11 / 26.1.x |

Drop the matching JAR into `plugins/`, restart your server, then run:

```text
/lpx menu
```

---

## Requirements

- Paper or Spigot
- LuckPerms
- Java version matching your Minecraft server version
- PlaceholderAPI only if you want TAB integration

---

## TAB Placeholder

Use this in TAB for formatted tab names:

```yaml
customtabname: "%lockiprefixes_tablist%"
```

Available placeholders:

| Placeholder | Output |
|---|---|
| `%lockiprefixes_prefix%` | Rank prefix |
| `%lockiprefixes_tablist%` | Prefix + separator + name |
| `%lockiprefixes_name%` | Prefix directly followed by player name |

---

## Links

- [GitHub](https://github.com/leifiyoo/lockiprefixes)
- [LuckPerms](https://luckperms.net)
- [TAB](https://github.com/NEZNAMY/TAB)

