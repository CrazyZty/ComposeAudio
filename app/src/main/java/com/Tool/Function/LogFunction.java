package com.Tool.Function;

import android.util.Log;

import com.Tool.Global.Constant;
import com.Tool.Global.Variable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Created by zhengtongyu on 16/5/23.
 */
public class LogFunction {
    private static final String tag = "AppLog";

    private static BufferedWriter errorOutputStream;

    public static synchronized void UpdateErrorOutputStream() {
        try {
            Log.d("刷新error文件输出流", "刷新开始");

            File file = new File(Variable.ErrorFilePath);

            if (!file.exists()) {
                file.createNewFile();
            }

            errorOutputStream = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(Variable.ErrorFilePath, true)));
        } catch (Exception e) {
            Log.e("刷新error日志文件输出流出错", e.toString());
        }
    }

    public static synchronized void FinishErrorOutputStream() {
        try {
            errorOutputStream.close();
        } catch (Exception e) {
            Log.e("关闭error文件出错", e.toString());
        }
    }

    public static String getStackTrace(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static String getStackInformation(String content) {
        StringBuffer buffer = new StringBuffer(content == null ? content = "" : content);
        String className;
        String methodName;
        int lineNumber;

        try {
            final int beginStackIndex = 2;
            final int outputStackLength = 2;
            int endStackIndex = beginStackIndex + outputStackLength;
            StackTraceElement[] element = new Throwable().getStackTrace();

            int totalStackLength = element.length;

            if (endStackIndex > totalStackLength) {
                endStackIndex = totalStackLength;
            }

            for (int index = beginStackIndex; index < endStackIndex; index++) {
                className = element[index].getFileName();
                methodName = element[index].getMethodName();
                lineNumber = element[index].getLineNumber();
                buffer.append("\n").append(" ").append(methodName).append("(").append(className)
                        .append(":").append(lineNumber).append(")");
            }
        } catch (Exception e) {
            Log.e(tag + ":获取stack调用信息异常", e.toString());
        }

        return buffer.toString();
    }

    /*
     * 打印日志数据
	 */
    public static void log(String title, String content) {
        if (Constant.Debug) {
            Log.d(tag + ":" + title, getStackInformation(content));
        }
    }

    /*
     * 打印错误日志数据，同时将数据写到外文件
     */
    public static void error(String title, String content) {
        if (Constant.Debug) {
            Log.e(tag + " :" + title, getStackInformation(content));
        }

        try {
            if (errorOutputStream == null || !FileFunction.IsFileExists(Variable.ErrorFilePath)) {
                UpdateErrorOutputStream();
            }

            errorOutputStream
                    .write(CommonFunction.GetDate() + "   " + title + ":" + content + "\r\n");
            errorOutputStream.flush();
        } catch (Exception e) {
            Log.e(tag + ":打印error数据异常", e.toString());
        }
    }

    /*
     * 为不使getStackInformation少输出一层，故而放弃使用error(title，e.toString()));
     */
    public static void error(String title, Exception exception) {
        if (Constant.Debug) {
            Log.e(tag + ":" + title, getStackInformation(exception.toString()));
        }

        try {
            if (errorOutputStream == null || !FileFunction.IsFileExists(Variable.ErrorFilePath)) {
                UpdateErrorOutputStream();
            }

            errorOutputStream
                    .write(CommonFunction.GetDate() + "   " + title + ":" + exception.toString() +
                            "\r\n");
            errorOutputStream.flush();
        } catch (Exception e) {
            Log.e(tag + ":打印error数据异常", e.toString());
        }
    }
}
