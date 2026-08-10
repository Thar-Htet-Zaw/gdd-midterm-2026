# GDD Space Invaders Midterm Starter

This is a starting codebase for GDD Midterm.

## Code Modifications — Space Invaders (GDD Midterm)

### 1. Player ship moves in every direction
**File modified:** `Player.java`

Added a new `dy` field (vertical velocity) alongside the existing `dx` (horizontal velocity) already inherited from `Sprite`. Kept `dy` local to `Player` rather than touching `Sprite.java`, since only the player needs vertical movement.

- `act()` now applies `y += dy` each frame, using the same pattern as the existing `x += dx`, and clamps `y` between the top of the screen and `GROUND - PLAYER_HEIGHT` so the ship can't fly off-screen vertically — mirroring the existing horizontal clamp.
- `keyPressed()` / `keyReleased()` extended to handle `VK_UP` and `VK_DOWN`, setting/clearing `dy` the same way `VK_LEFT`/`VK_RIGHT` already set/clear `dx`.

### 2. Shot has a sound effect
**Files modified:** `Scene1.java`, `AudioPlayer.java`

Added a static `playOnce(String filePath)` method to `AudioPlayer`. Unlike the existing instance-based player (built for one continuously looping background track via `Clip.LOOP_CONTINUOUSLY`), `playOnce()` opens a fresh, short-lived `Clip` per call and auto-closes it when playback finishes. This lets multiple effects overlap (e.g. firing rapidly) without cutting each other off or interfering with the background music.

`Scene1`'s `TAdapter.keyPressed()` calls `AudioPlayer.playOnce("src/audio/fire.wav")` every time a shot is created on `VK_SPACE`.

### 3. Walls added, and walls are not enemies
**Files added:** `Wall.java` (new package `gdd.tile`)
**Files modified:** `Scene1.java`

`Wall` extends `Sprite` directly and lives in its own package, `gdd.tile` — deliberately separate from `gdd.sprite` where `Enemy`/`Alien1` live. It is **never** added to `Scene1`'s `enemies` list. This is the direct answer to "walls are not enemies, operate them as MAP tiles": walls are static level geometry, not AI-driven actors.

Walls are generated from `Scene1`'s existing MAP grid system (`generateMap()`), not a separate hardcoded object list:
- The `MAP` grid is procedurally generated each time the scene starts, mixing randomized diagonal "staircase" segments and horizontal "band" segments (each with a wide gap so the player can always pass through), so the layout differs every playthrough instead of being one fixed pattern.
- A parallel `wallDestroyed[][]` boolean grid tracks which MAP cells have been permanently destroyed by a shot, since the `Wall` objects themselves are rebuilt fresh every frame as the map scrolls.
- `updateWalls()` rebuilds the currently visible wall tiles each frame. Each MAP row only becomes visible once it's actually scrolled into view from the top edge of the screen — rows can't appear pre-placed mid-screen. This makes walls approach the player gradually, the same way enemies spawn and drift down, rather than the whole screen populating with walls at once.
- A short delay (`WALL_SPAWN_DELAY`, 3 seconds) holds off any walls appearing at scene start, so the player has a moment to get oriented before the first row even begins its descent.
- Wall tiles are drawn as a themed "armored bunker panel" (`drawWallTile()`) — a beveled teal-green block with corner rivets — visually distinct from the enemy/player/powerup sprites, using `Graphics` primitives only (no image asset needed).
- The decorative starfield background was decoupled from the wall grid into its own independent system (`starX[]`/`starY[]`, `drawStarfield()`), since originally stars and walls shared the same MAP array.

### 4. Ship explodes on wall contact, with sound
**File modified:** `Scene1.java`

Inside `update()`, right after `player.act()` (so it checks the player's post-movement position), a standard AABB rectangle-overlap test runs between the player's bounding box and every currently active wall tile. On overlap: the player's sprite image switches to the explosion image, `player.setDying(true)` is called, and `AudioPlayer.playOnce(PLAYER_EXPLODE_SOUND)` plays the sound.

No new game-over logic was added — `drawPlayer()` already contained an `isDying()` check (originally written for the old, now-unused bomb-collision system) that calls `player.die()` and sets `inGame = false`. This step just triggers that existing flow via wall contact instead of duplicating a second death path.

### 5. Shot destroys wall, with sound
**File modified:** `Scene1.java`

Inside the existing shot-update loop in `update()`, a Shot-vs-Wall collision check mirrors the pre-existing Shot-vs-Enemy check just above it. On a hit:
- The wall's origin MAP cell is permanently marked destroyed via `wallDestroyed[wall.getMapRow()][wall.getMapCol()] = true`, so it stays gone even as the map continues to scroll and rebuild.
- The wall is removed from the current frame's active list.
- `AudioPlayer.playOnce(WALL_BREAK_SOUND)` plays.
- The shot is consumed (`shot.die()`), same as hitting an enemy.

### Other fixes
- **Pre-existing compile errors fixed** (not introduced this session): `Enemy`, `Explosion`, `Shot`, and `Alien1`'s inner `Bomb` class only implemented `act(int direction)` as an overload, never actually satisfying `Sprite`'s abstract no-arg `act()`. Added empty no-arg `act()` overrides to each — real per-frame behavior is unchanged, still driven by `act(int direction)` or direct field updates in `Scene1`.
- **Dead code** (old starfield-cluster drawing, unused bomb-drawing method, a few unused fields) was left in the file as comments rather than deleted, to keep old logic available for reference.

### Why these design decisions
- **Wall isn't in `gdd.sprite` / doesn't extend `Enemy`:** the assignment explicitly penalizes walls not being defined as distinct MAP tiles. Keeping `Wall` in its own package and off the `enemies` list is the clearest, most defensible way to satisfy that.
- **MAP is procedurally generated instead of fixed:** a single hardcoded layout looked repetitive and cramped; procedural generation with mandatory clear gaps between segments keeps every playthrough different while staying visually readable.
- **All one-shot sound effects go through `AudioPlayer.playOnce()`** instead of a separate utility class, so all audio playback (looping background music and one-shot SFX) lives in a single, consistent class.

## References
This project is based from this 
[Space Invader](https://github.com/janbodnar/Java-Space-Invaders) repository.