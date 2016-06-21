package com.Tool.Function;

import android.app.Application;
import android.os.Environment;

import com.Tool.Global.Variable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zhengtongyu on 16/5/23.
 */
public class FileFunction {
    public static boolean IsExitsSdcard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean IsFileExists(String path) {
        if (CommonFunction.isEmpty(path)) {
            return false;
        }

        return new File(path).exists();
    }

    private static void CreateDirectory(String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void InitStorage(Application application) {
        if (!FileFunction.IsExitsSdcard()) {
            Variable.StorageDirectoryPath = application.getFilesDir().getAbsolutePath();
        } else {
            Variable.StorageDirectoryPath =
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/ComposeAudio/";
        }

        Variable.ErrorFilePath = Variable.StorageDirectoryPath + "error.txt";

        CreateDirectory(Variable.StorageDirectoryPath);
    }

    public static void SaveFile(String url, String content) {
        SaveFile(url, content, true, false);
    }

    public static void SaveFile(String url, String content, boolean cover, boolean append) {
        FileOutputStream out = null;
        File file = new File(url);

        try {
            if (file.exists()) {
                if (cover) {
                    file.delete();
                    file.createNewFile();
                }
            } else {
                file.createNewFile();
            }

            out = new FileOutputStream(file, append);
            out.write(content.getBytes());
            out.close();
            LogFunction.log("保存文件" + url, "保存文件成功");
        } catch (Exception e) {
            LogFunction.error("保存文件" + url, e);

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void DeleteFile(String path) {
        if (CommonFunction.notEmpty(path)) {
            File file = new File(path);

            if (file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    LogFunction.error("删除本地文件失败", e);
                }
            }
        }
    }

    public static void CopyFile(String oldPath, String newPath) {
        try {
            int byteRead;

            File oldFile = new File(oldPath);
            File newFile = new File(newPath);

            if (oldFile.exists()) { //文件存在时
                if (newFile.exists()) {
                    newFile.delete();
                }

                newFile.createNewFile();

                FileInputStream inputStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream outputStream = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];

                while ((byteRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, byteRead);
                }

                inputStream.close();
            }
        } catch (Exception e) {
            LogFunction.error("复制单个文件操作出错", e);
        }
    }

    public static FileInputStream GetFileInputStreamFromFile(String fileUrl) {
        FileInputStream fileInputStream = null;

        try {
            File file = new File(fileUrl);

            fileInputStream = new FileInputStream(file);
        } catch (Exception e) {
            LogFunction.error("GetBufferedInputStreamFromFile异常", e);
        }

        return fileInputStream;
    }

    public static FileOutputStream GetFileOutputStreamFromFile(String fileUrl) {
        FileOutputStream bufferedOutputStream = null;

        try {
            File file = new File(fileUrl);

            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();

            bufferedOutputStream = new FileOutputStream(file);
        } catch (Exception e) {
            LogFunction.error("GetFileOutputStreamFromFile异常", e);
        }

        return bufferedOutputStream;
    }

    public static BufferedOutputStream GetBufferedOutputStreamFromFile(String fileUrl) {
        BufferedOutputStream bufferedOutputStream = null;

        try {
            File file = new File(fileUrl);

            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();

            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            LogFunction.error("GetBufferedOutputStreamFromFile异常", e);
        }

        return bufferedOutputStream;
    }

    public static void RenameFile(String oldPath, String newPath) {
        if (CommonFunction.notEmpty(oldPath) && CommonFunction.notEmpty(newPath)) {
            File newFile = new File(newPath);

            if (newFile.exists()) {
                newFile.delete();
            }

            File oldFile = new File(oldPath);

            if (oldFile.exists()) {
                try {
                    oldFile.renameTo(new File(newPath));
                } catch (Exception e) {
                    LogFunction.error("删除本地文件失败", e);
                }
            }
        }
    }
}
