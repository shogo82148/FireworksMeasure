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
import android.widget.Toast;

public class Overlay extends View implements SensorEventListener, PreviewCallback, LocationListener {
	private final int REPEAT_INTERVAL = 100;
	private final int MESSAGE_WHAT = 100;
	
	//�Z���T���������߂̃t�B�[���h
	private SensorManager manager;		//�Z���T�Ǘ��̃N���X
	private List<Sensor> temp_sensors;	//���x�Z���T
	private float temp = 20;			//���݂̉��x
	private List<Sensor> acc_sensors;	//�����x�Z���T
	private float[] accValues = new float[3];	//�����x�Z���T�̒l
	private List<Sensor> mag_sensors;	//���C�Z���T
	private float[] magValues = new float[3];	//���C�Z���T�̒l
	
	//�[���̎p��
	private float[] tmpR = new float[16];
	private float[] R = new float[16];
	private float[] orientation = new float[3];
	
	//GPS���
	private Location location;
	
	//private int threshold = 20;
	//private long last_light = Long.MAX_VALUE;
	private int width, height;
	
	protected boolean isRepeat = false; //True:�^�C�}���쒆 False:�^�C�}��~
	protected long startTime;	//�v�����J�n�����Ƃ��̎���
	
	private double earthR = 6378137; //�n���̔��a
	
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
		
		//�Z���T�̐ݒ�
		manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		acc_sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		temp_sensors = manager.getSensorList(Sensor.TYPE_TEMPERATURE);
		mag_sensors = manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		
		stopTimer();
	}
	
	/**
	 * �^�C�}������J�n����
	 */
	private void startTimer() {
		//�^�C�}�̋N��
		isRepeat = true;
		Message message = new Message();
        message.what = MESSAGE_WHAT;
		handler.sendMessageDelayed(message, REPEAT_INTERVAL);
		startTime = SystemClock.uptimeMillis();
		
		//�Z���T�̎擾�J�n
		if(acc_sensors!=null && acc_sensors.size()>0)
			manager.registerListener(this, acc_sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
		if(mag_sensors!=null && mag_sensors.size()>0)
			manager.registerListener(this, mag_sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
		if(temp_sensors!=null && temp_sensors.size()>0)
			manager.registerListener(this, temp_sensors.get(0), SensorManager.SENSOR_DELAY_UI);
	}
	
	/**
	 * �^�C�}������~����
	 */
	private void stopTimer() {
		isRepeat = false;
		manager.unregisterListener(this);
		//last_light = Long.MAX_VALUE;
	}
	
	/**
	 * �^�C�}���N�����Ă���̌o�ߎ��Ԃ�Ԃ�
	 * @return �^�C�}���J�n���Ă���̌o�ߎ���(ms)�B
	 */
	private long getTime() {
		return SystemClock.uptimeMillis() - startTime;
	}
	
	/**
	 * ���݂̋p��Ԃ��B
	 * @return ���݂̋p[rad]
	 */
	private double getElevation() {
		return -orientation[1];
	}
	
	/**
	 * ���݂̕��ʂ�Ԃ�
	 * @return �k����̊p�x[rad]
	 */
	private double getCompus() {
		return orientation[0];
	}
		
	@Override
	protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
		this.width = width;
		this.height = height;
	}
	
	/**
	 * �`�掞�ɌĂяo����郁�\�b�h
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.TRANSPARENT);
		
		//��ʒ����Ƀ��C����`��
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		canvas.drawLine(0, height/2, width, height/2, paint);
		canvas.drawLine(width/2, 0, width/2, height, paint);
		
		if(location == null) {
			canvas.drawText("���݈ʒu���擾��...", 0, 15, paint);
		}
		
		String message = "";
		if(isRepeat) {
			//�^�C�}���쒆�̃��b�Z�[�W
			message = "�ԉ΂���ʒ����ɓ���A�����������ʂ��^�b�`(" + 
				getTime() + "ms, �p:" + 
				(int)(Math.toDegrees(getElevation())+0.5) + "�x, ����:" +
				(int)(Math.toDegrees(getCompus())+0.5)+ "�x)";
		} else {
			//�^�C�}��~���̃��b�Z�[�W
			message = "�ԉ΂����������ʂ��^�b�`";
		}
		canvas.drawText(message, 0, height-5, paint);
	}
	
	/**
	 * ��ʂ��^�b�`���ꂽ�Ƃ�
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(isRepeat) {
			stopTimer();
			if(location==null) showResult();
			else showMap();
		} else {
			startTimer();
		}
		invalidate();
		return super.onTouchEvent(event);
	}
	
	private void showResult() {
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
	}
	
	private void showMap() {
		Intent intent = new Intent(getContext(),
	       		MapActivity.class);
		intent.putExtra("latitude", 37.436567);//location.getLatitude());
		intent.putExtra("longitude", 138.839035);//location.getLongitude());
		getContext().startActivity(intent);
	}
	
	/**
	 * ���茋�ʃ_�C�A���O��\������
	 * @param message
	 */
	private void alert(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		
		builder.setTitle("���茋��");
		builder.setMessage(message);
		builder.setPositiveButton("�Čv��", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Do Nothing
			}
		});
		builder.setNegativeButton("�n�}��\��", new DialogInterface.OnClickListener() {
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
		//ToDo: �v���r���[�f������͂��Ď������ł���Ƃ�����
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
