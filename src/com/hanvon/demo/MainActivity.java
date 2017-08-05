package com.hanvon.demo;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Data;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hanvon.HWCloudManager;
import com.hanvon.utils.BitmapUtil;

public class MainActivity extends Activity {

	private Button button1;
	private Button button2;
	private Button button3;
	private Button button4;
	private ImageView iv_image;
	private LinearLayout ll_data;
	private TextView xm, zw, gs, dz, sj, dh, email;
	private ProgressDialog pd;
	private DiscernHandler discernHandler;
	private Gson gson;
	private Card card = new Card();

	String picPath = null;
	String result = null;
	private HWCloudManager hwCloudManagerBcard; // 名片

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); // remove title bar
		setContentView(R.layout.activity_main);

		/**
		 * your_android_key 是您在开发者中心申请的android_key 并 申请了云名片识别服务
		 * 开发者中心：http://developer.hanvon.com/
		 */
		hwCloudManagerBcard = new HWCloudManager(this,
				"26f86972-30ed-427c-bc62-a8acc0164467");

		discernHandler = new DiscernHandler();

		button1 = (Button) findViewById(R.id.button1);
		button2 = (Button) findViewById(R.id.button2);
		button3 = (Button) findViewById(R.id.button3);
		button4 = (Button) findViewById(R.id.button4);
		iv_image = (ImageView) findViewById(R.id.iv_image);
		ll_data = (LinearLayout) findViewById(R.id.ll_card);
		button1.setOnClickListener(listener);
		button2.setOnClickListener(listener);
		button3.setOnClickListener(listener);
		button4.setOnClickListener(listener);

		picPath = getPhotopath();
		gson = new Gson();

		xm = (TextView) findViewById(R.id.tv_xm);
		zw = (TextView) findViewById(R.id.tv_zw);
		gs = (TextView) findViewById(R.id.tv_gs);
		dz = (TextView) findViewById(R.id.tv_dz);
		sj = (TextView) findViewById(R.id.tv_sj);
		dh = (TextView) findViewById(R.id.tv_dh);
		email = (TextView) findViewById(R.id.tv_email);
	}

	OnClickListener listener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			switch (view.getId()) {
			case R.id.button1:
				setVis(true);
				// 调用系统相机
				Intent intentPhote = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intentPhote.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
				File out = new File(picPath);
				Uri uri = Uri.fromFile(out);
				// 获取拍照后未压缩的原图片，并保存在uri路径中
				intentPhote.putExtra(MediaStore.EXTRA_OUTPUT, uri);
				startActivityForResult(intentPhote, 1);
				break;

			case R.id.button2:
				setVis(true);
				// 识别
				pd = ProgressDialog
						.show(MainActivity.this, "", "正在识别请稍后......");
				DiscernThread discernThread = new DiscernThread();
				new Thread(discernThread).start();
				break;
			case R.id.button3:
				// 激活系统图库，选择一张图片
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(intent, 0);
				break;
			case R.id.button4:
				//导入联系人
				if(card.getName().size()>0&&card.getMobile().size()>0){
					try {
						AddContact(card.getName().get(0),card.getMobile().get(0));
						Toast.makeText(getApplicationContext(), "导入成功", Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), "导入失败", Toast.LENGTH_LONG).show();
					}
				}else{
					Toast.makeText(getApplicationContext(), "信息不全，无法导入联系人", Toast.LENGTH_LONG).show();
				}
				break;
			}
		}
	};

	public class DiscernThread implements Runnable {

		@Override
		public void run() {
			try {
				/**
				 * 调用汉王云名片识别方法
				 */
				result = hwCloudManagerBcard.cardLanguage("chns", picPath);
				// result = hwCloudManagerBcard.cardLanguage4Https("chns",
				// picPath);
			} catch (Exception e) {
				// TODO: handle exception
			}
			Bundle mBundle = new Bundle();
			mBundle.putString("responce", result);
			Message msg = new Message();
			msg.setData(mBundle);
			discernHandler.sendMessage(msg);
		}
	}

	public class DiscernHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			pd.dismiss();
			Bundle bundle = msg.getData();
			String responce = bundle.getString("responce");
			card = gson.fromJson(responce, Card.class);
			if (card.getCode().equals("0")) {
				setVis(false);
				setData();
			} else {
				Toast.makeText(getApplicationContext(), card.getResult(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private String getPhotopath() {
		// 照片全路径
		String fileName;
		// 文件夹路径
		String pathUrl = Environment.getExternalStorageDirectory() + "/mymy/";
		String imageName = "imageOne.png";
		File file = new File(pathUrl);
		file.mkdirs();// 创建文件夹
		fileName = pathUrl + imageName;
		return fileName;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 0:
			if (data != null) {
				Uri uri = data.getData();
				// 通过uri获取图片路径
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor cursor = getContentResolver().query(uri, proj, null,
						null, null);
				int column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				picPath = cursor.getString(column_index);
				System.out.println(picPath);

				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(picPath, options);
				options.inSampleSize = BitmapUtil.calculateInSampleSize(
						options, 1280, 720);
				options.inJustDecodeBounds = false;
				Bitmap bitmap = BitmapFactory.decodeFile(picPath, options);
				iv_image.setImageBitmap(bitmap);
			}
			break;
		case 1:
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(picPath, options);
			options.inSampleSize = BitmapUtil.calculateInSampleSize(options,
					1280, 720);
			options.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeFile(picPath, options);
			iv_image.setImageBitmap(bitmap);
			break;
		default:
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setData() {
		if (card.getName().size() > 0) {
			xm.setText(card.getName().get(0));
		}
		if (card.getTitle().size() > 0) {
			zw.setText(card.getTitle().get(0));
		}
		if (card.getComp().size() > 0) {
			gs.setText(card.getComp().get(0));
		}
		if (card.getAddr().size() > 0) {
			dz.setText(card.getAddr().get(0));
		}
		if (card.getMobile().size() > 0) {
			sj.setText(card.getMobile().get(0));
		}
		if (card.getTel().size() > 0) {
			dh.setText(card.getTel().get(0));
		}
		if (card.getEmail().size() > 0) {
			email.setText(card.getEmail().get(0));
		}
	}

	private void setVis(Boolean bl) {
		if (bl) {
			iv_image.setVisibility(View.VISIBLE);
			ll_data.setVisibility(View.GONE);
		} else {
			iv_image.setVisibility(View.GONE);
			ll_data.setVisibility(View.VISIBLE);
		}
	}
	
	public void AddContact(String name,String phone) throws Exception{
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        ContentResolver resolver = getApplicationContext().getContentResolver();
        ContentValues values = new ContentValues();
        long contactid = ContentUris.parseId(resolver.insert(uri, values));
         
        uri = Uri.parse("content://com.android.contacts/data");
         
        //添加姓名
        values.put("raw_contact_id", contactid);
        values.put(Data.MIMETYPE, "vnd.android.cursor.item/name");
        values.put("data1", name);
        resolver.insert(uri, values);
        values.clear();
         
        //添加电话
        values.put("raw_contact_id", contactid);
        values.put(Data.MIMETYPE, "vnd.android.cursor.item/phone_v2");
        values.put("data1", phone);
        resolver.insert(uri, values);
        values.clear();
         
        //添加Email
//        values.put("raw_contact_id", contactid);
//        values.put(Data.MIMETYPE, "vnd.android.cursor.item/email_v2");
//        values.put("data1", email);
//        resolver.insert(uri, values);
    }
}
