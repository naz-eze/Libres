package app.libres.android.rest;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class RetrofitClient {

    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final String CACHE_DIR = "httpCache";
    private final static String BASE_URL = "http://libres.eu-west-2.elasticbeanstalk.com:8080/";
    private final static String BASE_LOCAL_URL = "http://:8080/";
    private final static String BASE_EMULATOR_LOCAL_URL = "http://10.0.2.2:8080/";

    private static Retrofit instance;

    public static Retrofit getInstance(Context context) {
        if (instance == null) {
            return new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getHttpClient(context))
                    .build();
        } else return instance;
    }

    private static OkHttpClient getHttpClient(Context context) {
        File httpCacheDirectory = new File(context.getCacheDir(), CACHE_DIR);
        Cache cache = new Cache(httpCacheDirectory, TEN_MB);

        return new OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(3, SECONDS)
                .addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        try {
                            return chain.proceed(chain.request());
                        } catch (Exception e) {
                            Request offlineRequest = chain.request().newBuilder()
                                    .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 72)
                                    .build();
                            return chain.proceed(offlineRequest);
                        }
                    }
                }).build();
    }
}
