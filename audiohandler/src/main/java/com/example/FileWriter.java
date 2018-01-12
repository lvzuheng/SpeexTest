package com.example;

import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by lzh on 2017/12/27.
 */

public class FileWriter {

    FileOutputStream fos;

    private static FileWriter fileWriter;

    public static FileWriter getInstance() throws FileNotFoundException {
        if (fileWriter == null)
            fileWriter = new FileWriter();
        return fileWriter;
    }

    public FileWriter() throws FileNotFoundException {

    }

    public void start() throws FileNotFoundException {
        if (fos == null) {
            fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/Download/" + "voice");
        }
    }

    public void write(byte[] bytes) throws IOException {
        if (fos != null) {
            fos.write(bytes);
        }

    }

    public void write(byte[] bytes, int off, int len) {
        if (fos != null) {
            Log.e("lzh", "audio:write:" + bytes.length + "," + off + "," + len + "," + fos);
            try {
                fos.write(bytes, off, len);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FileInputStream read() throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/Download/" + "voice");
        return fis;
    }

    public FileInputStream read(String filename) throws FileNotFoundException {
        Log.e("lzh", "audio:" + Environment.getExternalStorageDirectory() + "/Download/" + filename);
        FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/Download/" + filename);
        return fis;
    }

    public void flush() {
        try {
            fos.flush();
            Log.e("lzh", "audio:flush:" + fos);
            fos.close();
            fos = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
