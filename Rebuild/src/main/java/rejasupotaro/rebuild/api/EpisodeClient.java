package rejasupotaro.rebuild.api;

import android.util.Log;

import org.apache.http.Header;

import java.io.UnsupportedEncodingException;
import java.util.List;

import rejasupotaro.asyncrssclient.AsyncRssClient;
import rejasupotaro.asyncrssclient.AsyncRssResponseHandler;
import rejasupotaro.asyncrssclient.RssFeed;
import rejasupotaro.asyncrssclient.RssItem;
import rejasupotaro.rebuild.models.Episode;

public class EpisodeClient {

    private static final String TAG = EpisodeClient.class.getSimpleName();

    private static final AsyncRssClient sAsyncRssClient = new AsyncRssClient();

    public static interface EpisodeClientResponseHandler {
        public void onSuccess(List<Episode> episodeList);

        public void onFailure();
    }

    public void request(final EpisodeClientResponseHandler handler) {
        List<Episode> episodeList = Episode.find();
        if (episodeList != null && episodeList.size() > 0) {
            handler.onSuccess(episodeList);
            return;
        }

        sAsyncRssClient.read(
                "http://feeds.rebuild.fm/rebuildfm",
                new AsyncRssResponseHandler() {
                    @Override
                    public void onSuccess(RssFeed rssFeed) {
                        List<RssItem> rssItemList = rssFeed.getRssItemList();
                        List<Episode> episodeList = Episode.newEpisodeFromEntity(rssItemList);

                        for (Episode episode : episodeList) {
                            episode.upsert();
                        }

                        handler.onSuccess(episodeList);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] body, Throwable throwable) {
                        for (Header header : headers) {
                            Log.e(TAG, header.getName() + " => " + header.getValue());
                        }
                        try {
                            Log.e(TAG, new String(body, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        Log.e(TAG, throwable.toString());

                        handler.onFailure();
                    }
                }
        );
    }
}