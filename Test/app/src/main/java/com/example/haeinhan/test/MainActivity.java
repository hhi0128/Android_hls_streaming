package com.example.haeinhan.test;

import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.hls.DefaultHlsTrackSelector;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsMasterPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.hls.PtsTimestampAdjusterProvider;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements ManifestFetcher.ManifestCallback<HlsPlaylist>,
        ExoPlayer.Listener,HlsSampleSource.EventListener, AudioManager.OnAudioFocusChangeListener{

    private SurfaceView surface;
    private TextView txt_playState;
    private ExoPlayer player;
    private PlayerControl playerControl;
    private String urlString;
    private Handler mainHandler;
    private AudioManager am;
    private String userAgent;
    private ManifestFetcher<HlsPlaylist> playlistFetcher;
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int MAIN_BUFFER_SEGMENTS = 254;
    private static final int TYPE_VIDEO = 0;
    private TrackRenderer videoRenderer;
    private MediaCodecAudioTrackRenderer audioRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
    }

    private void initialize(){
        surface = (SurfaceView)findViewById(R.id.surfaceView);
        txt_playState = (TextView)findViewById(R.id.textView);
        player = ExoPlayer.Factory.newInstance(2);
        playerControl = new PlayerControl(player);
        urlString = "your_hls_url";
        am = (AudioManager) this.getApplicationContext().getSystemService(this.AUDIO_SERVICE);
        mainHandler = new Handler();
        userAgent = Util.getUserAgent(this, "MainActivity");
        HlsPlaylistParser parser = new HlsPlaylistParser();
        playlistFetcher = new ManifestFetcher<>(urlString, new DefaultUriDataSource(this, userAgent), parser);
        playlistFetcher.singleLoad(mainHandler.getLooper(), this);
    }

    @Override
    public void onSingleManifest(HlsPlaylist manifest){
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        PtsTimestampAdjusterProvider timestampAdjusterProvider = new PtsTimestampAdjusterProvider();
        boolean haveSubtitles = false;
        boolean haveAudios = false;
        if(manifest instanceof HlsMasterPlaylist){
            HlsMasterPlaylist masterPlaylist = (HlsMasterPlaylist) manifest;
            haveSubtitles = !masterPlaylist.subtitles.isEmpty();
        }

        DataSource dataSource = new DefaultUriDataSource(this, bandwidthMeter, userAgent);
        HlsChunkSource chunkSource = new HlsChunkSource(true, dataSource, manifest,
                DefaultHlsTrackSelector.newDefaultInstance(this), bandwidthMeter,
                timestampAdjusterProvider, HlsChunkSource.ADAPTIVE_MODE_SPLICE);
        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl,
                MAIN_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, this, TYPE_VIDEO);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(this, sampleSource,
                MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT);
        this.videoRenderer = videoRenderer;
        this.audioRenderer = audioRenderer;
        pushSurface(false); // here we pushsurface
        player.prepare(videoRenderer,audioRenderer); //prepare
        player.addListener(this); //add listener for the text field
        if (requestFocus())
            player.setPlayWhenReady(true);
    }

    public boolean requestFocus(){
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                am.requestAudioFocus(MainActivity.this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
    }

    private void pushSurface(boolean blockForSurfacePush) {
        if (videoRenderer == null) {return;}
        if (blockForSurfacePush) {
            player.blockingSendMessage(
                    videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface.getHolder().getSurface());
        } else {
            player.sendMessage(
                    videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface.getHolder().getSurface());
        }
    }

    @Override
    public void onSingleManifestError(IOException e) {

    }
    // I'll upload this code on drive then just extarct it and understand ok
    //lets check
    // also watch my videos with my daughter
    //thanks!!!
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        String text = "";
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
        txt_playState.setText(text);

        //for the text feild
    }

    @Override
    public void onPlayWhenReadyCommitted() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format, long mediaStartTimeMs, long mediaEndTimeMs) {

    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {

    }

    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {

    }

    @Override
    public void onLoadError(int sourceId, IOException e) {

    }

    @Override
    public void onUpstreamDiscarded(int sourceId, long mediaStartTimeMs, long mediaEndTimeMs) {

    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger, long mediaTimeMs) {

    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

}
