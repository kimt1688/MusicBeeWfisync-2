    -keep public class * extends android.app.Activity {
        public <init>();
    }
    -keep public class * extends androidx.core.app.ComponentActivity {
        public <init>();
    }
    // Or more generally for any class an Activity might extend from
    -keep public class * extends android.app.Application
    -keep public class * extends android.app.Service
    -keep public class * extends android.content.BroadcastReceiver
    -keep public class * extends android.content.ContentProvider
    -keep public class com.android.vending.licensing.ILicensingService
    -keepclasseswithmembernames class * {
        native <methods>;
    }
    -keepclasseswithmembers class * {
        public <init>(android.content.Context, android.util.AttributeSet);
    }
    -keepclasseswithmembers class * {
        public <init>(android.content.Context, android.util.AttributeSet, int);
    }
    -keepclassmembers class * extends android.app.Activity {
       public void *(android.view.View);
    }
    -keepclassmembers enum * {
        public static **[] values();
        public static ** valueOf(java.lang.String);
    }
    