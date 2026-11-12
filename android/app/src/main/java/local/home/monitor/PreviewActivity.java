package local.home.monitor;
import android.app.Activity;import android.os.Bundle;import android.content.Intent;import android.view.KeyEvent;
public class PreviewActivity extends Activity {
 private Dashboard view;
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);view=new Dashboard(this);setContentView(view);if(!ClientConfig.ready(this))startActivity(new Intent(this,SettingsActivity.class));}
 @Override public void onResume(){super.onResume();if(ClientConfig.ready(this))view.start();}
 @Override public void onPause(){view.stop();super.onPause();}
 @Override public boolean onKeyUp(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_MENU){startActivity(new Intent(this,SettingsActivity.class));return true;}return super.onKeyUp(key,event);}
}
