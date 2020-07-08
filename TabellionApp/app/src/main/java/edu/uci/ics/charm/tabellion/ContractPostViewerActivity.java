package edu.uci.ics.charm.tabellion;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

/*
Created Date: 04/30/2020
Created By: Myles Liu
Last Modified: 04/30/2020
Last Modified By: Myles Liu
Notes:

 */

public class ContractPostViewerActivity extends AppCompatActivity implements GestureDetector.OnGestureListener{

    private ImageView contractImageView;
    private LinearLayout statusLinearLayout;

    private Context context = this;
    private MyApp myApp;
    private String TAG = "ContractPostViewerActivity";

    public int page_counter = 1;

    // The following two are for detecting swipe
    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    // For gesture detection
    GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract_post_viewer);
        myApp = (MyApp)getApplication();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        contractImageView = (ImageView)findViewById(R.id.contract_post_viewer_imageView);
        statusLinearLayout = (LinearLayout)findViewById(R.id.contract_post_viewer_linearLayout_overlap_review_note);

        if(myApp.getCurrentViewingContract().getContractStatus() == 2){
            statusLinearLayout.setVisibility(View.VISIBLE);
        }

        hideSystemUI();   // Should use style for theme instead (However, for some unknown reasons, style cannot hide navigation bar)

        goToPageCounterPage("Ndoc-");

        gestureDetector = new GestureDetector(context, ContractPostViewerActivity.this);

    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        //     | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        //     | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        //     | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.STATUS_BAR_HIDDEN
        );
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 > x1)
                    {
                        Log.d(TAG, "onTouchEvent: Left to Right swipe [Previous]");
                        Log.d(TAG, "onTouchEvent: isSwipeBackEnabled: " + myApp.getCurrentViewingContract().isSwipeBackEnabled());
                        Log.d(TAG, "onTouchEvent: current page counter: " + page_counter);
                        if(myApp.getCurrentViewingContract().isSwipeBackEnabled() & page_counter != 1){
                            OperationsWithNDK.turnOffLedLightForSigning();
                            --page_counter;
                            goToPageCounterPage("Ndoc-");
                        } else if (myApp.getCurrentViewingContract().isSwipeBackEnabled()){
                            Toast.makeText(context, R.string.already_first_page_during_signing, Toast.LENGTH_SHORT).show();
                        }
                    }
                    // Right to left swipe action
                    else
                    {
                        Log.d(TAG, "onTouchEvent: Right to Left swipe [Next]");Log.d(TAG, "onTouchEvent: isSwipeBackEnabled: " + myApp.getCurrentViewingContract().isSwipeBackEnabled());
                        Log.d(TAG, "onTouchEvent: current page counter: " + page_counter);
                        if(myApp.getCurrentViewingContract().isSwipeBackEnabled() & page_counter < myApp.getCurrentViewingContract().getTotalImageNums()){
                            ++page_counter;
                            goToPageCounterPage("Ndoc-");
                        } else if (myApp.getCurrentViewingContract().isSwipeBackEnabled()){
                            Toast.makeText(context, R.string.already_last_page_during_signing, Toast.LENGTH_SHORT).show();
                        }
                    }

                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void goToPageCounterPage(final String fileName){
        String fname = fileName + page_counter + ".png";
        if(page_counter == myApp.getCurrentViewingContract().getTotalImageNums() + 1) {
            fname = "Nlast-1.png";
        }
        File imagemain = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + myApp.getCurrentViewingContract().getContractId() + "/", fname);

        String path = imagemain.getPath();
        Bitmap image = BitmapFactory.decodeFile(path);
        contractImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        contractImageView.setImageBitmap(image);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
