package com.github.bitsky;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ISpriteMain extends ApplicationAdapter {
    private AnimatedSprite sprite;
    private ShapeRenderer shapeRenderer;
    private Camera camera;
    @Override
    public void create() {
        this.camera = new OrthographicCamera(100, 100);
        this.shapeRenderer = new ShapeRenderer();
        this.sprite = new AnimatedSprite();
        AnimatedSpriteBone bone2 = this.sprite.addChildNodeTo(this.sprite.rootBone);
        bone2.baseTransform.translation.set(0, 2);
        AnimatedSpriteBone bone3 = this.sprite.addChildNodeTo(bone2);
        bone3.baseTransform.translation.set(2, 0);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin();
        sprite.drawDebugBones(shapeRenderer);
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
