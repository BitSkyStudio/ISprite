package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class GraphEditor extends Editor {
    private final Texture linkInputTexture;
    private final Texture linkInputFilledTexture;
    private final Texture linkOutputTexture;

    private HashMap<UUID, GraphNode> nodes;
    private DragAndDrop dragAndDrop;
    private Table rightClickMenu;
    private HashMap<String, Supplier<GraphNode>> nodeTypes;
    private HashMap<UUID,InputProperty> properties;

    private FinalPoseGraphNode finalPoseGraphNode;

    public GraphEditor() {
        this.linkInputTexture = new Texture("link_input.png");
        this.linkInputFilledTexture = new Texture("link_input_filled.png");
        this.linkOutputTexture = new Texture("link_output.png");

        this.dragAndDrop = new DragAndDrop();
        this.nodes = new HashMap<>();

        this.finalPoseGraphNode = addNode(new FinalPoseGraphNode(), Vector2.Zero);

        this.properties = new HashMap<>();

        rightClickMenu = null;

        nodeTypes = new HashMap<>();
        nodeTypes.put("Animated Pose", AnimatedPoseGraphNode::new);
        nodeTypes.put("Blend Pose", BlendPoseGraphNode::new);
        nodeTypes.put("Multiply Pose", MultiplyPoseGraphNode::new);
        nodeTypes.put("Playback Speed", PlaybackSpeedGraphNode::new);
        nodeTypes.put("Add Pose", AddPoseGraphNode::new);
        nodeTypes.put("State Machine", StateGraphNode::new);
    }

    public void removeNode(GraphNode node) {
        node.window.remove();

        this.nodes.remove(node.id, node);
        this.nodes.forEach((nodeKey, nodeValue) -> nodeValue.disconnectAll(node));
    }

    public <T extends GraphNode> T addNode(T node, Vector2 position){
        this.nodes.put(node.id, node);
        this.stage.addActor(node.window);
        node.window.pack();
        node.window.setPosition(position.x, position.y);
        return node;
    }

    @Override
    public void render() {
        super.render();
        finalPoseGraphNode.tick(Gdx.graphics.getDeltaTime());
        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            stage.getCamera().position.add(-ISpriteMain.getMouseDeltaX(), ISpriteMain.getMouseDeltaY(), 0);
        }
        if(Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)){
            if(rightClickMenu != null)
                rightClickMenu.remove();

            this.rightClickMenu = new Table();
            stage.addActor(rightClickMenu);
            Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            rightClickMenu.setPosition(position.x, position.y);
            for(Map.Entry<String, Supplier<GraphNode>> entry : nodeTypes.entrySet()){
                TextButton button = new TextButton(entry.getKey(), ISpriteMain.getSkin());
                button.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                        addNode(entry.getValue().get(), new Vector2(position.x, position.y));
                        if(rightClickMenu != null) {
                            rightClickMenu.remove();
                            rightClickMenu = null;
                        }
                    }
                });
                rightClickMenu.add(button).row();
            }
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)){
            if(rightClickMenu != null) {
                rightClickMenu.remove();
                rightClickMenu = null;
            }
        }

        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        for(GraphNode node : nodes.values()){
            for(Map.Entry<String, UUID> entry : node.inputs.entrySet()){
                Actor firstActor = node.inputActors.get(entry.getKey());
                Actor secondActor = nodes.get(entry.getValue()).outputActor;
                GraphNode secondNode = nodes.get(entry.getValue());

                Vector2 vector1 = firstActor.localToStageCoordinates(new Vector2(firstActor.getWidth()/2, firstActor.getHeight()/2));
                Vector2 vector2 = secondActor.localToStageCoordinates(new Vector2(secondActor.getWidth()/2, secondActor.getHeight()/2));

                // edge fix
                this.shapeRenderer.setColor(Color.valueOf("DA863E"));
                this.shapeRenderer.circle(secondNode.window.getX() + secondNode.window.getWidth(), vector2.y,10);
                shapeRenderer.rectLine(vector1.x, vector1.y, secondNode.window.getX() + secondNode.window.getWidth(), vector2.y, 10, Color.valueOf("A4DDDB"), Color.valueOf("DA863E"));
                // shapeRenderer.line(firstActor.localToStageCoordinates(new Vector2(firstActor.getWidth()/2, firstActor.getHeight()/2)), secondActor.localToStageCoordinates(new Vector2(secondActor.getWidth()/2, secondActor.getHeight()/2)));
            }
        }
        shapeRenderer.end();
        shapeRenderer.setProjectionMatrix(camera.combined);
    }

    public static class ConnectionData{
        public final UUID first;
        public ConnectionData(UUID first) {
            this.first = first;
        }
    }

    public abstract class GraphNode {
        public final UUID id;
        public final Window window;
        public final HashMap<String,UUID> inputs;
        public final HashMap<String,Actor> inputActors;
        public final HashMap<String, TextureRegion> inputRegions;
        public final HashMap<String, HorizontalGroup> inputFields;
        public Actor outputActor;

        public final VerticalGroup verticalGroup;

        public GraphNode(String name, String description, boolean removable) {
            this.id = UUID.randomUUID();
            this.window = new Window(name, ISpriteMain.getSkin());
            this.window.setKeepWithinStage(false);

            this.inputFields = new HashMap<>();
            this.inputRegions = new HashMap<>();
            this.inputs = new HashMap<>();
            this.inputActors = new HashMap<>();
            this.verticalGroup = new VerticalGroup();

            final HorizontalGroup miniToolBar = new HorizontalGroup();
            if (removable) {
                final TextButton removeButton = new TextButton("X", this.window.getSkin());
                removeButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        GraphEditor.this.removeNode(GraphEditor.GraphNode.this);
                        super.clicked(event, x, y);
                    }
                });

                miniToolBar.addActor(removeButton);
            }
            miniToolBar.addActor(new Label(description, this.window.getSkin()));
            this.verticalGroup.addActor(miniToolBar);
            this.verticalGroup.columnLeft();


            if (hasOutput()) {
                HorizontalGroup hgroup = new HorizontalGroup();
                hgroup.addActor(new Label("output", ISpriteMain.getSkin()));
                Actor dragOutput = new Image(new TextureRegion(linkOutputTexture));
                hgroup.addActor(dragOutput);
                dragAndDrop.addSource(new DragAndDrop.Source(dragOutput) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent inputEvent, float v, float v1, int i) {
                        dragOutput.setVisible(false);
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        Image connection = new Image(linkOutputTexture);
                        payload.setDragActor(connection);
                        payload.setObject(new ConnectionData(id));
                        dragAndDrop.setDragActorPosition(dragOutput.getWidth(), -dragOutput.getHeight() / 2);
                        return payload;
                    }

                    @Override
                    public void dragStop(InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        dragOutput.setVisible(true);
                        super.dragStop(event, x, y, pointer, payload, target);
                    }
                });
                verticalGroup.addActor(hgroup);
                outputActor = dragOutput;
            }

            window.add(this.verticalGroup);

        }

        /**
         * remove every connection with specified node
         * @param graphNode
         */
        public void disconnectAll(GraphNode graphNode) {

            ArrayList<String> toRemove = new ArrayList<>();

            for (String key : this.inputs.keySet()) {
                UUID val = this.inputs.get(key);
                if (val.equals(graphNode.id)) {
                    this.inputRegions.get(key).setTexture(linkInputTexture);
                    toRemove.add(key);
                }
            }

            toRemove.forEach(this.inputs::remove);
        }

        public void addInput(String name) {
            HorizontalGroup hgroup = new HorizontalGroup();
            this.inputFields.put(name, hgroup);
            hgroup.setName(name);
            TextureRegion textureRegion = new TextureRegion(linkInputTexture);
            Actor dragInput = new Image(textureRegion);
            this.inputRegions.put(name, textureRegion);
            hgroup.addActor(dragInput);
            hgroup.addActor(new Label(name, ISpriteMain.getSkin()));

            dragInput.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    inputs.remove(name);
                    textureRegion.setTexture(linkInputTexture);
                    super.clicked(event, x, y);
                }
            });

            dragAndDrop.addTarget(new DragAndDrop.Target(dragInput) {
                @Override
                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    return payload.getObject() instanceof ConnectionData;
                }
                @Override
                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    ConnectionData output = (ConnectionData) payload.getObject();
                    inputs.put(name, output.first);
                    textureRegion.setTexture(linkInputFilledTexture);
                }
            });
            verticalGroup.addActor(hgroup);
            this.inputActors.put(name, dragInput);
        }
        public abstract AnimatedSpritePose getOutputPose();
        public void tick(float step){
            for(UUID input : inputs.values()){
                nodes.get(input).tick(step);
            }
        }
        public void reset(){
            for(UUID input : inputs.values()){
                nodes.get(input).reset();
            }
        }
        public boolean isFinished(){
            for(UUID input : inputs.values()){
                if(!nodes.get(input).isFinished())
                    return false;
            }
            return true;
        }
        public boolean hasOutput(){
            return true;
        }
        public AnimatedSpritePose getInput(String input){
            return nodes.get(inputs.get(input)).getOutputPose();
        }
    }
    public class FinalPoseGraphNode extends GraphNode{
        public FinalPoseGraphNode() {
            super("Final Pose", "Pose to be displayed by the animation renderer.", false);
            addInput("Out");
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            throw new IllegalStateException();
        }
        @Override
        public boolean hasOutput() {
            return false;
        }
    }

    public class AnimatedPoseGraphNode extends GraphNode{
        public final SpriteAnimation animation;
        public boolean isLooping;

        public float time;
        public AnimatedPoseGraphNode() {
            super("Animated Pose", "Animated Pose Node", true);

            this.animation = new SpriteAnimation();
            TextButton enterButton = new TextButton("Edit", ISpriteMain.getSkin());
            enterButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ISpriteMain.getInstance().setEditor(new AnimationEditor(animation));
                }
            });
            CheckBox checkBox = new CheckBox("loop", ISpriteMain.getSkin());
            checkBox.setChecked(isLooping);
            checkBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    isLooping = checkBox.isChecked();
                }
            });

            this.verticalGroup.addActor(enterButton);
        }

        @Override
        public void reset() {
            time = 0;
        }
        @Override
        public void tick(float step) {
            time += step;
            time %= animation.getAnimationLength();
        }
        @Override
        public boolean isFinished() {
            if(isLooping)
                return false;
            return time > animation.getAnimationLength();
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return animation.getPose(time);
        }
    }
    public class BlendPoseGraphNode extends GraphNode{
        public float blendValue;
        public BlendPoseGraphNode() {
            super("Blend Pose", "Blends two inputs.", true);
            addInput("Pose1");
            addInput("Pose2");
            this.blendValue = 0.5f;
            this.window.add(new Label("Blend: ", ISpriteMain.getSkin()));
            TextField textField = new TextField(""+blendValue, ISpriteMain.getSkin());
            textField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            textField.setTextFieldListener((textField1, c) -> {
                try {
                    blendValue = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    textField.setText(""+blendValue);
                }
            });
            this.window.add(textField);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose1").lerp(getInput("Pose2"), blendValue);
        }
    }
    public class MultiplyPoseGraphNode extends GraphNode{
        public float multiplyValue;
        public MultiplyPoseGraphNode() {
            super("Multiply Pose", "Multiplies pose by set value.", true);
            addInput("Pose");
            this.multiplyValue = 1f;
            this.window.add(new Label("Multiply: ", ISpriteMain.getSkin()));
            TextField textField = new TextField(""+multiplyValue, ISpriteMain.getSkin());
            textField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            textField.setTextFieldListener((textField1, c) -> {
                try {
                    multiplyValue = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    textField.setText(""+multiplyValue);
                }
            });
            this.window.add(textField);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose").multiply(multiplyValue);
        }
    }
    public class AddPoseGraphNode extends GraphNode{
        public AddPoseGraphNode() {
            super("Add Pose", "Combine two poses.", true);
            addInput("Pose1");
            addInput("Pose2");
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose1").add(getInput("Pose2"));
        }
    }
    public class PlaybackSpeedGraphNode extends GraphNode{
        public float speed;
        public PlaybackSpeedGraphNode() {
            super("Playback Speed", "Changes playback speed.", true);
            addInput("Pose");
            this.speed = 1;
            this.window.add(new Label("Speed: ", ISpriteMain.getSkin()));
            TextField textField = new TextField(""+speed, ISpriteMain.getSkin());
            textField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            textField.setTextFieldListener((textField1, c) -> {
                try {
                    speed = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    textField.setText(""+speed);
                }
            });
        }
        @Override
        public void tick(float step) {
            nodes.get(inputs.get("Pose")).tick(step*speed);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose");
        }
    }
    public class StateGraphNode extends GraphNode {
        public final AnimationStateMachine stateMachine;

        public UUID currentState;
        public int transitionId;
        public float transitionTime;
        public StateGraphNode() {
            super("State Machine", "Carries internal state.", true);
            this.stateMachine = new AnimationStateMachine();
            TextButton enterButton = new TextButton("Edit", ISpriteMain.getSkin());
            enterButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ISpriteMain.getInstance().setEditor(new StateMachineEditor(stateMachine));
                }
            });
            this.verticalGroup.addActor(enterButton);
        }
        @Override
        public void reset() {
            this.currentState = stateMachine.startState;
            this.transitionId = -1;
            getInputByState(currentState).reset();
        }
        @Override
        public void tick(float step) {
            getInputByState(currentState).tick(step);
            if(transitionId != -1){
                AnimationStateMachine.StateTransition transition = stateMachine.states.get(currentState).transitions.get(transitionId);
                transitionTime += step;
                if(transitionTime > transition.blendTime){
                    currentState = transition.target;
                    transitionId = -1;
                }
            } else {
                ArrayList<AnimationStateMachine.StateTransition> transitions = stateMachine.states.get(currentState).transitions;
                for(int i = 0;i < transitions.size();i++){
                    AnimationStateMachine.StateTransition transition = transitions.get(i);
                    boolean failed = false;
                    for(AnimationStateMachine.TransitionCondition condition : transition.conditions){
                        if(!condition.comparator.passes(properties.get(condition.propertyId).value, condition.value)){
                            failed = true;
                            break;
                        }
                    }
                    if(transition.requireFinished && !getInputByState(currentState).isFinished())
                        failed = true;
                    if(!failed){
                        transitionId = i;
                        transitionTime = 0;
                        getInputByState(transition.target).reset();
                        break;
                    }
                }
            }
        }

        @Override
        public boolean isFinished() {
            return getInputByState(currentState).isFinished() && stateMachine.states.get(currentState).endState;
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            GraphNode first = getInputByState(currentState);
            if(transitionId != -1){
                AnimationStateMachine.StateTransition transition = stateMachine.states.get(currentState).transitions.get(transitionId);
                GraphNode second = getInputByState(transition.target);
                return first.getOutputPose().lerp(second.getOutputPose(), transition.interpolationFunction.function.apply(transitionTime/transition.blendTime));
            } else {
                return first.getOutputPose();
            }
        }
        public GraphNode getInputByState(UUID state){
            throw new IllegalStateException("Implement");
        }
    }

    @Override
    public void resize(int width, int height) {
        this.stage.getViewport().setWorldSize(width, width/16f*9);
        super.resize(width, height);
    }

    @Override
    public boolean scrolled(float v, float v1) {

        /*this.stage.getViewport().update(
            (int) (this.stage.getViewport().getScreenWidth() + v1 * 16),
            (int) (this.stage.getViewport().getScreenHeight() + v1 * 9)
        );*/
        return super.scrolled(v, v1);
    }
    public class InputProperty{
        public UUID id;
        public String name;
        public float value;
        public Float resetValue;
        public InputProperty() {
            this.id = UUID.randomUUID();
            this.name = "property";
            this.value = 0;
            this.resetValue = null;
        }
    }
}
