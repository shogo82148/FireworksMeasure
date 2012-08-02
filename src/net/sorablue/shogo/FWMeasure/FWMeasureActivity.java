/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sorablue.shogo.FWMeasure;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class FWMeasureActivity extends Activity{
	public static final String PREFERENCES_NAME = "FireworksMeasure";
	
	private static final int MENU_ID_SETTING = (Menu.FIRST + 1);

	private PowerManager pm;
	private WakeLock lock;
	private LocationManager locationManager;
	private Overlay overlay;
	private CameraPreview preview;
	
    public FWMeasureActivity() {
    }

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //�J�����N��
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        overlay = new Overlay(this);
        preview = new CameraPreview(this, overlay);
        setContentView(preview);
        
        //�I�[�o�[���C�ǉ�
        addContentView(overlay, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        //�p���[�}�l�[�W�����擾
        pm = (PowerManager)getSystemService(POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "FWMeasure");
    }

    @Override
    public void onStart() {
    	super.onStart();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, // �v���o�C�_
    		 0, // �ʒm�̂��߂̍ŏ����ԊԊu
    		 0, // �ʒm�̂��߂̍ŏ������Ԋu
    		 overlay); // �ʒu��񃊃X�i�[
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	locationManager.removeUpdates(overlay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlay.loadSettings();
        //��ʂ����ON��Ԃ�
        lock.acquire();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
        lock.release();
    }
    
	// �I�v�V�������j���[���ŏ��ɌĂяo����鎞��1�x�����Ăяo����܂�
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // ���j���[�A�C�e����ǉ����܂�
        menu.add(Menu.NONE, MENU_ID_SETTING, Menu.NONE, "�ݒ�");
        return super.onCreateOptionsMenu(menu);
    }

    // �I�v�V�������j���[�A�C�e�����I�����ꂽ���ɌĂяo����܂�
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
        default:
            ret = super.onOptionsItemSelected(item);
            break;
        case MENU_ID_SETTING:
            showSettingActivity();
            break;
        }
        return ret;
    }

    private void showSettingActivity() {
		final Intent intent = new Intent(this,
	       		SettingActivity.class);
		startActivity(intent);
    }
}
