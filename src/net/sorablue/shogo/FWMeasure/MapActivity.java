package net.sorablue.shogo.FWMeasure;

import android.content.Intent;
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
    }
    
    public void onStart() {
    	super.onStart();
        Intent intent = getIntent();
		latitude = intent.getDoubleExtra("latitude", 0);
		longitude = intent.getDoubleExtra("longitude", 0);
		Toast.makeText(this, latitude+","+longitude, Toast.LENGTH_LONG).show();

		GeoPoint gp = 
			new GeoPoint((int)(latitude*1E6),
				         (int)(longitude*1E6));
        m_controller.setCenter(gp);
    }
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
