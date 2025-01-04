package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private Table playTable;
    private AnimationPlayerWidget animationPlayer;

    private Stage graphStage;

    private TextButton playButton;
    private boolean playing;

    private Table propertiesTable;

    public GraphEditor() {
        this.linkInputTexture = new Texture("link_input.png");
        this.linkInputFilledTexture = new Texture("link_input_filled.png");
        this.linkOutputTexture = new Texture("link_output.png");

        this.graphStage = new Stage();

        this.dragAndDrop = new DragAndDrop();
        this.nodes = new HashMap<>();

        this.finalPoseGraphNode = addNode(new FinalPoseGraphNode(), Vector2.Zero);

        this.properties = new HashMap<>();

        this.playTable = new Table();
        this.playTable.setFillParent(true);
        this.playTable.left();
        this.stage.addActor(playTable);
        this.animationPlayer = new AnimationPlayerWidget();
        this.playTable.add(animationPlayer).row();
        HorizontalGroup buttonGroup = new HorizontalGroup();
        TextButton resetButton = new TextButton("Reset", ISpriteMain.getSkin());
        resetButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                finalPoseGraphNode.reset();
                setPlaying(false);
            }
        });
        this.playButton = new TextButton("Play", ISpriteMain.getSkin());
        playButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setPlaying(!playing);
            }
        });
        buttonGroup.addActor(resetButton);
        buttonGroup.addActor(playButton);
        this.playTable.add(buttonGroup).row();

        TextButton addPropertyButton = new TextButton("Add Property", ISpriteMain.getSkin());
        addPropertyButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                TextField input = new TextField("name", ISpriteMain.getSkin());
                Dialog dialog = new Dialog("Create property", ISpriteMain.getSkin(), "dialog") {
                    public void result(Object obj) {
                        if (obj instanceof String) {
                            addProperty(input.getText());
                        }
                    }
                };
                dialog.setMovable(false);
                dialog.button("Cancel");
                dialog.button("Ok", "");
                dialog.getContentTable().add(input);
                dialog.show(stage);
            }
        });
        this.playTable.add(addPropertyButton).row();

        this.propertiesTable = new Table();
        this.playTable.add(propertiesTable).row();
        refreshPropertyTable();

        setPlaying(false);

        rightClickMenu = null;

        nodeTypes = new HashMap<>();
        nodeTypes.put("Animated Pose", AnimatedPoseGraphNode::new);
        nodeTypes.put("Blend Pose", BlendPoseGraphNode::new);
        nodeTypes.put("Multiply Pose", MultiplyPoseGraphNode::new);
        nodeTypes.put("Playback Speed", PlaybackSpeedGraphNode::new);
        nodeTypes.put("Add Pose", AddPoseGraphNode::new);
        nodeTypes.put("State Machine", StateGraphNode::new);
    }
    public JSONObject save(){
        JSONObject json = new JSONObject();
        json.put("final", finalPoseGraphNode.save());
        JSONObject nodesJson = new JSONObject();
        for(Map.Entry<UUID, GraphNode> entry : nodes.entrySet()){
            if(entry.getValue() == finalPoseGraphNode)
                continue;
            nodesJson.put(entry.getKey().toString(), entry.getValue().save());
        }
        json.put("nodes", nodesJson);
        JSONObject propertiesJson = new JSONObject();
        for(Map.Entry<UUID, InputProperty> entry : properties.entrySet()){
            JSONObject propertyJson = new JSONObject();
            propertyJson.put("id", entry.getValue().id);
            propertyJson.put("name", entry.getValue().name);
            propertyJson.put("value", entry.getValue().value);
            if(entry.getValue().resetValue != null)
                propertyJson.put("resetValue", entry.getValue().resetValue);
            propertiesJson.put(entry.getKey().toString(), propertyJson);
        }
        json.put("properties", propertiesJson);
        return json;
    }
    public void load(JSONObject json){
        finalPoseGraphNode.load(json.getJSONObject("final"));
        for(GraphNode node : nodes.values()){
            if(node != finalPoseGraphNode){
                removeNode(node);
            }
        }
        JSONObject nodesJson = json.getJSONObject("nodes");
        for(String id : nodesJson.keySet()){
            JSONObject nodeJson = nodesJson.getJSONObject(id);
            GraphNode node = nodeTypes.get(nodeJson.getString("type")).get();
            node.load(nodeJson);
            addNode(node, new Vector2(node.window.getX(), node.window.getY()));
        }
        properties.clear();
        JSONObject propertiesJson = json.getJSONObject("properties");
        for(String id : propertiesJson.keySet()){
            JSONObject propertyJson = propertiesJson.getJSONObject(id);
            InputProperty inputProperty = new InputProperty();
            inputProperty.id = UUID.fromString(propertyJson.getString("id"));
            inputProperty.name = propertyJson.getString("name");
            inputProperty.value = propertyJson.getFloat("value");
            inputProperty.resetValue = propertyJson.has("resetValue")?propertyJson.getFloat("resetValue"):null;
            properties.put(inputProperty.id, inputProperty);
;        }
        refreshPropertyTable();
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        this.playButton.setText(playing?"Pause":"Play");
    }

    public void removeNode(GraphNode node) {
        node.window.remove();

        this.nodes.remove(node.id, node);
        this.nodes.forEach((nodeKey, nodeValue) -> nodeValue.disconnectAll(node));
    }

    public <T extends GraphNode> T addNode(T node, Vector2 position){
        this.nodes.put(node.id, node);
        this.graphStage.addActor(node.window);
        node.window.pack();
        node.window.setPosition(position.x, position.y);
        return node;
    }
    public void addProperty(String name){
        InputProperty property = new InputProperty();
        property.name = name;
        this.properties.put(property.id, property);
        refreshPropertyTable();
    }
    private void refreshPropertyTable(){
        propertiesTable.clearChildren();
        for(InputProperty property : properties.values()){
            Label name = new Label(property.name, ISpriteMain.getSkin());
            TextField valueField = new TextField(String.valueOf(property.value), ISpriteMain.getSkin());
            valueField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            valueField.setTextFieldListener((textField1, c) -> {
                try {
                    property.value = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    valueField.setText(String.valueOf(property.value));
                }
            });
            TextButton removeButton = new TextButton("remove", ISpriteMain.getSkin());
            removeButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    properties.remove(property.id);
                    refreshPropertyTable();
                }
            });
            propertiesTable.add(name, valueField, removeButton).row();
        }
    }

    @Override
    public void render() {
        graphStage.act();
        graphStage.draw();
        super.render();
        if(playing) {
            finalPoseGraphNode.tick(Gdx.graphics.getDeltaTime());
            if(finalPoseGraphNode.isFinished())
                setPlaying(false);
        }
        this.animationPlayer.pose = finalPoseGraphNode.getInput("Out");
        for(InputProperty property : properties.values()){
            if(property.resetValue != null) {
                if(property.value != property.resetValue) {
                    property.value = property.resetValue;
                    refreshPropertyTable();
                }
            }
        }

        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            graphStage.getCamera().position.add(-ISpriteMain.getMouseDeltaX(), ISpriteMain.getMouseDeltaY(), 0);
        }
        if(Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)){
            if(rightClickMenu != null)
                rightClickMenu.remove();

            this.rightClickMenu = new Table();
            graphStage.addActor(rightClickMenu);
            Vector3 position = graphStage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            rightClickMenu.setPosition(position.x, position.y);
            for(Map.Entry<String, Supplier<GraphNode>> entry : nodeTypes.entrySet()){
                TextButton button = new TextButton(entry.getKey(), ISpriteMain.getSkin());
                button.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Vector3 position = graphStage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
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

        shapeRenderer.setProjectionMatrix(graphStage.getCamera().combined);
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
        public void refresh(){}
        public abstract String getTypeName();

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

        public void addInput(String name){
            addInput(name, name);
        }
        public void addInput(String id, String name) {
            HorizontalGroup hgroup = new HorizontalGroup();
            this.inputFields.put(id, hgroup);
            hgroup.setName(name);
            TextureRegion textureRegion = new TextureRegion(linkInputTexture);
            Actor dragInput = new Image(textureRegion);
            this.inputRegions.put(id, textureRegion);
            hgroup.addActor(dragInput);
            hgroup.addActor(new Label(name, ISpriteMain.getSkin()));

            dragInput.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    inputs.remove(id);
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
                    inputs.put(id, output.first);
                    textureRegion.setTexture(linkInputFilledTexture);
                }
            });
            verticalGroup.addActor(hgroup);
            this.inputActors.put(id, dragInput);
        }

        public abstract AnimatedSpritePose getOutputPose();

        public void tick(float step) {
            for(UUID input : inputs.values()){
                nodes.get(input).tick(step);
            }
        }

        public void reset() {
            for(UUID input : inputs.values()){
                nodes.get(input).reset();
            }
        }
        public boolean isFinished() {
            for(UUID input : inputs.values()){
                if(!nodes.get(input).isFinished())
                    return false;
            }
            return true;
        }
        public boolean hasOutput() {
            return true;
        }

        public AnimatedSpritePose getInput(String input) {
            if(!inputs.containsKey(input))
                return new AnimatedSpritePose(new HashMap<>());
            return nodes.get(inputs.get(input)).getOutputPose();
        }

        public JSONObject save(){
            JSONObject json = new JSONObject();
            json.put("type", getTypeName());
            JSONObject inputsJson = new JSONObject();
            for(Map.Entry<String, UUID> entry : inputs.entrySet()){
                inputsJson.put(entry.getKey(), entry.getValue().toString());
            }
            json.put("inputs", inputsJson);
            JSONObject position = new JSONObject();
            position.put("x", window.getX());
            position.put("y", window.getY());
            json.put("position", position);
            return json;
        }
        public void load(JSONObject json){
            JSONObject inputsJson = json.getJSONObject("inputs");
            for(String name : inputsJson.keySet()){
                inputs.put(name, UUID.fromString(inputsJson.getString(name)));
                inputRegions.get(name).setTexture(linkInputFilledTexture);
            }
            JSONObject position = json.getJSONObject("position");
            window.setPosition(position.getFloat("x"), position.getFloat("y"));
        }
    }

    public class FinalPoseGraphNode extends GraphNode{
        public FinalPoseGraphNode() {
            super("Final Pose", "Pose to be displayed by the animation renderer.", false);
            addInput("Out");
        }

        @Override
        public String getTypeName() {
            return "";
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
        private CheckBox loopingCheckBox;
        public AnimatedPoseGraphNode() {
            super("Animated Pose", "KeyFramed animation.", true);

            this.animation = new SpriteAnimation();
            TextButton enterButton = new TextButton("Edit", ISpriteMain.getSkin());
            enterButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ISpriteMain.getInstance().setEditor(new AnimationEditor(animation));
                }
            });
            this.loopingCheckBox = new CheckBox("loop", ISpriteMain.getSkin());
            loopingCheckBox.setChecked(isLooping);
            loopingCheckBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    isLooping = loopingCheckBox.isChecked();
                }
            });
            this.verticalGroup.addActor(loopingCheckBox);
            this.verticalGroup.addActor(enterButton);
        }

        @Override
        public void reset() {
            time = 0;
        }
        @Override
        public void tick(float step) {
            time += step;
            if(isLooping)
                time %= animation.getAnimationLength();
        }
        @Override
        public boolean isFinished() {
            if(isLooping)
                return false;
            return time > animation.getAnimationLength();
        }

        @Override
        public String getTypeName() {
            return "Animated Pose";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return animation.getPose(time);
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("looping", isLooping);
            json.put("animation", animation.save());
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.isLooping = json.getBoolean("looping");
            this.loopingCheckBox.setChecked(isLooping);
            this.animation.load(json.getJSONObject("animation"));
        }
    }

    public class BlendPoseGraphNode extends GraphNode{
        public float blendValue;
        private TextField blendValueField;
        public BlendPoseGraphNode() {
            super("Blend Pose", "Blends two inputs.", true);
            addInput("Pose1");
            addInput("Pose2");

            this.blendValue = 0.5f;
            this.window.add(new Label("Blend: ", ISpriteMain.getSkin()));

            this.blendValueField = new TextField(String.valueOf(blendValue), ISpriteMain.getSkin());
            blendValueField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            blendValueField.setTextFieldListener((textField1, c) -> {
                try {
                    blendValue = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    blendValueField.setText(String.valueOf(blendValue));
                }
            });
            this.window.add(blendValueField);
        }

        @Override
        public String getTypeName() {
            return "Blend Pose";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose1").lerp(getInput("Pose2"), blendValue);
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("blendValue", blendValue);
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.blendValue = json.getFloat("blendValue");
            blendValueField.setText(String.valueOf(blendValue));
        }
    }

    public class MultiplyPoseGraphNode extends GraphNode{
        public float multiplyValue;
        private TextField multiplyValueField;
        public MultiplyPoseGraphNode() {
            super("Multiply Pose", "Multiplies pose by set value.", true);
            addInput("Pose");
            this.multiplyValue = 1f;
            this.window.add(new Label("Multiply: ", ISpriteMain.getSkin()));
            this.multiplyValueField = new TextField(""+multiplyValue, ISpriteMain.getSkin());
            multiplyValueField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            multiplyValueField.setTextFieldListener((textField1, c) -> {
                try {
                    multiplyValue = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    multiplyValueField.setText(""+multiplyValue);
                }
            });
            this.window.add(multiplyValueField);
        }

        @Override
        public String getTypeName() {
            return "Multiply Pose";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose").multiply(multiplyValue);
        }
        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("multiplyValue", multiplyValue);
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.multiplyValue = json.getFloat("multiplyValue");
            multiplyValueField.setText(String.valueOf(multiplyValue));
        }
    }

    public class AddPoseGraphNode extends GraphNode{
        public AddPoseGraphNode() {
            super("Add Pose", "Combine two poses.", true);
            addInput("Pose1");
            addInput("Pose2");
        }

        @Override
        public String getTypeName() {
            return "Add Pose";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose1").add(getInput("Pose2"));
        }
    }

    public class PlaybackSpeedGraphNode extends GraphNode{
        public float speed;
        private TextField speedField;
        public PlaybackSpeedGraphNode() {
            super("Playback Speed", "Changes playback speed.", true);
            addInput("Pose");

            this.speed = 1;
            this.window.add(new Label("Speed: ", ISpriteMain.getSkin()));

            this.speedField = new TextField(String.valueOf(speed), ISpriteMain.getSkin());
            speedField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            speedField.setTextFieldListener((textField1, c) -> {
                try {
                    speed = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    speedField.setText(String.valueOf(speed));
                }
            });
            this.window.add(speedField);
        }

        @Override
        public void tick(float step) {
            nodes.get(inputs.get("Pose")).tick(step*speed);
        }

        @Override
        public String getTypeName() {
            return "Playback Speed";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose");
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("speed", speed);
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.speed = json.getFloat("speed");
            speedField.setText(String.valueOf(speed));
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
            if(getInputByState(currentState) == null)
                return;
            getInputByState(currentState).reset();
        }

        @Override
        public void tick(float step) {
            if(getInputByState(currentState) == null)
                return;
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
                        if(getInputByState(transition.target) == null)
                            return;
                        getInputByState(transition.target).reset();
                        break;
                    }
                }
            }
        }

        @Override
        public boolean isFinished() {
            if(getInputByState(currentState) == null)
                return true;
            return getInputByState(currentState).isFinished() && stateMachine.states.get(currentState).endState;
        }

        @Override
        public String getTypeName() {
            return "State Machine";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            GraphNode first = getInputByState(currentState);
            if(first == null)
                return BoneEditor.EMPTY_POSE;
            if(transitionId != -1){
                AnimationStateMachine.StateTransition transition = stateMachine.states.get(currentState).transitions.get(transitionId);
                GraphNode second = getInputByState(transition.target);
                if(second == null)
                    return first.getOutputPose();
                return first.getOutputPose().lerp(second.getOutputPose(), transition.interpolationFunction.function.apply(transitionTime/transition.blendTime));
            } else {
                return first.getOutputPose();
            }
        }

        public GraphNode getInputByState(UUID state) {
            UUID id = inputs.get(state.toString());
            if(id != null)
                return nodes.get(id);
            return null;
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("stateMachine", stateMachine.save());
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.stateMachine.load(json.getJSONObject("stateMachine"));
        }

        @Override
        public void refresh() {
            this.inputFields.values().forEach(Actor::remove);
            this.inputFields.clear();
            this.inputRegions.clear();
            this.inputActors.clear();
            for(Map.Entry<UUID, AnimationStateMachine.State> entry : stateMachine.states.entrySet()){
                addInput(entry.getKey().toString(), entry.getValue().name);
            }
            window.pack();
        }
    }

    @Override
    public void resize(int width, int height) {
        graphStage.getViewport().setWorldSize(width, width/16f*9);
        graphStage.getViewport().update(width, height);
        super.resize(width, height);
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, graphStage));
        for(GraphNode node : nodes.values()){
            node.refresh();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        graphStage.dispose();
    }

    @Override
    public boolean scrolled(float v, float v1) {
        return super.scrolled(v, v1);
    }

    public static class InputProperty {
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

    public class AnimationPlayerWidget extends Widget{
        private final PolygonSpriteBatch polygonSpriteBatch;
        public AnimatedSpritePose pose;
        public AnimationPlayerWidget() {
            this.polygonSpriteBatch = new PolygonSpriteBatch();
            this.pose = BoneEditor.EMPTY_POSE;
        }
        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            batch.draw(linkInputTexture, getX(), getY(), getMinWidth(), getMinHeight());
            batch.end();
            polygonSpriteBatch.setProjectionMatrix(batch.getProjectionMatrix());
            polygonSpriteBatch.begin();
            Rectangle scissors = new Rectangle();
            Rectangle clipBounds = new Rectangle(getX(),getY(),getMinWidth(),getMinHeight());
            ScissorStack.calculateScissors(stage.getCamera(), polygonSpriteBatch.getTransformMatrix(), clipBounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {
                for(VertexedImage image : ISpriteMain.getInstance().sprite.images)
                    image.draw(polygonSpriteBatch, pose, getX(), getY());
                polygonSpriteBatch.flush();
                ScissorStack.popScissors();
            }
            polygonSpriteBatch.end();
            batch.begin();
        }
        @Override
        public float getMinWidth() {
            return 300;
        }
        @Override
        public float getMinHeight() {
            return 300;
        }

        @Override
        public boolean remove() {
            polygonSpriteBatch.dispose();
            return super.remove();
        }
    }
}
