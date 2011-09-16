package com.rj.pixelesque;

import android.graphics.Color;


public class PixelArtState {
	public final static int PENCIL = 1;
	public final static int ERASER = 2;
	public final static int LINE = 3;
	public final static int RECTANGLE = 4;
	public final static int DRAW = 5;
	
	public int mode = DRAW;
	
	
	public int selectedColor = Color.CYAN;
	
	
	
}
