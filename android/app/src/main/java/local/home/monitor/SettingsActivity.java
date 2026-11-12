package local.home.monitor;
import android.app.Activity;import android.os.Bundle;import android.widget.*;import android.text.InputType;import java.net.URI;
public class SettingsActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(48,32,48,32);
 TextView title=new TextView(this);title.setText("Подключение к серверу. HTTP допустим только в доверенной сети.");layout.addView(title);
 EditText url=new EditText(this);url.setHint("https://server.example.org/metrics");url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);url.setText(ClientConfig.url(this));layout.addView(url);
 EditText token=new EditText(this);token.setHint("Токен чтения метрик");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setText(ClientConfig.token(this));layout.addView(token);
 Button save=new Button(this);save.setText("Сохранить");layout.addView(save);save.setOnClickListener(v->{try{URI u=new URI(url.getText().toString().trim());if(!("http".equals(u.getScheme())||"https".equals(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null||token.getText().length()<24)throw new Exception();getSharedPreferences("connection",MODE_PRIVATE).edit().putString("url",u.toString()).putString("token",token.getText().toString().trim()).apply();finish();}catch(Exception e){Toast.makeText(this,"Укажите URL /metrics и токен минимум 24 символа",Toast.LENGTH_LONG).show();}});setContentView(layout);}
}
