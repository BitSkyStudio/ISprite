package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class GraphEditor extends Editor {
    private final Texture linkInputTexture;
    private final Texture linkInputFilledTexture;
    private final Texture linkOutputTexture;

    private List<ConnectionRecord> connectionRecords = new ArrayList<>();
    private HashMap<UUID, GraphNode> nodes;
    private DragAndDrop dragAndDrop;
    private float time;

    private Table rightClickMenu;
    private HashMap<String, Supplier<GraphNode>> nodeTypes;
    public GraphEditor() {
        this.linkInputTexture = new Texture("link_input.png");
        this.linkInputFilledTexture = new Texture("link_input_filled.png");
        this.linkOutputTexture = new Texture("link_output.png");

        this.dragAndDrop = new DragAndDrop();
        this.nodes = new HashMap<>();

        addNode(new FinalPoseGraphNode(), Vector2.Zero);

        rightClickMenu = null;

        nodeTypes = new HashMap<>();
        nodeTypes.put("Animated Pose", AnimatedPoseGraphNode::new);
        nodeTypes.put("Blend Pose", BlendPoseGraphNode::new);
        nodeTypes.put("Multiply Pose", MultiplyPoseGraphNode::new);
        nodeTypes.put("Add Pose", AddPoseGraphNode::new);
    }
    public void addNode(GraphNode node, Vector2 position){
        this.nodes.put(node.id, node);
        this.stage.addActor(node.window);
        // node.window.pack();
        node.window.setPosition(position.x, position.y);
    }

    public void removeNode(GraphNode node) {
        node.window.remove();

        this.connectionRecords.removeIf(connectionRecord -> connectionRecord.uuid1.equals(node.id) || connectionRecord.uuid2.equals(node.id));

        this.nodes.remove(node.id, node);
        this.nodes.forEach((nodeKey, nodeValue) -> nodeValue.disconnectAll(node));
    }

    @Override
    public void render() {
        super.render();
        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            stage.getCamera().position.add(-Gdx.input.getDeltaX()*2f, Gdx.input.getDeltaY()*2f, 0);
        }
        this.shapeRenderer.setProjectionMatrix(this.stage.getCamera().combined);
        this.shapeRenderer.begin();
        this.shapeRenderer.set(ShapeRenderer.ShapeType.Filled);

        for (ConnectionRecord connectionRecord : connectionRecords) {
            this.shapeRenderer.setColor(Color.valueOf("DA863E"));
            this.shapeRenderer.circle(connectionRecord.getWindow1().getX() + connectionRecord.getWindow1().getWidth(), connectionRecord.getWindow1().getY() + connectionRecord.getActor1().getParent().getY() + 16, 10);
            this.shapeRenderer.rectLine(
                connectionRecord.getWindow1().getX() + connectionRecord.getWindow1().getWidth(),
                connectionRecord.getWindow1().getY() + connectionRecord.getActor1().getParent().getY() + 16,
                connectionRecord.getWindow2().getX() + 12,
                connectionRecord.getWindow2().getY() + connectionRecord.getActor2().getParent().getY() + 16,
                10, Color.valueOf("DA863E"), Color.valueOf("A4DDDB"));
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
        for(GraphNode node : nodes.values()){
            for(Map.Entry<String, UUID> entry : node.inputs.entrySet()){
                Actor firstActor = node.inputActors.get(entry.getKey());
                Actor secondActor = nodes.get(entry.getValue()).outputActor;
                shapeRenderer.line(firstActor.localToStageCoordinates(new Vector2(firstActor.getWidth()/2, firstActor.getHeight()/2)), secondActor.localToStageCoordinates(new Vector2(secondActor.getWidth()/2, secondActor.getHeight()/2)));
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

            /*
            for (GraphNode graphNode : this.nodes.values()) {
                for (UUID input : graphNode.inputs.values()) {
                    GraphNode connectsTo = this.nodes.get(input);

                }
            }*/
        // this.shapeRenderer.rectLine(startDragLocation.x, startDragLocation.y, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 10, Color.ORANGE, Color.BLUE);
        this.shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    public abstract class GraphNode{

        public final UUID id;
        public final Window window;
        public final HashMap<String,UUID> inputs;
        public final HashMap<String, TextureRegion> inputRegions;

        public final VerticalGroup verticalGroup;
      
          this.inputRegions = new HashMap<>();
        public final HashMap<String,Actor> inputActors;
        public Actor outputActor;

        public GraphNode(String name, String description, boolean removable) {
        
            this.id = UUID.randomUUID();
            this.window = new Window(name, ISpriteMain.getSkin());
            // this.window.pack();
            this.inputs = new HashMap<>();

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

            if(hasOutput()){
                HorizontalGroup hgroup = new HorizontalGroup();
                hgroup.addActor(new Label("Out", ISpriteMain.getSkin()));
                Actor dragOutput = new Image(new TextureRegion(linkOutputTexture));
                hgroup.addActor(dragOutput);
                dragAndDrop.addSource(new DragAndDrop.Source(dragOutput) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent inputEvent, float v, float v1, int i) {

                        final DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        payload.setObject(new ConnectionPayload(id, dragOutput, window));

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

                        super.dragStop(event, x, y, pointer, payload, target);
                    }
                });
                verticalGroup.addActor(hgroup);

                        dragOutput.setVisible(true);
                        super.dragStop(event, x, y, pointer, payload, target);
                    }
                });
                window.add(hgroup).row();
                outputActor = dragOutput;
            }

            window.add(verticalGroup);
        }

        /**
         * remove every connection with specified node
         * @param graphNode
         */
        public void disconnectAll(GraphNode graphNode) {

            List<String> toRemove = new ArrayList<>();

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
            HorizontalGroup hgroup = new HorizontalGroup();
            TextureRegion inputRegion = new TextureRegion(linkInputTexture);

            this.inputRegions.put(name, inputRegion);

            Image dragInput = new Image(inputRegion);
            hgroup.addActor(dragInput);
            hgroup.addActor(new Label(name, ISpriteMain.getSkin()));

            dragAndDrop.addTarget(new DragAndDrop.Target(dragInput) {
                @Override

                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    return true;

                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    return payload.getObject() instanceof ConnectionData;

                }

                @Override

                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    inputRegion.setTexture(linkInputFilledTexture);

                    ConnectionPayload payload1 = ((ConnectionPayload) payload.getObject());
                    inputs.put(name, payload1.getUuid());

                    ConnectionRecord connectionRecord = new ConnectionRecord(payload1.getActor(), dragInput, payload1.getWindow(), window, payload1.getUuid(), id);
                    connectionRecords.add(connectionRecord);
                }
            });
            verticalGroup.addActor(hgroup);

                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    ConnectionData output = (ConnectionData) payload.getObject();
                    inputs.put(name, output.first);
                }
            });
            window.add(hgroup).row();
            this.inputActors.put(name, dragInput);

        }
        public abstract AnimatedSpritePose getOutputPose();
        public boolean hasOutput(){
            return true;
        }
        public AnimatedSpritePose getInput(String input){
            return nodes.get(inputs.get(input)).getOutputPose();
        }
    }

    private static class ToolPanelWindow extends Window {

        public final Button buttonAddPose;
        public final Button buttonAddAND;

        public ToolPanelWindow(String title) {
            super(title, new Skin(Gdx.files.internal("skin/uiskin.json")));
            this.setMovable(true);

            this.buttonAddPose = new TextButton("Add Pose", this.getSkin());
            this.buttonAddAND = new TextButton("Add AND", this.getSkin());
            this.bottom().left();
            this.add(this.buttonAddPose);
            this.add(this.buttonAddAND);
            this.pack();

            this.setWidth(Gdx.graphics.getWidth());
            this.setY(Gdx.graphics.getHeight() - this.getHeight());
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
        }
    }

    public class FinalPoseGraphNode extends GraphNode{
        public FinalPoseGraphNode() {
            super("Final Pose", "Pose to be displayed by the animation renderer.", false);

            this.window.bottom();
            addInput("Input");
            // this.window.setWidth(200);
            this.window.pack();
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

    public class AnimatedPoseGraphNode extends GraphNode {

        public final SpriteAnimation animation;

        public AnimatedPoseGraphNode() {
            super("Animated Pose", "Animation Node", true);
            this.animation = new SpriteAnimation();
            TextButton enterButton = new TextButton("Edit", ISpriteMain.getSkin());
            enterButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ISpriteMain.getInstance().setEditor(new AnimationEditor(animation));
                }
            });
            this.verticalGroup.addActor(enterButton);
            this.window.pack();
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return animation.getPose(time);
        }
    }


    public class AnimatedPoseAndNode extends GraphNode {

        public AnimatedPoseAndNode() {
            super("AND", "Simple AnimPose merger.", true);
            addInput("Input1");
            addInput("Input2");
            this.window.pack();
        }

        @Override
        public AnimatedSpritePose getOutputPose() {
            return null;
        }
    }

    private class ConnectionPayload {
        private final UUID uuid;
        private final Actor actor;
        private final Window window;

        public ConnectionPayload(UUID uuid, Actor actor, Window window) {
            this.uuid = uuid;
            this.actor = actor;
            this.window = window;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Actor getActor() {
            return actor;
        }

        public Window getWindow() {
            return window;
        }
    }

    private class ConnectionRecord {
        private final Actor actor1;
        private final Actor actor2;

        private final Window window1;
        private final Window window2;

        private final UUID uuid1;
        private final UUID uuid2;

        public ConnectionRecord(Actor actor1, Actor actor2, Window window1, Window window2, UUID uid1, UUID uid2) {
            this.actor1 = actor1;
            this.actor2 = actor2;
            this.window1 = window1;
            this.window2 = window2;
            this.uuid1 = uid1;
            this.uuid2 = uid2;
        }

        public Actor getActor1() {
            return actor1;
        }

        public Actor getActor2() {
            return actor2;
        }

        public Window getWindow1() {
            return window1;
        }

        public Window getWindow2() {
            return window2;
        }
    };

    public class BlendPoseGraphNode extends GraphNode{
        public float blendValue;
        public BlendPoseGraphNode() {
            super("Blend Pose");
            addInput("first");
            addInput("second");
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
            return getInput("first").lerp(getInput("second"), blendValue);
        }
    }
    public class MultiplyPoseGraphNode extends GraphNode{
        public float multiplyValue;
        public MultiplyPoseGraphNode() {
            super("Multiply Pose");
            addInput("first");
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
            return getInput("first").multiply(multiplyValue);
        }
    }
    public class AddPoseGraphNode extends GraphNode{
        public AddPoseGraphNode() {
            super("Add Pose");
            addInput("first");
            addInput("second");
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("first").add(getInput("second"));
        }
    }

}
