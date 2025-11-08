<table style="width: 100%; border-collapse: collapse;">
  <tr>
    <td style="width: 124px; vertical-align: top; text-align: center;">
      <div style="display: flex; justify-content: center; align-items: center;">
        <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/zipestudio.png?raw=true" title="It's me">
      </div>
    </td>
    <td style="vertical-align: top;">
      <div style="display: flex; flex-direction: column;">
        <div style="display: flex;">
          <a href="https://github.com/ZipeStudio/Tools-On-My-Back">
            <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/github.png?raw=true" title="Github page">
          </a>
          <a href="https://modrinth.com/mod/Tools-On-My-Back">
            <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/modrinth.png?raw=true" title="Modrinth page">
          </a>
          <a href="https://www.curseforge.com/minecraft/mc-mods/Tools-On-My-Back">
            <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/curseforge.png?raw=true" title="CurseForge page">
          </a>
        </div>
        <div style="display: flex;">
          <a href="https://discord.com/invite/XmGF7rkkuY">
            <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/discord.png?raw=true" title="Discord account">
          </a>
          <a href="https://t.me/zipeleaf">
            <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/telegram.png?raw=true" title="Telegram channel">
          </a>
          <a href="https://ko-fi.com/zipestudio/tip">
            <img src="https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/support.png?raw=true" title="Support me (thx)">
          </a>
        </div>
      </div>
    </td>
  </tr>
</table>

!["Description" Title](https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/ZSdescription.png?raw=true)

**Tools On My Back** — A client-side mod that visually displays the last used tool on the player's back. Adds immersion and style to your adventures, giving you the look of a seasoned traveler.

![banner](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/tomb_banner.gif?raw=true)

<br/>

### Showcase
![showcase](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/showcase.gif?raw=true)

!["Compatibility" Title](https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/ZScompatible.png?raw=true)

### Mod Compatibility
- Works seamlessly with other popular mods.

![mods_showcase](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/mods_showcase.gif?raw=true)

### Multiplayer Friendly
- Fully client-side — visible only to you.

![multiplayer_showcase](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/multiplayer_showcase.gif?raw=true)

### Custom Models (Resource Packs)
- Supports resource packs with custom tools.

![rp_showcase](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/rp_showcase.gif?raw=true)

!["Configurable" Title](https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/ZSconfigurable.png?raw=true)

### Right/Left Hand Support
> ![right_hand](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/hand_swap.png?raw=true)
> <details><summary>Hands Showcase</summary>
>
> Right Hand
> ![right_hand](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/right_hand.png?raw=true)
>
> Left Hand
> ![left_hand](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/left_hand.png?raw=true)
> </details>

<br>

### Settings
> ![settings](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/settings.png?raw=true)
> <details><summary>⚙️ Note:</summary>
> Most basic settings (like enabling the mod or rendering with a cape/elytra) can be configured in-game via the YACL menu.</br>
> However, adding or modifying items in the tools list currently requires manual editing of the .json5 config file (see below).</br>
> Full YACL support for tool configuration is still in development.
> </details>

<br>

### Simple Configuration File
The configuration file can be found at:</br>
`/config/tools-on-my-back.json5`

<pre style="border-left:3px solid #4CAF50; padding:10px;"><code>{
  "enableMod": true,
  "renderWithCape": true,
  "renderWithElytra": true,
  "tools": [
    {
      "itemId": "minecraft:diamond_sword",
      "group": "BACK",
      "is3DModel": false,
      "scale": 1,
      "angle": 0,
      "offsetX": 0.0,
      "offsetY": 0.0,
      "offsetZ": 0
    },
    {
      "itemId": "farmersdelight:skillet",
      "group": "BACK",
      "is3DModel": true,
      "scale": 1.2,
      "angle": 157.5,
      "offsetX": 0.0,
      "offsetY": 0.0,
      "offsetZ": -0.040625
    },
    {
    // YOUR ITEMS
    }
  ]
}
</code></pre>

| Parameter                           | Description                                                                                                                                                                          |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **itemId**                          | The full item ID (e.g. `minecraft:diamond_sword` or `my_mod:my_item`).<br/>!["id_settings"](https://github.com/ZipeStudio/Tools-On-My-Back/blob/master/img/id_settings.png?raw=true) |
| **group**                           | Currently only `"BACK"` — doesn’t affect behavior yet.                                                                                                                               |
| **is3DModel**                       | Set to `true` for 3D item models (like a shield).                                                                                                                                    |
| **scale**                           | Controls the overall size of the displayed item.                                                                                                                                     |
| **angle**                           | Rotation angle of the item (in degrees).                                                                                                                                             |
| **offsetX<br/>offsetY<br/>offsetZ** | Adjusts item position relative to the player’s back.                                                                                                                                 |


!["Support" Title](https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/ZSsupport.png?raw=true)

### Want to support mod and authors? Just tell everyone about this mod!

Yeah, you got it right. Just by advertising, you will support the mod and the creators well. The more people will know about this mod, the more downloads it will have, more downloads will give good motivation to authors and increase income from the site (literally free donation). **Remember, advertising must not be intrusive and annoiyng!**

### What you can do?
- Make a video review / advertisement
- Share it on social media or Discord
- Tell your friends about this mod
- Add it to your modpack or just download and enjoy the game

> Every mention matters — thank you for helping the community grow 🤍

!["Licensing" Title](https://github.com/ZipeStudio/Vault/blob/main/design/mods/main/ZSlicensing.png?raw=true)

### [See the original mod repository](https://github.com/ZipeStudio/Tools-On-My-Back)