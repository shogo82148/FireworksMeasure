package net.sorablue.shogo.FWMeasure;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class PlaceSettingOverlay extends Overlay {
	private final Bitmap bmp;
	private GeoPoint gpoint;
	 
	public PlaceSettingOverlay(Bitmap bmp, GeoPoint gp) {
		this.bmp = bmp;
	    this.gpoint = gp;
	}
	 

	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		Projection pro = mapView.getProjection();//Mapと画面の位置を計算するオブジェクト
	    Point p = pro.toPixels(gpoint, null);    //ロケーションから、表示する位置を計算する
	    canvas.drawBitmap(bmp, p.x-bmp.getWidth()/2, p.y-bmp.getHeight(), null);  //表示する場所へ画像を配置する。
	}
	
	@Override
	public boolean onTap(GeoPoint gp, MapView mapView) {
		setLocation(gp);
		return true;
	}
	
	public GeoPoint getLocation() {
		return gpoint;
	}
	
	public GeoPoint setLocation(GeoPoint gp) {
		gpoint = gp;
		return gp;
	}
}
