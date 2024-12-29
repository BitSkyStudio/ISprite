package com.github.bitsky.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.github.bitsky.AnimationEditor;
import com.github.bitsky.AnimationTrack;
import com.github.bitsky.ISpriteMain;
import com.github.bitsky.SpriteAnimation;

import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Math.round;

public class DedicatedKeyFrameWindow extends Window {
    public static final float TIME_SUB_DIVISION = 64;

    private SpriteAnimation spriteAnimation;
    private ArrayList<KeyframeRow> keyframeRows = new ArrayList<>();

    private AnimationEditor animationEditor;

    private float animationStepTime;

    public DedicatedKeyFrameWindow(String title, SpriteAnimation animation, AnimationEditor animationEditor) {
        super(title, ISpriteMain.getSkin());
        this.spriteAnimation = animation;

        this.setResizable(true);
        this.left();

        this.animationEditor = animationEditor;
/*
        AnimationTrack.PropertyTrack<Float> propertyTrack = new AnimationTrack.PropertyTrack<>((first, second, t) -> second);
        propertyTrack.addKeyframe(0, 3f);
        propertyTrack.addKeyframe(3, 3f);
        propertyTrack.addKeyframe(1f, 0f);

        this.addKeyFrameRow(new KeyframeRow(propertyTrack, "TEST TRACK"));*/
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
    }

    private void addKeyFrameRow(KeyframeRow keyframeRow) {

        for (KeyframeRow keyframeRow1 : keyframeRows)
            if (keyframeRow1.propertyTrack.equals(keyframeRow.propertyTrack))
                return;

        // this.left();
        this.add(keyframeRow);
        this.row();
        this.keyframeRows.add(keyframeRow);
    }

    private void refreshTracks(UUID onlyUUID) {
        AnimationTrack animationTrack = this.spriteAnimation.getTrack(onlyUUID);
        String boneName = ISpriteMain.getInstance().sprite.bones.get(onlyUUID).name;
        addKeyFrameRow(new KeyframeRow(animationTrack.translations, "Translation(" + boneName + ")"));
        addKeyFrameRow(new KeyframeRow(animationTrack.scales, "Scale(" + boneName + ")"));
        addKeyFrameRow(new KeyframeRow(animationTrack.rotations, "Rotation(" + boneName + ")"));
    }

    private void clearTracks() {
        this.keyframeRows.forEach(this::removeActor);
        this.keyframeRows.clear();
    }

    public void setAnimationStepTime(float animationStepTime) {
        this.animationStepTime = animationStepTime;
    }

    public void setAnimation(SpriteAnimation animation, UUID id) {
        this.spriteAnimation = animation;
        this.clearTracks();
        this.refreshTracks(id);
    }

    private class KeyframeMarker {

        private KeyframeRow keyframeRow;

        private float parentX;
        private float parentY;

        final Rectangle rectangle;

        private float time;

        public KeyframeMarker(KeyframeRow row, float time) {
            this.keyframeRow = row;
            this.time = time;
            this.rectangle = new Rectangle();
        }

        public void draw(ShapeRenderer shapeRenderer, float x, float y) {
            shapeRenderer.setColor(mouseColliding() ? Color.valueOf("CE78AD") : Color.valueOf("893168"));
            shapeRenderer.circle(x + time * TIME_SUB_DIVISION, y + 20, 10);

            this.parentX = x;
            this.parentY = y;
        }

        public boolean mouseColliding() {
            float x = parentX + time * TIME_SUB_DIVISION - 10;
            float y = parentY + 10;
            float width = 20;
            float height = 20;
            this.rectangle.set(x, y, width, height);
            return this.rectangle.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        }

        public void changeTime(float time) {
            if (time < 0)
                time = 0;
            if(this.keyframeRow.propertyTrack.track.containsKey(time))
                return;
            this.keyframeRow.propertyTrack.modifyKeyframe(this.time, time);
            this.time = time;
            DedicatedKeyFrameWindow.this.getTitleLabel().setText("TIME: (S)" + time);
        }
    }

