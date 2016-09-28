package kh.android.fullsleep;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Project FullSleep
 * <p>
 * Created by 宇腾 on 2016/9/26.
 * Edited by 宇腾
 */
/*
感谢以下项目：
GravityBox
 */
public class MainHook implements IXposedHookLoadPackage{
    public static final String TAG = "FullSleep-";
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.internal.policy.impl.GlobalActions";
    public static final String CLASS_ACTION = "com.android.internal.policy.impl.GlobalActions.Action";

    private Context mContext;
    public static Resources mAppRes;

    private static Drawable mSleepIcon;
    private static String mSleepString;
    private static String mStatusString;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals(PACKAGE_NAME)) {
            try {
                final Class<?> globalActionsClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS, loadPackageParam.classLoader);
                final Class<?> actionClass = XposedHelpers.findClass(CLASS_ACTION, loadPackageParam.classLoader);

                XposedBridge.hookAllConstructors(globalActionsClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mContext = (Context) param.args[0];
                        Context appContext = mContext.createPackageContext(
                                getClass().getPackage().getName(), Context.CONTEXT_IGNORE_SECURITY);
                        mAppRes = appContext.getResources();

                        mSleepIcon = mAppRes.getDrawable(R.drawable.ic_sleep);
                        mSleepString = mAppRes.getString(R.string.action_sleep);
                        mStatusString = mAppRes.getString(R.string.text_state);
                    }
                });
                XposedHelpers.findAndHookMethod(globalActionsClass, "createDialog", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        @SuppressWarnings("unchecked")
                        List<Object> mItems = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mItems.add(Proxy.newProxyInstance(loadPackageParam.classLoader, new Class<?>[] { actionClass },
                                new SleepAction()));
                        mAdapter.notifyDataSetChanged();
                    }
                });
            } catch (Throwable e) {
                XposedBridge.log(e);
            }

        }
    }
    private static class SleepAction  implements InvocationHandler {
        private Context mContext;
        public static final String GREENIFY_PKG = "com.oasisfeng.greenify";
        public static final String GREENIFY_SLEEP_CLOSE_SCREEN = GREENIFY_PKG + ".HibernateAndLockScreen";
        public static final String GREENIFY_MAIN_ACTIVITY = GREENIFY_PKG + ".GreenifyActivity";
        public SleepAction() {
        }
        private void start (Context context, String pkg, String activity) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(pkg,activity));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                XposedBridge.log(TAG + e.getMessage());
            }
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mSleepIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mSleepString);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setText(mStatusString);

                return v;
            } else if (methodName.equals("onPress")) {
                start(mContext, GREENIFY_PKG, GREENIFY_SLEEP_CLOSE_SCREEN);
                return null;
            } else if (methodName.equals("onLongPress")) {
                start(mContext, GREENIFY_PKG, GREENIFY_MAIN_ACTIVITY);
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else {
                return null;
            }
        }
    }
}
