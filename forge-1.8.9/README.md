<p align="center">
  <img src="https://github.com/bewf/HitSpan/raw/main-1.8.9/GitAssets/icon4.png" width="400">
</p>

**HitSpan** is a lightweight, client-side HUD mod for Minecraft **1.8.9** and **1.12.2** that displays combat-related information such as:

- **Hit range (reach)**
- **Knockback distance**
- **CPS**
- **Combo Counter**

I made this because there were no good alternatives that were accurate, clean, and configurable.

If you run into issues or have suggestions, feel free to open an issue on  
[GitHub](https://github.com/bewf/HitSpan/issues)

---

<p align="center">
  <img src="https://github.com/bewf/HitSpan/raw/main-1.8.9/GitAssets/ExampleGifCrop.gif" width="520"><br>
  <sub><em>Example of HitSpan in use.</em></sub>
</p>

## Features

- **Dynamic range tracking**
  - Uses Minecraft’s own ray tracing from the player’s eye position to the target’s hitbox.
  - Green for long-range hits  
  - Yellow for mid-range hits  
  - Red for close-range hits  
  - Fully configurable thresholds and colours.
- **Knockback tracking**
  - Measures horizontal knockback over multiple ticks after a hit.
- **CPS HUD**
  - Displays your clicks per second with configurable averaging and formatting
- **Combos HUD**
  - Shows how many times you hit an entity without them hitting you.
- **OneConfig HUD**
  - Drag, scale, and toggle each HUD element.
  - Integrated with the OneConfig HUD editor.
- **Client-side & multiplayer-safe**
  - Does not send packets or modify reach.
  - Safe to use on servers.

## OneConfig Integration

<p align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/8e6cc1e06e776169ada6c760e65fb9d3e2fa6e76.png" width="900"><br>
  <sub><em>HitSpan is fully configurable through OneConfig.</em></sub>
</p>

---

##### Code licensed under [ARR](https://github.com/bewf/HitSpan/blob/main/LICENSE.md). Unmodified redistribution **in free modpacks** is permitted with credit. See the [full license](https://github.com/bewf/HitSpan/blob/main/LICENSE.md) on GitHub for details.
