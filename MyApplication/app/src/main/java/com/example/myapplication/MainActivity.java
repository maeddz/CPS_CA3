package com.example.myapplication;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.Toast;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Button;
import android.view.View.OnClickListener;

public class MainActivity extends Activity {

    private Spinner mSpinner;
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    Button imageButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mSpinner = new Spinner(this);

        setContentView(R.layout.activity_main);
        Toast.makeText(this, "Click on Ball to play!:))", Toast.LENGTH_SHORT).show();
        addListenerOnButton();
    }
    public void addListenerOnButton() {
        imageButton = (Button) findViewById(R.id.imageButtonSelector);
        imageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSpinner.setBackgroundResource(R.drawable.wood);
                setContentView(mSpinner);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSpinner.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSpinner.stopSimulation();
    }

    class Spinner extends FrameLayout implements SensorEventListener {
        private static final float sBallDiameter = 0.004f;
        private final int mDstWidth;
        private final int mDstHeight;
        private Sensor mGyroscope;
        private long mLastT;
        private float mXDpi;
        private float mYDpi;
        private float mMetersToPixelsX;
        private float mMetersToPixelsY;
        private float mXOrigin;
        private float mYOrigin;
        private float mSensorX;
        private float mSensorY;
        private float mHorizontalBound;
        private float mVerticalBound;
        final ParticleSystem mParticleSystem;
        class Particle extends View {
            private float mPosX;
            private float mPosY;
            private float mVelX;
            private float mVelY;
            public Particle(Context context) {
                super(context);
            }
            public Particle(Context context, AttributeSet attrs) {
                super(context, attrs);
            }
            public Particle(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }
            @TargetApi(Build.VERSION_CODES.N)
            public void computePhysics(float sx, float sy, float dT) {
                double Usx=Math.abs(9.81f*Math.cos(sy)) - Math.abs(9.81f*Math.sin(sy)*0.15f);
                double Usy = Math.abs(9.81f*Math.cos(sx)) - Math.abs(9.81f*Math.sin(sx)*0.15f);
                if(Usx >0 && Usy>0){
                    final int dirx = -(int)(Math.abs(mVelX)/mVelX);
                    final int diry = -(int)(Math.abs(mVelY)/mVelY);
                    final double ax =  9.81f*Math.sin(sy) - dirx*9.81f*Math.cos(sy)*0.1f;
                    final double ay = -9.81f*Math.sin(sx) - diry*9.81f*Math.cos(sx)*0.1f;
                    mPosX += mVelX * dT + ax * dT * dT / 2;
                    mPosY += mVelY * dT + ay * dT * dT / 2;
                    mVelX += ax * dT;
                    mVelY += ay * dT;
                }

            }
            public void resolveCollisionWithBounds() {
                final float xmax = mHorizontalBound;
                final float ymax = mVerticalBound;
                final float x = mPosX;
                final float y = mPosY;
                if (x > xmax) {
                    mPosX = xmax;
                    mVelX = 0;
                } else if (x < -xmax) {
                    mPosX = -xmax;
                    mVelX = 0;
                }
                if (y > ymax) {
                    mPosY = ymax;
                    mVelY = 0;
                } else if (y < -ymax) {
                    mPosY = -ymax;
                    mVelY = 0;
                }
            }
        }
        class ParticleSystem {
            private Particle mBalls ;
            ParticleSystem() {
                mBalls = new Particle(getContext());
                mBalls.setBackgroundResource(R.drawable.ball);
                mBalls.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(mBalls, new ViewGroup.LayoutParams(mDstWidth, mDstHeight));
                mBalls.setTranslationX(0);
                mBalls.setTranslationY(0);
            }
            private void updatePositions(float sx, float sy, long timestamp) {
                final long t = timestamp;
                if (mLastT != 0) {
                    final float dT = (float) (t - mLastT) / 1000.f;
                    final float dwx = (sx)*dT;
                    final float dwy = sy*(dT);
                    Particle ball = mBalls;
                    ball.computePhysics(dwx, dwy, dT);
                }
                mLastT = t;
            }
            public void update(float sx, float sy, long now) {
                updatePositions(sx, sy, now);
                Particle curr=mBalls;
                curr.resolveCollisionWithBounds();
            }
            public float getPosX() {
                return mBalls.mPosX;
            }
            public float getPosY() {
                return mBalls.mPosY;
            }
        }
        public void startSimulation() {
            mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }
        public Spinner(Context context) {
            super(context);
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mXDpi = metrics.xdpi;
            mYDpi = metrics.ydpi;
            mMetersToPixelsX = mXDpi / 0.0254f;
            mMetersToPixelsY = mYDpi / 0.0254f;
            mDstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f);
            mDstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);
            mParticleSystem = new ParticleSystem();
        }
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mXOrigin = (w - mDstWidth) * 0.5f;
            mYOrigin = (h - mDstHeight) * 0.5f;
            mHorizontalBound = ((w / mMetersToPixelsX - sBallDiameter) * 0.5f);
            mVerticalBound = ((h / mMetersToPixelsY - sBallDiameter) * 0.5f);
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE)
                return;
            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    mSensorX = event.values[0];
                    mSensorY = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    mSensorX = -event.values[1];
                    mSensorY = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    mSensorX = -event.values[0];
                    mSensorY = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    mSensorX = event.values[1];
                    mSensorY = -event.values[0];
                    break;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final ParticleSystem particleSystem = mParticleSystem;
            final long now = System.currentTimeMillis();
            final float wX = mSensorX;
            final float wY = mSensorY;

            particleSystem.update((int) wX, (int) wY, now);

            final float xc = mXOrigin;
            final float yc = mYOrigin;
            final float xs = mMetersToPixelsX;
            final float ys = mMetersToPixelsY;

            final float x = xc + particleSystem.getPosX() * xs;
            final float y = yc - particleSystem.getPosY() * ys;
            particleSystem.mBalls.setTranslationX(x);
            particleSystem.mBalls.setTranslationY(y);

            invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}