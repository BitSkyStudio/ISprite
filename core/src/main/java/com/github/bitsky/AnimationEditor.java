package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.github.bitsky.ui.DedicatedKeyFrameWindow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationEditor extends Editor {

    private SpriteAnimation animation;
    private UUID movingId;
    public float time;
    public boolean playing;

    private DedicatedKeyFrameWindow keyFramesWindow;

    public SelectBox<EInterpolationFunction> functionSelectBox;
    public Label functionSelectLabel;

    public AnimationEditor(SpriteAnimation animation) {
        this.movingId = null;
        this.animation = animation;
        this.time = 0;
        this.playing = false;

        this.createUI();
    }

    /**
     * Creates all ui related elements and listeners
     */
    private void createUI() {
        Skin skin = new Skin(Gdx.files.internal("./skin/uiskin.json"));

        functionSelectBox = new SelectBox<>(skin);
        functionSelectBox.setItems(EInterpolationFunction.values());
        functionSelectBox.setWidth(300);

        this.stage.addActor(functionSelectBox);
        functionSelectLabel = new Label("Int. function of current keyframe", skin);

        this.stage.addActor(functionSelectLabel);

        // ** create window **
        this.keyFramesWindow = new DedicatedKeyFrameWindow("Key Frames", this.animation, this);
        keyFramesWindow.setWidth(Gdx.graphics.getWidth());
        keyFramesWindow.setHeight(this.camera.viewportHeight / 5);

        this.stage.addActor(keyFramesWindow);

    }

    @Override
    public void resize(int width, int height) {
        functionSelectLabel.setPosition(0, height-functionSelectLabel.getHeight());
        functionSelectBox.setPosition(0, height-functionSelectBox.getHeight() - 20);
        super.resize(width, height);
    }

    @Override
    public void render() {
        super.render();
        this.keyFramesWindow.setAnimationStepTime(this.time);
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
        AnimatedSpritePose pose = animation.getPose(time);
        HashMap<UUID, Transform> transforms = pose.getBoneTransforms(sprite, new Transform().lock());
        UUID moused = null;
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);
        for(Map.Entry<UUID, Transform> entry : transforms.entrySet()){
            if(entry.getValue().translation.dst(worldMouse) < 10f){
                moused = entry.getKey();
            }
        }

        polygonSpriteBatch.begin();
        for(VertexedImage image : sprite.images)
            image.draw(polygonSpriteBatch, pose);
        polygonSpriteBatch.end();

        shapeRenderer.begin();
        UUID finalMoused = moused;
        pose.drawDebugBones(sprite, shapeRenderer, uuid -> uuid.equals(finalMoused)?Color.RED:Color.GREEN);
        shapeRenderer.end();

        if(movingId != null){
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
                Transform parentTransform = transforms.get(movingBone.parent);
                AnimationTrack track = animation.getTrack(movingId);
                track.translations.addKeyframe(time, worldMouse.cpy().sub(parentTransform.translation).rotateRad(-parentTransform.rotation), functionSelectBox.getSelected());
                playing = false;
            }
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
            if(movingId == null) {
                movingId = moused;
            }
            else
                movingId = null;

            if (movingId != null)
                this.keyFramesWindow.setAnimation(animation, movingId);
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
            playing = !playing;
        }
        if(animation.getAnimationLength() == 0)
            playing = false;
        if(playing){
            time += Gdx.graphics.getDeltaTime();
            time %= animation.getAnimationLength();
        }

        stage.draw();
    }

    @Override
    public boolean scrolled(float v, float v1) {
        if(movingId != null){
            AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                AnimationTrack track = animation.getTrack(movingId);
                track.rotations.addKeyframe(time, animation.getPose(time).getBoneTransforms(sprite, new Transform().lock()).get(movingId).rotation-v1/10f, EInterpolationFunction.Linear);
                playing = false;
            }
            return true;
        }
        return super.scrolled(v, v1);
    }

    public boolean isPlaying() {
        return playing;
    }
}
