package com.example.gravity;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;


public class MainActivity extends Activity {

    private GameView mGameView;
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mGameView = new GameView(this);
        mGameView.setBackgroundResource(R.drawable.woodz);
        setContentView(mGameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGameView.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameView.stopSimulation();
    }

    class GameView extends FrameLayout implements SensorEventListener {
        private static final float sBallDiameter = 0.004f;

        private final int mDstWidth;
        private final int mDstHeight;

        private Sensor mGravity;
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
        private final ParticleSystem mParticleSystem;
        class Particle extends View {
            private float mPosX = (float) Math.random();
            private float mPosY = (float) Math.random();
            private float mVelX;
            private float mVelY;
            private float us = 0.15f;
            private float uk = 0.1f;

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
            public Particle(Context context, AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
                super(context, attrs, defStyleAttr, defStyleRes);
            }

            public void computePhysics(float sx, float sy, float dT) {
                final int mdirX=-(int)(Math.abs(mVelX)/mVelX);
                final int mdirY=-(int)(Math.abs(mVelY)/mVelY);
                float curUX=uk;
                float curUY=uk;
                if(mVelX==0)curUX=us;
                if(mVelY==0)curUY=us;
                float ax = -sx+mdirX*curUX*9.81f;
                float ay = -sy+mdirY*curUY*9.81f;

                if(Math.abs(sx)<Math.abs(mdirX*curUX*9.81f))ax=0;
                if(Math.abs(sy)<Math.abs(mdirY*curUY*9.81f))ax=0;

                mPosX += mVelX * dT + ax * dT * dT / 2;
                mPosY += mVelY * dT + ay * dT * dT / 2;

                mVelX += ax * dT;
                mVelY += ay * dT;
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
            }

            private void updatePositions(float sx, float sy, long timestamp) {
                final long t = timestamp;
                if (mLastT != 0) {
                    final float dT = (float) (t - mLastT) / 1000.f;
                    Particle ball = mBalls;
                    ball.computePhysics(sx, sy, dT);
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
            mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_GAME);
        }

        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }

        public GameView(Context context) {
            super(context);
            mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mXDpi = metrics.xdpi;
            mYDpi = metrics.ydpi;
            mMetersToPixelsX = mXDpi / 0.0254f;
            mMetersToPixelsY = mYDpi / 0.0254f;

            mDstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f);
            mDstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);
            mParticleSystem = new ParticleSystem();

            Options opts = new Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
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
            if (event.sensor.getType() != Sensor.TYPE_GRAVITY)
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
            final float sx = mSensorX;
            final float sy = mSensorY;

            particleSystem.update(sx, sy, now);

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