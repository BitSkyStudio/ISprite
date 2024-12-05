package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public abstract class Editor implements InputProcessor {

    protected OrthographicCamera camera;
    protected ShapeRenderer shapeRenderer;
    protected Stage stage;

    public Editor() {
        this.camera = new OrthographicCamera(1920, 1080);
        this.shapeRenderer = new ShapeRenderer();
        this.stage = new Stage();
    }
    public void render(){
        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            camera.position.add(-Gdx.input.getDeltaX()*2f, Gdx.input.getDeltaY()*2f, 0);
        }
        shapeRenderer.setAutoShapeType(true);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        stage.act();
        stage.draw();
    }
    public void resize(int width, int height){
        this.camera.setToOrtho(false, width, height);
        this.stage.getViewport().update(width, height);
    }
    public void dispose(){
        shapeRenderer.dispose();
        stage.dispose();
    }

    @Override
    public boolean keyDown(int i) {
        return stage.keyDown(i);
    }
    @Override
    public boolean keyUp(int i) {
        return stage.keyUp(i);
    }
    @Override
    public boolean keyTyped(char c) {
        return stage.keyTyped(c);
    }
    @Override
    public boolean touchDown(int i, int i1, int i2, int i3) {
        return stage.touchDown(i, i1, i2, i3);
    }
    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return stage.touchUp(i, i1, i2, i3);
    }
    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return stage.touchCancelled(i, i1, i2, i3);
    }
    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return stage.touchDragged(i, i1, i2);
    }
    @Override
    public boolean mouseMoved(int i, int i1) {
        return stage.mouseMoved(i, i1);
    }
    @Override
    public boolean scrolled(float v, float v1) {
        return stage.scrolled(v, v1);
    }
}
