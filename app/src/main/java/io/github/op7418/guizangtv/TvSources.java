package io.github.op7418.guizangtv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/**
 * Brand-neutral input-source switching built on the standard Android TV framework
 * (TvInputManager passthrough inputs). It is NOT verified on any Hisense model yet:
 * whether the TV's own live-TV app accepts the passthrough URI depends on the firmware.
 */
final class TvSources {
    private TvSources() { }

    /** All passthrough inputs (HDMI, AV, ...) the system reports, in a stable order. */
    static List<TvInputInfo> inputs(Context context) {
        List<TvInputInfo> result = new ArrayList<>();
        try {
            TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
            if (manager == null) return result;
            for (TvInputInfo info : manager.getTvInputList()) {
                if (info.isPassthroughInput()) result.add(info);
            }
        } catch (RuntimeException unavailable) {
            Log.w("TvHome.Sources", "TV inputs unavailable", unavailable);
            return new ArrayList<>();
        }
        result.sort((a, b) -> {
            String x = a.getId(), y = b.getId();
            return x.length() != y.length() ? Integer.compare(x.length(), y.length()) : x.compareTo(y);
        });
        return result;
    }

    static List<TvInputInfo> hdmiInputs(Context context) {
        List<TvInputInfo> hdmi = new ArrayList<>();
        for (TvInputInfo info : inputs(context)) {
            if (info.getType() == TvInputInfo.TYPE_HDMI) hdmi.add(info);
        }
        return hdmi;
    }

    static String label(Context context, TvInputInfo info) {
        CharSequence custom = info.loadCustomLabel(context);
        if (custom != null && custom.length() > 0) return custom.toString();
        CharSequence label = info.loadLabel(context);
        return label != null && label.length() > 0 ? label.toString() : info.getId();
    }

    static boolean open(Context context, TvInputInfo info) {
        try {
            Uri uri = TvContract.buildChannelUriForPassthroughInput(info.getId());
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException unavailable) {
            Log.w("TvHome.Sources", "Cannot open input " + info.getId(), unavailable);
            return false;
        }
    }

    /** "hdmi:N" home entries: the Nth HDMI input the system reports. */
    static boolean openHdmi(Context context, int port) {
        List<TvInputInfo> hdmi = hdmiInputs(context);
        return port >= 1 && port <= hdmi.size() && open(context, hdmi.get(port - 1));
    }

    /** A plain list of every input the system reports. */
    static void showPicker(Activity activity) {
        List<TvInputInfo> list = inputs(activity);
        if (list.isEmpty()) {
            new AlertDialog.Builder(activity).setTitle("信号源")
                    .setMessage("系统没有返回可切换的信号源。请用遥控器上的“信号源”键切换。")
                    .setNegativeButton("返回", null).show();
            return;
        }
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = label(activity, list.get(i));
        new AlertDialog.Builder(activity).setTitle("信号源")
                .setItems(labels, (d, which) -> {
                    if (!open(activity, list.get(which)))
                        Toast.makeText(activity, "这台电视不接受此方式切换，请用遥控器的“信号源”键", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("返回", null).show();
    }
}
