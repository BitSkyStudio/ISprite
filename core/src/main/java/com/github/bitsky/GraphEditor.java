package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraphEditor extends Editor {
    private final Texture linkInputTexture;
    private final Texture linkInputFilledTexture;
    private final Texture linkOutputTexture;

    private HashMap<UUID, GraphNode> nodes;
    private DragAndDrop dragAndDrop;
    private float time;
    public GraphEditor() {
        this.linkInputTexture = new Texture("link_input.png");
        this.linkInputFilledTexture = new Texture("link_input_filled.png");
        this.linkOutputTexture = new Texture("link_output.png");

        this.dragAndDrop = new DragAndDrop();
        this.nodes = new HashMap<>();

        addNode(new FinalPoseGraphNode(), Vector2.Zero);
    }
    public void addNode(GraphNode node, Vector2 position){
        this.nodes.put(node.id, node);
        this.stage.addActor(node.window);
        node.window.pack();
        node.window.setPosition(position.x, position.y);
    }
    @Override
    public void render() {
        super.render();
        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            stage.getCamera().position.add(-Gdx.input.getDeltaX()*2f, Gdx.input.getDeltaY()*2f, 0);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.P)){
            Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            addNode(new AnimatedPoseGraphNode(), new Vector2(position.x, position.y));
        }
    }
    public abstract class GraphNode{
        public final UUID id;
        public final Window window;
        public final HashMap<String,UUID> inputs;
        public GraphNode(String name) {
            this.id = UUID.randomUUID();
            this.window = new Window(name, ISpriteMain.getSkin());
            this.window.pack();
            this.inputs = new HashMap<>();
            if(hasOutput()){
                HorizontalGroup hgroup = new HorizontalGroup();
                hgroup.addActor(new Label("output", ISpriteMain.getSkin()));
                Actor dragOutput = new Image(new TextureRegion(linkOutputTexture));
                hgroup.addActor(dragOutput);
                dragAndDrop.addSource(new DragAndDrop.Source(dragOutput) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent inputEvent, float v, float v1, int i) {
                        return new DragAndDrop.Payload();
                    }
                });
                window.add(hgroup);
            }
        }
        public void addInput(String name){
            HorizontalGroup hgroup = new HorizontalGroup();
            Actor dragInput = new Image(new TextureRegion(linkInputTexture));
            hgroup.addActor(dragInput);
            hgroup.addActor(new Label(name, ISpriteMain.getSkin()));
            dragAndDrop.addTarget(new DragAndDrop.Target(dragInput) {
                @Override
                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    return false;
                }
                @Override
                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {

                }
            });
            window.add(hgroup);
        }
        public abstract AnimatedSpritePose getOutputPose();
        public boolean hasOutput(){
            return true;
        }
        public AnimatedSpritePose getInput(String input){
            return nodes.get(inputs.get(input)).getOutputPose();
        }
    }
    public class FinalPoseGraphNode extends GraphNode{
        public FinalPoseGraphNode() {
            super("Final Pose");
            addInput("final");
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
        public AnimatedPoseGraphNode() {
            super("Animated Pose");
            this.animation = new SpriteAnimation();
            TextButton enterButton = new TextButton("Enter", ISpriteMain.getSkin());
            enterButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ISpriteMain.getInstance().setEditor(new AnimationEditor(animation));
                }
            });
            this.window.add(enterButton);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return animation.getPose(time);
        }
    }
}
