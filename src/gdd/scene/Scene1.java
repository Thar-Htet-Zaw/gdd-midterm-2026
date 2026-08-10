package gdd.scene;

import gdd.AudioPlayer;
import gdd.Game;
import static gdd.Global.*;
import gdd.SpawnDetails;
import gdd.powerup.PowerUp;
import gdd.powerup.SpeedUp;
import gdd.sprite.Alien1;
import gdd.sprite.Enemy;
import gdd.sprite.Explosion;
import gdd.sprite.Player;
import gdd.sprite.Shot;
import gdd.tile.Wall;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Scene1 extends JPanel {

    private int frame = 0;
    private List<PowerUp> powerups;
    private List<Enemy> enemies;
    private List<Explosion> explosions;
    private List<Shot> shots;
    private Player player;
    // private Shot shot;

    final int BLOCKHEIGHT = 50;
    final int BLOCKWIDTH = 50;

    final int BLOCKS_TO_DRAW = BOARD_HEIGHT / BLOCKHEIGHT;

    private int direction = -1;
    private int deaths = 0;

    private boolean inGame = true;
    private String message = "Game Over";

    private final Dimension d = new Dimension(BOARD_WIDTH, BOARD_HEIGHT);
    private final Random randomizer = new Random();

    private Timer timer;
    private final Game game;

    // TODO load this map from a file
    // private int currentRow = -1;
    // private int mapOffset = 0;

    private static final int MAP_ROWS = 32;
    private static final int MAP_COLS = 12;

    // How long (in frames) to hold off spawning/scrolling any walls in
    // after the scene starts. At 60 FPS (see `timer` below), 180 frames
    // = 3 seconds. Gives the player a breather before the first wall
    // tile ever scrolls into view, so they can't die instantly on spawn.
    private static final int WALL_SPAWN_DELAY = 180;

    // Reusing explode.wav for wall destruction, since a wall breaking is
    // reasonably an "explosion"-type event and no dedicated wall-break
    // sound file was provided.
    private static final String WALL_BREAK_SOUND = "src/audio/explode.wav";

    // Same explode.wav reused for the player ship exploding on wall
    // contact - kept as a separate named constant so it can easily be
    // pointed at a different sound later if one is added.
    private static final String PLAYER_EXPLODE_SOUND = "src/audio/explode.wav";

    // Generated at gameInit() time - see generateMap(). Mixes diagonal
    // staircase wall segments and horizontal band wall segments randomly,
    // so the layout is different each playthrough instead of one fixed
    // hardcoded pattern.
    private int[][] MAP;

    // Wall tiles currently visible on screen this frame, rebuilt every
    // frame by updateWalls() based on scroll position. Used by both
    // drawMap() (rendering) and update() (collision, added in a later step)
    // so both stay in sync.
    private List<Wall> walls;

    // Permanently tracks which MAP cells have been destroyed by a shot,
    // indexed [row][col] matching MAP. Unlike `walls`, this persists across
    // frames (walls themselves are rebuilt every frame as the map scrolls).
    private boolean[][] wallDestroyed;

    // Decorative scrolling starfield background - fully independent of the
    // wall MAP grid, so stars and walls no longer share the same data.
    // Stars are drawn first (background), walls drawn on top afterward.
    private static final int STAR_COUNT = 50;
    private int[] starX;
    private int[] starY;

    private HashMap<Integer, SpawnDetails> spawnMap = new HashMap<>();
    private AudioPlayer audioPlayer;
    // private int lastRowToShow;
    // private int firstRowToShow;

    public Scene1(Game game) {
        this.game = game;
        // initBoard();
        // gameInit();
        loadSpawnDetails();
    }

    private void initAudio() {
        try {
            String filePath = "src/audio/scene1.wav";
            audioPlayer = new AudioPlayer(filePath);
            audioPlayer.play();
        } catch (Exception e) {
            System.err.println("Error initializing audio player: " + e.getMessage());
        }
    }

    private void loadSpawnDetails() {
        // TODO load this from a file
        spawnMap.put(50, new SpawnDetails("PowerUp-SpeedUp", 100, 0));
        spawnMap.put(200, new SpawnDetails("Alien1", 200, 0));
        spawnMap.put(300, new SpawnDetails("Alien1", 300, 0));

        spawnMap.put(400, new SpawnDetails("Alien1", 400, 0));
        spawnMap.put(401, new SpawnDetails("Alien1", 450, 0));
        spawnMap.put(402, new SpawnDetails("Alien1", 500, 0));
        spawnMap.put(403, new SpawnDetails("Alien1", 550, 0));

        spawnMap.put(500, new SpawnDetails("Alien1", 100, 0));
        spawnMap.put(501, new SpawnDetails("Alien1", 150, 0));
        spawnMap.put(502, new SpawnDetails("Alien1", 200, 0));
        spawnMap.put(503, new SpawnDetails("Alien1", 350, 0));
    }

    private void initBoard() {

    }

    public void start() {
        addKeyListener(new TAdapter());
        setFocusable(true);
        requestFocusInWindow();
        setBackground(Color.black);

        timer = new Timer(1000 / 60, new GameCycle());
        timer.start();

        gameInit();
        initAudio();
    }

    public void stop() {
        timer.stop();
        try {
            if (audioPlayer != null) {
                audioPlayer.stop();
            }
        } catch (Exception e) {
            System.err.println("Error closing audio player.");
        }
    }

    private void gameInit() {

        enemies = new ArrayList<>();
        powerups = new ArrayList<>();
        explosions = new ArrayList<>();
        shots = new ArrayList<>();
        walls = new ArrayList<>();

        MAP = generateMap();
        wallDestroyed = new boolean[MAP.length][MAP[0].length];

        starX = new int[STAR_COUNT];
        starY = new int[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = randomizer.nextInt(BOARD_WIDTH);
            starY[i] = randomizer.nextInt(BOARD_HEIGHT);
        }

        // for (int i = 0; i < 4; i++) {
        // for (int j = 0; j < 6; j++) {
        // var enemy = new Enemy(ALIEN_INIT_X + (ALIEN_WIDTH + ALIEN_GAP) * j,
        // ALIEN_INIT_Y + (ALIEN_HEIGHT + ALIEN_GAP) * i);
        // enemies.add(enemy);
        // }
        // }
        player = new Player();
        // shot = new Shot();
    }

    // Procedurally builds the wall MAP grid by randomly mixing two segment
    // types down the length of the map:
    //  - "diagonal" segments: a single wall cell per row, shifting one
    //    column left/right each row (staircase pattern, like the original
    //    fixed design).
    //  - "horizontal" segments: one row gets a wide band of wall cells
    //    with a gap left open, so the player always has room to pass
    //    through rather than being fully blocked.
    // Which segment type appears, how long it runs, and where gaps/columns
    // fall is randomized using the existing `randomizer`, so the layout is
    // different every time the scene starts instead of one fixed pattern.
    private int[][] generateMap() {
        int[][] map = new int[MAP_ROWS][MAP_COLS];

        int col = randomizer.nextInt(MAP_COLS);
        int direction = randomizer.nextBoolean() ? 1 : -1;

        // Every wall segment is now followed by a mandatory clear gap
        // (empty rows, no tiles at all) before the next one starts. This
        // is what actually fixes the "clustered" feel - previously every
        // single row belonged to some wall segment with no breathing room
        // between them, so the map read as one dense, unbroken corridor.
        final int MIN_CLEAR_ROWS = 3;
        final int MAX_CLEAR_ROWS = 5;

        int row = 0;
        while (row < MAP_ROWS) {
            int segmentLength = 3 + randomizer.nextInt(3); // 3-5 rows per segment
            segmentLength = Math.min(segmentLength, MAP_ROWS - row);

            boolean horizontalSegment = randomizer.nextBoolean();

            if (horizontalSegment) {
                // Place a single-row horizontal wall band with a wide,
                // generous gap so there's always a comfortable, clearly
                // visible lane through it - rather than a couple of
                // cramped columns.
                int wallRow = row + randomizer.nextInt(segmentLength);
                int gapWidth = 4 + randomizer.nextInt(2); // 4-5 column gap (of 12)
                int gapStart = randomizer.nextInt(MAP_COLS - gapWidth + 1);

                for (int c = 0; c < MAP_COLS; c++) {
                    if (c < gapStart || c >= gapStart + gapWidth) {
                        map[wallRow][c] = 1;
                    }
                }

                // Keep the diagonal anchor column in bounds for whenever
                // the next diagonal segment picks up again.
                col = Math.max(0, Math.min(MAP_COLS - 1, col));
            } else {
                // Diagonal staircase, one tile per row - already fairly
                // open (only 1 of 12 columns per row is blocked), kept as-is.
                for (int r = row; r < row + segmentLength; r++) {
                    map[r][col] = 1;
                    col += direction;
                    if (col <= 0 || col >= MAP_COLS - 1) {
                        direction *= -1;
                        col = Math.max(0, Math.min(MAP_COLS - 1, col));
                    }
                }
            }

            row += segmentLength;

            // Insert a guaranteed clear gap between this segment and the
            // next, so segments read as distinct, spaced-out obstacles
            // rather than bleeding into one another.
            row += MIN_CLEAR_ROWS + randomizer.nextInt(MAX_CLEAR_ROWS - MIN_CLEAR_ROWS + 1);
        }

        return map;
    }

    // Draws the decorative scrolling starfield background. Fully
    // independent of the wall MAP - star positions have nothing to do
    // with which cells contain walls, so this can be drawn first as a
    // background layer with walls drawn cleanly on top afterward.
    private void drawStarfield(Graphics g) {
        g.setColor(Color.WHITE);
        for (int i = 0; i < STAR_COUNT; i++) {
            int y = (starY[i] + frame) % BOARD_HEIGHT;
            g.fillOval(starX[i], y, 2, 2);
        }
    }
    private void drawMap(Graphics g) {
        // Background layer: decorative starfield, independent of walls.
        drawStarfield(g);

        // Foreground layer: wall tiles. The `walls` list is already
        // computed for this frame by updateWalls() (called from update(),
        // before repaint), so rendering here just draws whatever is
        // currently active - drawn after stars so walls sit cleanly on
        // top rather than blending with them.
        for (Wall wall : walls) {
            drawWallTile(g, wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
        }
    }

    // Rebuilds the `walls` list of currently visible wall tiles. Each MAP
    // row only starts existing on screen once enough scroll time has
    // passed for it to have "descended" from just above the top edge -
    // rows never appear pre-placed somewhere in the middle/bottom of the
    // screen. This makes walls enter one row at a time from the top and
    // drift down toward the player, the same way enemies spawn and move,
    // instead of the whole visible area popping in fully-formed at once.
    // Skips any MAP cell already marked destroyed in wallDestroyed.
    // Called once per frame from update().
    private void updateWalls() {
        walls.clear();

        // Don't spawn/scroll any wall tiles in until WALL_SPAWN_DELAY has
        // elapsed since the scene started, giving the player a moment to
        // get oriented before the very first row even begins its descent.
        if (frame < WALL_SPAWN_DELAY) {
            return;
        }

        // Total pixels scrolled since walls started (1 px/frame).
        int t = frame - WALL_SPAWN_DELAY;

        // Row index i "spawns" at the top edge (y = -BLOCKHEIGHT) at
        // t = i * BLOCKHEIGHT, then moves down 1 px/frame afterward:
        //   y(i, t) = t - (i + 1) * BLOCKHEIGHT
        // So a row is only ever drawn once its own spawn time has been
        // reached - it can't appear lower on screen before rows above it
        // have already scrolled past. maxRowIndex is the newest row that
        // has spawned by now; only that row and the ones still on screen
        // above/below it need checking.
        int maxRowIndex = t / BLOCKHEIGHT;
        int rowsToCheck = (BOARD_HEIGHT / BLOCKHEIGHT) + 2; // +2 for smooth scrolling
        int minRowIndex = Math.max(0, maxRowIndex - rowsToCheck + 1);

        for (int i = minRowIndex; i <= maxRowIndex; i++) {
            int y = t - (i + 1) * BLOCKHEIGHT;

            // Skip if row is completely off-screen
            if (y > BOARD_HEIGHT || y < -BLOCKHEIGHT) {
                continue;
            }

            // Loop back to the start of the generated MAP once we've
            // scrolled through all of it, so the level repeats.
            int mapRow = i % MAP.length;

            // Build a Wall tile for each solid, non-destroyed column in this row
            for (int col = 0; col < MAP[mapRow].length; col++) {
                if (MAP[mapRow][col] == 1 && !wallDestroyed[mapRow][col]) {
                    int x = col * BLOCKWIDTH;

                    Wall wall = new Wall(x, y, BLOCKWIDTH, BLOCKHEIGHT);
                    wall.setMapCell(mapRow, col);
                    walls.add(wall);
                }
            }
        }
    }

    // Draws a single wall tile as a solid block with a border, so it reads
    // clearly as level geometry (distinct from the enemy/player/powerup sprites).
    // Draws a single wall tile as an armored "bunker panel" - a beveled
    // block with corner rivets, fitting a Space Invaders sci-fi look
    // instead of a flat gray rectangle.
    private void drawWallTile(Graphics g, int x, int y, int width, int height) {
        // Base panel (teal-green armor plating)
        g.setColor(new Color(40, 180, 150));
        g.fillRect(x, y, width, height);

        // Beveled highlight edge (top-left), gives a raised 3D look
        g.setColor(new Color(150, 255, 220));
        g.fillRect(x, y, width, 3);
        g.fillRect(x, y, 3, height);

        // Beveled shadow edge (bottom-right)
        g.setColor(new Color(10, 90, 70));
        g.fillRect(x, y + height - 3, width, 3);
        g.fillRect(x + width - 3, y, 3, height);

        // Corner rivets/bolts for an armored panel feel
        g.setColor(new Color(20, 60, 50));
        int boltSize = 4;
        g.fillOval(x + 4, y + 4, boltSize, boltSize);
        g.fillOval(x + width - 8, y + 4, boltSize, boltSize);
        g.fillOval(x + 4, y + height - 8, boltSize, boltSize);
        g.fillOval(x + width - 8, y + height - 8, boltSize, boltSize);
    }

    // private void drawStarCluster(Graphics g, int x, int y, int width, int height) {
    //     // Set star color to white
    //     g.setColor(Color.WHITE);
    //
    //     // Draw multiple stars in a cluster pattern
    //     // Main star (larger)
    //     int centerX = x + width / 2;
    //     int centerY = y + height / 2;
    //     g.fillOval(centerX - 2, centerY - 2, 4, 4);
    //
    //     // Smaller surrounding stars
    //     g.fillOval(centerX - 15, centerY - 10, 2, 2);
    //     g.fillOval(centerX + 12, centerY - 8, 2, 2);
    //     g.fillOval(centerX - 8, centerY + 12, 2, 2);
    //     g.fillOval(centerX + 10, centerY + 15, 2, 2);
    //
    //     // Tiny stars for more detail
    //     g.fillOval(centerX - 20, centerY + 5, 1, 1);
    //     g.fillOval(centerX + 18, centerY - 15, 1, 1);
    //     g.fillOval(centerX - 5, centerY - 18, 1, 1);
    //     g.fillOval(centerX + 8, centerY + 20, 1, 1);
    // }

    private void drawAliens(Graphics g) {

        for (Enemy enemy : enemies) {

            if (enemy.isVisible()) {

                g.drawImage(enemy.getImage(), enemy.getX(), enemy.getY(), this);
            }

            if (enemy.isDying()) {

                enemy.die();
            }
        }
    }

    private void drawPowreUps(Graphics g) {

        for (PowerUp p : powerups) {

            if (p.isVisible()) {

                g.drawImage(p.getImage(), p.getX(), p.getY(), this);
            }

            if (p.isDying()) {

                p.die();
            }
        }
    }

    private void drawPlayer(Graphics g) {

        if (player.isVisible()) {

            g.drawImage(player.getImage(), player.getX(), player.getY(), this);
        }

        if (player.isDying()) {

            player.die();
            inGame = false;
        }
    }

    private void drawShot(Graphics g) {

        for (Shot shot : shots) {

            if (shot.isVisible()) {
                g.drawImage(shot.getImage(), shot.getX(), shot.getY(), this);
            }
        }
    }

    // private void drawBombing(Graphics g) {
    //
    //     // for (Enemy e : enemies) {
    //     //     Enemy.Bomb b = e.getBomb();
    //     //     if (!b.isDestroyed()) {
    //     //         g.drawImage(b.getImage(), b.getX(), b.getY(), this);
    //     //     }
    //     // }
    // }

    private void drawExplosions(Graphics g) {

        List<Explosion> toRemove = new ArrayList<>();

        for (Explosion explosion : explosions) {

            if (explosion.isVisible()) {
                g.drawImage(explosion.getImage(), explosion.getX(), explosion.getY(), this);
                explosion.visibleCountDown();
                if (!explosion.isVisible()) {
                    toRemove.add(explosion);
                }
            }
        }

        explosions.removeAll(toRemove);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        g.setColor(Color.black);
        g.fillRect(0, 0, d.width, d.height);

        g.setColor(Color.white);
        g.drawString("FRAME: " + frame, 10, 10);

        g.setColor(Color.green);

        if (inGame) {

            drawMap(g);  // Draw background stars first
            drawExplosions(g);
            drawPowreUps(g);
            drawAliens(g);
            drawPlayer(g);
            drawShot(g);

        } else {

            if (timer.isRunning()) {
                timer.stop();
            }

            gameOver(g);
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void gameOver(Graphics g) {

        g.setColor(Color.black);
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);

        g.setColor(new Color(0, 32, 48));
        g.fillRect(50, BOARD_WIDTH / 2 - 30, BOARD_WIDTH - 100, 50);
        g.setColor(Color.white);
        g.drawRect(50, BOARD_WIDTH / 2 - 30, BOARD_WIDTH - 100, 50);

        var small = new Font("Helvetica", Font.BOLD, 14);
        var fontMetrics = this.getFontMetrics(small);

        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(message, (BOARD_WIDTH - fontMetrics.stringWidth(message)) / 2,
                BOARD_WIDTH / 2);
    }

    private void update() {

        // Rebuild the visible wall tiles for this frame first, so that
        // collision checks later in this method (added in a following
        // step) and drawMap() during repaint both use up-to-date data.
        updateWalls();

        // Check enemy spawn
        // TODO this approach can only spawn one enemy at a frame
        SpawnDetails sd = spawnMap.get(frame);
        if (sd != null) {
            // Create a new enemy based on the spawn details
            switch (sd.type) {
                case "Alien1":
                    Enemy enemy = new Alien1(sd.x, sd.y);
                    enemies.add(enemy);
                    break;
                // Add more cases for different enemy types if needed
                case "Alien2":
                    // Enemy enemy2 = new Alien2(sd.x, sd.y);
                    // enemies.add(enemy2);
                    break;
                case "PowerUp-SpeedUp":
                    // Handle speed up item spawn
                    PowerUp speedUp = new SpeedUp(sd.x, sd.y);
                    powerups.add(speedUp);
                    break;
                default:
                    System.out.println("Unknown enemy type: " + sd.type);
                    break;
            }
        }

        if (deaths == NUMBER_OF_ALIENS_TO_DESTROY) {
            inGame = false;
            timer.stop();
            message = "Game won!";
        }

        // player
        player.act();

        // Player vs Wall collision: ship explodes on contact with a wall.
        // Reuses the same dying/explosion mechanism already handled in
        // drawPlayer() (isDying() -> die() + inGame = false), which was
        // originally built for the old bomb-collision system - this just
        // triggers that existing flow via wall contact instead.
        if (player.isVisible()) {
            int playerX = player.getX();
            int playerY = player.getY();

            for (Wall wall : walls) {
                int wallX = wall.getX();
                int wallY = wall.getY();
                int wallWidth = wall.getWidth();
                int wallHeight = wall.getHeight();

                if (playerX < wallX + wallWidth
                        && playerX + PLAYER_WIDTH > wallX
                        && playerY < wallY + wallHeight
                        && playerY + PLAYER_HEIGHT > wallY) {

                    var ii = new ImageIcon(IMG_EXPLOSION);
                    player.setImage(ii.getImage());
                    player.setDying(true);

                    AudioPlayer.playOnce(PLAYER_EXPLODE_SOUND);
                    break; // ship is already exploding, no need to keep checking
                }
            }
        }

        // Power-ups
        for (PowerUp powerup : powerups) {
            if (powerup.isVisible()) {
                powerup.act();
                if (powerup.collidesWith(player)) {
                    powerup.upgrade(player);
                }
            }
        }

        // Enemies
        for (Enemy enemy : enemies) {
            if (enemy.isVisible()) {
                enemy.act(direction);
            }
        }

        // shot
        List<Shot> shotsToRemove = new ArrayList<>();
        List<Wall> wallsToRemove = new ArrayList<>();
        for (Shot shot : shots) {

            if (shot.isVisible()) {
                int shotX = shot.getX();
                int shotY = shot.getY();

                for (Enemy enemy : enemies) {
                    // Collision detection: shot and enemy
                    int enemyX = enemy.getX();
                    int enemyY = enemy.getY();

                    if (enemy.isVisible() && shot.isVisible()
                            && shotX >= (enemyX)
                            && shotX <= (enemyX + ALIEN_WIDTH)
                            && shotY >= (enemyY)
                            && shotY <= (enemyY + ALIEN_HEIGHT)) {

                        var ii = new ImageIcon(IMG_EXPLOSION);
                        enemy.setImage(ii.getImage());
                        enemy.setDying(true);
                        explosions.add(new Explosion(enemyX, enemyY));
                        deaths++;
                        shot.die();
                        shotsToRemove.add(shot);
                    }
                }

                // Collision detection: shot and wall
                // Mirrors the shot/enemy check above. On a hit, the wall's
                // origin MAP cell is permanently marked destroyed (so it
                // stays gone across frames/scrolling), a destroy sound
                // plays, and the shot is consumed just like hitting an
                // enemy.
                for (Wall wall : walls) {
                    int wallX = wall.getX();
                    int wallY = wall.getY();
                    int wallWidth = wall.getWidth();
                    int wallHeight = wall.getHeight();

                    if (shot.isVisible()
                            && shotX >= (wallX)
                            && shotX <= (wallX + wallWidth)
                            && shotY >= (wallY)
                            && shotY <= (wallY + wallHeight)) {

                        wallDestroyed[wall.getMapRow()][wall.getMapCol()] = true;
                        wallsToRemove.add(wall);

                        AudioPlayer.playOnce(WALL_BREAK_SOUND);

                        shot.die();
                        shotsToRemove.add(shot);
                        break; // this shot is used up, stop checking other walls
                    }
                }

                int y = shot.getY();
                // y -= 4;
                y -= 20;

                if (y < 0) {
                    shot.die();
                    shotsToRemove.add(shot);
                } else {
                    shot.setY(y);
                }
            }
        }
        shots.removeAll(shotsToRemove);
        walls.removeAll(wallsToRemove);

        // enemies
        // for (Enemy enemy : enemies) {
        //     int x = enemy.getX();
        //     if (x >= BOARD_WIDTH - BORDER_RIGHT && direction != -1) {
        //         direction = -1;
        //         for (Enemy e2 : enemies) {
        //             e2.setY(e2.getY() + GO_DOWN);
        //         }
        //     }
        //     if (x <= BORDER_LEFT && direction != 1) {
        //         direction = 1;
        //         for (Enemy e : enemies) {
        //             e.setY(e.getY() + GO_DOWN);
        //         }
        //     }
        // }
        // for (Enemy enemy : enemies) {
        //     if (enemy.isVisible()) {
        //         int y = enemy.getY();
        //         if (y > GROUND - ALIEN_HEIGHT) {
        //             inGame = false;
        //             message = "Invasion!";
        //         }
        //         enemy.act(direction);
        //     }
        // }
        // bombs - collision detection
        // Bomb is with enemy, so it loops over enemies
        /*
        for (Enemy enemy : enemies) {

            int chance = randomizer.nextInt(15);
            Enemy.Bomb bomb = enemy.getBomb();

            if (chance == CHANCE && enemy.isVisible() && bomb.isDestroyed()) {

                bomb.setDestroyed(false);
                bomb.setX(enemy.getX());
                bomb.setY(enemy.getY());
            }

            int bombX = bomb.getX();
            int bombY = bomb.getY();
            int playerX = player.getX();
            int playerY = player.getY();

            if (player.isVisible() && !bomb.isDestroyed()
                    && bombX >= (playerX)
                    && bombX <= (playerX + PLAYER_WIDTH)
                    && bombY >= (playerY)
                    && bombY <= (playerY + PLAYER_HEIGHT)) {

                var ii = new ImageIcon(IMG_EXPLOSION);
                player.setImage(ii.getImage());
                player.setDying(true);
                bomb.setDestroyed(true);
            }

            if (!bomb.isDestroyed()) {
                bomb.setY(bomb.getY() + 1);
                if (bomb.getY() >= GROUND - BOMB_HEIGHT) {
                    bomb.setDestroyed(true);
                }
            }
        }
         */
    }

    private void doGameCycle() {
        frame++;
        update();
        repaint();
    }

    private class GameCycle implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            doGameCycle();
        }
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyReleased(KeyEvent e) {
            player.keyReleased(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            System.out.println("Scene2.keyPressed: " + e.getKeyCode());

            player.keyPressed(e);

            int x = player.getX();
            int y = player.getY();

            int key = e.getKeyCode();

            if (key == KeyEvent.VK_SPACE && inGame) {
                System.out.println("Shots: " + shots.size());
                if (shots.size() < 4) {
                    // Create a new shot and add it to the list
                    Shot shot = new Shot(x, y);
                    shots.add(shot);
                    AudioPlayer.playOnce("src/audio/fire.wav");
                }
            }

        }
    }
}