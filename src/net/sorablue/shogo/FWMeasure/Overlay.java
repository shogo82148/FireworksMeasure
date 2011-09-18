package net.sorablue.shogo.FWMeasure;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.PreviewCallback;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class Overlay extends View implements SensorEventListener, PreviewCallback, LocationListener {
	private final int REPEAT_INTERVAL = 100;
	private final int MESSAGE_WHAT = 100;
	
	//センサを扱うためのフィールド
	private SensorManager manager;		//センサ管理のクラス
	private List<Sensor> temp_sensors;	//温度センサ
	private float temp = 20;			//現在の温度
	private List<Sensor> acc_sensors;	//加速度センサ
	private float[] accValues = new float[3];	//加速度センサの値
	private List<Sensor> mag_sensors;	//磁気センサ
	private float[] magValues = new float[3];	//磁気センサの値
	
	//端末の姿勢
	private float[] tmpR = new float[16];
	private float[] R = new float[16];
	private float[] orientation = new float[3];
	
	//GPS情報
	private Location location;
	
	//private int threshold = 20;
	//private long last_light = Long.MAX_VALUE;
	private int width, height;
	
	protected boolean isRepeat = false; //True:タイマ動作中 False:タイマ停止
	protected long startTime;	//計測を開始したときの時刻
	
	private final double earthR = 6378137; //地球の半径
	private final boolean debug = false;
	
	private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (isRepeat) {
            	invalidate();
            	handler.sendMessageDelayed(obtainMessage(), REPEAT_INTERVAL);
            }
        }
    };
	
	public Overlay(Context context) {
		super(context);
		
		setDrawingCacheEnabled(true);
		
		//センサの設定
		manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		acc_sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		temp_sensors = manager.getSensorList(Sensor.TYPE_TEMPERATURE);
		mag_sensors = manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		
		stopTimer();
	}
	
	/**
	 * タイマ動作を開始する
	 */
	private void startTimer() {
		//タイマの起動
		isRepeat = true;
		Message message = new Message();
        message.what = MESSAGE_WHAT;
		handler.sendMessageDelayed(message, REPEAT_INTERVAL);
		startTime = SystemClock.uptimeMillis();
		
		//センサの取得開始
		if(acc_sensors!=null && acc_sensors.size()>0)
			manager.registerListener(this, acc_sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
		if(mag_sensors!=null && mag_sensors.size()>0)
			manager.registerListener(this, mag_sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
		if(temp_sensors!=null && temp_sensors.size()>0)
			manager.registerListener(this, temp_sensors.get(0), SensorManager.SENSOR_DELAY_UI);
	}
	
	/**
	 * タイマ動作を停止する
	 */
	private void stopTimer() {
		isRepeat = false;
		manager.unregisterListener(this);
		//last_light = Long.MAX_VALUE;
	}
	
	/**
	 * タイマが起動してからの経過時間を返す
	 * @return タイマを開始してからの経過時間(ms)。
	 */
	private long getTime() {
		return SystemClock.uptimeMillis() - startTime;
	}
	
	/**
	 * 現在の仰角を返す。
	 * @return 現在の仰角[rad]
	 */
	private double getElevation() {
		return -orientation[1];
	}
	
	/**
	 * 現在の方位を返す
	 * @return 北からの角度[rad]
	 */
	private double getCompus() {
		/* 国土地理院　地磁気測量 2000年のデータに基づく近似
		 * D2000.0=7°37.142'+21.622'Δφ-7.672'Δλ+0.442'Δφ2-0.320'ΔφΔλ-0.675'Δλ2
		 * Δφ=φ-37°N 、Δλ=λ-138°E */
		double latitude = 0, longitude = 0;
		if(location!=null) {
			latitude = location.getLatitude() - 37.0;
			longitude = location.getLongitude() - 138.0;
		}
		double d = 7.61903 + 0.36037 * latitude - 0.12787 * longitude
			+ 0.00737 * latitude * latitude
			- 0.00533 * latitude * longitude
			- 0.01125 * longitude * longitude;
		double ret = orientation[0] - Math.toRadians(d);
		if(ret<-Math.PI) {
			ret += 2 * Math.PI;
		}
		return ret;
	}
	
	private String getCompusName(double compus) {
		int t = (int)(compus/Math.PI*8 + 8.5) % 16;
		String[] name = {"南", "南南西", "南西", "西南西", "西", "西北西", "北西", "北北西", "北", "北北東", "北東", "東北東", "東", "東南東", "南東", "南南東"};
		return name[t];
	}
	
	private String getCompusString(double compus) {
		int t = (int)Math.floor(Math.toDegrees(compus)+0.5);
		if(0<=t && t<=90) {
			return "N" + t + "E";
		} else if(t>90) {
			return "S" + (180-t) + "E";
		} else if(t>=-90) {
			return "N" + (-t) + "W";
		}
		return "S" + (180+t) + "W";
	}
	
	@Override
	protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
		this.width = width;
		this.height = height;
	}
	
	/**
	 * 描画時に呼び出されるメソッド
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.TRANSPARENT);
		
		//画面中央にラインを描画
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		canvas.drawLine(0, height/2, width, height/2, paint);
		canvas.drawLine(width/2, 0, width/2, height, paint);
		
		if(location == null) {
			canvas.drawText("現在位置を取得中...", 0, 15, paint);
		}
		
		String message = "";
		if(isRepeat) {
			//タイマ動作中のメッセージ
			message = "花火を画面中央に入れ、音がしたら画面をタッチ(" + 
				getTime() + "ms, 仰角:" + 
				(int)(Math.toDegrees(getElevation())+0.5) + "度, 方位:" +
				getCompusString(getCompus());
		} else {
			//タイマ停止中のメッセージ
			message = "花火が見えたら画面をタッチ";
		}
		canvas.drawText(message, 0, height-5, paint);
	}
	
	/**
	 * 画面がタッチされたとき
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(isRepeat) {
			stopTimer();
			showResult();
		} else {
			startTimer();
		}
		invalidate();
		return super.onTouchEvent(event);
	}
	
	private void showResult() {
		long time = getTime(); //経過時間
		double speed = 331.5+0.6*temp;	//音速
		double d = time * speed / 1000.0; //花火までの距離
		double theta = getElevation(); //仰角
		double dsin = d*Math.sin(theta);
		double h = Math.sqrt(d*d+earthR*earthR+2*earthR*dsin)-earthR; //花火の高さ
		double l = Math.acos((earthR+dsin)/(earthR+h))*earthR; //花火央までの距離
		double compus = getCompus();
		
		String message = String.format(
				"遅延時間:%dms\n" +
				"仰角:%.1f度\n" +
				"音速:%.1fm/s(気温%.1f度時)\n" +
				"直線距離:%.1fm\n" +
				"高さ:%.1fm\n" +
				"距離:%.1fm\n" +
				"方位:%s(%s)",
				time, Math.toDegrees(theta),
				speed, temp, d, h, l,
				getCompusName(compus), getCompusString(compus)
				);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		
		builder.setTitle("測定結果");
		builder.setMessage(message);
		builder.setPositiveButton("再計測", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Do Nothing
			}
		});
		if(location!=null || debug) {
			double latitude = (location==null) ? 37.425382 : location.getLatitude();
			double longitude = (location==null) ? 138.778723 : location.getLongitude();
			double rad_latitude = Math.toRadians(latitude);
			double rad_longitude = Math.toRadians(longitude);
			double x = 0, y = 0, z = 1; //y:南北, z:経度0度, x経度90度
			double xx, yy, zz;
			
			//x軸回りの回転
			xx = x;
			yy =  y*Math.cos(l/earthR) + z*Math.sin(l/earthR);
			zz = -y*Math.sin(l/earthR) + z*Math.cos(l/earthR);
			
			//z軸周りの回転
			x =  xx*Math.cos(compus) + yy*Math.sin(compus);
			y = -xx*Math.sin(compus) + yy*Math.cos(compus);
			z = zz;
			
			//緯度方向への回転
			xx = x;
			yy =  y*Math.cos(rad_latitude) + z*Math.sin(rad_latitude);
			zz = -y*Math.sin(rad_latitude) + z*Math.cos(rad_latitude);
			
			//経度方向への回転
			x = xx*Math.cos(rad_longitude) + zz*Math.sin(rad_longitude);
			y = yy;
			z = -xx*Math.sin(rad_longitude) + zz*Math.cos(rad_longitude);
			
			//花火の緯度経度を算出
			double fw_latitude = Math.toDegrees(Math.asin(y));
			double fw_longitude = Math.toDegrees(Math.atan2(x, z)); 
			//Toast.makeText(getContext(), fw_latitude + "," + fw_longitude, Toast.LENGTH_LONG).show();
			
			//インテント作成
			final Intent intent = new Intent(getContext(),
		       		MapActivity.class);
			intent.putExtra("latitude", latitude);
			intent.putExtra("longitude", longitude);
			intent.putExtra("fw_latitude", fw_latitude);
			intent.putExtra("fw_longitude", fw_longitude);
		
			builder.setNegativeButton("地図を表示", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getContext().startActivity(intent);
				}
			});
		}
		builder.create().show();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do Nothing
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		int type = event.sensor.getType();
		if(type==Sensor.TYPE_ACCELEROMETER) {
			accValues[0] = event.values[0];
			accValues[1] = event.values[1];
			accValues[2] = event.values[2];
		} else if(type==Sensor.TYPE_MAGNETIC_FIELD) {
			magValues[0] = event.values[0];
			magValues[1] = event.values[1];
			magValues[2] = event.values[2];
		} else if(type==Sensor.TYPE_TEMPERATURE){
			temp = event.values[0];
		}
		
		SensorManager.getRotationMatrix(tmpR, null,
			accValues, magValues);
		SensorManager.remapCoordinateSystem(tmpR, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, R);
		SensorManager.getOrientation(R, orientation);
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		return ;
		//ToDo: プレビュー映像を解析して自動化できるといいな
		/*if(isRepeat) return ;
		Size size = camera.getParameters().getPreviewSize();
		final int frameSize = size.width*size.height;
		long count = 0;
		for(int i=0;i<frameSize;i++) {
			int y = (0xFF&(int)data[i]);
			count+=y;
		}
		long t = (long)(threshold*frameSize);
		if(count-last_light>t) startTimer();
		last_light = count;*/
	}

	@Override
	public void onLocationChanged(Location location) {
		this.location = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
