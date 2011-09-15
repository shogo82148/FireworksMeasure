package net.sorablue.shogo.FWMeasure;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class Overlay extends View implements SensorEventListener, PreviewCallback {
	private final int REPEAT_INTERVAL = 100;
	private final int MESSAGE_WHAT = 100;
	private SensorManager manager;
	private List<Sensor> acc_sensors;
	private List<Sensor> temp_sensors;
	private float[] sensorValues = new float[3];
	private int threshold = 20;
	private long last_light = Long.MAX_VALUE;
	private int width, height;
	
	protected boolean isRepeat = false;
	protected long startTime;
	
	private double earthR = 6378137; //�n���̔��a
	private float temp = 20;	//���x
	
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
		
		//�����x�Z���T�擾
		manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		acc_sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		temp_sensors = manager.getSensorList(Sensor.TYPE_TEMPERATURE);
		
		stopTimer();
	}
	
	private void startTimer() {
		isRepeat = true;
		Message message = new Message();
        message.what = MESSAGE_WHAT;
		handler.sendMessageDelayed(message, REPEAT_INTERVAL);
		startTime = SystemClock.uptimeMillis();
		manager.registerListener(this, acc_sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
		if(temp_sensors!=null && temp_sensors.size()>0)
			manager.registerListener(this, temp_sensors.get(0), SensorManager.SENSOR_DELAY_UI);
	}
	
	private void stopTimer() {
		isRepeat = false;
		manager.unregisterListener(this);
		last_light = Long.MAX_VALUE;
	}
	
	private long getTime() {
		return SystemClock.uptimeMillis() - startTime;
	}
	
	private double getElevation() {
		double len = 0;
		int i;
		for(i=0;i<sensorValues.length;i++) {
			len += sensorValues[i]*sensorValues[i];
		}
		len = Math.sqrt(len);
		
		double cos = -sensorValues[2] / len;
		double theta = Math.asin(cos);
		
		return theta;
	}
	
	@Override
	protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
		this.width = width;
		this.height = height;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.TRANSPARENT);
		
		//��ʒ����Ƀ��C����`��
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		canvas.drawLine(0, height/2, width, height/2, paint);
		canvas.drawLine(width/2, 0, width/2, height, paint);
		
		String message = "";
		if(isRepeat) {
			message = "�ԉ΂���ʒ����ɓ���A�����������ʂ��^�b�`(" + 
				getTime() + "ms, " + 
				(int)(getElevation()/Math.PI*180+0.5) + "�x)";
		} else {
			message = "�ԉ΂����������ʂ��^�b�`";
		}
		canvas.drawText(message, 0, height-5, paint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(isRepeat) {
			stopTimer();
			long time = getTime(); //�o�ߎ���
			double speed = 331.5+0.6*temp;	//����
			double d = time * speed / 1000.0; //�ԉ΂܂ł̋���
			double theta = getElevation(); //�p
			double dsin = d*Math.sin(theta);
			double h = Math.sqrt(d*d+earthR*earthR+2*earthR*dsin)-earthR; //�ԉ΂̍���
			double l = Math.acos((earthR+dsin)/(earthR+h))*earthR; //�ԉΉ��܂ł̋���
			
			String message = String.format(
					"�x������:%dms\n" +
					"�p:%.1f�x\n" +
					"�C��:%.1f�x\n" +
					"����:%.1fm/s\n" +
					"��������:%.1fm\n" +
					"����:%.1fm\n" +
					"����:%.1fm",
					time, theta/Math.PI*180, temp,
					speed, d, h, l
					);
			alert(message);
		} else {
			startTimer();
		}
		invalidate();
		return super.onTouchEvent(event);
	}
	
	private void alert(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		
		builder.setTitle("���茋��");
		builder.setMessage(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Do Nothing
			}
		});
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
			sensorValues[0] = event.values[0];
			sensorValues[1] = event.values[1];
			sensorValues[2] = event.values[2];
		} else if(type==Sensor.TYPE_TEMPERATURE){
			temp = event.values[0];
		}
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if(isRepeat) return ;
		Size size = camera.getParameters().getPreviewSize();
		final int frameSize = size.width*size.height;
		long count = 0;
		for(int i=0;i<frameSize;i++) {
			int y = (0xFF&(int)data[i]);
			count+=y;
		}
		long t = (long)(threshold*frameSize);
		if(count-last_light>t) startTimer();
		last_light = count;
	}
}
