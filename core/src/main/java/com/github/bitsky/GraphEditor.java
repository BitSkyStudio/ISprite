package com.github.bitsky;

import au.edu.federation.caliko.FabrikBone2D;
import au.edu.federation.caliko.FabrikChain2D;
import au.edu.federation.utils.Vec2f;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GraphEditor extends Editor {
    private final Texture linkInputTexture;
    private final Texture linkInputFilledTexture;
    private final Texture linkOutputTexture;

    private HashMap<UUID, GraphNode> nodes;
    private DragAndDrop dragAndDrop;
    private Table rightClickMenu;
    private HashMap<String, Supplier<GraphNode>> nodeTypes;
    public HashMap<UUID,InputProperty> properties;

    private FinalPoseGraphNode finalPoseGraphNode;

    private Table playTable;
    private AnimationPlayerWidget animationPlayer;

    private Stage graphStage;

    private TextButton playButton;
    private boolean playing;

    private Table propertiesTable;

    private UUID outputShowNode;

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
        this.stage.setScrollFocus(animationPlayer);
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
        nodeTypes.put("Symmetry Constraint", SymmetryConstraintGraphNode::new);
        nodeTypes.put("IK Constraint", IKConstraintGraphNode::new);
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
            node.id = UUID.fromString(id);
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
            valueField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || c=='.'|| c=='\n');
            valueField.setTextFieldListener((textField1, c) -> {
                if(c == '\n'){
                    stage.setKeyboardFocus(null);
                    try {
                        property.value = Float.parseFloat(textField1.getText());
                    } catch(NumberFormatException e){}
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

        super.render();
        if(playing) {
            finalPoseGraphNode.tick(Gdx.graphics.getDeltaTime());
            if(finalPoseGraphNode.isFinished())
                setPlaying(false);
        }
        if(this.outputShowNode == null || !nodes.containsKey(this.outputShowNode)){
            this.outputShowNode = finalPoseGraphNode.id;
        }
        this.animationPlayer.pose = nodes.get(outputShowNode).getOutputPose();
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
    }


    public static class ConnectionData{
        public final UUID first;
        public ConnectionData(UUID first) {
            this.first = first;
        }
    }

    public abstract class GraphNode {
        public UUID id;
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
            final TextButton showOutputButton = new TextButton("O", this.window.getSkin());
            showOutputButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    GraphEditor.this.outputShowNode = GraphNode.this.id;
                }
            });

            miniToolBar.addActor(showOutputButton);
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
                if(nodes.get(input).isFinished())
                    return true;
            }
            return inputs.isEmpty();
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
            return getInput("Out");
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
        private TextField blendValueField;
        public BlendPoseGraphNode() {
            super("Blend Pose", "Blends two inputs.", true);
            addInput("Pose1");
            addInput("Pose2");

            this.window.add(new Label("Blend: ", ISpriteMain.getSkin()));

            this.blendValueField = new TextField(String.valueOf(0.5), ISpriteMain.getSkin());
            this.window.add(blendValueField);
        }

        @Override
        public String getTypeName() {
            return "Blend Pose";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose1").lerp(getInput("Pose2"), evaluateExpression(blendValueField.getText()));
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("blendValue", blendValueField.getText());
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.blendValueField.setText(json.getString("blendValue"));
        }
    }

    public class MultiplyPoseGraphNode extends GraphNode{
        private TextField multiplyValueField;
        public MultiplyPoseGraphNode() {
            super("Multiply Pose", "Multiplies pose by set value.", true);
            addInput("Pose");
            this.window.add(new Label("Multiply: ", ISpriteMain.getSkin()));
            this.multiplyValueField = new TextField("1", ISpriteMain.getSkin());
            this.window.add(multiplyValueField);
        }

        @Override
        public String getTypeName() {
            return "Multiply Pose";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose").multiply(evaluateExpression(multiplyValueField.getText()));
        }
        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("multiplyValue", multiplyValueField.getText());
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.multiplyValueField.setText(json.getString("multiplyValue"));
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

    public class SymmetryConstraintGraphNode extends GraphNode{
        private SelectBox<UUID> projected;
        private SelectBox<UUID> center;
        private SelectBox<UUID> target;
        public SymmetryConstraintGraphNode() {
            super("Symmetry Constraint", "Symmetrical constraint.", true);
            addInput("Input");
            this.projected = new SelectBox<>(ISpriteMain.getSkin()){
                @Override
                public String toString(UUID object) {
                    return ISpriteMain.getInstance().sprite.bones.get(object).name;
                }
            };
            this.center = new SelectBox<>(ISpriteMain.getSkin()){
                @Override
                public String toString(UUID object) {
                    return ISpriteMain.getInstance().sprite.bones.get(object).name;
                }
            };
            this.target = new SelectBox<>(ISpriteMain.getSkin()){
                @Override
                public String toString(UUID object) {
                    return ISpriteMain.getInstance().sprite.bones.get(object).name;
                }
            };
            HorizontalGroup projectedGroup = new HorizontalGroup();
            projectedGroup.addActor(new Label("projected: ", ISpriteMain.getSkin()));
            projectedGroup.addActor(projected);
            verticalGroup.addActor(projectedGroup);

            HorizontalGroup centerGroup = new HorizontalGroup();
            centerGroup.addActor(new Label("center: ", ISpriteMain.getSkin()));
            centerGroup.addActor(center);
            verticalGroup.addActor(centerGroup);

            HorizontalGroup targetGroup = new HorizontalGroup();
            targetGroup.addActor(new Label("target: ", ISpriteMain.getSkin()));
            targetGroup.addActor(target);
            verticalGroup.addActor(targetGroup);
            refresh();
        }

        @Override
        public void refresh() {
            UUID[] items = ISpriteMain.getInstance().sprite.bones.keySet().toArray(UUID[]::new);
            this.projected.setItems(items);
            this.center.setItems(items);
            this.target.setItems(items);
        }

        @Override
        public String getTypeName() {
            return "Symmetry Constraint";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            AnimatedSpritePose pose = getInput("Input");
            UUID projected = this.projected.getSelected();
            UUID center = this.center.getSelected();
            UUID target = this.target.getSelected();
            if(projected == null || center == null || target == null || projected == center || center == target || target == projected)
                return pose;
            Transform pt = pose.boneTransforms.get(projected);
            if(pt != null) {
                pt.translation = null;
            } else {
                pt = new Transform();
                pose.boneTransforms.put(projected, pt);
            }
            HashMap<UUID, Transform> transforms = pose.getBoneTransforms(ISpriteMain.getInstance().sprite, new Transform().lock());
            Vector2 intendedPosition = transforms.get(center).translation.cpy().scl(2).sub(transforms.get(target).translation);
            Transform parentTransform = transforms.get(ISpriteMain.getInstance().sprite.bones.get(projected).parent);
            pt.translation = intendedPosition.rotateRad(-parentTransform.rotation).scl(1/parentTransform.scale).sub(parentTransform.translation);
            return pose;
        }

        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.target.setSelected(UUID.fromString(json.getString("target")));
            this.center.setSelected(UUID.fromString(json.getString("center")));
            this.projected.setSelected(UUID.fromString(json.getString("projected")));
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("target", target.getSelected());
            json.put("center", center.getSelected());
            json.put("projected", projected.getSelected());
            return json;
        }
    }

    public class IKConstraintGraphNode extends GraphNode{
        private SelectBox<UUID> start;
        private SelectBox<UUID> end;
        private SelectBox<UUID> target;
        private boolean isClockwise;
        private CheckBox isClockwiseCheckbox;
        public IKConstraintGraphNode() {
            super("IK Constraint", "IK constraint.", true);
            addInput("Input");
            this.start = new SelectBox<>(ISpriteMain.getSkin()){
                @Override
                public String toString(UUID object) {
                    return ISpriteMain.getInstance().sprite.bones.get(object).name;
                }
            };
            this.start.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    ArrayList<UUID> items = new ArrayList<>();
                    if(start.getSelected() != null){
                        ISpriteMain.getInstance().sprite.bones.get(start.getSelected()).childrenRecursive(items);
                    }
                    end.setItems(items.toArray(UUID[]::new));
                }
            });
            this.end = new SelectBox<>(ISpriteMain.getSkin()){
                @Override
                public String toString(UUID object) {
                    return ISpriteMain.getInstance().sprite.bones.get(object).name;
                }
            };
            this.target = new SelectBox<>(ISpriteMain.getSkin()){
                @Override
                public String toString(UUID object) {
                    return ISpriteMain.getInstance().sprite.bones.get(object).name;
                }
            };
            this.isClockwiseCheckbox = new CheckBox("clockwise", ISpriteMain.getSkin());
            isClockwiseCheckbox.setChecked(isClockwise);
            isClockwiseCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    isClockwise = isClockwiseCheckbox.isChecked();
                }
            });
            verticalGroup.addActor(isClockwiseCheckbox);
            HorizontalGroup startGroup = new HorizontalGroup();
            startGroup.addActor(new Label("start: ", ISpriteMain.getSkin()));
            startGroup.addActor(start);
            verticalGroup.addActor(startGroup);

            HorizontalGroup endGroup = new HorizontalGroup();
            endGroup.addActor(new Label("end: ", ISpriteMain.getSkin()));
            endGroup.addActor(end);
            verticalGroup.addActor(endGroup);

            HorizontalGroup targetGroup = new HorizontalGroup();
            targetGroup.addActor(new Label("target: ", ISpriteMain.getSkin()));
            targetGroup.addActor(target);
            verticalGroup.addActor(targetGroup);
            refresh();
        }

        @Override
        public void refresh() {
            UUID[] items = ISpriteMain.getInstance().sprite.bones.keySet().toArray(UUID[]::new);
            this.start.setItems(items);
            this.start.fire(new ChangeListener.ChangeEvent());
            this.target.setItems(items);
        }

        @Override
        public String getTypeName() {
            return "IK Constraint";
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            AnimatedSpritePose pose = getInput("Input");
            UUID start = this.start.getSelected();
            UUID end = this.end.getSelected();
            UUID target = this.target.getSelected();
            if(start == null || end == null || target == null || start == end || end == target || target == start)
                return pose;

            ArrayList<UUID> boneChain = new ArrayList<>();
            boneChain.add(end);
            for(UUID current = end;!start.equals(current);){
                UUID parent = ISpriteMain.getInstance().sprite.bones.get(current).parent;
                current = parent;
                boneChain.add(parent);
            }
            Collections.reverse(boneChain);

            HashMap<UUID, Transform> transforms = pose.getBoneTransforms(ISpriteMain.getInstance().sprite, new Transform().lock());

            float constraint = 90;
            FabrikChain2D chain = new FabrikChain2D();
            Vector2 firstBone = transforms.get(boneChain.get(0)).translation;
            Vector2 secondBone = transforms.get(boneChain.get(1)).translation;
            Vec2f prevPos = new Vec2f(secondBone.x, secondBone.y);
            FabrikBone2D bone = new FabrikBone2D(new Vec2f(firstBone.x, firstBone.y), prevPos);
            chain.addBone(bone);
            for(int i = 2;i < boneChain.size();i++){
                Vector2 position = transforms.get(boneChain.get(i)).translation;
                Vec2f newPosition = new Vec2f(position.x, position.y);
                Vec2f direction = newPosition.minus(prevPos);
                chain.addConsecutiveConstrainedBone(direction, direction.length(), isClockwise?constraint:0, isClockwise?0:constraint);
                prevPos = newPosition;
            }
            Vector2 targetPos = transforms.get(target).translation;
            chain.setMaxIterationAttempts(20);
            chain.solveForTarget(new Vec2f(targetPos.x, targetPos.y));

            float prevRot = transforms.get(start).rotation;
            Vec2f prevDir = new Vec2f((float) -Math.sin(prevRot), (float) -Math.cos(prevRot));
            for(int i = 0;i < chain.getNumBones();i++){
                Vec2f newDirection = chain.getBone(i).getDirectionUV();
                float angle = prevDir.getSignedAngleDegsTo(newDirection);
                Transform t = pose.boneTransforms.computeIfAbsent(boneChain.get(i), k -> new Transform());
                t.rotation = (float) Math.toRadians(angle);
                prevDir = newDirection;
            }

            return pose;
        }

        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.target.setSelected(UUID.fromString(json.getString("target")));
            this.end.setSelected(UUID.fromString(json.getString("end")));
            this.start.setSelected(UUID.fromString(json.getString("start")));
            this.isClockwise = json.getBoolean("clockwise");
            this.isClockwiseCheckbox.setChecked(isClockwise);
        }

        @Override
        public JSONObject save() {
            JSONObject json = super.save();
            json.put("target", target.getSelected());
            json.put("end", end.getSelected());
            json.put("start", start.getSelected());
            json.put("clockwise", isClockwise);
            return json;
        }
    }

    public class PlaybackSpeedGraphNode extends GraphNode{
        private TextField speedField;
        public PlaybackSpeedGraphNode() {
            super("Playback Speed", "Changes playback speed.", true);
            addInput("Pose");

            this.window.add(new Label("Speed: ", ISpriteMain.getSkin()));

            this.speedField = new TextField("1", ISpriteMain.getSkin());
            this.window.add(speedField);
        }

        @Override
        public void tick(float step) {
            nodes.get(inputs.get("Pose")).tick(step*evaluateExpression(speedField.getText()));
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
            json.put("speed", speedField.getText());
            return json;
        }
        @Override
        public void load(JSONObject json) {
            super.load(json);
            this.speedField.setText(json.getString("speed"));
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
            reset();
            refresh();
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
                        if(condition.propertyId == null || properties.get(condition.propertyId) == null || !condition.comparator.passes(properties.get(condition.propertyId).value, condition.value)){
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
                        if(transition.resets)
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
            this.stateMachine.load(json.getJSONObject("stateMachine"));
            refresh();
            super.load(json);
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
        public Vector2 offset;
        public float zoom;
        private Texture background;
        public AnimationPlayerWidget() {
            this.background = new Texture("background.png");
            this.polygonSpriteBatch = new PolygonSpriteBatch();
            this.pose = BoneEditor.EMPTY_POSE;
            this.offset = new Vector2(0, 0);
            addListener(new InputListener(){
                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    offset.x += ISpriteMain.getMouseDeltaX();
                    offset.y -= ISpriteMain.getMouseDeltaY();
                }

                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                    zoom -= amountY/10f;
                    zoom = Math.min(Math.max(zoom, 0.5f), 2f);
                    return true;
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    return true;
                }
            });
            this.zoom = 1;
        }
        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            batch.draw(background, getX(), getY(), getMinWidth(), getMinHeight());
            batch.end();
            polygonSpriteBatch.setProjectionMatrix(batch.getProjectionMatrix());
            polygonSpriteBatch.begin();
            Rectangle scissors = new Rectangle();
            Rectangle clipBounds = new Rectangle(getX(),getY(),getMinWidth(),getMinHeight());
            ScissorStack.calculateScissors(stage.getCamera(), new Matrix4(), clipBounds, scissors);
            polygonSpriteBatch.setTransformMatrix(new Matrix4().scale(zoom, zoom, 1));
            if (ScissorStack.pushScissors(scissors)) {
                for(VertexedImage image : ISpriteMain.getInstance().sprite.images)
                    image.draw(polygonSpriteBatch, pose, getX()/zoom + offset.x, getY()/zoom + offset.y);
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
    public float evaluateExpression(String expression){
        try {
            ExpressionBuilder builder = new ExpressionBuilder(expression);
            builder.variables(properties.values().stream().map(p -> p.name).toArray(String[]::new));
            Expression e = builder.build();
            for (InputProperty prop : properties.values()) {
                e.setVariable(prop.name, prop.value);
            }
            return (float) e.evaluate();
        } catch (Exception e){
            return 0;
        }
    }
}
