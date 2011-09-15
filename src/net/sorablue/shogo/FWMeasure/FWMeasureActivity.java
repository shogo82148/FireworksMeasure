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
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single view: an EditText that
 * displays and edits some internal text.
 */
public class FWMeasureActivity extends Activity {

	private PowerManager pm;
	private WakeLock lock;
	
    public FWMeasureActivity() {
    }

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //カメラ起動
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Overlay overlay = new Overlay(this);
        CameraPreview preview = new CameraPreview(this, overlay);
        setContentView(preview);
        
        //オーバーレイ追加
        addContentView(overlay, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        //パワーマネージャを取得
        pm = (PowerManager)getSystemService(POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "FWMeasure");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //画面を常にON状態に
        lock.acquire();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
        lock.release();
    }
}
