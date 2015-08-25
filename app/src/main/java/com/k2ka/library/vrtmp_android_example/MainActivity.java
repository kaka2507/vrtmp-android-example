package com.k2ka.library.vrtmp_android_example;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.k2ka.library.vrtmp.RTMPPublisher;
import com.k2ka.library.vrtmp.RTMPPublisherListener;
import com.k2ka.library.vrtmp.io.RTMPConnection;
import com.k2ka.library.vrtmp.utils.Util;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity implements RTMPPublisherListener, View.OnClickListener {
    private static final String TAG = "MainActivity";
    Button btnControl;
    EditText txtLog;
    RTMPPublisher publisher = null;
    boolean running = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnControl = (Button) findViewById(R.id.btnTest);
        btnControl.setOnClickListener(this);

        txtLog = (EditText) findViewById(R.id.txtLog);
    }

    @Override
    public void onInitComplete() {
        running = true;
        Thread pushThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = getResources().openRawResource(R.raw.test);
                    // read flv header
                    byte[] flv_header = new byte[9];
                    Util.readBytesUntilFull(inputStream, flv_header);
                    Log.d(TAG, "flv header:" + Util.toHexString(flv_header));
                    long beginTimestamp = System.currentTimeMillis();
                    while (running) {
                        //read flv tag
                        int previousTagLength = Util.readUnsignedInt32(inputStream);
                        int tagType = inputStream.read();
                        int tagSize = Util.readUnsignedInt24(inputStream);
                        int timestamp = Util.readUnsignedInt24(inputStream);
                        int timestampEx = inputStream.read();
                        timestamp = (timestampEx << 24) | timestamp;
                        int streamID = Util.readUnsignedInt24(inputStream);
                        byte[] body = new byte[tagSize];
                        Util.readBytesUntilFull(inputStream, body);
                        Log.d(TAG, "tag type:" + Util.toHexString((byte)tagType) + " length:" + tagSize + " timestamp:" + timestamp);
                        publisher.SendFLVTag(tagType, body, tagSize, timestamp);
                        long delta = System.currentTimeMillis() - beginTimestamp;
                        if(delta < timestamp) {
                            Thread.sleep(timestamp - delta);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "read resource from raw fail with e:" + e.toString());
                }
            }
        });
        pushThread.start();
    }

    @Override
    public void onError(final RTMPPublisher.ERROR error, final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                running = false;
                publisher.Release();
                publisher = null;
                btnControl.setText(getResources().getString(R.string.start_stream));
                Log.d(TAG, "onError error:" + error + " msg:" + s);
                txtLog.append("onError error:" + error + " msg:" + s + "\n");
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnTest) {
            if(((Button)v).getText().toString().equals(getResources().getString(R.string.start_stream)) && !running) {
                String rtmp_url = "rtmp://120.138.75.44:8080/live"; // rtmp server url
                String channel_name = "test"; // channel name
                ExecutorService worker = Executors.newSingleThreadExecutor();
                publisher = new RTMPConnection();
                publisher.Init(rtmp_url, worker, this, channel_name);
                btnControl.setText(getResources().getString(R.string.stop_stream));
                running = true;
            } else if(((Button)v).getText().toString().equals(getResources().getString(R.string.stop_stream))) {
                publisher.Release();
                publisher = null;
                running = false;
                btnControl.setText(getResources().getString(R.string.start_stream));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
