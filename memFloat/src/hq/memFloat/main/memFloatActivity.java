package hq.memFloat.main;

import hq.memFloat.R;
import hq.memFloat.service.FloatService;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class memFloatActivity extends Activity {
	Button btnstart;
	Button btnstop;
	TextView tv;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		btnstart = (Button) findViewById(R.id.btnstart);
		btnstart.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent service = new Intent();
				service.setClass(memFloatActivity.this, FloatService.class);
				startService(service);
			}
		});

		btnstop = (Button) findViewById(R.id.btnstop);
		btnstop.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceStop = new Intent();
				serviceStop.setClass(memFloatActivity.this, FloatService.class);
				stopService(serviceStop);
			}
		});
		tv = (TextView) findViewById(R.id.tv);

		String str = new StringBuilder().append("\n").append("˵����")
				.append("\n")
				.append("1.�������������ƶ�").append("\n")
				.append("2.ʵʱ��ʾ��ǰ�ڴ�����").append("\n")
				.append("3.�ϲ����ݱ�ʾ�����ڴ�ֵ").append("\n")
				.append("4.�²����ݱ�ʾ���ڴ�ֵ").append("\n")
				.append("5.������������ֹر�Сͼ���ֱ�ӹر�").append("\n").append("\n")
				.toString();
		tv.setText(str);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.v("stop", "stop");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.v("restart", "restart");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Intent service = new Intent();
		service.setClass(memFloatActivity.this, FloatService.class);
		startService(service);
	}
}