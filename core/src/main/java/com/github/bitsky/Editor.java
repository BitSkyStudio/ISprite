package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

public abstract class Editor {
    protected OrthographicCamera camera;
    protected ShapeRenderer shapeRenderer;
    public Editor() {
        this.camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.shapeRenderer = new ShapeRenderer();
    }
    public void render(){
        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            camera.position.add(-Gdx.input.getDeltaX()*2f, Gdx.input.getDeltaY()*2f, 0);
        }
        shapeRenderer.setAutoShapeType(true);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
    }
    public void resize(int width, int height){
        this.camera.setToOrtho(false, width, height);
    }
    public void dispose(){
        shapeRenderer.dispose();
    }
}
