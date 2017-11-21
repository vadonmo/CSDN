package com.vadon;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSDN {

	public static void main(String[] args) throws IOException,
			InterruptedException {

		CSDN csdn = new CSDN();
		long start = new Date().getTime();
		csdn.visitBlog();
		long end = new Date().getTime();
		System.out.println("消耗时间:" + (end - start));
	}

	public static InputStream doGet(String urlstr) throws IOException {
		URL url = new URL(urlstr);
		CookieHandler.setDefault(new CookieManager(null,
				CookiePolicy.ACCEPT_ALL));
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setInstanceFollowRedirects(false);
		conn.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
		if (conn.getResponseCode() != 200) {
			return new InputStream() {
				@Override
				public int read() throws IOException {
					return 0;
				}
			};
		}
		InputStream inputStream = conn.getInputStream();
		return inputStream;
	}

	public static String inputStreamToString(InputStream is, String charset)
			throws IOException {
		byte[] bytes = new byte[1024];
		int byteLength = 0;
		StringBuffer sb = new StringBuffer();
		while ((byteLength = is.read(bytes)) != -1) {
			sb.append(new String(bytes, 0, byteLength, charset));
		}
		return sb.toString();
	}

	private String uid = "vadonmo";
	private String csdnBlogUrl = "http://blog.csdn.net/";
	private String firstBlogListPageUrl = "http://blog.csdn.net/" + uid; // 博客主页
	private String nextPagePanner = "<a href=\"/" + uid
			+ "/article/list/[0-9]{1,10}\">下一页</a>"; // 下一页的正则表达式
	private String nextPageUrlPanner = "/" + uid + "/article/list/[0-9]{1,10}"; // 下一页Url的正则表达式
	private String artlUrl = "/" + uid + "/article/details/[0-9]{8,8}"; // 博客utl的正则表达式

	private Set<String> blogListPageUrls = new TreeSet<>();

	// private Set<String> blogUrls = new TreeSet<>();

	public void visitBlog() throws IOException, InterruptedException {
		Set<String> blogUrls = addBlogUrl();
		// blogUrls.add("vadonmo/article/details/78513487");
		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3); 
		blogUrls.add("vadonmo/article/details/78578161");
		for (String blogUrl : blogUrls) {
			final String artlUrl = csdnBlogUrl + blogUrl;
			fixedThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					int count = 0;
					while (count < 100) {
						InputStream is;
						try {
							is = doGet(artlUrl);
							if (is != null) {
								System.out.printf("%s访问成功%d次\n", artlUrl,
										++count);
							}
							is.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}
	}

	/**
	 * @return
	 * @throws IOException
	 *             加载所有的bolg地址
	 */
	public Set<String> addBlogUrl() throws IOException {
		blogListPageUrls.add(firstBlogListPageUrl);
		addBlogListPageUrl(firstBlogListPageUrl, blogListPageUrls);
		Set<String> blogUrls = new TreeSet<String>();
		for (String bolgListUrl : blogListPageUrls) {
			blogUrls.addAll(addBlogUrl(bolgListUrl));
		}
		return blogUrls;
	}

	public static byte[] readStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = -1;
		while ((len = inStream.read(buffer)) != -1) {
			outSteam.write(buffer, 0, len);
		}
		outSteam.close();
		inStream.close();
		return outSteam.toByteArray();
	}

	/**
	 * 通过下一页，遍历所有博客目录页面链接
	 * 
	 * @param pageUrl
	 * @param pagelistUrls
	 * @throws IOException
	 */
	public void addBlogListPageUrl(String pageUrl, Set<String> pagelistUrls)
			throws IOException {
		InputStream is = doGet(pageUrl);
		byte[] bytes = new byte[1024];
		int byteLength = 0;
		StringBuffer sb = new StringBuffer();
		try {
			bytes = readStream(is);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String pageStr = sb.toString();
		is.close();
		Pattern nextPagePattern = Pattern.compile(nextPagePanner);
		Matcher nextPagematcher = nextPagePattern.matcher(pageStr);
		if (nextPagematcher.find()) {
			nextPagePattern = Pattern.compile(nextPageUrlPanner);
			nextPagematcher = nextPagePattern.matcher(nextPagematcher.group(0));
			if (nextPagematcher.find()) {
				pagelistUrls.add(csdnBlogUrl + nextPagematcher.group(0));
				System.out.println("成功添加博客列表页面地址：" + csdnBlogUrl
						+ nextPagematcher.group(0));
				addBlogListPageUrl(csdnBlogUrl + nextPagematcher.group(0),
						pagelistUrls);
			}
		}
	}

	/**
	 * 添加搜索博客目录的博客链接
	 * 
	 * @param blogListURL
	 *            博客目录地址
	 * @param artlUrls
	 *            存放博客访问地址的集合
	 * @throws IOException
	 */
	public Set<String> addBlogUrl(String blogListURL) throws IOException {
		Set<String> artlUrls = new TreeSet<String>();
		InputStream is = doGet(blogListURL);
		String pageStr = inputStreamToString(is, "UTF-8");
		is.close();
		Pattern pattern = Pattern.compile(artlUrl);
		Matcher matcher = pattern.matcher(pageStr);
		while (matcher.find()) {
			String e = matcher.group(0);
			System.out.println("成功添加博客地址：" + e);
			artlUrls.add(e);
		}
		return artlUrls;
	}

	public static InputStream sendGet(String url) {
		BufferedReader in = null;
		try {
			String urlNameString = url;
			URL realUrl = new URL(urlNameString);
			// 打开和URL之间的连接
			HttpURLConnection connection = (HttpURLConnection) realUrl
					.openConnection();
			// 设置通用的请求属性
			connection.setRequestProperty("Host", "blog.csdn.net");
			connection
					.setRequestProperty("Accept",
							"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language",
					"zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			connection.setRequestProperty("Referer",
					"http://blog.csdn.net/vadonmo/");
			connection
					.setRequestProperty(
							"Cookie",
							"uuid_tt_dd=5615260459071786878_20170914; "
									+ "UN=vadonmo; UE=\"vadonmo@126.com\"; BT=1510557952773; "
									+ "Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1510532517; bdshare_firstime=1506734534554; "
									+ "__utma=17226283.469766013.1506734935.1506734935.1508744961.2;"
									+ " __utmz=17226283.1506734935.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); "
									+ "ADHOC_MEMBERSHIP_CLIENT_ID1.0=e065c740-fc96-7593-1eff-b730cb33a2ed; "
									+ "kd_user_id=6681bcd3-21ee-4187-ae83-2d963aabc9c1; uuid=0f39ea1a-bea3-4c34-9211-ae4952a2bebb; "
									+ "dc_tos=ozef5c; dc_session_id=1510647381104; aliyungf_tc=AQAAABtXEwwMgA4AjlKAcYL9acuTDo18");
			connection
					.setRequestProperty("User-Agent",
							"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:55.0) Gecko/20100101 Firefox/55.0");
			connection.setRequestProperty("DNT", "1");
			connection.setRequestProperty("Connection", "keep-alive");
			connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
			connection.setRequestProperty("Pragma", "no-cache");
			connection.setRequestProperty("Cache-Control", "no-cache");
			// 建立实际的连接
			connection.connect();
			InputStream inputStream = connection.getInputStream();
			return inputStream;
		} catch (Exception e) {
			System.out.println("发送GET请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输入流
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		// return result;
		return null;
	}
}
