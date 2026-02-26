# TABLIST Plugin Setup Guide

This guide tells you exactly what to change and where.

## Correct links
- Guide: https://github.com/leifiyoo/lockiprefixes/blob/main/TABLIST_PLUGIN_GUIDE.md
- Changelog page: https://github.com/leifiyoo/lockiprefixes/blob/main/CHANGELOG.json
- Changelog raw (used by the plugin): https://raw.githubusercontent.com/leifiyoo/lockiprefixes/main/CHANGELOG.json

## 1) LockiPrefixes config (this repo)
Edit `latest/src/main/resources/config.yml`.

- `tablist.format` at line ~19:
	- Default: `"{prefix} &7| &f{name}"`
	- Change this if you want a different internal tab format.
- `tablist.sorting.enabled` at line ~22:
	- `true` = sort by rank priority
	- `false` = disable sorting
- `tablist.animation.enabled` at line ~26:
	- `true` = animated gradient for configured groups
- `tablist.animation.groups` at line ~29:
	- Add or edit gradients per group, e.g. `owner: "#FF0000,#FF00FF,#FF0000"`

Also edit rank priorities under `groups:` (starts at line ~39):
- Each group has `priority` (higher value = higher rank).

## 2) If you use the external TAB plugin
When TAB is installed, LockiPrefixes disables its internal tablist manager automatically.
You must set placeholders inside TAB.

Edit your TAB plugin config (usually `plugins/TAB/config.yml` or layout files) and use:
- `%lockiprefixes_formatted%` for full formatted output
- `%lockiprefixes_tablist%` for tablist output

Example:
```yaml
playerlist-objective:
	value: "%lockiprefixes_tablist%"
```

## 3) In-game clickable guide link source
The click URL in the GUI is defined in:
- `latest/src/main/java/de/locki/lockiprefixes/gui/PrefixMenuManager.java`
- Around lines 836–837 (`ClickEvent.openUrl(...)` and hover text).

## 4) Plugin metadata link source
The plugin "website" shown by server tools is defined in:
- `latest/src/main/resources/plugin.yml` line ~7
- `modern/src/main/resources/plugin.yml` line ~7
- `mid/src/main/resources/plugin.yml` line ~7
- `legacy/src/main/resources/plugin.yml` line ~6

## 5) Apply changes
After editing configs on your server:
1. Save file(s)
2. Restart server (recommended), or reload LockiPrefixes
3. Verify with TAB list in-game
