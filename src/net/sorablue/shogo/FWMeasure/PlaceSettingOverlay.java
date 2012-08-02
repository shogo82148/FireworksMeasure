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
		Projection pro = mapView.getProjection();//Map�Ɖ�ʂ̈ʒu���v�Z����I�u�W�F�N�g
	    Point p = pro.toPixels(gpoint, null);    //���P�[�V��������A�\������ʒu���v�Z����
	    canvas.drawBitmap(bmp, p.x-bmp.getWidth()/2, p.y-bmp.getHeight(), null);  //�\������ꏊ�։摜��z�u����B
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
