package local.home.monitor;
import android.service.dreams.DreamService;
public class ServerDream extends DreamService {
 private Dashboard view;
 @Override public void onAttachedToWindow(){super.onAttachedToWindow();setInteractive(false);setFullscreen(true);setScreenBright(false);view=new Dashboard(this);setContentView(view);}
 @Override public void onDreamingStarted(){super.onDreamingStarted();view.start();}
 @Override public void onDreamingStopped(){if(view!=null)view.stop();super.onDreamingStopped();}
 @Override public void onDetachedFromWindow(){if(view!=null)view.stop();super.onDetachedFromWindow();}
}
