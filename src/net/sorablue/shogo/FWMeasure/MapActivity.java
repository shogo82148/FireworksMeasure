package net.sorablue.shogo.FWMeasure;

import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MapActivity extends com.google.android.maps.MapActivity{
	private static final int MENU_ID_SHARE = (Menu.FIRST + 1);
	private MapController m_controller;
	private double latitude, longitude;
	private String shareString;
	
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
      //icons from http://mapicons.nicolasmollet.com/
    	List<com.google.android.maps.Overlay> list = m.getOverlays();
        {
        	Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.flag);
        	MapOverlay overlay = new MapOverlay(bmp, gp);
        	list.add(overlay);
        }
        {
        	Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.fireworks);
        	MapOverlay overlay = new MapOverlay(bmp, new GeoPoint(
        			(int)(intent.getDoubleExtra("fw_latitude", 0)*1E6),
			        (int)(intent.getDoubleExtra("fw_longitude", 0)*1E6)
			        )	);
        	list.add(overlay);
        	String position = String.format("%.6f,%.6f",
    				intent.getDoubleExtra("fw_latitude", 0), intent.getDoubleExtra("fw_longitude", 0));
        	shareString = this.getString(R.string.msgShare, position);
        }
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	// オプションメニューが最初に呼び出される時に1度だけ呼び出されます
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューアイテムを追加します
        menu.add(Menu.NONE, MENU_ID_SHARE, Menu.NONE, getString(R.string.menuShare));
        return super.onCreateOptionsMenu(menu);
    }

    // オプションメニューアイテムが選択された時に呼び出されます
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
        default:
            ret = super.onOptionsItemSelected(item);
            break;
        case MENU_ID_SHARE:
            share();
            break;
        }
        return ret;
    }
    
    private void share() {
		final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, shareString);
		startActivity(Intent.createChooser(intent, getString(R.string.menuShare)));
    }
}
