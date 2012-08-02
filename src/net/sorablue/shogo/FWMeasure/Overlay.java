package net.sorablue.shogo.FWMeasure;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.PreviewCallback;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class Overlay extends View implements SensorEventListener, PreviewCallback, LocationListener {
	private final int REPEAT_INTERVAL = 50;
	private final int MESSAGE_WHAT = 100;
	private static final int SOUND_RATE = 11025;
	
	private Context context;
	
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
	private float[] Rotation = new float[16];
	private float[] orientation = new float[3];
	
	//GPS���
	private Location location;
	
	private boolean enableAutoDetect = false;
	private int detectionRange = 0;
	private int cameraThreshold = 0;
	
	private long brightness = 0;
	private long last_brightness = 0;
	
	private int bufferSizeRecord;
	private short[] bufferRecord;
	private AudioRecord audioRecord;
	private boolean isRecording = false;
	private boolean enableSoundDetect = false;
	private int soundThreshold;
	private int frequency = 100;
	private int soundPower;

	private int width, height;
	
	protected boolean isRepeat = false; //True:�^�C�}���쒆 False:�^�C�}��~
	protected long startTime;	//�v�����J�n�����Ƃ��̎���
	
	private final double earthR = 6378137; //�n���̔��a
	private final boolean debug = true;
	
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
		this.context = context;
		
		setDrawingCacheEnabled(true);
		
		//�Z���T�̐ݒ�
		manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		acc_sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		temp_sensors = manager.getSensorList(Sensor.TYPE_TEMPERATURE);
		mag_sensors = manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		
		startRecording();
		loadSettings();
		stopTimer();
	}
	
	private void startRecording() {
		bufferSizeRecord = AudioRecord.getMinBufferSize(
				SOUND_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		Log.d("record", "buffersize = " + bufferSizeRecord);
		if(bufferSizeRecord>0) {
			bufferRecord = new short[bufferSizeRecord / 2];
			audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC,
					SOUND_RATE,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSizeRecord);
			audioRecord.startRecording();
			isRecording = true;
			Thread thread = new Thread(new Runnable() {
				public void run() {
					int f = -1;
					float[] sintable = new float[bufferRecord.length];
					float[] costable = new float[bufferRecord.length];
					int tablesize = 0;
					while(isRecording) {
						if(frequency>0 && f != frequency) {
							float wavelength = (float)SOUND_RATE / frequency;
							int numwave = (int)(bufferRecord.length / wavelength);
							tablesize = (int)(wavelength * numwave);
							double omega = 2 * Math.PI / wavelength;
							Log.d("record", "frequency: " + frequency);
							Log.d("record", "wavelength: " + wavelength);
							Log.d("record", "numwave: " + numwave);
							Log.d("record", "tablesize: " + tablesize);
							Log.d("record", "omega: " + omega);
							for(int i=0; i<tablesize; i++) {
								sintable[i] = (float)(Math.sin(omega*i));
								costable[i] = (float)(Math.cos(omega*i));
							}
							f = frequency;
						}
						int size = audioRecord.read(bufferRecord, 0, bufferRecord.length);
						if(size==0)	continue;
						
						float sinpower = 0;
						float cospower = 0;
						for(int i=0; i<tablesize; i++) {
							sinpower += sintable[i] * bufferRecord[i];
							cospower += costable[i] * bufferRecord[i];
						}
						soundPower = (int)Math.sqrt(sinpower * sinpower + cospower * cospower) / 256;
					}
				} 
			});
			thread.start();
		}
	}
	
	public void release() {
		isRecording = false;
		audioRecord.stop();
		audioRecord.release();
		audioRecord = null;
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
		/* ���y�n���@�@�n���C���� 2000�N�̃f�[�^�Ɋ�Â��ߎ�
		 * D2000.0=7��37.142'+21.622'����-7.672'����+0.442'����2-0.320'���Ӄ���-0.675'����2
		 * ����=��-37��N �A����=��-138��E */
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
		String[] name = context.getResources().getStringArray(R.array.compus_names);
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
	 * �`�掞�ɌĂяo����郁�\�b�h
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.TRANSPARENT);
		
		//��ʒ����Ƀ��C����`��
		Paint paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.WHITE);
		canvas.drawLine(0, height/2, width, height/2, paint);
		canvas.drawLine(width/2, 0, width/2, height, paint);
		
		if(enableAutoDetect) {
			//�������o�͈͂�`��
			canvas.drawRect(
					width/2 - detectionRange,
					height/2 - detectionRange,
					width/2 + detectionRange,
					height/2 + detectionRange,
					paint);
			//���邳��\��
			final long threshold =
					(long)cameraThreshold *
					(2*detectionRange+1) * (2*detectionRange+1);
			float val = (float)(brightness-last_brightness) / threshold / 2;
			if(val<0) val = 0;
			if(val>1) val = 1;
			float left = width / 2.0f + 5;
			float top = 5.0f;
			float right = width - 5.0f;
			float bottom = 15.0f;
			Paint fill = new Paint();
			fill.setColor(Color.YELLOW);
			canvas.drawRect(
					left,
					top,
					(right-left)*val+left,
					bottom,
					fill);
			canvas.drawRect(
					left,
					top,
					right,
					bottom,
					paint);
			canvas.drawLine(
					(left+right)/2, top,
					(left+right)/2, bottom,
					paint);
		}
		
		if(enableSoundDetect) {
			float val = soundPower / 256.0f;
			if(val<0) val = 0;
			if(val>1) val = 1;
			float left = width / 2.0f + 5;
			float top = 20.0f;
			float right = width - 5.0f;
			float bottom = 30.0f;
			Paint fill = new Paint();
			fill.setColor(Color.YELLOW);
			canvas.drawRect(
					left,
					top,
					(right-left)*val+left,
					bottom,
					fill);
			canvas.drawRect(
					left,
					top,
					right,
					bottom,
					paint);			
		}
		
		if(location == null) {
			canvas.drawText(context.getString(R.string.locating_msg), 0, 15, paint);
		}
		
		String message = "";
		if(isRepeat) {
			//�^�C�}���쒆�̃��b�Z�[�W
			message = getTime() + context.getString(R.string.unit_ms) + ", " +
				context.getString(R.string.elevation) + ":" + 
				(int)(Math.toDegrees(getElevation())+0.5) + 
				context.getString(R.string.unit_angle) + ", " +
				context.getString(R.string.compus) + ":" +
				getCompusString(getCompus());
			message = String.format(context.getString(R.string.center_fw_msg), message);
		} else {
			//�^�C�}��~���̃��b�Z�[�W
			message = context.getString(R.string.see_fw_msg);
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
			showResult();
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
		double compus = getCompus();
		
		String message = context.getString(R.string.delay_time_result, time);
		message += context.getString(R.string.elevation_result, Math.toDegrees(theta));
		message += context.getString(R.string.sound_speed_result, speed);
		message += context.getString(R.string.temp_result, temp);		
		message += context.getString(R.string.one_line_distance_result, d);
		message += context.getString(R.string.height_result, h);
		message += context.getString(R.string.distance_result, l);
		message += context.getString(R.string.compus_result, getCompusName(compus));
		message += context.getString(R.string.compus_result2, getCompusString(compus));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		
		builder.setTitle(R.string.result_title);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.remeasure_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//Do Nothing
			}
		});
		{
			double latitude = 0;
			double longitude = 0;
			if(location==null) {
				SharedPreferences settings = context.getSharedPreferences(FWMeasureActivity.PREFERENCES_NAME, FWMeasureActivity.MODE_PRIVATE);
				latitude = settings.getInt("latitude", 0) * 1e-6;
				longitude = settings.getInt("longitude", 0) * 1e-6;
			} else {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
			}
			double rad_latitude = Math.toRadians(latitude);
			double rad_longitude = Math.toRadians(longitude);
			double x = 0, y = 0, z = 1; //y:��k, z:�o�x0�x, x�o�x90�x
			double xx, yy, zz;
			
			//x�����̉�]
			xx = x;
			yy =  y*Math.cos(l/earthR) + z*Math.sin(l/earthR);
			zz = -y*Math.sin(l/earthR) + z*Math.cos(l/earthR);
			
			//z������̉�]
			x =  xx*Math.cos(compus) + yy*Math.sin(compus);
			y = -xx*Math.sin(compus) + yy*Math.cos(compus);
			z = zz;
			
			//�ܓx�����ւ̉�]
			xx = x;
			yy =  y*Math.cos(rad_latitude) + z*Math.sin(rad_latitude);
			zz = -y*Math.sin(rad_latitude) + z*Math.cos(rad_latitude);
			
			//�o�x�����ւ̉�]
			x = xx*Math.cos(rad_longitude) + zz*Math.sin(rad_longitude);
			y = yy;
			z = -xx*Math.sin(rad_longitude) + zz*Math.cos(rad_longitude);
			
			//�ԉ΂̈ܓx�o�x���Z�o
			double fw_latitude = Math.toDegrees(Math.asin(y));
			double fw_longitude = Math.toDegrees(Math.atan2(x, z)); 
			//Toast.makeText(getContext(), fw_latitude + "," + fw_longitude, Toast.LENGTH_LONG).show();
			
			//�C���e���g�쐬
			final Intent intent = new Intent(getContext(),
		       		MapActivity.class);
			intent.putExtra("latitude", latitude);
			intent.putExtra("longitude", longitude);
			intent.putExtra("fw_latitude", fw_latitude);
			intent.putExtra("fw_longitude", fw_longitude);
		
			builder.setNegativeButton(R.string.map_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					getContext().startActivity(intent);
				}
			});
		}
		builder.create().show();
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do Nothing
	}

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
		SensorManager.remapCoordinateSystem(tmpR, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, Rotation);
		SensorManager.getOrientation(Rotation, orientation);
	}
	
	public void onPreviewFrame(byte[] data, Camera camera) {
		final Size size = camera.getParameters().getPreviewSize();
		final int width = size.width;
		final int height = size.height;
		final int left = width/2 - detectionRange;
		final int top = height/2 - detectionRange;
		final int right = width/2 + detectionRange;
		final int bottom = height/2 + detectionRange;
		long count = 0;
		
		for(int y=top; y<=bottom; y++) {
			for(int x=left; x<=right; x++) {
				int offset = y * width + x;
				count += 0xFF & data[offset];
			}
		}
		last_brightness = brightness;
		brightness = count;

		if(!enableAutoDetect || isRepeat) return ;
		
		final long threshold =
				(long)cameraThreshold *
				(2*detectionRange+1) * (2*detectionRange+1);
		if(brightness-last_brightness>threshold) {
			startTimer();
			last_brightness = 0;
		}
	}

	public void onLocationChanged(Location location) {
		this.location = location;
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	public void loadSettings() {
		SharedPreferences settings = context.getSharedPreferences(
				FWMeasureActivity.PREFERENCES_NAME,
				FWMeasureActivity.MODE_PRIVATE);

		enableAutoDetect = settings.getBoolean("enableAutoDetect", false);
		detectionRange = settings.getInt("detectionRange", 0);
		cameraThreshold = settings.getInt("cameraThreshold", 0);
		enableSoundDetect = settings.getBoolean("enableSoundDetect", false);
		soundThreshold = settings.getInt("soundThreshold", 0);
		int f = settings.getInt("frequency", 0);
		if(f<20) f=20;
		frequency = f;
	}
}
