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

        this.left();

        this.spriteAnimation.boneTracks.values().forEach(animationTrack -> {
            this.add(new KeyframeRow(animationTrack.translations, "Translation"));
            this.add(new KeyframeRow(animationTrack.scales, "Scale"));
            this.add(new KeyframeRow(animationTrack.rotations, "Rotation"));
            this.row();
        });
        this.left();

        AnimationTrack.PropertyTrack<Float> testTrack = new AnimationTrack.PropertyTrack<>((first, second, t) -> second);

        testTrack.addKeyframe(3, 10f);
        testTrack.addKeyframe(1, 3f);
        testTrack.addKeyframe(0.5f, 10f);

        this.add(new KeyframeRow(testTrack, "Test Track"));
        this.row();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
    }

    private class KeyframeRow extends Actor {

        private float timeSubDivision = 64;

        private AnimationTrack.PropertyTrack<?> propertyTrack;
        private BitmapFont bitmapFont;

        private ShapeRenderer shapeRenderer;

        private float x;
        private float y;

        private String trackName;

        public KeyframeRow(AnimationTrack.PropertyTrack<?> propertyTrack, String trackName) {
            this.propertyTrack = propertyTrack;
            this.shapeRenderer = new ShapeRenderer();
            setWidth(1000);
            setHeight(40);

            this.trackName = trackName;

            this.bitmapFont = new BitmapFont();
        }

        private void drawMarker(ShapeRenderer shapeRenderer, float time) {
            shapeRenderer.setColor(Color.valueOf("EDDFEF"));
            shapeRenderer.circle(x + time*timeSubDivision, y + 20, 10);

        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            x = this.getParent().getX() + getX() + 200;
            y = this.getParent().getY() + getY();

            super.draw(batch, parentAlpha);

            this.setWidth(this.getParent().getWidth());

            batch.end();

            // shapeRenderer.getRenderer().flush();
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.setAutoShapeType(true);
            shapeRenderer.begin();
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);

            shapeRenderer.setColor(Color.valueOf("B7B6C1"));
            shapeRenderer.rect(x-200, y, 200, getHeight());

            for (int i = 0; i < getWidth() / timeSubDivision; i ++) {
                shapeRenderer.setColor(i%2 == 0 ? Color.valueOf("464655") : Color.valueOf("94958B"));
                shapeRenderer.rect(x + i*timeSubDivision, y, timeSubDivision, getHeight());
            }

            if (this.propertyTrack != null) {
                this.propertyTrack.track.forEach((time, val) -> {
                    drawMarker(shapeRenderer, time);
                });
            }

            shapeRenderer.end();
            batch.begin();

            this.bitmapFont.setColor(Color.valueOf("464655"));
            this.bitmapFont.draw(batch, this.trackName, x-200, getY() + getHeight() / 2f);
        }
    }
}
