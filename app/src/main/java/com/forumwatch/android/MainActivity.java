package com.forumwatch.android;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.database.sqlite.*;
import android.database.Cursor;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private android.content.SharedPreferences prefs;
    private DB db;
    private LinearLayout root, form, logBox;
    private EditText forum, username, password, members, tgToken, tgChat, waToken, waPhoneId, waRecipient;
    private TextView status, logs;
    private Button scanButton;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("fw", MODE_PRIVATE); db = new DB(this);
        getWindow().setStatusBarColor(Color.rgb(17,24,39)); getWindow().setNavigationBarColor(Color.rgb(17,24,39));
        buildUi(); load();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9);
    }
    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density); }
    private TextView text(String s,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(0,dp(5),0,dp(5)); return t; }
    private EditText field(String hint, boolean secret){ EditText e=new EditText(this); e.setSingleLine(true); e.setTextSize(14); e.setHint(hint); e.setPadding(dp(12),dp(9),dp(12),dp(9)); e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(203,213,225))); if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); return e; }
    private void section(String title){ TextView t=text(title,16,Color.rgb(30,41,59),true); t.setPadding(0,dp(16),0,dp(6)); form.addView(t); }
    private void add(EditText e){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); form.addView(e,p); }
    private Button button(String label){ Button b=new Button(this); b.setText(label); b.setAllCaps(false); return b; }
    private void buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(18),dp(18),dp(18),dp(24)); root.setBackgroundColor(Color.rgb(245,247,251)); scroll.addView(root); setContentView(scroll);
        LinearLayout header=new LinearLayout(this); header.setOrientation(1); header.setPadding(dp(18),dp(18),dp(18),dp(18)); header.setBackgroundColor(Color.rgb(17,24,39));
        header.addView(text("FORUMWATCH",22,Color.WHITE,true)); header.addView(text("Forum monitoring · Android",13,Color.rgb(203,213,225),false)); root.addView(header);
        status=text("●  Ready · Configure your forum below",13,Color.rgb(22,101,52),true); status.setPadding(dp(4),dp(14),0,dp(8)); root.addView(status);
        form=new LinearLayout(this); form.setOrientation(1); root.addView(form);
        section("FORUM CONNECTION"); forum=field("Forum base URL (https://example.com/)",false); add(forum); username=field("Forum username",false); add(username); password=field("Forum password",true); add(password);
        section("TRACKED MEMBERS"); members=new EditText(this); members.setHint("One member ID per line, e.g. 150535"); members.setMinLines(3); members.setGravity(Gravity.TOP); members.setTextSize(14); members.setPadding(dp(12),dp(10),dp(12),dp(10)); members.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.LTGRAY)); form.addView(members);
        section("TELEGRAM NOTIFICATIONS"); tgToken=field("Telegram bot token",true); add(tgToken); tgChat=field("Telegram chat ID",false); add(tgChat);
        section("WHATSAPP CLOUD API (OPTIONAL)"); waToken=field("WhatsApp access token",true); add(waToken); waPhoneId=field("WhatsApp phone number ID",false); add(waPhoneId); waRecipient=field("Recipient number with country code",false); add(waRecipient);
        LinearLayout actions=new LinearLayout(this); actions.setOrientation(1); actions.setPadding(0,dp(12),0,0); root.addView(actions);
        Button save=button("Save settings"); actions.addView(save); save.setOnClickListener(v->{save(); note("Settings saved on this device.");});
        scanButton=button("Start scan now"); actions.addView(scanButton); scanButton.setOnClickListener(v->{save(); runScan();});
        Button test=button("Send Telegram test"); actions.addView(test); test.setOnClickListener(v->{save(); io.execute(()->{try{sendTelegram("ForumWatch Android test notification"); ui(()->note("Telegram test sent."));}catch(Exception ex){ui(()->note("Telegram error: "+ex.getMessage()));}});});
        Button login=button("Test forum login"); actions.addView(login); login.setOnClickListener(v->{save(); io.execute(()->{try{String result=loginForum(); ui(()->note(result));}catch(Exception ex){ui(()->note("Login test failed: "+ex.getMessage()));}});});
        section("ACTIVITY LOG"); logs=text("No scans yet.",12,Color.rgb(71,85,105),false); logs.setBackgroundColor(Color.WHITE); logs.setPadding(dp(12),dp(12),dp(12),dp(12)); root.addView(logs);
        root.addView(text("Note: ForumWatch is a reconstructed Android client. vBulletin installations differ; login/session and page parsing may require adjustment for your forum version.",11,Color.GRAY,false));
    }
    private void load(){ forum.setText(prefs.getString("forum","")); username.setText(prefs.getString("username","")); password.setText(prefs.getString("password","")); members.setText(prefs.getString("members","")); tgToken.setText(prefs.getString("tgToken","")); tgChat.setText(prefs.getString("tgChat","")); waToken.setText(prefs.getString("waToken","")); waPhoneId.setText(prefs.getString("waPhoneId","")); waRecipient.setText(prefs.getString("waRecipient","")); }
    private void save(){ prefs.edit().putString("forum",forum.getText().toString().trim()).putString("username",username.getText().toString()).putString("password",password.getText().toString()).putString("members",members.getText().toString()).putString("tgToken",tgToken.getText().toString().trim()).putString("tgChat",tgChat.getText().toString().trim()).putString("waToken",waToken.getText().toString().trim()).putString("waPhoneId",waPhoneId.getText().toString().trim()).putString("waRecipient",waRecipient.getText().toString().trim()).apply(); }
    private void ui(Runnable r){ main.post(r); }
    private void note(String s){ status.setText("●  "+s); append(s); }
    private void append(String s){ String old=logs.getText().toString(); String stamp=new java.text.SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()); logs.setText(stamp+"  "+s+"\n"+old); }
    private String base(){ String b=prefs.getString("forum","").trim(); while(b.endsWith("/"))b=b.substring(0,b.length()-1); if(!b.startsWith("https://"))throw new IllegalArgumentException("Use the forum's HTTPS base URL."); return b; }
    private HttpURLConnection connect(String url,String method) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestProperty("User-Agent","ForumWatchAndroid/1.0"); c.setInstanceFollowRedirects(true); return c; }
    private String read(HttpURLConnection c) throws Exception { InputStream in=(c.getResponseCode()<400)?c.getInputStream():c.getErrorStream(); if(in==null)return ""; ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))!=-1)out.write(buf,0,n); return out.toString("UTF-8"); }
    private String loginForum() throws Exception {
        String b=base(), u=prefs.getString("username",""), p=prefs.getString("password",""); if(u.isEmpty()||p.isEmpty())return "Enter forum username and password first.";
        HttpURLConnection c=connect(b+"/login.php?do=login","POST"); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/x-www-form-urlencoded"); String body="vb_login_username="+enc(u)+"&vb_login_password="+enc(p)+"&cookieuser=1&do=login&securitytoken=guest";
        c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8)); String html=read(c); String cookie=c.getHeaderField("Set-Cookie"); if(cookie!=null)prefs.edit().putString("cookie",cookie.split(";",2)[0]).apply();
        if(html.toLowerCase(Locale.ROOT).contains("invalid password")||html.toLowerCase(Locale.ROOT).contains("incorrect password"))return "Forum rejected the supplied credentials.";
        return "Login request completed (verify forum-specific authentication in scan results).";
    }
    private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private void runScan(){ scanButton.setEnabled(false); note("Scanning members…"); io.execute(()->{int checked=0,online=0; try{String b=base(); String cookie=prefs.getString("cookie",""); String[] ids=prefs.getString("members","").split("[\\r\\n,; ]+"); for(String id:ids){ if(id.trim().isEmpty())continue; String html=get(b+"/member.php?u="+enc(id.trim()),cookie); String state=presence(html); String old=db.getState(id.trim()); db.put(id.trim(),state); checked++; if("Online".equals(state)){online++; if(!"Online".equals(old))notifyChange("Member "+id.trim()+" is now online");} else if("Offline".equals(state)&&"Online".equals(old)){notifyChange("Member "+id.trim()+" is now offline");} }
                final int count=checked, on=online; ui(()->{note("Scan complete · "+count+" members checked · "+on+" online");scanButton.setEnabled(true);});
            }catch(Exception ex){ui(()->{note("Scan failed: "+ex.getMessage());scanButton.setEnabled(true);});}}); }
    private String get(String url,String cookie)throws Exception{HttpURLConnection c=connect(url,"GET"); if(cookie!=null&&!cookie.isEmpty())c.setRequestProperty("Cookie",cookie); return read(c);}
    private String presence(String html){String h=html.toLowerCase(Locale.ROOT); if(h.contains("currently active")||h.contains("is online")||h.contains("online now")||h.matches("(?s).*<span[^>]*class=[\"'][^\"']*online[^\"']*[\"'].*"))return "Online"; if(h.contains("last activity")||h.contains("last seen")||h.contains("offline"))return "Offline"; return "Unknown";}
    private void notifyChange(String msg){try{if(!prefs.getString("tgToken","").isEmpty())sendTelegram(msg); if(!prefs.getString("waToken","").isEmpty())sendWhatsApp(msg);}catch(Exception e){ui(()->append("Notification error: "+e.getMessage()));}}
    private void sendTelegram(String message)throws Exception{String token=prefs.getString("tgToken","");String chat=prefs.getString("tgChat",""); if(token.isEmpty()||chat.isEmpty())throw new Exception("Configure Telegram bot token and chat ID."); HttpURLConnection c=connect("https://api.telegram.org/bot"+token+"/sendMessage","POST"); c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");String data="chat_id="+enc(chat)+"&text="+enc(message);c.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));String r=read(c);if(c.getResponseCode()>=400)throw new Exception(r);}
    private void sendWhatsApp(String message)throws Exception{String token=prefs.getString("waToken","");String phone=prefs.getString("waPhoneId","");String to=prefs.getString("waRecipient","");if(token.isEmpty()||phone.isEmpty()||to.isEmpty())throw new Exception("Complete WhatsApp Cloud API settings.");HttpURLConnection c=connect("https://graph.facebook.com/v20.0/"+phone+"/messages","POST");c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Content-Type","application/json");JSONObject j=new JSONObject();j.put("messaging_product","whatsapp");j.put("to",to);j.put("type","text");j.put("text",new JSONObject().put("body",message));c.getOutputStream().write(j.toString().getBytes(StandardCharsets.UTF_8));String r=read(c);if(c.getResponseCode()>=400)throw new Exception(r);}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
    private class DB extends SQLiteOpenHelper { DB(Context c){super(c,"forumwatch.db",null,1);} public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE member_state(member_id TEXT PRIMARY KEY,state TEXT,updated INTEGER)");}public void onUpgrade(SQLiteDatabase d,int a,int b){} String getState(String id){Cursor c=getReadableDatabase().rawQuery("SELECT state FROM member_state WHERE member_id=?",new String[]{id});String s=null;if(c.moveToFirst())s=c.getString(0);c.close();return s;}void put(String id,String state){android.content.ContentValues v=new android.content.ContentValues();v.put("member_id",id);v.put("state",state);v.put("updated",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("member_state",null,v,SQLiteDatabase.CONFLICT_REPLACE);} }
}
