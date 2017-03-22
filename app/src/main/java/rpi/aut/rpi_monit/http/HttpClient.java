package rpi.aut.rpi_monit.http;


import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSink;
import rpi.aut.rpi_monit.AppConfig;
import rpi.aut.rpi_monit.lib.BitMask;
import rpi.aut.rpi_monit.lib.Utils;

public class HttpClient {

    // We want to be able to add some specifications for this request(for example if it needs or not to be authenticated)
    // okhhtp doesn't allow setting custom variables on the request, nor to extend the request class
    //  so we're going to fake this behavior by using a dummy header which will contain the information we want
    private static final String FLAGS_HEADER = UUID.randomUUID().toString();

    public static final int FLAG_SKIP_AUTHORIZATION = 1<<0;
    private static final AtomicInteger FLAG_GENERATOR = new AtomicInteger(1);

    public static int nextRequestFlag(){
        return 1 << FLAG_GENERATOR.getAndIncrement();
    }

    public static int getFlags(Request request){
        return getFlags(request, 0);
    }

    public static int getFlags(Request request, int defaultValue){
        if(request == null){
            return defaultValue;
        }
        String flagsHeader = request.header(FLAGS_HEADER);
        if(TextUtils.isEmpty(flagsHeader)){
            return defaultValue;
        }
        try{
            return Integer.parseInt(flagsHeader);
        }catch (NumberFormatException e){
            return defaultValue;
        }
    }

    public static boolean hasFlag(Request request, int flag){
        return BitMask.contains(getFlags(request, 0), flag);
    }

    public static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    public static abstract class JsonRequestBody extends RequestBody {

        @Override
        public MediaType contentType() {
            return JSON_TYPE;
        }

    }

    private static final RequestBody EMPTY_JSON_REQUEST_BODY = new JsonRequestBody() {
        @Override
        public void writeTo(BufferedSink sink) throws IOException {

        }
    };

    public static InputStreamReader getInputStreamReader(Response response){

        try{
            return CloseableStreamReader.create(response);
        }catch (IOException e){
            return null;
        }
    }

    public static String readResponseBody(Response response){
        if(response == null){
            return null;
        }
        InputStreamReader is = getInputStreamReader(response);
        if(is == null){
            return null;
        }
        StringBuilder mBuff = new StringBuilder();
        try{
            BufferedReader r = new BufferedReader(is);
            String line;
            while ((line = r.readLine()) != null) {
                mBuff.append(line);
            }
            is.close();

        }catch (IOException e){

        }
        return mBuff.toString();
    }

    private static volatile HttpClient sInstance;
    private static Scheduler sHttpScheduler;

    public static final int FLAG_SKIP_APP_AUTHORIZATION = HttpClient.nextRequestFlag();

