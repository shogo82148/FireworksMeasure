package net.sorablue.shogo.FWMeasure;

import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MapActivity extends com.google.android.maps.MapActivity{
	private MapController m_controller;
	private double latitude, longitude;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        MapView m = (MapView)findViewById(R.id.map);
        m.setEnabled(true);
        m.setClickable(true);
        m_controller = m.getController();
        
        //引数の取得
        Intent intent = getIntent();
		latitude = intent.getDoubleExtra("latitude", 0);
		longitude = intent.getDoubleExtra("longitude", 0);
		
		//表示位置の設定
		GeoPoint gp = 
			new GeoPoint((int)(latitude*1E6),
				         (int)(longitude*1E6));
        m_controller.setCenter(gp);
		
     // 画像を地図上に配置するオーバーレイ
    	List<com.google.android.maps.Overlay> list = m.getOverlays();
        {
        	Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.violet);
        	MapOverlay overlay = new MapOverlay(bmp, gp);
        	list.add(overlay);
        }
        {
        	Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.violet);
        	MapOverlay overlay = new MapOverlay(bmp, new GeoPoint(
        			(int)(intent.getDoubleExtra("fw_latitude", 0)*1E6),
			        (int)(intent.getDoubleExtra("fw_longitude", 0)*1E6)
			        )	);
        	list.add(overlay);
        }        
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
