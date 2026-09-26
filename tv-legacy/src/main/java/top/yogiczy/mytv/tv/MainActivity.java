package top.yogiczy.mytv.tv;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.SurfaceView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * API 18 fallback shell. It deliberately uses framework Views instead of Compose.
 */
public final class MainActivity extends Activity {
    private static final String DEFAULT_SOURCE = "http://192.168.2.1:88/iptv.m3u8";
    private static final String SOURCE_KEY = "source";

    private final List<Channel> channels = new ArrayList<Channel>();
    private ArrayAdapter<String> channelAdapter;
    private EditText sourceInput;
    private TextView status;
    private ListView channelList;
    private SurfaceView surfaceView;
    private IjkMediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadSource();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(18, 14, 18, 14);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        sourceInput = new EditText(this);
        sourceInput.setSingleLine(true);
        sourceInput.setTextColor(Color.WHITE);
        sourceInput.setHintTextColor(Color.GRAY);
        sourceInput.setHint("直播源地址");
        sourceInput.setText(getPreferences(0).getString(SOURCE_KEY, DEFAULT_SOURCE));
        toolbar.addView(sourceInput, new LinearLayout.LayoutParams(0, 52, 1f));

        Button loadButton = new Button(this);
        loadButton.setText("加载");
        loadButton.setFocusable(true);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadSource();
            }
        });
        toolbar.addView(loadButton, new LinearLayout.LayoutParams(110, 52));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 62));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);

        surfaceView = new SurfaceView(this);
        surfaceView.setFocusable(false);
        content.addView(surfaceView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.75f));

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(14, 0, 0, 0);
        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setText("请选择频道");
        status.setGravity(Gravity.CENTER_VERTICAL);
        side.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 52));

        channelList = new ListView(this);
        channelList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        channelList.setItemsCanFocus(false);
        channelAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_activated_1,
                new ArrayList<String>());
        channelList.setAdapter(channelAdapter);
        channelList.setOnItemClickListener((parent, view, position, id) -> playChannel(position));
        side.addView(channelList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content.addView(side, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ProgressBar progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(1, 1));
        setContentView(root);
        channelList.requestFocus();
    }

    private void loadSource() {
        final String source = sourceInput.getText().toString().trim();
        if (source.length() == 0) {
            status.setText("请输入直播源地址");
            return;
        }
        getPreferences(0).edit().putString(SOURCE_KEY, source).apply();
        status.setText("正在加载直播源...");
        new SourceTask().execute(source);
    }

    private final class SourceTask extends AsyncTask<String, Void, List<Channel>> {
        private String error;

        @Override
        protected List<Channel> doInBackground(String... sources) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(sources[0]).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "okhttp");
                InputStream input = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line).append('\n');
                }
                reader.close();
                return parse(body.toString());
            } catch (Exception ex) {
                error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                return new ArrayList<Channel>();
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(List<Channel> result) {
            channels.clear();
            channels.addAll(result);
            channelAdapter.clear();
            for (Channel channel : channels) {
                channelAdapter.add(channel.name);
            }
            channelAdapter.notifyDataSetChanged();
            if (channels.isEmpty()) {
                status.setText("直播源为空或加载失败: " + (error == null ? "无频道" : error));
            } else {
                status.setText("已加载 " + channels.size() + " 个频道");
                channelList.requestFocus();
                channelList.setSelection(0);
            }
        }
    }

    private List<Channel> parse(String body) {
        ArrayList<Channel> result = new ArrayList<Channel>();
        String pendingName = null;
        String pendingAgent = null;
        String[] lines = body.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.length() == 0) continue;
            if (line.startsWith("#EXTINF")) {
                int comma = line.lastIndexOf(',');
                pendingName = comma >= 0 ? line.substring(comma + 1).trim() : "未命名频道";
                pendingAgent = attribute(line, "http-user-agent");
            } else if (!line.startsWith("#") && pendingName != null) {
                result.add(new Channel(pendingName, line, pendingAgent));
                pendingName = null;
                pendingAgent = null;
            }
        }
        return result;
    }

    private String attribute(String line, String name) {
        String key = name + "=\"";
        int start = line.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = line.indexOf('"', start);
        return end > start ? line.substring(start, end) : null;
    }

    private void playChannel(int position) {
        if (position < 0 || position >= channels.size()) return;
        Channel channel = channels.get(position);
        try {
            if (player == null) {
                IjkMediaPlayer.loadLibrariesOnce(null);
                player = new IjkMediaPlayer();
                player.setDisplay(surfaceView.getHolder());
                player.setOnPreparedListener(mp -> mp.start());
                player.setOnErrorListener((mp, what, extra) -> {
                    status.setText("播放失败: " + what + "/" + extra);
                    return false;
                });
            } else {
                player.stop();
                player.reset();
            }
            if (channel.userAgent != null) {
                java.util.HashMap<String, String> headers = new java.util.HashMap<String, String>();
                headers.put("User-Agent", channel.userAgent);
                player.setDataSource(this, android.net.Uri.parse(channel.url), headers);
            } else {
                player.setDataSource(channel.url);
            }
            status.setText("正在播放: " + channel.name);
            player.prepareAsync();
        } catch (Throwable ex) {
            status.setText("播放器不可用: " + ex.getClass().getSimpleName());
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private static final class Channel {
        final String name;
        final String url;
        final String userAgent;

        Channel(String name, String url, String userAgent) {
            this.name = name;
            this.url = url;
            this.userAgent = userAgent;
        }
    }
}
