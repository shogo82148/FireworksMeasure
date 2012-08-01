package net.sorablue.shogo.FWMeasure;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SettingActivity extends Activity {
	static final int SETTING_LOCATION = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        
        Button button = (Button) findViewById(R.id.buttonSetPlace);
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		setPlace();
            }
        });
    }
    
    private void setPlace() {
		SharedPreferences settings = getSharedPreferences(FWMeasureActivity.PREFERENCES_NAME, MODE_PRIVATE);
		Intent intent = new Intent(this,
	       		PlaceSettingActivity.class);
		intent.putExtra("latitude", settings.getInt("latitude", 0));
		intent.putExtra("longitude", settings.getInt("longitude", 0));
		startActivityForResult(intent, SETTING_LOCATION);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	switch(requestCode) {
    		case SETTING_LOCATION:
    		{
    			SharedPreferences settings = getSharedPreferences(FWMeasureActivity.PREFERENCES_NAME, MODE_PRIVATE);
    			SharedPreferences.Editor editor = settings.edit();
    			editor.putInt("latitude", intent.getIntExtra("latitude", 0));
    			editor.putInt("longitude", intent.getIntExtra("longitude", 0));
    			editor.commit();
    		}
    			break;
    		default:
    			break;
    	}
    }
}
