package com.github.bitsky.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.github.bitsky.AnimationTrack;
import com.github.bitsky.SpriteAnimation;

import java.util.ArrayList;

import static java.lang.Math.round;

public class DedicatedKeyFrameWindow extends Window {
    private SpriteAnimation spriteAnimation;
    private ArrayList<KeyframeRow> keyframeRows = new ArrayList<>();

    private float animationStepTime;

    public DedicatedKeyFrameWindow(String title, SpriteAnimation animation) {
        super(title, new Skin(Gdx.files.internal("skin/uiskin.json")));
        this.spriteAnimation = animation;

        this.setResizable(true);
        this.left();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            refreshTracks();
        }

    }

    private void addKeyFrameRow(KeyframeRow keyframeRow) {
        // this.left();
        this.add(keyframeRow);
        this.row();
        this.keyframeRows.add(keyframeRow);
    }

    private void refreshTracks() {
        this.getChildren().forEach(child-> {
            if (child instanceof KeyframeRow)
                child.remove();
        });
        this.spriteAnimation.boneTracks.values().forEach(animationTrack -> {
            addKeyFrameRow(new KeyframeRow(animationTrack.translations, "Translation"));
            addKeyFrameRow(new KeyframeRow(animationTrack.scales, "Scale"));
            addKeyFrameRow(new KeyframeRow(animationTrack.rotations, "Rotation"));
        });
    }

    public void setAnimationStepTime(float animationStepTime) {
        this.animationStepTime = animationStepTime;
    }

    private class KeyframeRow extends Actor {

        public static final float TIME_SUB_DIVISION = 64;
        public static final float HEIGHT = 40;

        private final AnimationTrack.PropertyTrack<?> propertyTrack;
        private final BitmapFont bitmapFont;

        private ShapeRenderer shapeRenderer;

        private float x;
        private float y;

        private String trackName;

        public KeyframeRow(AnimationTrack.PropertyTrack<?> propertyTrack, String trackName) {
            this.propertyTrack = propertyTrack;
            this.shapeRenderer = new ShapeRenderer();

            // setWidth(1000);
            setHeight(HEIGHT);

            this.trackName = trackName;

            this.bitmapFont = new BitmapFont();
        }

        private void drawMarker(ShapeRenderer shapeRenderer, float time) {
            shapeRenderer.setColor(Color.valueOf("EDDFEF"));
            shapeRenderer.circle(x + time * TIME_SUB_DIVISION, y + 20, 10);

        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            x = this.getParent().getX() + getX() + 200;
            y = this.getParent().getY() + getY();

            super.draw(batch, parentAlpha);
            this.setWidth(1920);

            // end previous batch
            batch.end();

            // setup shape renderer
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.setAutoShapeType(true);
            shapeRenderer.begin();
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);

            // draw timeline
            for (int i = 0; i < getWidth() / TIME_SUB_DIVISION; i ++) {
                shapeRenderer.setColor(i%2 == 0 ? Color.valueOf("464655") : Color.valueOf("94958B"));
                shapeRenderer.rect(x + i* TIME_SUB_DIVISION, y, TIME_SUB_DIVISION, getHeight());
            }

            // draw property track markers
            if (this.propertyTrack != null) {
                this.propertyTrack.track.forEach((time, val) -> {
                    drawMarker(shapeRenderer, time);
                });
            }

            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rectLine(
                (x + animationStepTime*TIME_SUB_DIVISION), y,
                (x + animationStepTime*TIME_SUB_DIVISION), y+getHeight(),
                4
            );

            // draw box under the track name
            shapeRenderer.setColor(Color.valueOf("B7B6C1"));
            shapeRenderer.rect(x-200, y, 200, getHeight());

            shapeRenderer.end();
            batch.begin();

            this.bitmapFont.setColor(Color.valueOf("464655"));
            this.bitmapFont.draw(batch, this.trackName, x-200, getY() + getHeight() / 2f);
        }
    }
}
