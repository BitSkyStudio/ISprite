package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import games.spooky.gdx.nativefilechooser.NativeFileChooserCallback;
import games.spooky.gdx.nativefilechooser.NativeFileChooserConfiguration;
import games.spooky.gdx.nativefilechooser.NativeFileChooserIntent;
import games.spooky.gdx.nativefilechooser.NativeFilesChooserCallback;
import org.json.JSONObject;

import java.util.*;

public class BoneEditor extends Editor {
    public static AnimatedSpritePose EMPTY_POSE = new AnimatedSpritePose(new HashMap<>());
    private UUID movingId;
    private Table boneHierarchyTable;
    private Tree<BoneNode,AnimatedSpriteBone> tree;
    private HashMap<UUID,BoneNode> boneNodes;
    private List<VertexedImage> imageList;

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
        this.boneHierarchyTable.add(tree).row();
        this.imageList = new List<>(ISpriteMain.getSkin()){
            @Override
            public String toString(VertexedImage object) {
                return object.name;
            }
        };
        this.imageList.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(this.getTapCount() == 2 && imageList.getSelected() != null){
                    TextField input = new TextField(imageList.getSelected().name, ISpriteMain.getSkin());
                    Dialog dialog = new Dialog("Enter new name", ISpriteMain.getSkin(), "dialog") {
                        public void result(Object obj) {
                            if (obj instanceof String) {
                                imageList.getSelected().name = input.getText();
                                refreshImages();
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
        boneHierarchyTable.add(imageList).row();
        TextButton shiftUpImageButton = new TextButton("Shift Image Up", ISpriteMain.getSkin());
        shiftUpImageButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(imageList.getSelected() != null){
                    Collections.swap(sprite.images, imageList.getSelectedIndex(), imageList.getSelectedIndex()==0?imageList.getItems().size-1:imageList.getSelectedIndex()-1);
                    refreshImages();
                }
            }
        });
        boneHierarchyTable.add(shiftUpImageButton).row();
        TextButton shiftDownImageButton = new TextButton("Shift Image Down", ISpriteMain.getSkin());
        shiftDownImageButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(imageList.getSelected() != null){
                    Collections.swap(sprite.images, imageList.getSelectedIndex(), imageList.getSelectedIndex()==imageList.getItems().size-1?0:imageList.getSelectedIndex()+1);
                    refreshImages();
                }
            }
        });
        boneHierarchyTable.add(shiftDownImageButton).row();
        TextButton removeImageButton = new TextButton("Remove Image", ISpriteMain.getSkin());
        removeImageButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(imageList.getSelected() != null){
                    sprite.images.remove(imageList.getSelected());
                    refreshImages();
                }
            }
        });
        boneHierarchyTable.add(removeImageButton).row();
        TextButton addImageButton = new TextButton("Add Image", ISpriteMain.getSkin());
        addImageButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
            NativeFileChooserConfiguration conf = new NativeFileChooserConfiguration();
            conf.mimeFilter = "image/png";
            conf.intent = NativeFileChooserIntent.OPEN;
            conf.title = "Import image";
            conf.directory = new FileHandle(".");
            ISpriteMain.getInstance().fileChooser.chooseFiles(conf, new NativeFilesChooserCallback() {
                @Override
                public void onCancellation() {

                }

                @Override
                public void onError(Exception e) {

                }

                @Override
                public void onFilesChosen(Array<FileHandle> files) {
                    for(FileHandle fileHandle : files) {
                        sprite.images.add(new VertexedImage(new Texture(fileHandle), fileHandle.name()));
                        refreshImages();
                        imageList.setSelectedIndex(imageList.getItems().size - 1);
                    }
                }
            });
            }
        });
        boneHierarchyTable.add(addImageButton);
        refreshImages();
    }
    private void refreshImages(){
        imageList.setItems(ISpriteMain.getInstance().sprite.images.toArray(VertexedImage[]::new));
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
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;

        VertexedImage selectedImage = imageList.getSelected();

        spriteBatch.begin();
        if(selectedImage != null)
            selectedImage.drawPreview(spriteBatch);
        spriteBatch.end();
        polygonSpriteBatch.begin();
        for(VertexedImage image : sprite.images)
            image.draw(polygonSpriteBatch, EMPTY_POSE, 0, 0);
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
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        UUID finalMoused = moused;
        EMPTY_POSE.drawDebugBones(sprite, shapeRenderer, uuid -> uuid.equals(finalMoused)? Color.RED:(tree.getSelection().contains(boneNodes.get(uuid))?Color.BLUE:Color.GREEN));
        if(selectedImage != null)
            selectedImage.debugDraw(shapeRenderer);
        shapeRenderer.end();

        if(Gdx.input.isKeyJustPressed(Input.Keys.V) && selectedImage != null){
            selectedImage.addPoint(worldMouse.cpy(), sprite.rootBone);
        }

        if(Gdx.input.isKeyPressed(Input.Keys.B) && selectedImage != null){
            float speed = 0.5f * Gdx.graphics.getDeltaTime() * (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)?3:1);
            Matrix4 matrix = selectedImage.getTransformMatrix();
            for(VertexedImage.Vertex vertex : selectedImage.points) {
                Vector3 pos = new Vector3(vertex.position.x, vertex.position.y, 0);
                pos.prj(matrix);
                if(pos.dst(worldMouse3) > 10f)
                    continue;
                if(!tree.getSelection().isEmpty()){
                    vertex.addWeight(tree.getSelection().getLastSelected().getValue().id, speed/tree.getSelection().size());
                }
            }
        }
        if(Gdx.input.isKeyPressed(Input.Keys.H) && selectedImage != null){
            Matrix4 matrix = selectedImage.getTransformMatrix();
            selectedImage.points.removeIf(vertex -> {
                Vector3 pos = new Vector3(vertex.position.x, vertex.position.y, 0);
                pos.prj(matrix);
                return pos.dst(worldMouse3) < 10f;
            });
        }
        if(Gdx.input.isKeyPressed(Input.Keys.N) && selectedImage != null){
            selectedImage.transform.translation.add(ISpriteMain.getMouseDeltaX(), -ISpriteMain.getMouseDeltaY());
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
                AnimatedSpriteBone mousedBone = sprite.bones.get(moused);
                if(mousedBone.children.isEmpty()) {
                    boolean used = false;
                    for(VertexedImage image : sprite.images){
                        for(VertexedImage.Vertex vertex : image.points){
                            for(UUID id : vertex.weights.keySet()){
                                if(id.equals(mousedBone.id)){
                                    used = true;
                                    break;
                                }
                            }
                            if(used)
                                break;
                        }
                    }
                    if(!used) {
                        sprite.removeNode(sprite.bones.get(moused));
                        boneNodes.remove(moused).remove();
                    }
                }
            }
            if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
                BoneNode node = boneNodes.get(moused);
                movingId = sprite.addChildNodeTo(sprite.bones.get(moused)).id;
                BoneNode newNode = new BoneNode(sprite.bones.get(movingId));
                node.add(newNode);
                boneNodes.put(movingId, newNode);
            }
        }
        super.render();
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
            if(imageList.getSelected() != null)
                imageList.getSelected().transform.rotation -= v1/10f;
        }
        return super.scrolled(v, v1);
    }
}