    public static RequestBuilder newRequest(){
        if(sHttpScheduler == null){
            sHttpScheduler = Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return new RequestBuilder(getInstance(), AppConfig.getRemoteUri()).setScheduler(sHttpScheduler);
    }

    public static RequestBuilder newRequest(Uri baseUri){
        if(sHttpScheduler == null){
            sHttpScheduler = Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return new RequestBuilder(getInstance(), baseUri).setScheduler(sHttpScheduler);
    }

    public static RequestBuilder get(Object... path){
        return newRequest(HttpConstants.Method.GET, makePath(path));
    }

    public static RequestBuilder post(Object... path){
        return newRequest(HttpConstants.Method.POST, makePath(path));
    }

    public static RequestBuilder get(Uri baseUri, Object... path){
        return newRequest(HttpConstants.Method.GET, baseUri, makePath(path));
    }

    public static RequestBuilder post(Uri baseUri, Object... path){
        return newRequest(HttpConstants.Method.POST, baseUri, makePath(path));
    }

    private static String makePath(Object[] pathChunks){
        if(pathChunks.length > 0){
            if(pathChunks.length == 1){
                return pathChunks[0] == null ? null : String.valueOf(pathChunks[0]);
            }else{
                List<String> values = new ArrayList<>();
                for(Object chunk: pathChunks){
                    if(chunk != null){
                        values.add(String.valueOf(chunk));
                    }
                }
                return TextUtils.join("/", values).replaceAll("//","/");
            }
        }
        return null;
    }

    private static RequestBuilder newRequest(HttpConstants.Method method, String path){
        return newRequest().method(method).appendPath(path);
    }

    private static RequestBuilder newRequest(HttpConstants.Method method, Uri baseUri, String path){
        return newRequest(baseUri).method(method).appendPath(path);
    }

    public static HttpClient getInstance(){
        if(sInstance == null){
            synchronized (HttpClient.class){
                if(sInstance != null){
                    return sInstance;
                }
                Builder builder = new Builder()
                        .setClientFactory(okClient -> {

                            // handling authentication
                            okClient.addInterceptor(chain -> {
                                Request request =  chain.request();

                                return chain.proceed(request);
                            });

                            // logging needs to be the last one
                            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                            okClient.addInterceptor(logging);

                        });

                Context ctx = Utils.getGlobalContext();
                String packageName = ctx.getPackageName();
                String appName = "ConsumerApp";
                String test = Utils.isAppDebuggable() ? " TEST " : " RELEASE ";
                String appVersion = "";
                try{
                    appVersion = ctx.getPackageManager().getPackageInfo(packageName, 0).versionName;
                } catch (PackageManager.NameNotFoundException nnfe) {

                }

                String userAgent = appName + "/" + appVersion + test + "(Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MANUFACTURER + " " + Build.MODEL + ")";
                final Map<String, String> defaultHeaders = new HashMap<>();
                defaultHeaders.put(HttpConstants.USER_AGENT_HEADER, userAgent);
                defaultHeaders.put(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.JSON_CONTENT);
                defaultHeaders.put(HttpConstants.ACCEPT_HEADER, HttpConstants.JSON_CONTENT);


                builder.setDefaulteaders(defaultHeaders).setJsonDecoder(Utils.getDefaultJsonConverter());

                sInstance = new HttpClient(builder);
            }

        }
        return sInstance;
    }




    private final Map<String, String> mDefaultHeaders;
    private final OkHttpClient mOkClient;
    private final Gson mJsonDecoder;

    public boolean needsAuthorization(Request request){
        return !hasFlag(request, FLAG_SKIP_AUTHORIZATION);
    }

    protected HttpClient(Builder config){
        mDefaultHeaders = config.mDefaultHeaders == null ? Collections.emptyMap() : config.mDefaultHeaders;
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        config.mClientFactory.configure(clientBuilder);
        if(config.mSkipSslCheck){
            try{
                X509TrustManager trustManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                };

                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new X509TrustManager[]{trustManager}, new java.security.SecureRandom());
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                clientBuilder.sslSocketFactory(sslSocketFactory, trustManager);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }

        mOkClient = clientBuilder.build();
        mJsonDecoder = config.mGson;
    }

    public static class Builder{
        private Map<String, String> mDefaultHeaders;
        private OkClientFactory mClientFactory;
        private boolean mSkipSslCheck;
        private Gson mGson;

        public Builder setDefaulteaders(Map<String, String> headers){
            mDefaultHeaders = Collections.unmodifiableMap(headers);
            return this;
        }

        public Builder setClientFactory(OkClientFactory factory){
            mClientFactory = factory;
            return this;
        }

        public Builder setBypassSslCheck(boolean value){
            mSkipSslCheck = value;
            return this;
        }

        public Builder setJsonDecoder(Gson gson){
            mGson = gson;
            return this;
        }

        public HttpClient build(){
            return new HttpClient(this);
        }
    }

    public Call newCall(Request request){
        return mOkClient.newCall(request);
    }

    private Observable<Response> enqueue(final Request request, Scheduler scheduler){
        return enqueue(request, 0, scheduler);
    }

