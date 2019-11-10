package com.example.face_recognition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Environment;

/**
 * PictureCompressUtil
 * 图像压缩工厂类
 *
 * Bitmap.compress方法确实可以压缩图片，但压缩的是存储大小，即你放到disk上的大小
 * 内存中的bitmap大小不变
 * 如果想要显示的Bitmap占用的内存少一点，还是需要去设置加载的像素长度和宽度（变成缩略图）
 *
 * 图片有三种存在形式：硬盘上时是file，网络传输时是stream，内存中是stream或bitmap，所谓的质量压缩，它其实只能实现对file的影响，你可以把一个file转成bitmap再转成file，或者直接将一个bitmap转成file时，这个最终的file是被压缩过的，但是中间的bitmap并没有被压缩（或者说几乎没有被压缩，我不确定），因为bigmap在内存中的大小是按像素计算的，也就是width * height，对于质量压缩，并不会改变图片的像素，所以就算质量被压缩了，但是bitmap在内存的占有率还是没变小，但你做成file时，它确实变小了；
 * 而尺寸压缩由于是减小了图片的像素，所以它直接对bitmap产生了影响，当然最终的file也是相对的变小了；
 *
 * compressAndGenImage()  将图片按质量压缩成指定大小，并将图像生成指定的路径
 * compressBySampleSize() 将图片按采样率进行压缩，并将图像生成指定的路径
 *
 *
 */
public class PictureCompressUtil {


    /**
     * 按质量压缩，并将图像生成指定的路径
     *
     * @param imgPath
     * @param outPath
     * @param maxSize      目标将被压缩到小于这个大小（KB）。
     * @param needsDelete  是否压缩后删除原始文件
     * @throws IOException
     */
    public void compressByQuality(String imgPath, String outPath, int maxSize, boolean needsDelete)
            throws IOException {
        compressAndGenImage(getBitmap(imgPath), outPath, maxSize);

        // Delete original file
        if (needsDelete) {
            File file = new File(imgPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }


    /**
     * 压缩图片（质量压缩）
     *
     * @param bm       图片格式 jpeg,png,webp
     * @param quality  图片的质量,0-100,数值越小质量越差
     * @param maxSize  压缩后图片的最大kb值
     */
    public static File compressByQuality(Bitmap bm,int quality,int maxSize) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, quality, bos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        long length=bos.toByteArray().length;
        while (length/ 1024 > maxSize) { // 循环判断如果压缩后图片是否大于minSize,大于继续压缩
            bos.reset();// 重置bos即清空bos
            quality -= 5;// 每次都减少5
            bm.compress(Bitmap.CompressFormat.JPEG, quality, bos);// 这里压缩options%，把压缩后的数据存放到baos中
            length = bos.toByteArray().length;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String filename = format.format(date);
        File file = new File(Environment.getExternalStorageDirectory(), filename + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(bos.toByteArray());
                fos.flush();
                fos.close();
            } catch (IOException e) {

                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }
        recycleBitmap(bm);
        return file;
    }

    public static void recycleBitmap(Bitmap... bitmaps) {
        if (bitmaps == null) {
            return;
        }
        for (Bitmap bm : bitmaps) {
            if (null != bm && !bm.isRecycled()) {
                bm.recycle();
            }
        }
    }



    /**
     * 通过像素压缩图像，这将改变图像的宽度/高度。用于获取缩略图
     *
     * @param imgPath   image path
     * @param pixelW    目标宽度像素
     * @param pixelH    高度目标像素
     * @return
     */
    public Bitmap ratio(String imgPath, float pixelW, float pixelH) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true，即只读边不读内容
        newOpts.inJustDecodeBounds = true;
        newOpts.inPreferredConfig = Config.RGB_565;
        // 获取位图信息，但请注意位图现在为空
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath, newOpts);

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 想要缩放的目标尺寸,现在大部分手机都是1080*1920，参考值可以让宽高都缩小一倍
        float hh = pixelH;// 设置高度为960f时，可以明显看到图片缩小了
        float ww = pixelW;// 设置宽度为540f，可以明显看到图片缩小了
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// 设置缩放比例
        // 开始压缩图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(imgPath, newOpts);
        // 压缩好比例大小后再进行质量压缩
//      return compressByQuality(bitmap, 100,1000); // 这里再进行质量压缩的意义不大，反而耗资源，删除
        return bitmap;
    }

