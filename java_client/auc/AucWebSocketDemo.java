package com.example.auc;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AucWebSocketDemo {

    private static final String SUBMIT_URL = "https://openspeech-direct.zijieapi.com/api/v3/auc/bigmodel/submit";
    private static final String QUERY_URL = "https://openspeech-direct.zijieapi.com/api/v3/auc/bigmodel/query";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)    // 连接超时
    .readTimeout(60, TimeUnit.SECONDS)      // 读取超时
    .writeTimeout(60, TimeUnit.SECONDS)     // 写入超时
    .callTimeout(60, TimeUnit.SECONDS)      // 整个调用超时(包括重定向)
    .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        // 从命令行参数中获取对应的值
        final String APPID = args[0];
        final String TOKEN = args[1];
        final String FILE_URL = args[2];
        
        String[] result = submitTask(FILE_URL, APPID, TOKEN);
        String taskId = result[0];
        String xTtLogid = result[1];

        while (true) {
            Response queryResponse = queryTask(taskId, xTtLogid, APPID, TOKEN);
            String code = queryResponse.header("X-Api-Status-Code", "");
            if ("20000000".equals(code)) {
                System.out.println(queryResponse.body().string());
                System.out.println("SUCCESS!");
                System.exit(0);
            } else if (!"20000001".equals(code) && !"20000002".equals(code)) {
                System.out.println("FAILED!");
                System.exit(1);
            }
            Thread.sleep(1000);
        }
    }

    private static String[] submitTask(String FILE_URL, String APPID, String TOKEN) throws IOException {
        String taskId = UUID.randomUUID().toString();
        System.out.println("Submit task id: " + taskId);
        // 创建 user 部分
        Map<String, String> user = new HashMap<>();
        user.put("uid", "fake_uid");

        // 创建 audio 部分
        Map<String, Object> audio = new HashMap<>();
        audio.put("url", FILE_URL);

        // 创建 corpus 部分
        Map<String, String> corpus = new HashMap<>();
        corpus.put("correct_table_name", "");
        corpus.put("context", "");

        // 创建 request 部分
        Map<String, Object> innerRequest = new HashMap<>();
        innerRequest.put("model_name", "bigmodel");
        innerRequest.put("enable_channel_split", true);
        innerRequest.put("enable_ddc", true);
        innerRequest.put("enable_speaker_info", true);
        innerRequest.put("enable_punc", true);
        innerRequest.put("enable_itn", true);
        innerRequest.put("corpus", corpus);

        // 创建主 request 对象
        Map<String, Object> mainRequest = new HashMap<>();
        mainRequest.put("user", user);
        mainRequest.put("audio", audio);
        mainRequest.put("request", innerRequest);

        // 使用 Gson 转换为 JSON 字符串
        Gson gson = new Gson();
        String jsonString = gson.toJson(mainRequest);
        System.out.println("Submit mainRequest: " + jsonString + "\n");   
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
        .url(SUBMIT_URL)
        .header("X-Api-App-Key", APPID)  // 注意这里的APPID应该是字符串变量
        .header("X-Api-Access-Key", TOKEN)  // 注意这里的TOKEN应该是字符串变量
        .header("X-Api-Resource-Id", "volc.bigasr.auc")
        .header("X-Api-Request-Id", taskId)  // 注意这里的taskId应该是字符串变量
        .header("X-Api-Sequence", "-1")
        .header("Content-Type", "application/json")  // 注意这里的taskId应该是字符串变量
        .post(body)  // 这里的body应该是RequestBody对象
        .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            System.out.println("Response Body: " + responseBody); // 打印响应体内容
            if ("20000000".equals(response.header("X-Api-Status-Code"))) {
                System.out.println("Submit task response header X-Api-Status-Code: " + response.header("X-Api-Status-Code"));
                System.out.println("Submit task response header X-Api-Message: " + response.header("X-Api-Message"));
                String xTtLogid = response.header("X-Tt-Logid", "");
                System.out.println("Submit task response header X-Tt-Logid: " + xTtLogid + "\n");
                return new String[]{taskId, xTtLogid};
            } else {
                System.out.println("Submit task failed and the response headers are: " + response.headers());
                System.exit(1);
            }
        }
        return new String[]{taskId};
    }

    private static Response queryTask(String taskId, String xTtLogid, String APPID, String TOKEN) throws IOException {
        RequestBody body = RequestBody.create("{}", JSON);
        
        Request request = new Request.Builder()
        .url(QUERY_URL)
        .header("X-Api-App-Key", APPID)  // 注意这里的APPID应该是字符串变量
        .header("X-Api-Access-Key", TOKEN)  // 注意这里的TOKEN应该是字符串变量
        .header("X-Api-Resource-Id", "volc.bigasr.auc")
        .header("X-Api-Request-Id", taskId)
        .header("X-Tt-Logid", xTtLogid)
        .post(body)  // 这里的body应该是RequestBody对象
        .build();

        Response response = client.newCall(request).execute();
        if (response.header("X-Api-Status-Code") != null) {
            System.out.println("Query task response header X-Api-Status-Code: " + response.header("X-Api-Status-Code"));
            System.out.println("Query task response header X-Api-Message: " + response.header("X-Api-Message"));
            System.out.println("Query task response header X-Tt-Logid: " + response.header("X-Tt-Logid") + "\n");
        } else {
            System.out.println("Query task failed and the response headers are: " + response.headers());
            System.exit(1);
        }
        return response;
    }
    
}
