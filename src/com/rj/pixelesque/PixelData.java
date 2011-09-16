package com.rj.pixelesque;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import android.graphics.Color;
import android.util.Log;

import com.rj.pixelesque.History.HistoryAction;
import com.rj.processing.mt.Cursor;

public class PixelData {
	public static final int MAX_BACKSTACK = 3;
	public ColorStack[][] data;
	int width; int height;
	public History history;
	public ArrayList<Cursor> cursors = new ArrayList<Cursor>();
	public float scale;
	public float topx, topy;
	public boolean outline;
	public String name;
	

	
	public static class ColorStack {
		private ArrayList<Integer> ints = new ArrayList<Integer>(MAX_BACKSTACK);
		
		public void pushColor(int color) {
			ints.add(color);
		}
		
		public int pushUnderneathColor() {
			if (ints.size() > 1) { 
				int lastcolor = ints.get(ints.size()-2);
				ints.add(lastcolor);
				return lastcolor;
			}
			return 0;
		}
		
		public void popColor() {
			if (ints.size() > 0) {
				ints.remove(ints.size()-1);
			}
		}
		
		public int getLastColor() {
			if (ints.size() > 0)
				return ints.get(ints.size()-1);
			return Color.TRANSPARENT;
		}
		
		public void clearStack(int tocolor) {
			ints.clear();
			ints.add(tocolor);
		}
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (int i : ints) {
				b.append(i+",");
			}
			return b.toString();
		}
	}

	public PixelData() {
		this(12,16);
	}
	
	public PixelData(PImage image, String name) {
		this(image.width, image.height);
		image.loadPixels();
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				int color = image.get(i, j);
				data[i][j].pushColor(color);
			}
		}
		this.name = name;
	}
	
	public PixelData(int width, int height) {
		this.width = width;
		this.height = height;
		data = new ColorStack[width][];
		for (int i = 0; i < data.length; i++) {
			data[i] = new ColorStack[height];
			for (int j = 0; j < data[i].length; j++) {
				data[i][j] = new ColorStack();
				data[i][j].pushColor(Color.TRANSPARENT);
			}
		}
		topx = 0;
		topy = 0;
		scale = 1;
		outline = true;
		history = new History(this);
	}

	public PImage render(PApplet papp) {
		return render(papp, width, height);
	}
	public PImage render(PApplet papp, int width, int height) {
		PGraphics p = papp.createGraphics(width, height, PApplet.P2D);
		p.beginDraw();
		
		float boxsize = getBoxsize(width, height, 1);
		Log.d("PixelData", "Rendering: boxsize "+boxsize+" widthheight:"+width+"x"+height+"   ");
		
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				int color = data[x][y].getLastColor();
				p.noStroke();
				p.fill(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color));
				p.rect(boxsize * x, boxsize * y, boxsize, boxsize);
			}
		}

		p.endDraw();
		return p.get();
	}
	
	
	public void draw(PApplet p) {
		if (data == null || data[0] == null) return;
		
		float boxsize = getBoxsize(p.width, p.height);

		for (int x = 0; x < data.length; x++) {

			for (int y = 0; y < data[x].length; y++) {
				float left = topx + boxsize * x;
				float top = topy + boxsize * y;
				if (top + boxsize > 0 && left + boxsize > 0 && top < p.height && left < p.width) { 
					if (outline) p.stroke(127);
					int color = data[x][y].getLastColor();
					p.fill(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color));
					p.rect(left, top, boxsize, boxsize);
				}
			}
		}
		synchronized(cursors) {
			for (Cursor c : cursors) {
				int[] coords = getDataCoordsFromXY(p, c.currentPoint.x, c.currentPoint.y);
				int x = coords[0]; int y = coords[1];
				if (isValid(x,y)) {
					float extra = 30;
					p.fill(255,255,255,80);
					p.rect(topx + boxsize * x - extra, topy + boxsize * y - extra, boxsize + extra*2, boxsize + extra*2);
				}
			}
		}
	}
	
	public String dumpBoard() {
		StringBuilder b = new StringBuilder();
		
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				b.append(data[x][y].toString());
				b.append("|");
			}
			b.append("\n");
		}
		return b.toString();
	}
	
	
	float getBoxsize(float width, float height) {
		return getBoxsize(width, height, scale);
	}
	float getBoxsize(float width, float height, float scale) {
		float boxwidth = width / this.width * scale;
		float boxheight = height / this.height * scale;
		float boxsize = PApplet.min(boxwidth, boxheight);
		return boxsize;
	}
	
	float getWidth(PApplet p) {
		return getBoxsize(p.width, p.height) * this.width;
	}
	float getHeight(PApplet p) {
		return getBoxsize(p.width, p.height) * this.height;
	}
	
	int[] coords = new int[2];
	int[] getDataCoordsFromXY(PApplet p, float mousex, float mousey) {
		mousex -= this.topx;
		mousey -= this.topy;

		float boxsize = this.getBoxsize(p.width, p.height);
		int x = (int) (mousex / boxsize);
		int y = (int) (mousey / boxsize);

		int[] coords = new int[2];
		coords[0] = x;
		coords[1] = y;
		return coords;
	}
	/*
	x = (fx-tx)/s
	s * x = (fx-tx)
	s * x + tx = fx
	*/
	
	float[] getDataCoordsFloatFromXY(PApplet p, float mousex, float mousey) {
		mousex -= this.topx;
		mousey -= this.topy;

		float boxsize = this.getBoxsize(p.width, p.height);
		float x = (mousex / boxsize);
		float y = (mousey / boxsize);

		float[] coords = new float[2];
		coords[0] = x;
		coords[1] = y;
		return coords;
	}
	
	float[] getXYFromDataCoordsFloat(PApplet p, float x, float y) {

		float boxsize = this.getBoxsize(p.width, p.height);
		float mousex = (x * boxsize);
		float mousey = (y * boxsize);

		mousex += this.topx;
		mousey += this.topy;
		
		float[] coords = new float[2];
		coords[0] = mousex;
		coords[1] = mousey;
		return coords;
	}




	public void flipColor(int x, int y, int tocolor) {
		if (isValid(x,y)) {
			int c = data[x][y].getLastColor();
			if (c == tocolor) {
				int color = data[x][y].pushUnderneathColor();
				history.add(new History.HistoryAction(x,y,color));
			} else {
				data[x][y].pushColor(tocolor);
				history.add(new History.HistoryAction(x,y,tocolor));
			}
		} else {
		}
	}
	
	
	public void eraseColor(int x, int y) {
		setColor(x,y,Color.TRANSPARENT);
	}
	public void setColor(int x, int y, int color) {
		if (isValid(x,y)) {
			data[x][y].pushColor(color);
			history.add(new History.HistoryAction(x,y,color));
		}
	}
	
	public void rectangle(int x, int y, int width, int height, int color) {
		Log.d("PixelData", "Rectangle: x"+x+" y:"+y+" width:"+width+" height:"+height+" color:"+color+" ");
		if (isValid(x,y) && isValid(x+width, y+height)) {
			Log.d("PixelData", "Rectangle Valid : x"+x+" y:"+y+" width:"+width+" height:"+height+" color:"+color+" ");
			HistoryAction action = new HistoryAction();
			for (int i=x; i<=x+width; i++) {
				for (int j=y; j<=y+height; j++) {
					data[i][j].pushColor(color);
					action.addPoint(i, j, color);
				}
			}
			history.add(action);
		}
	}
	
	public boolean isValid(int x, int y) {
		return 	x >= 0 && x < this.width && y >= 0 && y < this.height;
	}
	public boolean isValid(int[] coords) {
		return isValid(coords[0], coords[1]);
	}
	
	public void addCursor(Cursor c) {
		synchronized(cursors) {
			cursors.add(c);
		}
	}
	
	public void removeCursor(Cursor c) {
		synchronized(cursors) {
			if (cursors.remove(c)) {
				return;
			} else {
				for (Cursor cc : cursors) {
					if (cc.curId == c.curId) {
						cursors.remove(cc);
						return;
					}
				}
			}
		}
	}
	
	public boolean hasCursor(Cursor c) {
		if ( cursors.contains(c) ) {
			return true;
		} else {
			synchronized(cursors) {
				for (Cursor cc : cursors) {
					if (cc.curId == c.curId) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void clearCursors() {
		synchronized(cursors) {
			cursors.clear();
		}
	}
	
	
	public void setName(String name) {
		this.name = name;
	}
}
