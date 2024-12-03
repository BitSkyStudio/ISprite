package com.github.bitsky;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ISpriteMain extends ApplicationAdapter {
    public AnimatedSprite sprite;
    private SpriteAnimation animation;
    private float time;
    public Editor editor;

    private Skin skin;
    @Override
    public void create() {
        this.skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        this.sprite = new AnimatedSprite();
        AnimatedSpriteBone bone2 = this.sprite.addChildNodeTo(this.sprite.rootBone);
        bone2.baseTransform.translation.set(0, 200);
        AnimatedSpriteBone bone3 = this.sprite.addChildNodeTo(bone2);
        bone3.baseTransform.translation.set(200, 0);
        this.animation = new SpriteAnimation();
        this.animation.boneTracks.put(bone2.id, new AnimationTrack());
        this.animation.boneTracks.get(bone2.id).rotations.addKeyframe(0, 0f);
        this.animation.boneTracks.get(bone2.id).rotations.addKeyframe(5, (float) (Math.PI/2f));
        this.time = 0;
        this.editor = new BoneEditor();
        Gdx.input.setInputProcessor(this.editor);
    }

    @Override
    public void render() {
        this.time += Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        editor.render();
        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
            if(editor instanceof BoneEditor){
                editor = new AnimationEditor();
            } else {
                editor = new BoneEditor();
            }
            Gdx.input.setInputProcessor(editor);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.editor.resize(width, height);
    }

    @Override
    public void dispose() {
        editor.dispose();
    }

    public static ISpriteMain getInstance(){
        return (ISpriteMain) Gdx.app.getApplicationListener();
    }
    public static Skin getSkin(){
        return ((ISpriteMain) Gdx.app.getApplicationListener()).skin;
    }
}