    private class KeyframeRow extends Actor {

        public static final float HEIGHT = 40;

        private final AnimationTrack.PropertyTrack<?> propertyTrack;
        private final BitmapFont bitmapFont;
        private final ShapeRenderer shapeRenderer;

        private final ArrayList<KeyframeMarker> markers;

        private KeyframeMarker mouseDragMarker;

        private boolean mouseHovering;

        private float x;
        private float y;
        private final String trackName;

        public KeyframeRow(AnimationTrack.PropertyTrack<?> propertyTrack, String trackName) {
            this.propertyTrack = propertyTrack;
            this.shapeRenderer = new ShapeRenderer();
            this.markers = new ArrayList<>();

            // setWidth(1000);
            setHeight(HEIGHT);

            this.trackName = trackName;
            this.bitmapFont = new BitmapFont();

            this.updateMarkers();
        }

        private void updateMarkers() {
            // this.markers.clear();

            this.propertyTrack.track.forEach((time, value) -> {
                if (this.markers.stream().filter(marker -> marker.time == time).findAny().isEmpty())
                    this.markers.add(new KeyframeMarker(KeyframeRow.this, time));
            });
        }

        @Override
        public Actor hit(float x, float y, boolean touchable) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                this.markers.forEach(marker -> {
                    if (marker.mouseColliding())
                        this.mouseDragMarker = marker;
                });
                if(this.mouseDragMarker == null){
                    animationEditor.time = (x - (this.getParent().getX() + getX() + 200)) / TIME_SUB_DIVISION;
                    animationEditor.playing = false;
                }
            }
            this.mouseHovering = true;
            return super.hit(x, y, touchable);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (DedicatedKeyFrameWindow.this.animationEditor.isPlaying()) {
                DedicatedKeyFrameWindow.this.getTitleLabel().setText("PLAYBACK " + animationStepTime);
            }

            this.updateMarkers();

            x = this.getParent().getX() + getX() + 200;
            y = this.getParent().getY() + getY();

            if (this.mouseDragMarker != null) {
                this.mouseDragMarker.changeTime((Gdx.input.getX() - x) / TIME_SUB_DIVISION);

                if(Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)){
                    this.mouseDragMarker.keyframeRow.propertyTrack.track.remove(this.mouseDragMarker.time);
                    this.mouseDragMarker.keyframeRow.markers.remove(mouseDragMarker);
                    this.mouseDragMarker = null;
                }
                /*if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
                    AnimationTrack.ValueInterpolationPair pair = this.mouseDragMarker.keyframeRow.propertyTrack.track.get(this.mouseDragMarker.time);
                    this.mouseDragMarker.keyframeRow.propertyTrack.addKeyframe(this.mouseDragMarker.time, (Object) pair.value, pair.interpolationFunction);
                    this.mouseDragMarker.changeTime(1000);
                }*/
                if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    this.mouseDragMarker = null;
                }
            }

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
                shapeRenderer.setColor(i%2 == 0 ? Color.valueOf("2E1C2B") : Color.valueOf("191018"));
                shapeRenderer.rect(x + i* TIME_SUB_DIVISION, y, TIME_SUB_DIVISION, getHeight());
            }

            this.markers.forEach(marker -> marker.draw(shapeRenderer, x, y));

            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rectLine(
                (x + animationStepTime*TIME_SUB_DIVISION), y,
                (x + animationStepTime*TIME_SUB_DIVISION), y+getHeight(),
                4
            );

            // draw box under the track name
            shapeRenderer.setColor(Color.valueOf("4A1942"));
            shapeRenderer.rect(x-200, y, 200, getHeight());

            shapeRenderer.end();
            batch.begin();

            this.bitmapFont.setColor(Color.valueOf("EAEAEA"));
            this.bitmapFont.draw(batch, this.trackName, getX(), getY() + getHeight() / 2f);

            this.mouseHovering = false;
        }
    }
}
