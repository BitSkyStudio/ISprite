package com.github.bitsky.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.github.bitsky.AnimationTrack;
import com.github.bitsky.SpriteAnimation;

public class DedicatedKeyFrameWindow extends Window {
    private SpriteAnimation spriteAnimation;

    public DedicatedKeyFrameWindow(String title, SpriteAnimation animation) {
        super(title, new Skin(Gdx.files.internal("skin/uiskin.json")));
        this.spriteAnimation = animation;

        this.spriteAnimation.boneTracks.values().forEach(animationTrack -> {
            this.add(new KeyframeRow(animationTrack.rotations));
        });
        this.left();
        this.add(new KeyframeRow(null));
        this.row();
        this.add(new KeyframeRow(null));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
    }

    private class KeyframeRow extends Actor {

        private AnimationTrack.PropertyTrack<?> propertyTrack;
        private BitmapFont bitmapFont;

        private ShapeRenderer shapeRenderer;

        public KeyframeRow(AnimationTrack.PropertyTrack<?> propertyTrack) {
            this.propertyTrack = propertyTrack;
            this.shapeRenderer = new ShapeRenderer();
            setWidth(1000);
            setHeight(40);

            this.bitmapFont = new BitmapFont();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);

            this.setWidth(this.getParent().getWidth());

            batch.end();

            // shapeRenderer.getRenderer().flush();
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.setAutoShapeType(true);
            shapeRenderer.begin();
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);

            float x = this.getParent().getX() + getX() + 200;
            float y = this.getParent().getY() + getY();

            shapeRenderer.setColor(Color.valueOf("B7B6C1"));
            shapeRenderer.rect(x-200, y, 200, getHeight()-5);

            for (int i = 0; i < getWidth() / 64; i ++) {
                shapeRenderer.setColor(i%2 == 0 ? Color.valueOf("464655") : Color.valueOf("94958B"));
                shapeRenderer.rect(x + i*64, y, 64, getHeight() - 5);
            }

            // shapeRenderer.rect(getX(), getY(), getWidth(), getHeight());
            shapeRenderer.end();
            batch.begin();

            this.bitmapFont.draw(batch, "KeyFrames", x-200, getY() + getHeight() /2f);
        }
    }
}
