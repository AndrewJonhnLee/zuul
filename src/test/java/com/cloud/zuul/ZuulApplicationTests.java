package com.cloud.zuul;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ZuulApplicationTests {

	private static Logger log = LoggerFactory.getLogger(ZuulApplicationTests.class);

	@Test
	public void contextLoads() {

		String token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJ4eCIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSIsInRydXN0Il0sImV4cCI6MTUxNzQwNDg3OSwidXNlcklkIjoiMTAwMDciLCJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXSwianRpIjoiNGM0NGFlN2MtOTYzNi00NzkyLTkwOTUtMmJjNDM5Yjc5YTFmIiwiY2xpZW50X2lkIjoienVsbC1jbGllbnQifQ.t4kvJwiW16AN6brVGhDcsnPZITLKJU3I1faR4Rd3Ywo";
		String URL="http://192.168.1.177:8551/order1/xkMerchantaccount/doAccountMoney?id=10007&accountchange=1&operatstatus=加款";

		OkHttpClient client= new OkHttpClient.Builder().build();
		Request clinetRequest = new Request.Builder()
				.url(URL)
				.addHeader("Authorization","bearer "+token)
				.build();
		try {

			long start=new Date().getTime();
			log.info("开始时间====:"+start);
			int i=0;
			while (i<100){
				Response response = client.newCall(clinetRequest).execute();
				log.info("第"+i+"次请求========"+response.body().string());
				i++;

			}

			long stop=new Date().getTime();
			log.info("结束时间====:"+stop+"======日期:"+new Date().toString());
			long spend=stop-start;

			log.info("用时====:"+spend);



		} catch (IOException e) {
			e.printStackTrace();
		}


	}

}
