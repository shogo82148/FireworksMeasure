package net.sorablue.shogo.FWMeasure;

import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class PlaceSettingActivity extends MapActivity {
	private MapController m_controller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        final MapView m = (MapView)findViewById(R.id.map);
        m.setEnabled(true);
        m.setClickable(true);
        m_controller = m.getController();
        
        //現在地を表示
        final MyLocationOverlay location_overlay =
                new MyLocationOverlay(getApplicationContext(), m);
        location_overlay.onProviderEnabled(LocationManager.GPS_PROVIDER); //GPSを使用
        location_overlay.enableMyLocation();
        location_overlay.runOnFirstFix(new Runnable() {
            public void run() {
                m.getController().animateTo(
                		location_overlay.getMyLocation()); // 現在位置を自動追尾する
            }
        });
        m.getOverlays().add(location_overlay);
    }
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
