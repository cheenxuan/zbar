package cn.bertsir.zbar;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import cn.bertsir.zbar.Qr.Symbol;
import cn.bertsir.zbar.view.ScanView;


public class QRActivity extends Activity implements View.OnClickListener {

    private CameraPreview cp;
    public BeepManager beepManager;
    private ScanView sv;
    private ImageView mo_scanner_back;
    private ImageView iv_flash;
    private TextView mTvFlashDesc;
    private static final String TAG = "QRActivity";
    private TextView tv_title;
    private RelativeLayout fl_title;
    private TextView tv_des;
    private QrConfig options;
    private boolean isFlash = false;
    private static final int PERMISSIONS_REQUEST_CAMERA = 200;

    private PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public boolean onRequestPermission() {
            ActivityCompat.requestPermissions(QRActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        options = (QrConfig) getIntent().getExtras().get(QrConfig.EXTRA_THIS_CONFIG);

        Symbol.scanType = options.getScan_type();
        Symbol.scanFormat = options.getCustombarcodeformat();
        Symbol.is_only_scan_center = options.isOnly_center();
        Symbol.is_auto_zoom = options.isAuto_zoom();
        setContentView(R.layout.activity_qr);
        initView();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (permissionCallback != null) {
                permissionCallback.onRequestPermission();
            }
            return;
        }else{
            startScan();
        }
    }
    
    private void startScan(){
        if (cp != null) {
            cp.setScanCallback(resultCallback);
            cp.start();
            beepManager = new BeepManager(this);
            isFlash = cp.getFlash();
        }
        sv.onResume();
    }

    private void initView() {
        cp = (CameraPreview) findViewById(R.id.cp);

        sv = (ScanView) findViewById(R.id.sv);
        sv.setType(options.getScan_view_type());
        sv.startScan();
        mo_scanner_back = (ImageView) findViewById(R.id.mo_scanner_back);
        mo_scanner_back.setOnClickListener(this);

        iv_flash = (ImageView) findViewById(R.id.iv_flash);
        mTvFlashDesc = (TextView) findViewById(R.id.tv_flash_light);
        iv_flash.setOnClickListener(this);

        tv_title = (TextView) findViewById(R.id.tv_title);
        fl_title = (RelativeLayout) findViewById(R.id.fl_title);
        tv_des = (TextView) findViewById(R.id.tv_des);

        fl_title.setVisibility(View.VISIBLE);
        iv_flash.setVisibility(View.VISIBLE);
        tv_des.setVisibility(View.VISIBLE);

        tv_des.setText(options.getDes_text());
        tv_title.setText(options.getTitle_text());

        sv.setCornerColor(options.getCORNER_COLOR());
        sv.setLineSpeed(options.getLine_speed());
        sv.setLineColor(options.getLINE_COLOR());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setSeekBarColor(SeekBar seekBar, int color) {
        seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    private ScanCallback resultCallback = new ScanCallback() {
        @Override
        public void onScanResult(String result) {
            if (options.isPlay_sound()) {
                //扫描成功播放声音滴一下，可根据需要自行确定什么时候播
                beepManager.playBeepSoundAndVibrate();
            }
            if (cp != null) {
                cp.setFlash(false);
                isFlash = false;
            }
            if(QrManager.getInstance().getResultCallback() != null){
                QrManager.getInstance().getResultCallback().onScanSuccess(result);
            }else{
                Toast.makeText(QRActivity.this,"扫描失败,请重新扫描",Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cp != null) {
            cp.setFlash(false);
            isFlash = false;
            cp.stop();
        }
        beepManager.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cp != null) {
            cp.stop();
        }
        if(beepManager != null){
            beepManager.close();
        }
        sv.onPause();
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_flash) {
            if (cp != null) {
                isFlash = !isFlash;
                cp.setFlash(isFlash);
                iv_flash.setImageDrawable(getResources().getDrawable(isFlash ? R.drawable.icon_scan_flash_light_on:R.drawable.icon_scan_flash_light_off));
                mTvFlashDesc.setText(isFlash ? "轻点关闭" : "轻点照亮");

            }
        } else if (v.getId() == R.id.mo_scanner_back) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScan();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.camera_permission_required, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
            default:
                break;
        }
    }
}
