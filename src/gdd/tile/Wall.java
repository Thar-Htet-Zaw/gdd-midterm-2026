package gdd.tile;

import gdd.sprite.Sprite;

/**
 * Represents a single wall tile that is part of the level's MAP grid.
 *
 * IMPORTANT: Wall is intentionally placed in its own package (gdd.tile),
 * separate from gdd.sprite.Enemy, and is never added to Scene1's `enemies`
 * list. Walls are static MAP geometry, not enemies. Their position and
 * lifecycle are driven by Scene1's existing MAP grid / scrolling system,
 * not by independent AI behavior like Enemy subclasses have.
 */
public class Wall extends Sprite {

    private final int width;
    private final int height;

    // Which cell of Scene1's MAP grid this on-screen tile was generated
    // from. Needed so that destroying this tile (via a shot) can be
    // permanently recorded against the correct MAP cell, since Wall
    // objects themselves are rebuilt fresh every frame as the map scrolls.
    private int mapRow;
    private int mapCol;

    public Wall(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setMapCell(int mapRow, int mapCol) {
        this.mapRow = mapRow;
        this.mapCol = mapCol;
    }

    public int getMapRow() {
        return mapRow;
    }

    public int getMapCol() {
        return mapCol;
    }

    @Override
    public void act() {
        // Walls do not move or think on their own. Their on-screen position
        // is recalculated each frame by Scene1's scrolling MAP logic, and
        // their visible/destroyed state is toggled externally when hit by
        // a shot. This method is intentionally empty (required override
        // since Sprite declares act() as abstract).
    }
}