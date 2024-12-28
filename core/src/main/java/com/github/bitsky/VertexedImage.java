package com.github.bitsky;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;

import java.util.ArrayList;

public class VertexedImage {
    public final Texture texture;
    public ArrayList<Vector2> points;
    public VertexedImage(Texture texture) {
        this.texture = texture;
        this.points = new ArrayList<>();
    }
    public void draw(PolygonSpriteBatch polygonSpriteBatch, float x, float y){
        float[] vertices = new float[points.size()*2];
        for(int i = 0;i < points.size();i++){
            vertices[(i*2)] = points.get(i).x;
            vertices[(i*2)+1] = points.get(i).y;
        }
        ShortArray indices = new DelaunayTriangulator().computeTriangles(vertices, true);
        PolygonRegion polygonRegion = new PolygonRegion(new TextureRegion(texture), vertices, indices.items);
        polygonSpriteBatch.draw(polygonRegion, x, y);
    }
    public void debugDraw(ShapeRenderer shapeRenderer, float x, float y){
        shapeRenderer.setColor(Color.PURPLE);
        shapeRenderer.rect(x, y, x+texture.getWidth(), y+texture.getHeight());

        float[] vertices = new float[points.size()*2];
        for(int i = 0;i < points.size();i++){
            vertices[(i*2)] = points.get(i).x;
            vertices[(i*2)+1] = points.get(i).y;
        }
        ShortArray indices = new DelaunayTriangulator().computeTriangles(vertices, false);
        shapeRenderer.setColor(Color.YELLOW);
        for(int i = 0;i < indices.size/3;i++){
            Vector2 v1 = points.get(indices.get(i*3));
            Vector2 v2 = points.get(indices.get(i*3+1));
            Vector2 v3 = points.get(indices.get(i*3+2));
            shapeRenderer.line(v1, v2);
            shapeRenderer.line(v2, v3);
            shapeRenderer.line(v1, v3);
        }

        shapeRenderer.setColor(Color.RED);
        for(Vector2 point : points) {
            shapeRenderer.circle(point.x, point.y, 5);
        }
    }
}

