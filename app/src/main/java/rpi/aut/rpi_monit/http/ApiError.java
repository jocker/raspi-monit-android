package rpi.aut.rpi_monit.http;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.UnknownHostException;

import okhttp3.Response;

public class ApiError extends Throwable{
    public static final int
            KIND_NETWORK = 1,
            KIND_HTTP = 2,
            KIND_CONVERSION = 3,
            KIND_UNKNOWN = 4,
            KIND_AUTHORIZATION = 5,
            KIND_BAD_REQUEST = 6,
            KIND_UNREACHABLE_HOST = 7,
            KIND_RESPONSE_BODY = 8,
            KIND_UNSUPPORTED_APP_VERSION = 9;

    public static String peekErrorMessage(String rawBodyJson){
        try{
            JsonObject obj = new JsonParser().parse(rawBodyJson).getAsJsonObject();
            if(obj.has("errors")){
                return obj.get("errors").getAsJsonObject().get("message").getAsString();
            }else if(obj.has("error")){
                return obj.get("error").getAsString();
            }else if(obj.has("message")){
                return obj.get("message").getAsString();
            }
        }catch (Exception e){
            return null;
        }

        return null;
    }

    private static boolean isNullOrEmpty(String value){
        return value == null || "".equals(value.trim());
    }

    public static ApiError networkError(IOException e){
        final int kind;

        if(e instanceof UnknownHostException){
            kind = ApiError.KIND_UNREACHABLE_HOST;
        }else{
            kind = ApiError.KIND_NETWORK;
        }
        return new ApiError(kind, null, e, null);
    }

    public final int kind;
    public final Response response;
    private final String mRawResponseBody;
    public final Throwable originalError;
    private volatile String mMessage;

    public ApiError(int kind, Response response, Throwable originalError, String errorBody){
        this.kind = kind;
        this.response = response;
        this.originalError = originalError;
        mRawResponseBody = errorBody;
    }

    @Override
    public String getMessage(){
        if(mMessage != null){
            return mMessage;
        }
        if(!isNullOrEmpty(mRawResponseBody)){
            String message = peekErrorMessage(mRawResponseBody);
            if(!isNullOrEmpty(message)){
                return mMessage = message;
            }
        }

        switch (this.kind){
            case ApiError.KIND_NETWORK:
                return "Network Error";
            case ApiError.KIND_HTTP:
                return "Http error";
            case ApiError.KIND_CONVERSION:
                return "Conversion error";
        }
        return super.getMessage();
    }

    public String getResponseBody(){
        return mRawResponseBody;
    }



}