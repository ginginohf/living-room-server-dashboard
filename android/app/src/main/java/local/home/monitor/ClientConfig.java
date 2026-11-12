package local.home.monitor;
import android.content.Context;
public final class ClientConfig {
 private ClientConfig(){}
 public static String url(Context c){return c.getSharedPreferences("connection",Context.MODE_PRIVATE).getString("url","");}
 public static String token(Context c){return c.getSharedPreferences("connection",Context.MODE_PRIVATE).getString("token","");}
 public static boolean ready(Context c){return !url(c).isEmpty()&&!token(c).isEmpty();}
}
