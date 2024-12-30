package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.*;

public class BoneEditor extends Editor {
    public static AnimatedSpritePose EMPTY_POSE = new AnimatedSpritePose(new HashMap<>());
    private UUID movingId;
    private Table boneHierarchyTable;
    private Tree<BoneNode,AnimatedSpriteBone> tree;
    private HashMap<UUID,BoneNode> boneNodes;

    public BoneEditor() {
        this.movingId = null;
        this.boneHierarchyTable = new Table();
        this.boneHierarchyTable.setFillParent(true);
        this.boneHierarchyTable.right();
        stage.addActor(this.boneHierarchyTable);
        this.boneNodes = new HashMap<>();
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
        this.boneHierarchyTable.clearChildren();
        this.tree = new Tree<>(ISpriteMain.getSkin());
        BoneNode root = new BoneNode(sprite.rootBone);
        tree.add(root);
        recurseAddHierarchy(root);
        this.boneHierarchyTable.add(tree);
    }
    private void recurseAddHierarchy(BoneNode node){
        boneNodes.put(node.getValue().id, node);
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
        for(UUID child : node.getValue().children){
            AnimatedSpriteBone childBone = sprite.bones.get(child);
            BoneNode childNode = new BoneNode(childBone);
            node.add(childNode);
            recurseAddHierarchy(childNode);
        }
    }
    public class BoneNode extends Tree.Node<BoneNode,AnimatedSpriteBone,HorizontalGroup>{
        public final Label label;
        public BoneNode(AnimatedSpriteBone bone){
            super(new HorizontalGroup());
            this.label = new Label(bone.name, ISpriteMain.getSkin());
            getActor().addActor(label);
            TextButton colorButton = new TextButton(" ", ISpriteMain.getSkin());
            colorButton.setColor(bone.color);
            colorButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Random random = new Random();
                    Color newColor = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f);
                    colorButton.setColor(newColor);
                    bone.color = newColor;
                }
            });
            getActor().addActor(colorButton);
            getActor().addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if(this.getTapCount() == 2){
                        TextField input = new TextField(bone.name, ISpriteMain.getSkin());
                        Dialog dialog = new Dialog("Enter new name", ISpriteMain.getSkin(), "dialog") {
                            public void result(Object obj) {
                                if (obj instanceof String) {
                                    bone.name = input.getText();
                                    label.setText(bone.name);
                                }
                            }
                        };
                        dialog.setMovable(false);
                        dialog.button("Cancel");
                        dialog.button("Ok", "");
                        dialog.getContentTable().add(input);
                        dialog.show(stage);
                    }
                }
            });
            setValue(bone);
        }
    }
    @Override
    public void render() {
        super.render();
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;

        spriteBatch.begin();
        sprite.image.drawPreview(spriteBatch);
        spriteBatch.end();
        polygonSpriteBatch.begin();
        sprite.image.draw(polygonSpriteBatch, EMPTY_POSE);
        polygonSpriteBatch.end();

        HashMap<UUID, Transform> transforms = EMPTY_POSE.getBoneTransforms(sprite, new Transform().lock());
        UUID moused = null;
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);
        for(Map.Entry<UUID, Transform> entry : transforms.entrySet()){
            if(entry.getValue().translation.dst(worldMouse) < 10f){
                moused = entry.getKey();
            }
        }

        shapeRenderer.begin();
        UUID finalMoused = moused;
        EMPTY_POSE.drawDebugBones(sprite, shapeRenderer, uuid -> uuid.equals(finalMoused)? Color.RED:(tree.getSelection().contains(boneNodes.get(uuid))?Color.BLUE:Color.GREEN));
        sprite.image.debugDraw(shapeRenderer);
        shapeRenderer.end();

        if(Gdx.input.isKeyJustPressed(Input.Keys.V)){
            sprite.image.addPoint(worldMouse.cpy(), sprite.rootBone);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.B)){
            float speed = 0.5f * Gdx.graphics.getDeltaTime();
            for(VertexedImage.Vertex vertex : sprite.image.points) {
                if(vertex.position.dst(worldMouse) > 10f)
                    continue;
                if(!tree.getSelection().isEmpty()){
                    vertex.addWeight(tree.getSelection().getLastSelected().getValue().id, speed/tree.getSelection().size());
                }
            }
        }
        if(Gdx.input.isKeyPressed(Input.Keys.N)){
            sprite.image.transform.translation.add(Gdx.input.getDeltaX(), -Gdx.input.getDeltaY());
        }


        if(movingId != null){
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                Transform parentTransform = transforms.get(movingBone.parent);
                movingBone.baseTransform.translation.set(worldMouse.cpy().sub(parentTransform.translation).rotateRad(-parentTransform.rotation));
            }
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
            if(movingId == null)
                movingId = moused;
            else
                movingId = null;
        }
        if(moused != null){
            if(Gdx.input.isKeyJustPressed(Input.Keys.D)){
                sprite.removeNode(sprite.bones.get(moused));
                boneNodes.remove(moused).remove();
            }
            if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
                BoneNode node = boneNodes.get(moused);
                movingId = sprite.addChildNodeTo(sprite.bones.get(moused)).id;
                BoneNode newNode = new BoneNode(sprite.bones.get(movingId));
                node.add(newNode);
                boneNodes.put(movingId, newNode);
            }
        }
    }

    @Override
    public boolean scrolled(float v, float v1) {
        if(movingId != null){
            AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                movingBone.baseTransform.rotation -= v1/10f;
            }
            return true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.N)){
            ISpriteMain.getInstance().sprite.image.transform.rotation -= v1/10f;
        }
        return super.scrolled(v, v1);
    }
}
