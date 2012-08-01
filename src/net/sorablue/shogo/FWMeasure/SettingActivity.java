package net.sorablue.shogo.FWMeasure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

public class SettingActivity extends Activity {
	static final int SETTING_LOCATION = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        
        final SharedPreferences settings = getSharedPreferences(
        		FWMeasureActivity.PREFERENCES_NAME, MODE_PRIVATE);
		final SharedPreferences.Editor editor = settings.edit();
        
		WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		
        Button button = (Button) findViewById(R.id.buttonSetPlace);
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		setPlace();
            }
        });
        
        CheckBox checkAutoDetect = (CheckBox)findViewById(R.id.checkAutoDetect);
        checkAutoDetect.setChecked(settings.getBoolean("enableAutoDetect", false));
        checkAutoDetect.setOnCheckedChangeListener(
        		new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						editor.putBoolean("enableAutoDetect", isChecked);
						editor.commit();
					}
        		});
        SeekBar seekBarDetectRange = (SeekBar)findViewById(R.id.seekBarDetectRange);
        seekBarDetectRange.setMax(Math.min(disp.getWidth(), disp.getHeight())/2);
        seekBarDetectRange.setProgress(settings.getInt("detectionRange", 0));
        seekBarDetectRange.setOnSeekBarChangeListener(
        		new SeekBar.OnSeekBarChangeListener() {
					
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// do nothing
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// do nothing
					}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						editor.putInt("detectionRange", progress);
						editor.commit();
					}
				});
        SeekBar seekBarCameraThreshold = (SeekBar)findViewById(R.id.seekBarCameraThreshold);
        seekBarCameraThreshold.setMax(127);
        seekBarCameraThreshold.setProgress(settings.getInt("cameraThreshold", 64));
        seekBarCameraThreshold.setOnSeekBarChangeListener(
        		new SeekBar.OnSeekBarChangeListener() {
					
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// do nothing
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// do nothing
					}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						editor.putInt("cameraThreshold", progress);
						editor.commit();
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
