<div align="center">

```
  _               _   _ ____            __ _
 | |    ___   ___| | _(_)  _ \ _ __ ___|  (_)_  _____  ___
 | |   / _ \ / __| |/ / | |_) | '__/ _ \ |  \ \/ / _ \/ __|
 | |__| (_) | (__|   <| |  __/| | |  __/ | |  >  <  __/\__ \
 |_____\___/ \___|_|\_\_|_|   |_|  \___|_|_| /_/\_\___||___/
```

**Rank-based chat & tablist formatting for Paper servers.**  
Set up everything in-game  no YAML editing required.

[![MC](https://img.shields.io/badge/Minecraft-1.8--1.21-brightgreen?style=flat-square)](https://papermc.io)
[![Paper](https://img.shields.io/badge/Paper-1.8%2B-5865F2?style=flat-square)](https://papermc.io)
[![LuckPerms](https://img.shields.io/badge/Requires-LuckPerms-orange?style=flat-square)](https://luckperms.net)
[![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)](LICENSE)

</div>

---

## What it does

- In-game **rank editor**  create ranks, pick a style, done
- Chat format and tablist name **auto-derived** from a single template
- **LuckPerms group created automatically** when you create a rank
- Works with the [TAB](https://github.com/NEZNAMY/TAB) plugin out of the box
- Hot-reload  no server restart needed- Supports **Minecraft 1.8 through 1.21**
> **Required:** Set `enforce-secure-profile=false` in `server.properties` for chat formatting to work.

---

## Setup

**1. Requirements**

| | Min. Version |
|---|---|
| Java | 8 (1.8–1.16) / 16 (1.17–1.19) / 21 (1.20+) |
| Paper / Spigot | 1.8 |
| [LuckPerms](https://luckperms.net/) | latest |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | latest *(only needed for TAB integration)* |

**2. Pick the right JAR**

| JAR | Minecraft version |
|---|---|
| `lockiprefixes-legacy-x.x.x.jar` | 1.8 – 1.12 |
| `lockiprefixes-mid-x.x.x.jar` | 1.13 – 1.18 |
| `lockiprefixes-modern-x.x.x.jar` | 1.19 – 1.19.4 |
| `lockiprefixes-latest-x.x.x.jar` | 1.20 – 1.21 |

Drop the matching JAR into `plugins/` and restart.  
A `config.yml` is generated automatically.

**3. In-game**

```
/lpx menu
```

---

## Commands

| Command | What it does |
|---|---|
| `/lpx menu` | Open the rank manager |
| `/lpx reload` | Reload config |

Permission: `lockiprefixes.menu` / `lockiprefixes.reload`

---

## Config

`plugins/LockiPrefixes/config.yml`  normally you don't touch this, the GUI writes it for you.

```yaml
groups:
  owner:
    chat-format:    "&4&lOwner &8| &f{name} &7{message}"
    tablist-format: "&4&lOwner &8| &f{name}"
    priority:       100
```

---

## TAB Plugin

LockiPrefixes auto-detects TAB and disables its own tablist module.

Edit `plugins/TAB/groups.yml`:

```yaml
_DEFAULT_:
  tabprefix:      ""
  tabsuffix:      ""
  customtabname:  "%lockiprefixes_tablist%"
  tagprefix:      "%luckperms-prefix%"
```

Then run `/tab reload`.

---

## Placeholders

| Placeholder | Returns |
|---|---|
| `%lockiprefixes_prefix%` | Rank prefix only (e.g. `&4&lOwner `) |
| `%lockiprefixes_tablist%` | Prefix + separator + name  use in TAB |
| `%lockiprefixes_name%` | Prefix directly followed by player name |

---

## Build from source

```bash
git clone https://github.com/leifiyoo/lockiprefixes.git
cd lockiprefixes

# Build a specific version
./gradlew :legacy:shadowJar -x test   # 1.8–1.12
./gradlew :mid:shadowJar -x test      # 1.13–1.18
./gradlew :modern:shadowJar -x test   # 1.19–1.19.4
./gradlew :latest:shadowJar -x test   # 1.20–1.21

# Or build all at once
./gradlew shadowJar -x test
```

Output JARs land in `<module>/build/libs/`.

---

<div align="center">MIT License  free to use, modify, redistribute.</div>
