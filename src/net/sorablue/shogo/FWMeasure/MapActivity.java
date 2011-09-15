package net.sorablue.shogo.FWMeasure;

import android.os.Bundle;

import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MapActivity extends com.google.android.maps.MapActivity{
	MapController m_controller;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        MapView m = (MapView)findViewById(R.id.map);
        m.setEnabled(true);
        m.setClickable(true);
        m_controller = m.getController();
    }
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
