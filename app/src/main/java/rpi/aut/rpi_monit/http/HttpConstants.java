package rpi.aut.rpi_monit.http;

public interface HttpConstants {
    String
            ACCEPT_HEADER = "Accept",
            ACCEPT_ENCODING_HEADER = "Accept-Encoding",
            CONTENT_TYPE_HEADER = "Content-Type",
            USER_AGENT_HEADER = "User-Agent",
            CONTENT_ENCODING = "Content-Encoding",

    JSON_CONTENT = "application/json",
            GZIP_CONTENT = "application/x-gzip";

    String UTF8 = "UTF-8";

    String ETAG = "ETag";

    String
            GZIP = "gzip",
            DEFLATE = "deflate";

    String
            BASIC_AUTH_HEADER = "Authorization",
            TOKEN_AUTH_HEADER = "AuthorizationToken";


    enum Method{
        GET("GET", false),
        POST("POST", true),
        PUT("PUT", true),
        DELETE("DELETE", false);


        public final String httpName;
        public final boolean hasBody;

        Method(String name, boolean hasBody){
            this.httpName = name;
            this.hasBody = hasBody;
        }
    }

}

