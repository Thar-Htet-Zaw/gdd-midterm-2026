// Java program to play an Audio
// file using Clip Object
package gdd;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer {

    // Plays a short, one-shot sound effect (shot fired, wall destroyed,
    // ship exploded, etc.) - separate from this class's normal instance
    // usage above, which is built for a single continuously looping
    // background track (see the constructor's Clip.LOOP_CONTINUOUSLY).
    // Each call here opens its own fresh, short-lived Clip and closes it
    // automatically once playback finishes, so multiple effects can
    // overlap (e.g. firing rapidly) without cutting each other off or
    // interfering with any looping background music already playing.
    public static void playOnce(String filePath) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
        } catch (Exception e) {
            System.err.println("Error playing sound effect '" + filePath + "': " + e.getMessage());
        }
    }

    // to store current position
    Long currentFrame;
    Clip clip;

    // current status of clip
    String status;

    AudioInputStream audioInputStream;
    String filePath;

    // constructor to initialize streams and clip
    public AudioPlayer(String filePath)
            throws UnsupportedAudioFileException,
            IOException, LineUnavailableException {
        // create AudioInputStream object
        this.filePath = filePath;
        audioInputStream
                = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());

        // create clip reference
        clip = AudioSystem.getClip();

        // open audioInputStream to the clip
        clip.open(audioInputStream);

        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public static void main(String[] args) {
        try {
            String filePath = "src/audio/title.wav";
            AudioPlayer audioPlayer = new AudioPlayer(filePath);

            audioPlayer.play();
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("1. pause");
                System.out.println("2. resume");
                System.out.println("3. restart");
                System.out.println("4. stop");
                System.out.println("5. Jump to specific time");
                int c = sc.nextInt();
                audioPlayer.gotoChoice(c);
                if (c == 4) {
                    break;
                }
            }
            sc.close();
        } catch (Exception ex) {
            System.out.println("Error with playing sound.");
            ex.printStackTrace();

        }
    }

    // Work as the user enters his choice
    private void gotoChoice(int c)
            throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        switch (c) {
            case 1:
                pause();
                break;
            case 2:
                resumeAudio();
                break;
            case 3:
                restart();
                break;
            case 4:
                stop();
                break;
            case 5:
                System.out.println("Enter time (" + 0
                        + ", " + clip.getMicrosecondLength() + ")");
                Scanner sc = new Scanner(System.in);
                long c1 = sc.nextLong();
                jump(c1);
                break;

        }

    }

    // Method to play the audio
    public void play() {
        //start the clip
        clip.start();

        status = "play";
    }


    // Method to pause the audio
    public void pause() {
        if (status.equals("paused")) {
            System.out.println("audio is already paused");
            return;
        }
        this.currentFrame
                = this.clip.getMicrosecondPosition();
        clip.stop();
        status = "paused";
    }

    // Method to resume the audio
    public void resumeAudio() throws UnsupportedAudioFileException,
            IOException, LineUnavailableException {
        if (status.equals("play")) {
            System.out.println("Audio is already "
                    + "being played");
            return;
        }
        clip.close();
        resetAudioStream();
        clip.setMicrosecondPosition(currentFrame);
        this.play();
    }

    // Method to restart the audio
    public void restart() throws IOException, LineUnavailableException,
            UnsupportedAudioFileException {
        clip.stop();
        clip.close();
        resetAudioStream();
        currentFrame = 0L;
        clip.setMicrosecondPosition(0);
        this.play();
    }

    // Method to stop the audio
    public void stop() throws UnsupportedAudioFileException,
            IOException, LineUnavailableException {
        currentFrame = 0L;
        clip.stop();
        clip.close();
    }

    // Method to jump over a specific part
    public void jump(long c) throws UnsupportedAudioFileException, IOException,
            LineUnavailableException {
        if (c > 0 && c < clip.getMicrosecondLength()) {
            clip.stop();
            clip.close();
            resetAudioStream();
            currentFrame = c;
            clip.setMicrosecondPosition(c);
            this.play();
        }
    }

    // Method to reset audio stream
    public void resetAudioStream() throws UnsupportedAudioFileException, IOException,
            LineUnavailableException {
        audioInputStream = AudioSystem.getAudioInputStream(
                new File(filePath).getAbsoluteFile());
        clip.open(audioInputStream);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

}