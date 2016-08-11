package hq.memFloat.service;

import hq.memFloat.R;
import hq.memFloat.main.memFloatActivity;
import hq.memFloat.model.MyApplication;
import hq.memFloat.model.memInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class FloatService extends Service {

	WindowManager wm = null;
	WindowManager.LayoutParams wmParams = null;
	View view;
	private float mTouchStartX;
	private float mTouchStartY;
	private float x;
	private float y;
	int state;
	TextView tx1;
	TextView tx;
	ImageView iv;
	private float StartX;
	private float StartY;
	int delaytime = 1000;

	private boolean mReflectFlg = false;

	private static final int NOTIFICATION_ID = 1; // ���id����Ϊ0,�ᵼ�²�������Ϊǰ̨service
	private static final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
			int.class, Notification.class };
	private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private NotificationManager mNotificationManager;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	@Override
	public void onCreate() {
		super.onCreate();
		// �����ȡ����
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		try {
			mStartForeground = FloatService.class.getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = FloatService.class.getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			mStartForeground = mStopForeground = null;
		}

		try {
			mSetForeground = getClass().getMethod("setForeground",
					mSetForegroundSignature);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(
					"OS doesn't have Service.startForeground OR Service.setForeground!");
		}

		// ֪ͨ��ʼ��
		Notification.Builder builder = new Notification.Builder(this);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, memFloatActivity.class), 0);
		builder.setContentIntent(contentIntent);
		builder.setSmallIcon(R.drawable.icon);
		builder.setTicker("Foreground Service Start");
		builder.setContentTitle("Foreground Service");
		builder.setContentText("Make this service run in the foreground.");
		notification = builder.build();

		view = LayoutInflater.from(this).inflate(R.layout.floating, null);
		tx = (TextView) view.findViewById(R.id.memunused);
		tx1 = (TextView) view.findViewById(R.id.memtotal);
		tx.setText("ʣ��" + (int) (memInfo.getmem_UNUSED(this) / 1024) + "M"
				+ "    ʹ��"
				+ (memInfo.getmem_TOLAL() - memInfo.getmem_UNUSED(this)) * 100
				/ memInfo.getmem_TOLAL() + "%");
		tx1.setText("�ܹ�" + (int) (memInfo.getmem_TOLAL() / 1024) + "M");
		iv = (ImageView) view.findViewById(R.id.img2);
		iv.setVisibility(View.GONE);
		createView();
		handler.postDelayed(task, delaytime);
	}

	void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		}
	}

	void startForegroundCompat(int id, Notification notification) {
		if (mReflectFlg) {
			// If we have the new startForeground API, then use it.
			if (mStartForeground != null) {
				mStartForegroundArgs[0] = Integer.valueOf(id);
				mStartForegroundArgs[1] = notification;
				invokeMethod(mStartForeground, mStartForegroundArgs);
				return;
			}

			// Fall back on the old API.
			mSetForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mSetForeground, mSetForegroundArgs);
			mNotificationManager.notify(id, notification);
		} else {
			/*
			 * ������ʹ�����·�������sdk���ڵ���5ʱ������sdk���еķ���startForeground����ǰ̨���У�
			 * ������÷���ȡ�õ�sdk level 5����ӦAndroid 2.0�����²��еľɷ���setForeground����ǰ̨����
			 */
			if (VERSION.SDK_INT >= 5) {
				startForeground(id, notification);
			} else {
				// Fall back on the old API.
				mSetForegroundArgs[0] = Boolean.TRUE;
				invokeMethod(mSetForeground, mSetForegroundArgs);
				mNotificationManager.notify(id, notification);
			}
		}
	}

	void stopForegroundCompat(int id) {
		if (mReflectFlg) {
			// If we have the new stopForeground API, then use it.
			if (mStopForeground != null) {
				mStopForegroundArgs[0] = Boolean.TRUE;
				invokeMethod(mStopForeground, mStopForegroundArgs);
				return;
			}

			// Fall back on the old API. Note to cancel BEFORE changing the
			// foreground state, since we could be killed at that point.
			mNotificationManager.cancel(id);
			mSetForegroundArgs[0] = Boolean.FALSE;
			invokeMethod(mSetForeground, mSetForegroundArgs);
		} else {
			/*
			 * ������ʹ�����·�������sdk���ڵ���5ʱ������sdk���еķ���stopForegroundֹͣǰ̨���У� ������÷���ȡ�õ�sdk
			 * level 5����ӦAndroid 2.0�����²��еľɷ���setForegroundֹͣǰ̨����
			 */
			if (VERSION.SDK_INT >= 5) {
				stopForeground(true);
			} else {
				// Fall back on the old API. Note to cancel BEFORE changing the
				// foreground state, since we could be killed at that point.
				mNotificationManager.cancel(id);
				mSetForegroundArgs[0] = Boolean.FALSE;
				invokeMethod(mSetForeground, mSetForegroundArgs);
			}
		}
	}

	private void createView() {
		SharedPreferences shared = getSharedPreferences("float_flag",Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = shared.edit();
		editor.putInt("float", 1);
		editor.commit();
		// ��ȡWindowManager
		wm = (WindowManager) getApplicationContext().getSystemService("window");
		// ����LayoutParams(ȫ�ֱ�������ز���
		wmParams = ((MyApplication) getApplication()).getMywmParams();
		wmParams.type =  WindowManager.LayoutParams. TYPE_SYSTEM_ALERT ;
//		wmParams.token =Activity.this.getCurrentFocus().getWindowToken();
		wmParams.flags |= 8;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP; // �����������������Ͻ�
		// ����Ļ���Ͻ�Ϊԭ�㣬����x��y��ʼֵ
		wmParams.x = 0;
		wmParams.y = 0;
		// �����������ڳ�������
		wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.format = 1;
		wmParams.windowAnimations = android.R.style.Animation_Translucent;
		wm.addView(view, wmParams);
		view.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// ��ȡ�����Ļ�����꣬������Ļ���Ͻ�Ϊԭ��
				x = event.getRawX();
				y = event.getRawY() - 25; // 25��ϵͳ״̬���ĸ߶�
				Log.i("currP", "currX" + x + "====currY" + y);// ������Ϣ
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					state = MotionEvent.ACTION_DOWN;
					StartX = x;
					StartY = y;
					// ��ȡ���View�����꣬���Դ�View���Ͻ�Ϊԭ��
					mTouchStartX = event.getX();
					mTouchStartY = event.getY();
					Log.i("startP", "startX" + mTouchStartX + "====startY"
							+ mTouchStartY);// ������Ϣ
					break;
				case MotionEvent.ACTION_MOVE:
					state = MotionEvent.ACTION_MOVE;
					updateViewPosition();
					break;
				case MotionEvent.ACTION_UP:
					state = MotionEvent.ACTION_UP;
					updateViewPosition();
					showImg();
					mTouchStartX = mTouchStartY = 0;
					break;
				}
				return true;
			}
		});

		iv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceStop = new Intent();
				serviceStop.setClass(FloatService.this, FloatService.class);
				stopService(serviceStop);
			}
		});

	}

	public void showImg() {
		if (Math.abs(x - StartX) < 1.5 && Math.abs(y - StartY) < 1.5
				&& !iv.isShown()) {
			iv.setVisibility(View.VISIBLE);
		} else if (iv.isShown()) {
			iv.setVisibility(View.GONE);
		}
	}

	private Handler handler = new Handler();
	private Runnable task = new Runnable() {
		public void run() {
			dataRefresh();
			handler.postDelayed(this, delaytime);
			wm.updateViewLayout(view, wmParams);
		}
	};
	private Notification notification;

	public void dataRefresh() {
		tx.setText("ʣ��" + (int) (memInfo.getmem_UNUSED(this) / 1024) + "M"
				+ "    ʹ��"
				+ (memInfo.getmem_TOLAL() - memInfo.getmem_UNUSED(this)) * 100
				/ memInfo.getmem_TOLAL() + "%");
		tx1.setText("�ܹ�" + (int) (memInfo.getmem_TOLAL() / 1024) + "M");
	}

	private void updateViewPosition() {
		// ���¸�������λ�ò���
		wmParams.x = (int) (x - mTouchStartX);
		wmParams.y = (int) (y - mTouchStartY);
		wm.updateViewLayout(view, wmParams);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("FloatService", "onStart");
		// setForeground(true);
//		startForegroundCompat(NOTIFICATION_ID, notification);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		handler.removeCallbacks(task);
		Log.d("FloatService", "onDestroy");
		wm.removeView(view);
//		stopForegroundCompat(NOTIFICATION_ID);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
