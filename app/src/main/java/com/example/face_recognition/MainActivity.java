package com.example.face_recognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private Uri imgUri; //记录拍照后的照片文件的地址(临时文件)
    private ImageView imgView;//用于查看照片的view
    private ImageView photo;
    private String uploadFileName;
    private byte[] fileBuf;
    private String uploadUrl = "http://172.20.10.3:8000/upload";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_activity);
        imgView=findViewById(R.id.imageView);
        photo=findViewById(R.id.photo);
    }


    //相册按钮点击事件
    public void select(View view) {
        String[] permissions=new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        //进行sdcard的读写请求
        //ContextCompat.checkSelfPermission确认是否被授予存储权限，如果未，请求，否则打开相册
        //PERMISSION_DENIED，权限检查结果：checkPermission如果尚未将权限授予给定包，则返回此结果。
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            //请求授予此应用程序的权限，传出值为1，标志为此次请求
            ActivityCompat.requestPermissions(this,permissions,1);
        }
        else{
            openGallery(); //打开相册，进行选择
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //请求权限的结果的回调。
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                //grantResults返回PackageManager.PERMISSION_GRANTED请求成功PackageManager.PERMISSION_DENIED拒绝请求
                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    openGallery();
                }
                else{
                    Toast.makeText(this,"读相册的操作被拒绝",Toast.LENGTH_LONG).show();
                }
        }
    }

    //打开相册,进行照片的选择
    private void openGallery(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");//选择图片
        startActivityForResult(intent,2);
    }

    //选择后照片的读取工作
    private void handleSelect(Intent intent){
        Cursor cursor=null;
        Uri uri=intent.getData();//上面打开相册操作intent返回的是图片地址，所以用getData()取出
        //如果直接是从"相册"中选择，则Uri的形式是"content://xxxx"的形式
        //equalsIgnoreCase 方法是比较两个String对象是否相等(并且忽略大小写)
        //getScheme返回当前链接使用的协议
        if("content".equalsIgnoreCase(uri.getScheme())){
            //通过uri获取真实的图片路径
            cursor= getContentResolver().query(uri,null,null,null,null);
            if(cursor.moveToFirst()){
                //getColumnIndexOrThrow返回给定列名的从零开始的索引
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                uploadFileName = cursor.getString(columnIndex);

                try {
                    Glide.with(this).load(uri)
                            .fitCenter()
                            .into(photo);

//                    InputStream inputStream = getContentResolver().openInputStream(uri);
//                    fileBuf=convertToBytes(inputStream);
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(fileBuf, 0, fileBuf.length);
//                    photo.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        else{
            Log.i("other","其它数据类型.....");
        }
        cursor.close();
    }





    //拍照按钮
    public void photo(View view) throws Exception{
        //删除并创建临时文件，用于保存拍照后的照片
        //android 6以后，写Sdcard是危险权限，需要运行时申请，但此处使用的是"关联目录"，无需！
        File outImg=new File(getExternalCacheDir(),"temp.jpg");
        if(outImg.exists()) outImg.delete();
        outImg.createNewFile();

        //复杂的Uri创建方式
        if(Build.VERSION.SDK_INT>=24)
            //这是Android 7后，更加安全的获取文件uri的方式
            //（需要配合FileProvider,在Manifest.xml中加以配置）
            imgUri= FileProvider.getUriForFile(this,"cn.john.app1.fileprovider",outImg);
        else
            imgUri=Uri.fromFile(outImg);



        //利用actionName和Extra,启动《相机Activity》intent方式调用camera
        Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
        // 调用 intent.putExtra(String name, Parcelable value)这个方法，是传递输出要保存的图片的路径，
        //  拍照后图片保存在imguri中，设置MediaStore.EXTRA_OUTPUT的方法，在手机存储卡上也会保存一份照片
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imgUri);
        //启动活动
        startActivityForResult(intent,1);
        //在 Activity 完成后收到结果需要调用startActivityForResult
    }


    //在启动的活动退出时调用onActivityResult
    //把拍的照片显示在ImageView中
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        //requestCode:请求码，有多个activity返回结果时判断是哪一个activity
        switch (requestCode) {
            //1是拍照activity返回的，2是相册取照片activity返回的
            case 1:
                //此时，相机拍照完毕
                //resultCode返回码，判断activity返回的状态，OK，CANCELED，RESULT_FIRST_USER
                if (resultCode == RESULT_OK) {
                    try {
                        //利用ContentResolver,查询临时文件 ，通过getContentResolver()可以得到当前应用的ContentResolver实例
                        //BitmapFactory.decodeStream（）将输入流解码为位图。
                        //openInputStream（URI）在与内容URI关联的内容上打开流。
                        //imageview.setImageBitmap设置一个位图作为此ImageView的内容。
                        Bitmap map = BitmapFactory.decodeStream(getContentResolver().openInputStream(imgUri));
                        imgView.setImageBitmap(map);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 2:
                handleSelect(data);
                break;
                default:
                    break;
        }
    }
    private byte[] convertToBytes(InputStream inputStream) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        inputStream.close();
        return  out.toByteArray();
    }

}
