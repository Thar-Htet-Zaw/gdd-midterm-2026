package gdd.sprite;

import static gdd.Global.*;
import javax.swing.ImageIcon;

public class Explosion extends Sprite {


    public Explosion(int x, int y) {

        initExplosion(x, y);
    }

    private void initExplosion(int x, int y) {

        this.x = x;
        this.y = y;

        var ii = new ImageIcon(IMG_EXPLOSION);

        // Scale the image to use the global scaling factor
        var scaledImage = ii.getImage().getScaledInstance(ii.getIconWidth() * SCALE_FACTOR,
                ii.getIconHeight() * SCALE_FACTOR,
                java.awt.Image.SCALE_SMOOTH);
        setImage(scaledImage);
    }

    public void act(int direction) {

        // this.x += direction;
    }

    @Override
    public void act() {
        // Required override to satisfy the abstract Sprite.act() contract.
        // Explosions don't move on their own (they just visibly count
        // down via visibleCountDown(), handled in Scene1). Left empty.
    }


}