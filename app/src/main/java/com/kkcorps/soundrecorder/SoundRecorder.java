package com.kkcorps.soundrecorder;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.LoginFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.MP3TrackImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;


public class SoundRecorder extends Activity {

    public static Context mContext;
    public static MediaRecorder mediaRecorder = new MediaRecorder();
    public static MediaPlayer mediaPlayer = new MediaPlayer();
    public static AudioRecord audioRecord;
    public static AudioTrack audioTrack;
    public boolean isMicrophoneActive = false, isAudioPlaying = false, isRecordingPaused = false;
    public static String DATA_PATH = "/mnt/external_sd/";
    public static String FILE_NAME = "Test.m4a";
    public static String OUTPUT_FILE_NAME = "output_new.m4a";
    public static String TAG = "SoundRecorderActivity";
    public static int FileDuration=0;
    public static List<Track> incompleteTracksList = new ArrayList<Track>();
    public static List<Movie> incompleteMovieList = new ArrayList<Movie>();
    public static List<List<Track>> movieTracks = new ArrayList<List<Track>>();
    public static int RECORDER_SAMPLERATE = 44100;
    public static int RECORDER_BPP = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_recorder);
        mContext = getApplicationContext();
        final ImageButton microphone_click = (ImageButton) findViewById(R.id.microphone_button);
        final ImageButton play_click = (ImageButton) findViewById(R.id.play_button);
        final ImageButton pause_click = (ImageButton) findViewById(R.id.pause_button);
        final Chronometer chronometerSeconds = (Chronometer) findViewById(R.id.chronometerSeconds);

        microphone_click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isMicrophoneActive){
                    //stopRecording();
                    mediaRecorder.stop();
                    //audioRecord.stop();

                    chronometerSeconds.stop();
                    chronometerSeconds.setBase(SystemClock.elapsedRealtime());
                    view.setBackgroundResource(R.drawable.ic_microphone);

                    mediaRecorder.reset();
                    //if(!isRecordingPaused) {
                    try {
                            File incompleteFile = new File(DATA_PATH+FILE_NAME);
                            //AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(incompleteFile));
                            Movie incompleteMovie = MovieCreator.build(DATA_PATH+FILE_NAME);
                            //AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl())
                            //incompleteTracksList.add((Track) aacTrack);
                            incompleteMovieList.add(incompleteMovie);
                    }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(mContext,"Recording could not be completed",Toast.LENGTH_SHORT).show();
                     }
                    //}
                    //appendRecordings();
                    File outfile = new File(DATA_PATH+OUTPUT_FILE_NAME);

                    Mp4ParserWrapper.append(DATA_PATH+OUTPUT_FILE_NAME,DATA_PATH+FILE_NAME);
                    isMicrophoneActive = false;
                    return;
                }

                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioChannels(2);
                mediaRecorder.setAudioEncodingBitRate(65536);
                //mediaRecorder.setAudioSamplingRate(44100);
                mediaRecorder.setOutputFile(DATA_PATH+FILE_NAME);

                Log.i(TAG,"DATA PATH: "+DATA_PATH+FILE_NAME);

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    view.setBackgroundResource(R.drawable.ic_microphone_active);
                    chronometerSeconds.setBase(SystemClock.elapsedRealtime());
                    chronometerSeconds.start();
                    isMicrophoneActive = true;
                }catch (Exception e){

                    e.printStackTrace();
                }
            }

        });

        play_click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    File file = new File(DATA_PATH+OUTPUT_FILE_NAME);
                    if(file.exists()) {
                        FileInputStream inputStream = new FileInputStream(file);
                        mediaPlayer.setDataSource(inputStream.getFD());
                        inputStream.close();
                        mediaPlayer.prepare();
                        Log.i(TAG,"Playing file: "+ DATA_PATH+OUTPUT_FILE_NAME);
                    }else{
                        Toast.makeText(getApplicationContext(),"Audio File Doesn't Exist!", Toast.LENGTH_SHORT).show();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
                isAudioPlaying = true;
            }
        });

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                play_click.setBackgroundResource(R.drawable.ic_media_stop);
                mediaPlayer.start();
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                play_click.setBackgroundResource(R.drawable.ic_media_play);
                mediaPlayer.stop();
                mediaPlayer.reset();
                isAudioPlaying = false;
            }
        });

        pause_click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaRecorder.stop();
                chronometerSeconds.stop();
                mediaRecorder.reset();
                try {
                        Movie incompleteMovie = MovieCreator.build(DATA_PATH+FILE_NAME);
                        incompleteMovieList.add(incompleteMovie);
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(mContext, "Some error was encountered",Toast.LENGTH_SHORT).show();
                        return;
                    }
                isMicrophoneActive = false;
                isRecordingPaused = true;
                Mp4ParserWrapper.append(DATA_PATH+OUTPUT_FILE_NAME,DATA_PATH+FILE_NAME);
                Toast.makeText(mContext,"Recording Paused",Toast.LENGTH_SHORT).show();
                return;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sound_recorder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
