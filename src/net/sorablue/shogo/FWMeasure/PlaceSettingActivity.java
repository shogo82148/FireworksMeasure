package net.sorablue.shogo.FWMeasure;

import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class PlaceSettingActivity extends MapActivity {
	private static final int MENU_ID_SET = (Menu.FIRST + 1);
	private static final int MENU_ID_MYLOCATION = (Menu.FIRST + 2);

	private MyLocationOverlay location_overlay;
	private PlaceSettingOverlay setting_overlay;
	private MapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        mapView = (MapView)findViewById(R.id.map);
        mapView.setEnabled(true);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);
        
    	List<com.google.android.maps.Overlay> list = mapView.getOverlays();
        
        //現在地を表示
    	location_overlay = new MyLocationOverlay(getApplicationContext(), mapView);
        location_overlay.onProviderEnabled(LocationManager.GPS_PROVIDER); //GPSを使用
        location_overlay.enableMyLocation();
        list.add(location_overlay);
        
        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", 0);
    	double longitude = intent.getDoubleExtra("longitude", 0);
		GeoPoint gp = 
				new GeoPoint((int)(latitude*1E6),
					         (int)(longitude*1E6));
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.flag);
    	setting_overlay = new PlaceSettingOverlay(bmp, gp);
    	list.add(setting_overlay);
    }
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューアイテムを追加します
        menu.add(Menu.NONE, MENU_ID_SET, Menu.NONE, getString(R.string.map_set_location));
        menu.add(Menu.NONE, MENU_ID_MYLOCATION, Menu.NONE, getString(R.string.map_set_myLocation));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
        default:
            ret = super.onOptionsItemSelected(item);
            break;
        case MENU_ID_SET:
        {
        	GeoPoint gp = setting_overlay.getLocation();
        	Intent intent = new Intent();
        	intent.putExtra("latitude", gp.getLatitudeE6()*1e-6);
        	intent.putExtra("longitude", gp.getLongitudeE6()*1e-6);
        	setResult(RESULT_OK, intent);
        	finish();
            break;
        }
        case MENU_ID_MYLOCATION:
        {
       		GeoPoint gp = location_overlay.getMyLocation();
       		setting_overlay.setLocation(gp);
       		mapView.getController().animateTo(gp);
       		break;
        }
        }
        return ret;
    }
}
