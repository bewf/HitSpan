# HitSpan

**HitSpan** is a lightweight, client-side HUD mod for Minecraft that provides accurate, real-time combat feedback. It displays your **hit range (reach)** using Minecraft’s own ray and hitbox logic, along with **knockback distance** measured over several ticks after a confirmed hit.

The goal of HitSpan is to stay **simple, accurate, and multiplayer-safe**. I made this mod because I couldn't find a good alternative for 1.8.9.

If you have any problems or suggestions feel free to open an **issue**. I will try my best to resolve any problems. 

## Features (In Depth)

* **Accurate hit range calculation**:

  HitSpan measures distance using Minecraft’s built-in ray tracing from the player’s eye position to the target’s hitbox. This makes sure that results closely match how the game itself determines hits, rather than using approximations or entity distances.

---
* **Dynamic range color feedback (optional)**:

  The range display can change color based on how far the hit was, this feature can help you maintain proper distancing without having to look at the number:

  * **Green** for long-range hits (default: ≥ 2.7 blocks)
  * **Yellow** for mid-range hits (default: ≥ 1.5 blocks)
  * **Red** for close-range hits
    All thresholds and colors are fully configurable.
---
* **Knockback tracking over multiple ticks**:

  Knockback is calculated by tracking the target’s horizontal displacement over several ticks after a confirmed hit, recording the maximum movement to give a more accurate representation of knockback strength.

---
* **OneConfig-powered HUD system**:

  Both the range and knockback displays use OneConfig's HUD framework, allowing:

  * Dragging and repositioning
  * Scaling
  * Toggling visibility
  * Clean integration with the OneConfig HUD editor
---
* **Fully client-side & multiplayer-safe**:

  HitSpan does not send packets, modify reach, or interact with the servers in any way. It only observes client-side data and server responses already visible to the player. This makes it fully safe for server use.
---

![Example of HUD (Customizable)](https://cdn.modrinth.com/data/cached_images/61c1c0fafd6fc225cb29c0d5c684d2fe0ab212f5.png)
> Example of HUD, my personal customization.

## What's New (v1.2.2)

* Migrated HUD rendering to the **OneConfig** HUD system
* Full HUD editor support (drag, scale, toggle)
* Improved hit detection reliability
* Dynamic range coloring with configurable thresholds
* Removed legacy HUD code and cleaned up internal logic
* Fixed One-Config Embed issue
* Fixed One-Config HUD not persisting issue
* bewf on top (secret)

---
##### Code licensed under [ARR](https://github.com/bewf/HitSpan/blob/main/LICENSE.md). Unmodified redistribution is permitted with credit. See the [full license](https://github.com/bewf/HitSpan/blob/main/LICENSE.md) for details.
