package com.example.paint;

import javafx.scene.paint.Color;

public class ColorUtils {
    public static int argb(Color c){
        int a = (int)Math.round(c.getOpacity()*255)&0xFF;
        int r = (int)Math.round(c.getRed()*255)&0xFF;
        int g = (int)Math.round(c.getGreen()*255)&0xFF;
        int b = (int)Math.round(c.getBlue()*255)&0xFF;
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
}
