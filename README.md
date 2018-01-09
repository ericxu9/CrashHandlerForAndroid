![UncaughtExceptionHandler](http://upload-images.jianshu.io/upload_images/1715317-db29a1034acda7bd.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

###### 简书文章地址：[https://www.jianshu.com/u/3b7248821149](https://www.jianshu.com/u/3b7248821149)

###### 关于`UncaughtExceptionHandler`，它是Java Thread类中定义的一个接口；用于处理未捕获的异常导致线程的终止（`注意：catch了的是捕获不到的`），当我们的应用crash的时候，就会走`UncaughtExceptionHandler`的`uncaughtException`方法，在该方法中可以获取到异常的信息，我们通过`setDefaultUncaughtExceptionHandler`该方法来设置线程的默认异常处理器，将异常信息保存到本地或者是上传到服务器，方便我们快速的定位问题；下面是一个简单`CrashHandler`的代码：

``` java
public class CrashHandler implements Thread.UncaughtExceptionHandler
{

    private static final String       PATH             = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String       FILE_NAME_SUFFIX = ".trace";
    private static       CrashHandler sInstance        = new CrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;
    private Context                         mContext;

    private CrashHandler()
    {
    }

    public static CrashHandler getInstance()
    {
        return sInstance;
    }

    public void init(@NonNull Context context)
    {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context.getApplicationContext();
    }

    /**
     * 当程序中有未被捕获的异常，系统将会调用这个方法
     *
     * @param t 出现未捕获异常的线程
     * @param e 得到异常信息
     */
    @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        try
        {
            //保存到本地
            exportExceptionToSDCard(e);
            //下面也可以写上传的服务器的代码
        } catch (Exception e1)
        {
            e1.printStackTrace();
        }
        e.printStackTrace();
        //如果系统提供了默认的异常处理器，则交给系统去结束程序，否则就自己结束自己
        if (mDefaultCrashHandler != null)
        {
            mDefaultCrashHandler.uncaughtException(t, e);
        } else
        {
            Process.killProcess(Process.myPid());
        }
    }

    /**
     * 导出异常信息到SD卡
     *
     * @param e
     */
    private void exportExceptionToSDCard(@NonNull Throwable e)
    {
        //判断SD卡是否存在
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            return;
        }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        File file = new File(PATH + File.separator + time + FILE_NAME_SUFFIX);

        try
        {
            //往文件中写入数据
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println(time);
            pw.println(appendPhoneInfo());
            e.printStackTrace(pw);
            pw.close();
        } catch (IOException e1)
        {
            e1.printStackTrace();
        } catch (PackageManager.NameNotFoundException e1)
        {
            e1.printStackTrace();
        }
    }

    /**
     * 获取手机信息
     */
    private String appendPhoneInfo() throws PackageManager.NameNotFoundException
    {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        StringBuilder sb = new StringBuilder();
        //App版本
        sb.append("App Version: ");
        sb.append(pi.versionName);
        sb.append("_");
        sb.append(pi.versionCode + "\n");

        //Android版本号
        sb.append("OS Version: ");
        sb.append(Build.VERSION.RELEASE);
        sb.append("_");
        sb.append(Build.VERSION.SDK_INT + "\n");

        //手机制造商
        sb.append("Vendor: ");
        sb.append(Build.MANUFACTURER + "\n");

        //手机型号
        sb.append("Model: ");
        sb.append(Build.MODEL + "\n");

        //CPU架构
        sb.append("CPU: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            sb.append(Arrays.toString(Build.SUPPORTED_ABIS));
        } else
        {
            sb.append(Build.CPU_ABI);
        }
        return sb.toString();
    }
}
```

###### 上面代码是当应用发生crash时，判断SD是否存在，然后拿到设备信息和异常信息保存到SD卡根路径.trace文件中；最后将异常交给系统处理，如果没有设置默认的异常处理，就自行终止应用。
###### Tips：涉及到SD卡，记得在Manifest中添加权限

``` xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

---- 

###### 在Application中初始化CrashHandler

``` java
public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        CrashHandler.getInstance().init(this);
    }
}
```

###### 下面我们手动抛出异常，然后查看SD卡下的异常信息

``` java
public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        //Android M 权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    private void initView()
    {
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        if (v == mButton)
        {
            throw new RuntimeException("自定义异常");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == 0)
        {
            for (int grantResult : grantResults)
            {
                if (grantResult == PackageManager.PERMISSION_DENIED)
                {
                    Toast.makeText(this, "write_external_storage denied！", Toast.LENGTH_SHORT).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

```
![根目录](http://upload-images.jianshu.io/upload_images/1715317-2c61d740871bdf73.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

###### 下面就是保存的设备信息和异常信息

![异常信息](http://upload-images.jianshu.io/upload_images/1715317-39682be7633ae610.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)