    private Observable<Response> enqueue(final Request request, int retryCount, Scheduler scheduler){

        if(scheduler == null){
            scheduler = Schedulers.io();
        }

        final Scheduler asyncScheduler = scheduler;


        final AtomicInteger retryCounter = new AtomicInteger(retryCount);
        final Call requestCall = newCall(request);

        return Flowable.<Response>create(emitter -> {

            final AtomicBoolean isDone = new AtomicBoolean(false);

            requestCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if(isDone.compareAndSet(false, true) && !emitter.isCancelled()){
                        emitter.onError(ApiError.networkError(e));
                        emitter.onComplete();
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(isDone.compareAndSet(false, true) && !emitter.isCancelled()){
                        if(!response.isSuccessful()){
                            String body = readResponseBody(response);
                            final int errorKind;
                            if(response.code() == 401){
                                errorKind = ApiError.KIND_AUTHORIZATION;
                            }else if(response.code() == 422){
                                errorKind = ApiError.KIND_BAD_REQUEST;
                            }else if(response.code() == 403){

                                mJsonDecoder.fromJson(body, JsonElement.class);
                                int kind = ApiError.KIND_BAD_REQUEST;

                                errorKind = kind;
                            }
                            else{
                                errorKind = ApiError.KIND_HTTP;
                            }
                            emitter.onError(new ApiError(errorKind, response, null, body));

                        }else{
                            emitter.onNext(response);
                        }
                        emitter.onComplete();
                    }
                }
            });

        }, BackpressureStrategy.MISSING)
                .toObservable()
                .subscribeOn(asyncScheduler)
                .onErrorResumeNext(throwable -> {
                    if(!requestCall.isCanceled() && (throwable instanceof ApiError)){
                        if(HttpConstants.Method.GET.httpName.equalsIgnoreCase(request.method())){
                            ApiError httpError = (ApiError)throwable;
                            if(httpError.kind == ApiError.KIND_NETWORK){
                                if(retryCounter.incrementAndGet() < 4){
                                    return enqueue(request, retryCounter.get(), asyncScheduler);
                                }
                            }
                        }
                    }

                    return Observable.error(throwable);
                });

    }

    public ExecutorService getCancellationExecutor(){
        return mOkClient.dispatcher().executorService();
    }

    protected void cancelCall(final Call call){
        if(!call.isCanceled()){
            getCancellationExecutor().execute(() -> {
                if(!call.isCanceled()){
                    call.cancel();
                }
            });
        }
    }



    public interface OkClientFactory{
        void configure(OkHttpClient.Builder clientBuilder);
    }

    public static class RequestBuilder{

        private Uri.Builder mUriBuilder;
        private final Request.Builder mOkRequestBuilder;
        private HttpConstants.Method mHttpMethod = HttpConstants.Method.GET;
        private RequestBody mRequestBody;
        private final Map<String, String> mHeaders = new HashMap<>();
        private final HttpClient mClient;
        private int mFlags = 0;
        private Scheduler mScheduler;

        public RequestBuilder(HttpClient client, Uri baseUri){
            mUriBuilder = baseUri.buildUpon();
            mOkRequestBuilder = new Request.Builder();
            mOkRequestBuilder.cacheControl(CacheControl.FORCE_NETWORK);
            mClient = client;
        }

        public RequestBuilder appendPath(String... path){
            if(path.length == 1){
                mUriBuilder.appendEncodedPath(path[0]);
            }else if(path.length > 1){
                mUriBuilder.appendEncodedPath(TextUtils.join("/", path));
            }

            return this;
        }

        public RequestBuilder setUri(Uri uri){
            mUriBuilder = uri.buildUpon();
            return this;
        }

        public RequestBuilder appendQueryParameter(String name, String value){
            mUriBuilder.appendQueryParameter(name, value);
            return this;
        }

        public RequestBuilder appendHeader(String name, String value){
            mHeaders.put(name, value);
            return this;
        }

        public RequestBuilder method(HttpConstants.Method method){
            mHttpMethod = method;
            return this;
        }

        public RequestBuilder skipAuthorization(){
            return addFlag(FLAG_SKIP_AUTHORIZATION);
        }

        public RequestBuilder setScheduler(Scheduler scheduler){
            mScheduler = scheduler;
            return this;
        }

        public RequestBuilder cacheControl(CacheControl cacheControl){
            mOkRequestBuilder.cacheControl(cacheControl);
            return this;
        }

        public RequestBuilder addFlag(int flag){
            mFlags = BitMask.addFlags(mFlags, flag);
            return this;
        }

        public RequestBuilder removeFlag(int flag){
            mFlags = BitMask.removeFlags(mFlags, flag);
            return this;
        }

        public RequestBuilder forceNetwork(){
            return this.cacheControl(CacheControl.FORCE_NETWORK);
        }

        public RequestBuilder setBody(RequestBody body){
            mRequestBody = body;
            return this;
        }

        public RequestBuilder setJsonBody(Object body){
            appendHeader("Transfer-Encoding","chunked");
            return setBody(new JsonRequestBody() {
                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    Writer writer = new OutputStreamWriter(sink.outputStream(), HttpConstants.UTF8);
                    writer.write(mClient.mJsonDecoder.toJson(body));
                    writer.flush();
                    writer.close();

                }
            });
        }

        private Request buildRequest(){
            mHeaders.putAll(mClient.mDefaultHeaders);

            for(Map.Entry<String, String> entry: mHeaders.entrySet()){
                mOkRequestBuilder.header(entry.getKey(), entry.getValue());
            }
            if(mFlags > 0){
                mOkRequestBuilder.header(FLAGS_HEADER, String.valueOf(mFlags));
            }

            RequestBody body = mHttpMethod.hasBody ? mRequestBody == null ? EMPTY_JSON_REQUEST_BODY : mRequestBody : null;

            return mOkRequestBuilder.method(mHttpMethod.httpName, body)
                    .url(mUriBuilder.build().toString()).build();
        }

        public Observable<ApiResponse<InputStreamReader>> buildAsStreamObservable(){
            final Request request = buildRequest();

            return mClient.enqueue(request, mScheduler).map(response -> {
                return ApiResponse.success(request, response, getInputStreamReader(response));
            }).onErrorReturn(throwable -> {
                Log.e("HTTPCLIENT", throwable.getMessage(), throwable);
                return ApiResponse.failure(request, null, throwable);
            });
        }

        public <T> Observable<ApiResponse<T>> buildAsObservable(JsonStreamReader<T> streamReader){
            return buildAsStreamObservable()
                    .map(response -> {
                        if(response.isSuccessful()){
                            InputStreamReader is = response.body;
                            JsonReader reader = new JsonReader(is);
                            try{
                                T result = streamReader.read(reader);
                                is.close();
                                return ApiResponse.success(response.request, response.response, result);
                            }catch (IOException e){
                                ApiResponse.failure(response.request, response.response, e);
                            }
                        }
                        return null;
                    });
        }

        public <T> Observable<ApiResponse<T>> buildAsObservable(final Type typeOfT){
            return buildAsStreamObservable().map(response -> {
                if(!response.isSuccessful()){
                    return ApiResponse.failure(response.request, response.response, response.error);
                }
                JsonElement jsonElement = mClient.mJsonDecoder.fromJson(response.body, JsonElement.class);
                try{
                    // There are some scenarios when the api responds with an error message body, but with 200 http status code
                    // This means that we cannot make the decoding directly from the input stream because we will not be able to rewind the input stream
                    //      thus we'll not be able to catch the error message
                    T instance = mClient.mJsonDecoder.fromJson(jsonElement, typeOfT);
                    return ApiResponse.success(response.request, response.response, instance);
                }catch (JsonParseException e){
                    Log.e("HttpRequest", e.getMessage(), e);
                    ApiError err = new ApiError(ApiError.KIND_RESPONSE_BODY, response.response, null, jsonElement == null ? null : jsonElement.toString());
                    return ApiResponse.failure(response.request, response.response, err);
                }

            });
        }

    }

    public interface JsonStreamReader<T>{
        T read(JsonReader reader) throws IOException;
    }


    private static class CloseableStreamReader extends InputStreamReader{

        public static InputStreamReader create(Response response) throws IOException{
            String contentEncoding = response.header(HttpConstants.CONTENT_ENCODING),
                    contentType = response.header(HttpConstants.CONTENT_TYPE_HEADER);
            final InputStream is = response.body().byteStream();

            if(HttpConstants.GZIP.equalsIgnoreCase(contentEncoding) || HttpConstants.GZIP_CONTENT.equalsIgnoreCase(contentType)){
                return new CloseableStreamReader(new GZIPInputStream(is), response);
            }else if(HttpConstants.DEFLATE.equalsIgnoreCase(contentEncoding)){
                return new CloseableStreamReader(new DeflaterInputStream(is), response);
            }

            return new CloseableStreamReader(is, response);
        }

        private final Response mOriginalResponse;

        CloseableStreamReader(InputStream is, Response originalResponse) {
            super(is);
            mOriginalResponse = originalResponse;
        }

        @Override
        public void close() throws IOException {
            super.close();
            mOriginalResponse.body().close();
        }
    }

}
