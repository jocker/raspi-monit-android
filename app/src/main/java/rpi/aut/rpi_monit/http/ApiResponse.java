package rpi.aut.rpi_monit.http;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import rpi.aut.rpi_monit.lib.Func1;

public class ApiResponse<T> {

    public final Request request;
    public final Response response;
    public final ApiError error;
    public final T body;

    public static <T> ApiResponse<T> success(Request request, Response response, T body){
        return new ApiResponse<>(request, response, null, body);
    }

    public static <T> ApiResponse<T> failure(ApiResponse<?> other){
        return failure(other.request, other.response, other.error);
    }

    public static <T> ApiResponse<T> failure(Request request, Response response, Throwable th){
        ApiError error;
        if(th instanceof IOException){
            error = new ApiError(ApiError.KIND_NETWORK, response, th, HttpClient.readResponseBody(response));
        }else if(th instanceof ApiError){
            error = (ApiError)th;
        }else{
            error = new ApiError(ApiError.KIND_UNKNOWN, response, th, HttpClient.readResponseBody(response));
        }
        if(response == null){
            response = error.response;
        }
        return new ApiResponse<>(request, response, error, null);
    }

    protected ApiResponse(Request request, Response response, ApiError error, T body){
        this.request = request;
        this.response = response;
        this.error = error;
        this.body = body;
    }

    public int getResponseCode(){
        if(this.response != null){
            return this.response.code();
        }
        return -1;
    }

    public boolean isSuccessful(){
        return this.error == null && (this.response != null && this.response.isSuccessful() || this.body != null);
    }

    public <Q> ApiResponse<Q> map(Func1<T, Q> transformer){
        if(!isSuccessful()){
            return ApiResponse.failure(request, response, error);
        }
        Q body = transformer.call(this.body);
        return ApiResponse.success(request, response, body);
    }

}