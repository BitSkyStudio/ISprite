package com.github.bitsky;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ShortArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VertexedImage {
    public final Texture texture;
    public ArrayList<Vertex> points;
    public Transform transform;
    public VertexedImage(Texture texture) {
        this.texture = texture;
        this.points = new ArrayList<>();
        this.transform = new Transform().lock();
    }
    public void addPoint(Vector2 position, AnimatedSpriteBone bone){
        HashMap<UUID,Float> weights = new HashMap<>();
        weights.put(bone.id, 1f);
        Vector3 vec = new Vector3(position.x, position.y, 0);
        vec.prj(getTransformMatrix().inv());
        this.points.add(new Vertex(new Vector2(vec.x, vec.y), weights));
    }
    private Matrix4 getTransformMatrix(){
        Matrix4 matrix = new Matrix4();
        matrix.translate(transform.translation.x + texture.getWidth()/2f, transform.translation.y + texture.getHeight()/2f, 0);
        matrix.rotateRad(Vector3.Z, transform.rotation);
        matrix.translate(-texture.getWidth()/2f, -texture.getHeight()/2f, 0);
        matrix.scl(transform.scale);
        return matrix;
    }
    public void drawPreview(SpriteBatch spriteBatch){
        spriteBatch.setTransformMatrix(getTransformMatrix());
        spriteBatch.setColor(1f, 1f, 1f, 0.3f);
        spriteBatch.draw(texture, 0, 0);
        spriteBatch.setTransformMatrix(new Matrix4());
    }
    public void draw(PolygonSpriteBatch polygonSpriteBatch, AnimatedSpritePose pose){
        float[] vertices = new float[points.size()*2];
        for(int i = 0;i < points.size();i++){
            vertices[(i*2)] = points.get(i).position.x;
            vertices[(i*2)+1] = points.get(i).position.y;
        }
        ShortArray indices = new DelaunayTriangulator().computeTriangles(vertices, true);
        PolygonRegion polygonRegion = new PolygonRegion(new TextureRegion(texture), vertices, indices.shrink());

        HashMap<UUID, Transform> defaultPositions = BoneEditor.EMPTY_POSE.getBoneTransforms(ISpriteMain.getInstance().sprite, new Transform().lock());
        HashMap<UUID, Transform> posePositions = pose.getBoneTransforms(ISpriteMain.getInstance().sprite, new Transform().lock());

        for(int i = 0;i < points.size();i++){
            Vector2 vertex = new Vector2(vertices[i*2], vertices[i*2+1]);
            Vector2 outputVertex = new Vector2();
            for(Map.Entry<UUID, Float> weightEntry : points.get(i).weights.entrySet()){
                float angleDiff = posePositions.get(weightEntry.getKey()).rotation-defaultPositions.get(weightEntry.getKey()).rotation;
                Vector2 positionDifference = vertex.cpy().sub(defaultPositions.get(weightEntry.getKey()).translation);
                positionDifference.rotateRad(angleDiff);
                outputVertex.add(positionDifference.add(posePositions.get(weightEntry.getKey()).translation).scl(weightEntry.getValue()));
            }
            vertices[i*2] = outputVertex.x;
            vertices[i*2+1] = outputVertex.y;
        }
        polygonSpriteBatch.setTransformMatrix(getTransformMatrix());
        polygonSpriteBatch.draw(polygonRegion, 0, 0);
        polygonSpriteBatch.setTransformMatrix(new Matrix4());
    }
    public void debugDraw(ShapeRenderer shapeRenderer){
        shapeRenderer.setTransformMatrix(getTransformMatrix());
        shapeRenderer.setColor(Color.PURPLE);
        shapeRenderer.rect(0, 0, texture.getWidth(), texture.getHeight());

        float[] vertices = new float[points.size()*2];
        for(int i = 0;i < points.size();i++){
            vertices[(i*2)] = points.get(i).position.x;
            vertices[(i*2)+1] = points.get(i).position.y;
        }
        ShortArray indices = new DelaunayTriangulator().computeTriangles(vertices, false);
        shapeRenderer.setColor(Color.YELLOW);
        for(int i = 0;i < indices.size/3;i++){
            Vector2 v1 = points.get(indices.get(i*3)).position;
            Vector2 v2 = points.get(indices.get(i*3+1)).position;
            Vector2 v3 = points.get(indices.get(i*3+2)).position;
            shapeRenderer.line(v1, v2);
            shapeRenderer.line(v2, v3);
            shapeRenderer.line(v1, v3);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for(Vertex point : points) {
            float start = 0;
            for(Map.Entry<UUID, Float> entry : point.weights.entrySet()) {
                shapeRenderer.setColor(ISpriteMain.getInstance().sprite.bones.get(entry.getKey()).color);
                shapeRenderer.arc(point.position.x, point.position.y, 8, start*360f, entry.getValue()*360);
                start += entry.getValue();
            }
        }
        shapeRenderer.end();
        shapeRenderer.begin();
        shapeRenderer.setTransformMatrix(new Matrix4());
    }
    public static class Vertex{
        public Vector2 position;
        public HashMap<UUID,Float> weights;
        public Vertex(Vector2 position, HashMap<UUID, Float> weights) {
            this.position = position;
            this.weights = weights;
        }
        public void addWeight(UUID bone, float value){
            value += weights.getOrDefault(bone, 0f);
            if(value >= 1){
                weights.clear();
                weights.put(bone, 1f);
                return;
            }
            weights.remove(bone);

            float sum = 0;
            for(Map.Entry<UUID, Float> w : weights.entrySet()){
                sum += w.getValue();
            }
            for(UUID id : weights.keySet()){
                weights.put(id, weights.get(id)/(sum/(1-value)));
            }
            weights.put(bone, value);
        }
    }
}

