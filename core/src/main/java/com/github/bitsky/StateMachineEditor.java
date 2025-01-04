package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class StateMachineEditor extends Editor{
    public final AnimationStateMachine stateMachine;
    private UUID moving;
    private UUID connecting;

    private Table rightClickMenu;
    private StateDistancePair rightClickPair;
    private final ArrayList<ActionPair> actions;

    public StateMachineEditor(AnimationStateMachine stateMachine) {
        this.actions = new ArrayList<>();
        this.stateMachine = stateMachine;
        this.rightClickMenu = null;

        actions.add(new ActionPair("Create State", () -> {
            Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);
            stateMachine.addState(worldMouse.cpy());
        }));

        actions.add(new ActionPair("Delete State", () -> {
            if (rightClickPair.distance < 50) {
                AnimationStateMachine.State removeState = rightClickPair.state;
                if(!stateMachine.startState.equals(removeState.id)) {
                    stateMachine.removeState(removeState.id);
                }
            }
        }));

        actions.add(new ActionPair("Rename", () -> {
            if (rightClickPair.distance < 50) {
                TextField input = new TextField(rightClickPair.state.name, ISpriteMain.getSkin());
                Dialog dialog = new Dialog("Enter new name", ISpriteMain.getSkin(), "dialog") {
                    public void result(Object obj) {
                        if (obj instanceof String) {
                            rightClickPair.state.name = input.getText();
                        }
                    }
                };
                dialog.setMovable(false);
                dialog.button("Cancel");
                dialog.button("Ok", "");
                dialog.getContentTable().add(input);
                dialog.show(stage);
            }
        }));

        actions.add(new ActionPair("Toggle Is End State", () -> {
            if (rightClickPair.distance < 50) {
                rightClickPair.state.endState = !rightClickPair.state.endState;
            }
        }));

        actions.add(new ActionPair("Begin Connect", () -> {
            if (rightClickPair.distance < 50) {
                connecting = rightClickPair.state.id;
            }
        }));

        actions.add(new ActionPair("Edit", new ActionCallback() {
            @Override
            public void run(AnimationStateMachine.StateTransition stateTransition) {
                TextField blendTime = new TextField(String.valueOf(stateTransition.blendTime), ISpriteMain.getSkin());
                blendTime.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));

                CheckBox requireFinished = new CheckBox("Require Finished", ISpriteMain.getSkin());
                requireFinished.setChecked(stateTransition.requireFinished);

                SelectBox<EInterpolationFunction> functionSelectBox = new SelectBox<>(ISpriteMain.getSkin());
                functionSelectBox.setItems(EInterpolationFunction.values());
                functionSelectBox.setSelected(stateTransition.interpolationFunction);

                Dialog editDialog = new Dialog("Edit", ISpriteMain.getSkin()){
                    @Override
                    protected void result(Object object) {
                        if(object instanceof String){
                            try {
                                stateTransition.blendTime = Float.parseFloat(blendTime.getText());
                            } catch(NumberFormatException e){}
                            stateTransition.requireFinished = requireFinished.isChecked();
                            stateTransition.interpolationFunction = functionSelectBox.getSelected();
                        }
                    }
                };
                editDialog.getContentTable().add(new Label("Blend time: ", ISpriteMain.getSkin()), blendTime).row();
                editDialog.getContentTable().add(new Label("Interpolation: ", ISpriteMain.getSkin()), functionSelectBox).row();
                editDialog.getContentTable().add(requireFinished).row();
                editDialog.button("Ok", "");
                editDialog.button("Cancel");
                editDialog.show(stage);
            }
            @Override public void run() {}
        }).setTransitionAction());

        actions.add(new ActionPair("Remove", new ActionCallback() {
            @Override
            public void run(AnimationStateMachine.StateTransition stateTransition) {
                for (AnimationStateMachine.State state : stateMachine.states.values())
                    state.transitions.remove(stateTransition);
            }

            @Override public void run() {}
        }).setTransitionAction());
    }

    @Override
    public void render() {
        AnimationStateMachine.StateTransition highlightedTransition = null;

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
            ISpriteMain.getInstance().setEditor(ISpriteMain.getInstance().graphEditor);
        }

        super.render();
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);

        shapeRenderer.begin();
        float radius = 50;
        for(AnimationStateMachine.State state : stateMachine.states.values()) {
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

                    highlightedTransition = transition;

                } else {
                    shapeRenderer.setColor(Color.WHITE);
                }
                AnimatedSpritePose.drawArrow(shapeRenderer, first, second, arrowSize);
                offsets.put(transition.target, offset+1);
            }

            if(worldMouse.dst(state.position) < radius){
                shapeRenderer.setColor(Color.RED);

                if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
                    moving = state.id;
                }
            } else {
                if(state.endState){
                    shapeRenderer.setColor(Color.YELLOW);
                } else {
                    shapeRenderer.setColor(Color.WHITE);
                }
            }
            shapeRenderer.circle(state.position.x, state.position.y, radius);
        }
        shapeRenderer.end();
        spriteBatch.begin();
        for(AnimationStateMachine.State state : stateMachine.states.values()){
            BitmapFont font = ISpriteMain.getSkin().getFont("default-font");
            if(stateMachine.startState.equals(state.id)){
                font.setColor(Color.GREEN);
            } else {
                font.setColor(Color.WHITE);
            }
            GlyphLayout glyphLayout = new GlyphLayout();
            glyphLayout.setText(font, state.name);
            font.draw(spriteBatch, state.name, state.position.x-glyphLayout.width/2, state.position.y+glyphLayout.height/2);
        }
        spriteBatch.end();

        if(!Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
            moving = null;
        } else if(moving != null) {
            stateMachine.states.get(moving).position.add(ISpriteMain.getMouseDeltaX(), -ISpriteMain.getMouseDeltaY());
        }
        if(connecting != null){
            shapeRenderer.begin();
            shapeRenderer.setColor(Color.WHITE);
            AnimatedSpritePose.drawArrow(shapeRenderer, stateMachine.states.get(connecting).position, worldMouse, 10);
            shapeRenderer.end();
        }
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && connecting != null){
            StateDistancePair pair = getNearestStateToCursor();
            if (pair.distance < 50) {
                stateMachine.states.get(connecting).addTransition(pair.state);
            }
            connecting = null;
        }

        if ((
            Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) ||
                Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)
            )
            && this.rightClickMenu != null) {
            this.rightClickMenu.remove();
            this.rightClickMenu = null;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {

            final AnimationStateMachine.StateTransition localStateTransition = highlightedTransition;

            if (this.rightClickMenu != null) {
                this.rightClickMenu.remove();
                this.rightClickMenu = null;
            }

            this.rightClickMenu = new Table();

            for (ActionPair actionPair : actions) {
                if (actionPair.isTransitionAction || highlightedTransition != null)
                    continue;

                TextButton actionButton = new TextButton(actionPair.name, ISpriteMain.getSkin());
                actionButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        actionPair.getAction().run();
                        super.clicked(event, x, y);
                    }
                });
                this.rightClickMenu.add(actionButton).row();
            }

            if (highlightedTransition != null) {
                for (ActionPair actionPair : actions.stream().filter(action -> action.isTransitionAction).collect(Collectors.toList())) {
                    TextButton actionButton = new TextButton("Transition: " + actionPair.name, ISpriteMain.getSkin());
                    actionButton.setColor(Color.ORANGE);
                    actionButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            ((ActionCallback) actionPair.getAction()).run(localStateTransition);
                            super.clicked(event, x, y);
                        }
                    });
                    this.rightClickMenu.add(actionButton).row();
                }
            }

            Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            rightClickMenu.setPosition(position.x, position.y);

            this.stage.addActor(rightClickMenu);

            this.rightClickPair = getNearestStateToCursor();
        }

        stage.draw();
    }

    private StateDistancePair getNearestStateToCursor() {
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);

        ArrayList<AnimationStateMachine.State> sortedStates = new ArrayList<>(stateMachine.states.values());
        sortedStates.sort((a, b) -> (int) (a.position.dst(worldMouse) - b.position.dst(worldMouse)));

        final StateDistancePair pair = new StateDistancePair();
        pair.state = sortedStates.get(0);
        pair.distance = pair.state.position.dst(worldMouse);

        return pair;
    }

    private static class StateDistancePair {
        public float distance;
        public AnimationStateMachine.State state;
    }

    private interface ActionCallback extends Runnable {
        void run(AnimationStateMachine.StateTransition stateTransition);
    }

    private static class ActionPair {
        private final String name;
        private final Runnable action;
        private boolean isTransitionAction;

        public ActionPair(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }

        public ActionPair setTransitionAction() {
            this.isTransitionAction = true;
            return this;
        }

        public Runnable getAction() {
            return action;
        }
    }

    @Override
    public void resize(int width, int height) {
        //this.stage.getViewport().setWorldSize(width, width/16f*9);
        super.resize(width, height);
    }
}
