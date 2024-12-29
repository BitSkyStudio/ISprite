package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
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

import java.util.*;
import java.util.List;

public class GraphEditor extends Editor {
    private final Texture linkInputTexture;
    private final Texture linkInputFilledTexture;
    private final Texture linkOutputTexture;

    private List<ConnectionRecord> connectionRecords = new ArrayList<>();
    private HashMap<UUID, GraphNode> nodes;
    private DragAndDrop dragAndDrop;
    private float time;

    private boolean isDragging;
    private Vector2 startDragLocation;

    private final ToolPanelWindow toolPanelWindow;

    public GraphEditor() {
        this.linkInputTexture = new Texture("link_input.png");
        this.linkInputFilledTexture = new Texture("link_input_filled.png");
        this.linkOutputTexture = new Texture("link_output.png");

        this.dragAndDrop = new DragAndDrop();
        this.nodes = new HashMap<>();

        addNode(new FinalPoseGraphNode(), Vector2.Zero);

        this.toolPanelWindow = new ToolPanelWindow("Tools");
        this.toolPanelWindow.buttonAddPose.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                addNode(new AnimatedPoseGraphNode(), new Vector2(position.x, position.y));
            }
        });
        this.toolPanelWindow.buttonAddAND.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                addNode(new AnimatedPoseAndNode(), new Vector2(position.x, position.y));
            }
        });

        this.stage.addActor(this.toolPanelWindow);
    }
    public void addNode(GraphNode node, Vector2 position){
        this.nodes.put(node.id, node);
        this.stage.addActor(node.window);
        // node.window.pack();
        node.window.setPosition(position.x, position.y);
    }

    public void removeNode(GraphNode node) {
        node.window.remove();
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
            this.shapeRenderer.rectLine(
                connectionRecord.actor1.getOriginX(),
                connectionRecord.actor1.getOriginY(),
                connectionRecord.actor2.getOriginX(),
                connectionRecord.actor2.getOriginY(),
                10);
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

        public GraphNode(String name, String description, boolean removable) {
            this.inputRegions = new HashMap<>();
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
                        startDragLocation = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
                        final DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        payload.setObject(new ConnectionPayload(id, dragOutput));
                        return payload;
                    }

                    @Override
                    public void dragStop(InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        startDragLocation = null;
                        super.dragStop(event, x, y, pointer, payload, target);
                    }
                });
                verticalGroup.addActor(hgroup);
            }

            window.add(verticalGroup);
        }

        /**
         * remove every connection with specified node
         * @param graphNode
         */
        public void disconnectAll(GraphNode graphNode) {

            for (String key : this.inputs.keySet()) {
                UUID val = this.inputs.get(key);
                if (val.equals(graphNode.id)) {
                    this.inputs.remove(key, val);
                    this.inputRegions.get(key).setTexture(linkInputTexture);
                }
            }
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
                }

                @Override
                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    inputRegion.setTexture(linkInputFilledTexture);

                    ConnectionPayload payload1 = ((ConnectionPayload) payload.getObject());
                    inputs.put(name, payload1.uuid);

                    ConnectionRecord connectionRecord = new ConnectionRecord(payload1.actor, dragInput);
                    connectionRecords.add(connectionRecord);
                }
            });
            verticalGroup.addActor(hgroup);
        }
        public abstract AnimatedSpritePose getOutputPose();
        public boolean hasOutput(){
            return true;
        }
        public AnimatedSpritePose getInput(String input){
            return nodes.get(inputs.get(input)).getOutputPose();
        }
    }

    private class ToolPanelWindow extends Window {

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

        public ConnectionPayload(UUID uuid, Actor actor) {
            this.uuid = uuid;
            this.actor = actor;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Actor getActor() {
            return actor;
        }
    }

    private class ConnectionRecord {
        private final Actor actor1;
        private final Actor actor2;

        public ConnectionRecord(Actor actor1, Actor actor2) {
            this.actor1 = actor1;
            this.actor2 = actor2;
        }

        public Actor getActor1() {
            return actor1;
        }

        public Actor getActor2() {
            return actor2;
        }
    };
}
