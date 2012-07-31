package net.sorablue.shogo.FWMeasure;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SettingActivity extends Activity {
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
		Intent intent = new Intent(this,
	       		PlaceSettingActivity.class);
		startActivity(intent);
    }
}
