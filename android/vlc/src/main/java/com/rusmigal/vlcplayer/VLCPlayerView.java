package com.rusmigal.vlcplayer;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.vlcplayer.R;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCUtil;
import org.videolan.vlc.listener.MediaListenerEvent;
import org.videolan.vlc.util.VLCOptions;

import java.util.ArrayList;

import static com.rusmigal.vlcplayer.VLCPlayerViewManager.mOptions;

public class VLCPlayerView extends FrameLayout
        implements IVLCVout.Callback, LifecycleEventListener, MediaListenerEvent, MediaPlayer.EventListener {

    private boolean pausedState;
    MyVideoView videoView;

    public enum Events {
        EVENT_PROGRESS("onVLCProgress"), EVENT_ENDED("onVLCEnded"), EVENT_STOPPED("onVLCStopped"),
        EVENT_PLAYING("onVLCPlaying"), EVENT_BUFFERING("onVLCBuffering"), EVENT_PAUSED("onVLCPaused"), RECEIVED_DIMENSIONS("onDimsReceived"),
        EVENT_ERROR("onVLCError"), EVENT_VOLUME_CHANGED("onVLCVolumeChanged"), EVENT_SEEK("onVLCVideoSeek");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    public static final String EVENT_PROP_POSITION = "position";
    public static final String EVENT_PROP_END = "endReached";
    public static final String EVENT_PROP_SEEK_TIME = "seekTime";

    private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;

    private String mSrcString;

    // media player
    private MediaPlayer mMediaPlayer = null;

    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;
    private int mVideoHeight;
    private int mVideoWidth;

    private int counter = 0;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;

    private int mCurrentSize = SURFACE_BEST_FIT;
    private Media media;
    private boolean autoPlay;

    public VLCPlayerView(ThemedReactContext context) {
        super(context);
        mThemedReactContext = context;
        mEventEmitter = mThemedReactContext.getJSModule(RCTEventEmitter.class);
        mThemedReactContext.addLifecycleEventListener(this);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.player, this);
        videoView = (MyVideoView) findViewById(R.id.vlc_surface);
        videoView.setMediaListenerEvent(this);
    }

    private void setMedia(String filePath) {
        // Set up video output
        ArrayList<String> options = new ArrayList<>(50);
        ArrayList<String> initOptions = new ArrayList<>(50);
        ReadableArray initOptionsRA = mOptions != null && mOptions.getArray("initOptions") != null ? mOptions.getArray("initOptions") : null;

        if (initOptionsRA != null) {
            for (int i = 0; i < initOptionsRA.size(); i++) {
                String str = initOptionsRA.getString(i);
                initOptions.add(str);
            }
        }

        for (int i = 0; i < initOptions.size(); i++) {
            options.add(initOptions.get(i).toString());
        }
        LibVLC  libvlc = new LibVLC(this.mThemedReactContext,options); 
        videoView.setLibVLC(libvlc);
        
        Uri uri = Uri.parse(filePath);
        String path = uri.toString();
        videoView.setPath(path);
        videoView.startPlay();
    }

    private static int getDeblocking(int deblocking) {
        int ret = deblocking;
        if (deblocking < 0) {
            /**
             * Set some reasonable sDeblocking defaults:
             *
             * Skip all (4) for armv6 and MIPS by default
             * Skip non-ref (1) for all armv7 more than 1.2 Ghz and more than 2 cores
             * Skip non-key (3) for all devices that don't meet anything above
             */
            VLCUtil.MachineSpecs m = VLCUtil.getMachineSpecs();
            if (m == null)
                return ret;
            if ((m.hasArmV6 && !(m.hasArmV7)) || m.hasMips)
                ret = 4;
            else if (m.frequency >= 1200 && m.processors > 2)
                ret = 1;
            else if (m.bogoMIPS >= 1200 && m.processors > 2) {
                ret = 1;
            } else
                ret = 3;
        } else if (deblocking > 4) { // sanity check
            ret = 3;
        }
        return ret;
    }

    private void releasePlayer() {
        videoView.onStop();
//        mMediaPlayer.stop();
//        final IVLCVout vout = mMediaPlayer.getVLCVout();
//        vout.removeCallback(this);
//        vout.detachViews();
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private void changeSurfaceSize(int width, int height) {
        // ф-ция работает неправильно:
        //   при изменении размеров surfaceView ф-ция вызывается трижды
        //     раз со значениями width, height, переданными из VLCPlayer.props.resize
        //     два и три со значениями, равными значениям расширения видео
        //  изменение размеров surfaceView с помощью surfaceHolder'а работает не так, как нужно:
        //    по документации surfaceHolder меняет значения width, height только один раз
        //    в действительности все не так как на самом деле - размеры width, heigth меняются, но только в сторону уменьшения

        // int screenWidth = width;
        // int screenHeight = height;
        // mVideoWidth = width;
        // mVideoHeight = height;
        // mVideoVisibleWidth = width;
        // mVideoVisibleHeight = height;

        // if (mMediaPlayer != null) {
        //     final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        //     vlcVout.setWindowSize(screenWidth, screenHeight);
        // }

        // double displayWidth = screenWidth, displayHeight = screenHeight;

        // if (screenWidth < screenHeight) {
        //     displayWidth = screenHeight;
        //     displayHeight = screenWidth;
        // }

        // // sanity check
        // if (displayWidth * displayHeight <= 1 || mVideoWidth * mVideoHeight <= 1) {
        //     return;
        // }

        // // compute the aspect ratio
        // double aspectRatio, visibleWidth;
        // if (mSarDen == mSarNum) {
        //     /* No indication about the density, assuming 1:1 */
        //     visibleWidth = mVideoVisibleWidth;
        //     aspectRatio = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
        // } else {
        //     /* Use the specified aspect ratio */
        //     visibleWidth = mVideoVisibleWidth * (double) mSarNum / mSarDen;
        //     aspectRatio = visibleWidth / mVideoVisibleHeight;
        // }

        // // compute the display aspect ratio
        // double displayAspectRatio = displayWidth / displayHeight;

        // counter++;

        // switch (mCurrentSize) {
        //     case SURFACE_BEST_FIT:
        //         if (counter > 2)
        //             if (displayAspectRatio < aspectRatio)
        //                 displayHeight = displayWidth / aspectRatio;
        //             else
        //                 displayWidth = displayHeight * aspectRatio;
        //         break;
        //     case SURFACE_FIT_HORIZONTAL:
        //         displayHeight = displayWidth / aspectRatio;
        //         break;
        //     case SURFACE_FIT_VERTICAL:
        //         displayWidth = displayHeight * aspectRatio;
        //         break;
        //     case SURFACE_FILL:
        //         break;
        //     case SURFACE_16_9:
        //         aspectRatio = 16.0 / 9.0;
        //         if (displayAspectRatio < aspectRatio)
        //             displayHeight = displayWidth / aspectRatio;
        //         else
        //             displayWidth = displayHeight * aspectRatio;
        //         break;
        //     case SURFACE_4_3:
        //         aspectRatio = 4.0 / 3.0;
        //         if (displayAspectRatio < aspectRatio)
        //             displayHeight = displayWidth / aspectRatio;
        //         else
        //             displayWidth = displayHeight * aspectRatio;
        //         break;
        //     case SURFACE_ORIGINAL:
        //         displayHeight = mVideoVisibleHeight;
        //         displayWidth = visibleWidth;
        //         break;
        // }

        // // set display size
        // int finalWidth = (int) Math.ceil(displayWidth * mVideoWidth / mVideoVisibleWidth);
        // int finalHeight = (int) Math.ceil(displayHeight * mVideoHeight / mVideoVisibleHeight);

        // SurfaceHolder holder = mSurface.getHolder();
        // holder.setFixedSize(finalWidth, finalHeight);

        // ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        // lp.width = finalWidth;
        // lp.height = finalHeight;
        // mSurface.setLayoutParams(lp);
        // mSurface.invalidate();
    }

    public void changeSurfaceLayout(int width, int height) {
        changeSurfaceSize(width, height);
    }

    public void setFilePath(String filePath) {
        this.mSrcString = filePath;
        setMedia(mSrcString);
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    /**
     * Play or pause the media.
     */
    public void setPaused(boolean paused) {
        pausedState = paused;
        if (mMediaPlayer != null) {
            if (paused) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();// pause -> stop, т.к. видео используется только для просмотра online видео
                }
            } else {
                if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.play();
                }
            }
        }
    }

    public void onDropViewInstance() {
        releasePlayer();
    }

    public void seek(float seek) {
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getTime());
        event.putDouble(EVENT_PROP_SEEK_TIME, seek);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), event);
        mMediaPlayer.setTime((long) (mMediaPlayer.getLength() * seek));
    }

    public void setVolume(int volume) {
        mMediaPlayer.setVolume(volume);
    }

    //@Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum,
                            int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mSarNum = sarNum;
        mSarDen = sarDen;
        // changeSurfaceLayout(width, height);

        WritableMap eventMap = Arguments.createMap();
        eventMap.putInt("width", width);
        eventMap.putInt("height", height);
        eventMap.putInt("visibleWidth", visibleWidth);
        eventMap.putInt("visibleHeight", visibleHeight);
        mEventEmitter.receiveEvent(getId(), Events.RECEIVED_DIMENSIONS.toString(), eventMap);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    //@Override
    public void onHardwareAccelerationError(IVLCVout vout) {
        // Handle errors with hardware acceleration
        this.releasePlayer();
        Toast.makeText(getContext(), "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onHostResume() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // Restore original state
                setPaused(pausedState);
            }
        });
    }

    @Override
    public void onHostPause() {
        setPaused(true);
    }

    @Override
    public void onHostDestroy() {

    }

    public void onEvent(MediaPlayer.Event event) {
        WritableMap eventMap = Arguments.createMap();
        switch (event.type) {
            case MediaPlayer.Event.EndReached:
                pausedState = false;
                eventMap.putBoolean(EVENT_PROP_END, true);
                mEventEmitter.receiveEvent(getId(), Events.EVENT_ENDED.toString(), eventMap);
                break;
            case MediaPlayer.Event.Stopped:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_STOPPED.toString(), null);
                break;
            case MediaPlayer.Event.Playing:
                eventMap.putDouble(EVENT_PROP_DURATION, mMediaPlayer.getLength());
                mEventEmitter.receiveEvent(getId(), Events.EVENT_PLAYING.toString(), eventMap);
                break;
            case MediaPlayer.Event.Buffering:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_BUFFERING.toString(), null);
                break;
            case MediaPlayer.Event.Paused:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_PAUSED.toString(), null);
                break;
            case MediaPlayer.Event.EncounteredError:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), null);
                break;
            case MediaPlayer.Event.TimeChanged:
                long ct = mMediaPlayer.getTime();
                long dur = mMediaPlayer.getLength();
                float pos = mMediaPlayer.getPosition();
                pos = pos == Double.POSITIVE_INFINITY ? -1 : pos;

                eventMap.putDouble(EVENT_PROP_CURRENT_TIME, ct);
                eventMap.putDouble(EVENT_PROP_DURATION, dur);
                eventMap.putDouble(EVENT_PROP_POSITION, pos);
                mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), eventMap);
                break;
        }
    }

    long time;

    @Override
    public void eventBuffing(int event, float buffing) {
        Log.i("yyl", "eventBuffing");
        mThemedReactContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEventEmitter.receiveEvent(getId(), Events.EVENT_BUFFERING.toString(), null);
            }
        });
    }

    @Override
    public void eventPlayInit(boolean openingVideo) {
        if (!openingVideo) {
            Log.i("yyl", "eventPlayInit - not openingVideo");
            time = System.currentTimeMillis();
            mThemedReactContext.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_STOPPED.toString(), null);
                }
            });
        } else {
            long useTime = System.currentTimeMillis() - time;
            Log.i("yyl", "=" + useTime);
        }
        if (openingVideo) {
            Log.i("yyl", "eventPlayInit - openingVideo");
        }
    }


    @Override
    public void eventStop(boolean isPlayError) {
        Log.i("yyl", "eventStop");
        mThemedReactContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEventEmitter.receiveEvent(getId(), Events.EVENT_STOPPED.toString(), null);
            }
        });

    }


    public void eventError(int error, boolean show) {
        Log.i("yyl", "eventError");
        mThemedReactContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), null);
            }
        });
    }

    @Override
    public void eventPlay(boolean isPlaying) {
        if (isPlaying) {
            Log.i("yyl", "eventPlay - isPlaying");
            long useTime = System.currentTimeMillis() - time;
            Log.i("yyl", "=" + useTime);

            mThemedReactContext.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PLAYING.toString(), null);
                }
            });

        } else {
            Log.i("yyl", "eventPlay - not isPlaying");
            mThemedReactContext.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_STOPPED.toString(), null);
                }
            });
        }
    }
}

