package com.example.paint;

import javafx.scene.input.MouseEvent;

public class HandTool implements Tool {
    private double lastSceneX, lastSceneY;

    @Override public String getName(){ return "Pan"; }

    @Override
    public void onPress(CanvasState s, HistoryManager h, MouseEvent e){
        lastSceneX = e.getSceneX();
        lastSceneY = e.getSceneY();
    }

    @Override
    public void onDrag(CanvasState s, HistoryManager h, MouseEvent e){
        double dxScene = e.getSceneX() - lastSceneX;
        double dyScene = e.getSceneY() - lastSceneY;

        // Convert scene delta → container delta (stable regardless of zoom)
        s.panByScene(dxScene, dyScene);

        lastSceneX = e.getSceneX();
        lastSceneY = e.getSceneY();
    }
}
