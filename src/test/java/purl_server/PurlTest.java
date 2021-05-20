/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package purl_server;

public class PurlTest {/*
	private String host ="http://localhost:8080";
	private String adminHost ="http://localhost:8080/api/purl";
	private String purlPath = "/test/test3/";
	
	@Before
	public void init() {
		PurlServerWebApp.main(new String[] {});
	}
	
	@Test
	public void test() {
		createPurl();
		checkCreatedPurl();
		modifyPurl();
		checkModifiedPurl();
		deletePurl();
	}

	private void createPurl() {
		int statusCode = -1;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(adminHost + purlPath);

			String json = "{\"type\":\"partial_302\",\"target\":\"http://matrikel.uni-rostock.de/id/\"}";
			StringEntity entity = new StringEntity(json);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("****", "****");
			httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));

			CloseableHttpResponse response = client.execute(httpPost);
			statusCode = response.getStatusLine().getStatusCode();

			String content = EntityUtils.toString(entity);
			System.out.println(content);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		assertEquals("Statuscode must be 201!", 201, statusCode);
	}

	private void checkCreatedPurl() {
		int statusCode = -1;
		String location = "";
		try (CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();) {
			
			HttpGet httpGet = new HttpGet(host + purlPath + "123");
			CloseableHttpResponse response = client.execute(httpGet);
			if(response.getFirstHeader("Location") != null) {
				location = response.getFirstHeader("Location").getValue();
			}
			statusCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		assertEquals("Statuscode must be 302!", 302, statusCode);
		assertEquals("Location header must contain 'http://matrikel.uni-rostock.de/id/{path}'", "http://matrikel.uni-rostock.de/id/123", location);
	}
	
	private void modifyPurl() {
		int statusCode = -1;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPut httpPut = new HttpPut(adminHost + purlPath);

			String json = "{\"type\":\"302\",\"target\":\"http://test333.de/\"}";
			StringEntity entity = new StringEntity(json);
			httpPut.setEntity(entity);
			httpPut.setHeader("Accept", "application/json");
			httpPut.setHeader("Content-type", "application/json");

			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			httpPut.addHeader(new BasicScheme().authenticate(creds, httpPut, null));

			CloseableHttpResponse response = client.execute(httpPut);
			statusCode = response.getStatusLine().getStatusCode();

			String content = EntityUtils.toString(entity);
			System.out.println(content);
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
				System.out.println(buffer.lines().collect(Collectors.joining("\n")));
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		assertEquals("Statuscode must be 200!", 200, statusCode);
	}
	
	private void checkModifiedPurl() {
		int statusCode = -1;
		String location = "";
		try (CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();) {
			
			HttpGet httpGet = new HttpGet(host + purlPath + "123");
			CloseableHttpResponse response = client.execute(httpGet);
			if(response.getFirstHeader("Location") != null) {
				location = response.getFirstHeader("Location").getValue();
			}
			statusCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		assertEquals("Statuscode must be 302!", 302, statusCode);
		assertEquals("Location header must contain 'http://test333.de/{path}'", "http://test333.de/123", location);
	}

	private void deletePurl() {
		int statusCode = -1;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpDelete httpDelete = new HttpDelete(adminHost + purlPath);

			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
			httpDelete.addHeader(new BasicScheme().authenticate(creds, httpDelete, null));

			CloseableHttpResponse response = client.execute(httpDelete);
			statusCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		assertEquals("Statuscode must be 200!", 200, statusCode);
	}*/
}