    /**
     * 压缩图像的大小，这将修改图像宽度/高度。用于获取缩略图
     *
     * @param bm
     * @param pixelW  target pixel of width
     * @param pixelH  target pixel of height
     * @return
     */
    public static Bitmap ratio(Bitmap bm, double pixelW, double pixelH) {
        //bitmap大小小于512kb，则直接返回，不需要压缩
        if (bm.getByteCount() < 512){
            return bm;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
        if (os.toByteArray().length / 1024 > 1024) {// 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            os.reset();// 重置baos即清空baos
            bm.compress(Bitmap.CompressFormat.JPEG, 50, os);// 这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        newOpts.inPreferredConfig = Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 想要缩放的目标尺寸,现在大部分手机都是1080*1920，参考值可以让宽高都缩小一倍
        double hh = pixelH;// 设置高度为960f时，可以明显看到图片缩小了
        double ww = pixelW;// 设置宽度为540f，可以明显看到图片缩小了
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        is = new ByteArrayInputStream(os.toByteArray());
        bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        // 压缩好比例大小后再进行质量压缩
        // return compress(bitmap, maxSize); // 这里再进行质量压缩的意义不大，反而耗资源，删除
        return bitmap;
    }





    /**
     * 按质量压缩，并将图像生成指定的路径
     *
     * @param bm
     * @param outPath
     * @param maxSize 目标将被压缩到小于这个大小（KB）。
     * @throws IOException
     */
    public void compressAndGenImage(Bitmap bm, String outPath, int maxSize) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // scale
        int options = 100;
        // Store the bitmap into output stream(no compress)
        bm.compress(Bitmap.CompressFormat.JPEG, options, os);
        // Compress by loop
        while (os.toByteArray().length / 1024 > maxSize) {
            // Clean up os
            os.reset();
            // interval 10
            options -= 10;
            bm.compress(Bitmap.CompressFormat.JPEG, options, os);
        }

        // Generate compressed image file
        FileOutputStream fos = new FileOutputStream(outPath);
        fos.write(os.toByteArray());
        fos.flush();
        fos.close();
    }

    /**
     * 比例和生成图片的路径指定
     *
     * @param bm
     * @param outPath
     * @param pixelW 目标宽度像素
     * @param pixelH 高度目标像素
     * @throws FileNotFoundException
     */
    public void compressBySampleSize(Bitmap bm, String outPath, float pixelW, float pixelH)
            throws FileNotFoundException {
        Bitmap bitmap = ratio(bm, pixelW, pixelH);
        storeImage(bitmap, outPath);
    }

    /**
     * 比例生成图片的路径指定
     *
     * @param imgPath
     * @param outPath
     * @param pixelW 目标宽度像素
     * @param pixelH 高度目标像素
     * @param needsDelete 是否压缩后删除原始文件
     * @throws FileNotFoundException
     */
    public void compressBySampleSize(String imgPath, String outPath, float pixelW, float pixelH, boolean needsDelete)
            throws FileNotFoundException {
        Bitmap bitmap = ratio(imgPath, pixelW, pixelH);
        storeImage(bitmap, outPath);
        if (needsDelete) {
            File file = new File(imgPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 将位图存储到指定的图像路径中
     *
     * @param bm
     * @param outPath
     * @throws FileNotFoundException
     */
    public void storeImage(Bitmap bm, String outPath) throws FileNotFoundException {
        FileOutputStream os = new FileOutputStream(outPath);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
    }

    /**
     * 从指定的图像路径获取位图
     * @param imgPath
     * @return
     */
    public Bitmap getBitmap(String imgPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = false;
        newOpts.inPurgeable = true;
        newOpts.inInputShareable = true;
        newOpts.inSampleSize = 1;
        newOpts.inPreferredConfig = Config.RGB_565;//设置RGB
        newOpts.inDither = true;
        return BitmapFactory.decodeFile(imgPath, newOpts);
    }

    /**
     * 压缩图片（质量压缩）
     * Bitmap.compress方法确实可以压缩图片，但压缩的是存储大小，即你放到disk上的大小
     * 内存中的bitmap大小不变
     * 如果想要显示的Bitmap占用的内存少一点，还是需要去设置加载的像素长度和宽度（变成缩略图）
     * @param bm       图片格式 jpeg,png,webp
     * @param maxSize  压缩后图片的最大kb值
     */
    public static Bitmap compress2Bitmap(Bitmap bm, int maxSize) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // scale
        int options = 100;
        // Store the bitmap into output stream(no compress)
        bm.compress(Bitmap.CompressFormat.JPEG, options, os);
        // Compress by loop
        while (os.toByteArray().length / 1024 > maxSize) {
            // Clean up os
            os.reset();
            // interval 10
            options -= 10;
            bm.compress(Bitmap.CompressFormat.JPEG, options, os);
        }
        return bm;
    }

    /**
     * 把Bitmap转Byte
     */
    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
