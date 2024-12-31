package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.HashMap;
import java.util.UUID;

public class StateMachineEditor extends Editor{
    public final AnimationStateMachine stateMachine;
    public StateMachineEditor(AnimationStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
    private UUID moving;
    private UUID connecting;
    @Override
    public void render() {
        super.render();
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);

        shapeRenderer.begin();
        float radius = 50;
        for(AnimationStateMachine.State state : stateMachine.states.values()){
            HashMap<UUID,Integer> offsets = new HashMap<>();
            for(AnimationStateMachine.StateTransition transition : state.transitions){
                AnimationStateMachine.State targetState = stateMachine.states.get(transition.target);
                Vector2 diff = targetState.position.cpy().sub(state.position).setLength(radius*1.05f);
                int offset = offsets.getOrDefault(transition.target, 0);
                float arrowSize = 10;
                Vector2 perpShift = diff.cpy().rotate90(1).setLength(arrowSize*(0.5f+offset));
                Vector2 first = diff.cpy().add(state.position).add(perpShift);
                Vector2 second = targetState.position.cpy().sub(diff).add(perpShift);
                if(Intersector.distanceSegmentPoint(first, second, worldMouse) < arrowSize/2){
                    shapeRenderer.setColor(Color.RED);
                } else {
                    shapeRenderer.setColor(Color.WHITE);
                }
                AnimatedSpritePose.drawArrow(shapeRenderer, first, second, arrowSize);
                offsets.put(transition.target, offset+1);
            }
            if(worldMouse.dst(state.position) < radius){
                shapeRenderer.setColor(Color.RED);
                if(Gdx.input.isKeyJustPressed(Input.Keys.R)){
                    TextField input = new TextField(state.name, ISpriteMain.getSkin());
                    Dialog dialog = new Dialog("Enter new name", ISpriteMain.getSkin(), "dialog") {
                        public void result(Object obj) {
                            if (obj instanceof String) {
                                state.name = input.getText();
                            }
                        }
                    };
                    dialog.setMovable(false);
                    dialog.button("Cancel");
                    dialog.button("Ok", "");
                    dialog.getContentTable().add(input);
                    dialog.show(stage);
                }
                if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
                    moving = state.id;
                }
                if(Gdx.input.isKeyJustPressed(Input.Keys.T)){
                    connecting = state.id;
                }
                if(!Gdx.input.isKeyPressed(Input.Keys.T)){
                    if(connecting != null && !connecting.equals(state.id)){
                        AnimationStateMachine.State original = stateMachine.states.get(connecting);
                        original.addTransition(state);
                    }
                }
            } else {
                shapeRenderer.setColor(Color.WHITE);
            }
            shapeRenderer.circle(state.position.x, state.position.y, radius);
        }
        shapeRenderer.end();
        spriteBatch.begin();
        for(AnimationStateMachine.State state : stateMachine.states.values()){
            BitmapFont font = ISpriteMain.getSkin().getFont("default-font");
            font.setColor(Color.WHITE);
            GlyphLayout glyphLayout = new GlyphLayout();
            glyphLayout.setText(font, state.name);
            font.draw(spriteBatch, state.name, state.position.x-glyphLayout.width/2, state.position.y+glyphLayout.height/2);
        }
        spriteBatch.end();
        if(Gdx.input.isKeyJustPressed(Input.Keys.G)){
            stateMachine.addState(worldMouse.cpy());
        }
        if(!Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
            moving = null;
        } else if(moving != null) {
            stateMachine.states.get(moving).position.add(ISpriteMain.getMouseDeltaX(), -ISpriteMain.getMouseDeltaY());
        }
        if(!Gdx.input.isKeyPressed(Input.Keys.T)){
            connecting = null;
        }
    }
}
