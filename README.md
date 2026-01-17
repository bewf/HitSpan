<p align="center">
  <img src="https://github.com/bewf/HitSpan/raw/main/GitAssets/icon4.png" width="400">
</p>

**HitSpan** is a lightweight, client-side HUD mod for Minecraft **1.8.9** that displays:

- **Hit range (reach)**
- **Knockback distance**

I made this because there are no good alternatives for 1.8.9. If you run into issues or have suggestions, feel free to open an issue on [GitHub](https://github.com/bewf/HitSpan/issues)

---

<p align="center">
  <img src="https://github.com/bewf/HitSpan/raw/main/GitAssets/ExampleGifCrop.gif" width="520"><br>
  <sub><em>Example of HitSpan in use.</em></sub>
</p>

---

## Features

- **Accurate hit range**
  - Uses Minecraft’s own ray tracing from the player’s eye position to the target’s hitbox.

- **Dynamic range colors (optional)**
  - Green for long-range hits  
  - Yellow for mid-range hits  
  - Red for close-range hits  
  - Fully configurable thresholds.

- **Knockback tracking**
  - Measures horizontal knockback over multiple ticks after a confirmed hit.

- **OneConfig HUD**
  - Drag, scale, and toggle each HUD element.
  - Integrated with the OneConfig HUD editor.

- **Client-side & multiplayer-safe**
  - Does not send packets or modify reach.
  - Safe to use on servers.

---
<p align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/61c1c0fafd6fc225cb29c0d5c684d2fe0ab212f5.png" width="400"><br>
  <sub><em>Example HUD (my personal customization).</em></sub>
</p>

---

##### Code licensed under [ARR](https://github.com/bewf/HitSpan/blob/main/LICENSE.md). Unmodified redistribution is permitted with credit. See the [full license](https://github.com/bewf/HitSpan/blob/main/LICENSE.md) for details.
