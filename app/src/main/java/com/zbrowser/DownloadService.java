package com.zbrowser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "zbrowser_download";
    private static final int NOTIFICATION_ID = 1001;

    public static final String EXTRA_URL = "download_url";
    public static final String EXTRA_FILENAME = "download_filename";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String downloadUrl = intent.getStringExtra(EXTRA_URL);
        String filename = intent.getStringExtra(EXTRA_FILENAME);

        if (downloadUrl == null || downloadUrl.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Security: validate URL scheme
        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            Log.w(TAG, "Blocked non-HTTP download URL: " + downloadUrl);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (filename == null || filename.isEmpty()) {
            filename = extractFilename(downloadUrl);
        }

        // Sanitize filename - remove path traversal attempts
        filename = filename.replaceAll("[\\\\/]", "_");
        filename = filename.replaceAll("\\.\\.", "_");
        // Remove null bytes and other dangerous characters
        filename = filename.replaceAll("[\\x00-\\x1f]", "");
        // Limit filename length
        if (filename.length() > 200) {
            String ext = "";
            int dot = filename.lastIndexOf('.');
            if (dot > 0) {
                ext = filename.substring(dot);
            }
            filename = filename.substring(0, 200 - ext.length()) + ext;
        }

        Notification notification = createNotification("Загрузка: " + filename, 0);
        startForeground(NOTIFICATION_ID, notification);

        final String finalFilename = filename;
        downloadFile(downloadUrl, finalFilename);

        return START_NOT_STICKY;
    }

    private void downloadFile(String urlStr, String filename) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                // Security: limit redirects
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    updateNotification("Ошибка загрузки: HTTP " + responseCode, 0);
                    return;
                }

                int fileLength = connection.getContentLength();

                // Determine MIME type from filename
                String mimeType = getMimeType(filename);
                if (mimeType == null || mimeType.isEmpty()) {
                    mimeType = "application/octet-stream";
                }

                OutputStream fileOutput = null;
                File outputFileLegacy = null;
                boolean useMediaStore = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: Use MediaStore API
                    useMediaStore = true;
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    android.net.Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) {
                        updateNotification("Ошибка: не удалось создать файл", 0);
                        return;
                    }
                    fileOutput = getContentResolver().openOutputStream(uri);
                    if (fileOutput == null) {
                        updateNotification("Ошибка: не удалось создать файл", 0);
                        return;
                    }

                    // Download to the MediaStore output stream
                    input = new BufferedInputStream(connection.getInputStream());
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        fileOutput.write(buffer, 0, count);

                        if (fileLength > 0) {
                            int progress = (int) (total * 100 / fileLength);
                            updateNotification("Загрузка: " + filename + " " + progress + "%", progress);
                        }
                    }

                    fileOutput.flush();

                    // Mark as complete
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);

                    updateNotification("Загрузка завершена: " + filename, 100);

                } else {
                    // Android 9 and below: use legacy file approach
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadDir.exists()) {
                        if (!downloadDir.mkdirs()) {
                            updateNotification("Ошибка: не удалось создать папку загрузок", 0);
                            return;
                        }
                    }

                    outputFileLegacy = getUniqueFile(downloadDir, filename);

                    input = new BufferedInputStream(connection.getInputStream());
                    output = new FileOutputStream(outputFileLegacy);

                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        output.write(buffer, 0, count);

                        if (fileLength > 0) {
                            int progress = (int) (total * 100 / fileLength);
                            updateNotification("Загрузка: " + outputFileLegacy.getName() + " " + progress + "%", progress);
                        }
                    }

                    output.flush();
                    updateNotification("Загрузка завершена: " + outputFileLegacy.getName(), 100);

                    // Scan file so it appears in the Downloads app
                    MediaScannerConnection.scanFile(this,
                            new String[]{outputFileLegacy.getAbsolutePath()},
                            new String[]{mimeType}, null);
                }

            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for download", e);
                updateNotification("Ошибка: нет разрешения на запись", 0);
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                updateNotification("Ошибка загрузки: " + filename, 0);
            } finally {
                if (output != null) {
                    try { output.close(); } catch (IOException ignored) {}
                }
                if (input != null) {
                    try { input.close(); } catch (IOException ignored) {}
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }

            stopForeground(true);
            stopSelf();
        }).start();
    }

    /**
     * Get MIME type from filename
     */
    private String getMimeType(String filename) {
        try {
            String ext = MimeTypeMap.getFileExtensionFromUrl(filename);
            if (ext != null && !ext.isEmpty()) {
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            }
        } catch (Exception e) {
            // Ignore
        }
        return "application/octet-stream";
    }

    /**
     * Generate a unique filename by appending (1), (2), etc. if file exists
     */
    private File getUniqueFile(File dir, String filename) {
        File file = new File(dir, filename);
        if (!file.exists()) return file;

        String name;
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            name = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        } else {
            name = filename;
        }

        int counter = 1;
        while (file.exists()) {
            file = new File(dir, name + " (" + counter + ")" + extension);
            counter++;
            // Safety limit
            if (counter > 100) break;
        }
        return file;
    }

    private String extractFilename(String url) {
        try {
            String path = new URL(url).getPath();
            String fname = path.substring(path.lastIndexOf('/') + 1);
            if (fname.isEmpty() || fname.length() > 255) {
                fname = "download_" + System.currentTimeMillis();
            }
            return fname;
        } catch (Exception e) {
            return "download_" + System.currentTimeMillis();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Загрузки ZBrowser",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Уведомления о загрузках");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text, int progress) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle("ZBrowser")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download);

        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false);
            builder.setOngoing(true);
        } else {
            builder.setProgress(0, 0, false);
            builder.setOngoing(false);
        }

        return builder.build();
    }

    private void updateNotification(String text, int progress) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text, progress));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
