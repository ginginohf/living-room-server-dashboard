package local.home.monitor;
import android.content.Context;import android.graphics.*;import android.os.Handler;import android.view.View;
import org.json.*;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.text.SimpleDateFormat;import java.util.*;import java.util.concurrent.*;

public class Dashboard extends View {
 private final Shader glow=new RadialGradient(210,60,1100,new int[]{Color.rgb(19,45,52),Color.rgb(8,15,24)},null,Shader.TileMode.CLAMP);
 private final SimpleDateFormat clockFormat=new SimpleDateFormat("HH:mm",Locale.ROOT), dateFormat=new SimpleDateFormat("EEEE, d MMMM",new Locale("ru")), secondsFormat=new SimpleDateFormat("HH:mm:ss",Locale.ROOT);
 private final Date now=new Date();
 private final Paint p=new Paint(3);private final Handler ui=new Handler();private ScheduledExecutorService worker;
 private JSONObject data=new JSONObject();private long lastSuccess=0;private volatile boolean running=false;private int generation=0;
 private static final int WHITE=Color.rgb(226,234,242),MUTED=Color.rgb(130,148,167),TEAL=Color.rgb(98,218,190),BLUE=Color.rgb(115,168,243),PURPLE=Color.rgb(181,156,239),AMBER=Color.rgb(235,188,117);
 private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL),light=Typeface.create("sans-serif-light",Typeface.NORMAL);
 public Dashboard(Context c){super(c);setLayerType(View.LAYER_TYPE_HARDWARE,null);}
 public void start(){if(running)return;running=true;final int gen=++generation;worker=Executors.newSingleThreadScheduledExecutor();worker.scheduleWithFixedDelay(()->fetch(gen),0,5,TimeUnit.SECONDS);ui.post(tick);}
 public void stop(){running=false;generation++;if(worker!=null)worker.shutdownNow();ui.removeCallbacks(tick);}
 private final Runnable tick=new Runnable(){public void run(){if(running){invalidate();ui.postDelayed(this,1000);}}};
 private void fetch(int gen){HttpURLConnection c=null;try{
  c=(HttpURLConnection)new URL(ClientConfig.url(getContext())).openConnection();c.setConnectTimeout(3500);c.setReadTimeout(3500);c.setRequestProperty("Authorization","Bearer "+ClientConfig.token(getContext()));
  if(c.getResponseCode()!=200)throw new IOException();ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0){b.write(buf,0,n);if(b.size()>250000)throw new IOException();}}
  final JSONObject value=new JSONObject(b.toString("UTF-8"));if(!value.has("timestamp"))throw new IOException();ui.post(()->{if(running&&generation==gen){data=value;lastSuccess=System.currentTimeMillis();invalidate();}});
 }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}}
 private void text(Canvas c,String s,float x,float y,float size,int color){p.setShader(null);p.setColor(color);p.setTypeface(size>=48?light:regular);p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
 private void right(Canvas c,String s,float x,float y,float size,int color){p.setTypeface(size>=48?light:regular);p.setTextSize(size);text(c,s,x-p.measureText(s),y,size,color);}
 private void rect(Canvas c,float x,float y,float w,float h,int color,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,radius,radius,p);}
 private void card(Canvas c,float x,float y,float w,float h){rect(c,x,y,w,h,Color.rgb(18,28,40),24);p.setColor(Color.rgb(36,48,62));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(x,y,x+w,y+h,24,24,p);p.setStyle(Paint.Style.FILL);}
 private String num(double x,int digits){return String.format(Locale.US,"%."+digits+"f",x).replace('.',',');}
 private String bytes(double n){return n>=1073741824?num(n/1073741824,1)+" ГиБ":num(n/1048576,1)+" МиБ";}
 private String rate(double n){return n>=1048576?num(n/1048576,1)+" МиБ/с":num(n/1024,0)+" КиБ/с";}
 private void bar(Canvas c,float x,float y,float w,float pct,int color){rect(c,x,y,w,8,Color.rgb(39,51,65),4);if(pct>0)rect(c,x,y,w*Math.min(1,pct/100),8,color,4);}
 private void graph(Canvas c,String key,float x,float y,float w,float h,int color,double max){JSONArray a=data.optJSONArray("history");if(a==null||a.length()<2)return;Path path=new Path();int n=a.length();for(int i=0;i<n;i++){float px=x+w*i/(n-1),py=y+h-(float)(Math.min(max,a.optJSONObject(i).optDouble(key,0))/max)*h;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setStrokeJoin(Paint.Join.ROUND);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(8,15,24));float scale=Math.min(getWidth()/1920f,getHeight()/1080f);c.save();c.translate((getWidth()-1920*scale)/2,(getHeight()-1080*scale)/2);c.scale(scale,scale);
  double phase=System.currentTimeMillis()/180000.0;c.translate((float)Math.sin(phase)*7,(float)Math.cos(phase)*5);
  p.setShader(glow);c.drawRect(0,0,1920,1080,p);p.setShader(null);
  boolean available=lastSuccess>0,stale=System.currentTimeMillis()-lastSuccess>20000;
  text(c,"Д О М А Ш Н И Й   С Е Р В Е Р",72,82,22,MUTED);text(c,"Всё под наблюдением",72,147,49,WHITE);
  rect(c,73,176,8,8,available&&!stale?TEAL:AMBER,4);
  String status=!available?"Подключение к серверу…":stale?"Нет связи · последние данные "+secondsFormat.format(lastSuccess):"Метрики сервера · обновление каждые 5 с";
  text(c,status,94,188,22,available&&!stale?MUTED:AMBER);
  now.setTime(System.currentTimeMillis());right(c,clockFormat.format(now),1848,129,80,WHITE);right(c,dateFormat.format(now),1848,180,24,MUTED);
  card(c,72,232,550,298);card(c,646,232,550,298);card(c,1220,232,628,298);
  text(c,"ПРОЦЕССОР",100,277,21,MUTED);text(c,available?num(data.optDouble("cpu"),0)+" %":"—",100,361,70,TEAL);
  right(c,available&&!data.isNull("temperature")?num(data.optDouble("temperature"),0)+" °C":"— °C",594,341,43,WHITE);
  text(c,available?data.optInt("cores")+" ядра  ·  load "+num(data.optDouble("load"),2):"Нагрузка CPU",100,399,20,MUTED);
  graph(c,"cpu",100,426,494,65,TEAL,100);text(c,"ИСТОРИЯ · ДО 10 МИН",100,511,15,MUTED);
  text(c,"ОПЕРАТИВНАЯ ПАМЯТЬ",674,277,21,MUTED);double total=data.optDouble("memory_total",1),used=data.optDouble("memory_used",0);
  text(c,available?num(100*used/total,0)+" %":"—",674,361,70,PURPLE);text(c,available?bytes(used)+" из "+bytes(total):"Ожидание данных",674,408,25,WHITE);bar(c,674,442,494,(float)(100*used/total),PURPLE);text(c,available?"Доступно "+bytes(total-used):"",674,489,22,MUTED);
  text(c,"СЕТЬ",1248,277,21,MUTED);right(c,data.optString("interface",""),1820,277,19,MUTED);
  text(c,"↓ ПРИЁМ",1248,322,18,TEAL);text(c,"↑ ОТДАЧА",1550,322,18,BLUE);text(c,available?rate(data.optDouble("rx")):"—",1248,368,39,WHITE);text(c,available?rate(data.optDouble("tx")):"—",1550,368,39,WHITE);
  double max=1024;JSONArray history=data.optJSONArray("history");if(history!=null)for(int i=0;i<history.length();i++){JSONObject o=history.optJSONObject(i);max=Math.max(max,Math.max(o.optDouble("rx"),o.optDouble("tx")));}
  graph(c,"rx",1248,405,572,81,TEAL,max*1.15);graph(c,"tx",1248,405,572,81,BLUE,max*1.15);text(c,"СКОРОСТЬ · ЛОКАЛЬНЫЙ ИНТЕРФЕЙС",1248,511,15,MUTED);
  JSONArray disks=data.optJSONArray("disks");String[] names={"Система · SSD","Медиатека · HDD","FileRise · HDD"};int[] colors={TEAL,BLUE,PURPLE};
  for(int i=0;i<3;i++){float x=72+i*600;card(c,x,554,576,268);JSONObject d=disks!=null?disks.optJSONObject(i):null;text(c,names[i],x+28,602,27,WHITE);
   if(d==null||!d.optBoolean("mounted")){text(c,d==null?"Ожидание данных":"Диск не подключён",x+28,677,30,AMBER);continue;}
   double pct=100*d.optDouble("used")/d.optDouble("total",1);text(c,num(pct,0)+" %",x+28,668,45,pct>90?AMBER:colors[i]);right(c,"свободно "+bytes(d.optDouble("free")),x+548,662,22,MUTED);bar(c,x+28,693,520,(float)pct,pct>90?AMBER:colors[i]);
   text(c,"Чтение  "+rate(d.optDouble("read")),x+28,743,21,WHITE);text(c,"Запись  "+rate(d.optDouble("write")),x+28,783,21,MUTED);right(c,"I/O  "+num(d.optDouble("busy"),0)+" %",x+548,783,21,MUTED);
  }
  card(c,72,846,1080,148);card(c,1176,846,672,148);JSONObject rates=data.optJSONObject("rates");boolean has=rates!=null&&rates.has("USD");
  text(c,"КУРС ЦБ РФ",100,885,17,MUTED);String date=has?rates.optString("date"):"ожидаем обновление";boolean oldRate=has&&(System.currentTimeMillis()/1000.0-rates.optDouble("fetched")>86400);right(c,date+(oldRate?" · нет свежих данных":""),1124,885,17,oldRate?AMBER:MUTED);
  text(c,"USD",100,951,25,MUTED);text(c,has?num(rates.optDouble("USD"),2)+" ₽":"—",177,953,43,WHITE);text(c,"EUR",635,951,25,MUTED);text(c,has?num(rates.optDouble("EUR"),2)+" ₽":"—",714,953,43,WHITE);
  text(c,"БЕЗ ПЕРЕЗАГРУЗКИ",1204,885,17,MUTED);long up=(long)data.optDouble("uptime");text(c,available?(up/86400)+" д  "+(up/3600%24)+" ч  "+(up/60%60)+" мин":"—",1204,953,43,WHITE);
  text(c,"SERVER / AMBIENT",72,1036,16,MUTED);right(c,stale&&available?"Показатели сохранены · соединение восстанавливается":"Любая кнопка пульта — вернуться",1848,1036,18,MUTED);c.restore();
 }
}
