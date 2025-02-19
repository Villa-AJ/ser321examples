/*
Combined Web Server in Java.

This server supports:
1) /
   - Serves the main page with a list of available endpoints and usage examples.
2) /random
   - Opens and returns the "www/index.html" page (showing a random image page).
3) /json
   - Returns a JSON object with a random image (header and image URL).
4) /file/filename
   - Checks for a file and returns a placeholder message if found.
5) /multiply?num1=3&num2=4
   - Multiplies two numbers (with error handling).
6) /github?query=users/amehlhase316/repos
   - Fetches GitHub repository data and displays it.
7) /weather?city=Phoenix&units=metric
   - Fetches weather data for the given city. <br>
     <strong>Parameters:</strong>
     - <code>city</code>: Name of the city.
     - <code>units</code>: Either <code>metric</code> or <code>imperial</code>.
8) /countdown?seconds=10&message=Time%27s%20up
   - Displays a countdown timer from the given seconds and shows the provided message when finished.
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebServer {

  // ---------- For /json and /random endpoints (original code) ----------
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();
  // ------------------------------------------------------------------------

  public static void main(String[] args) {
    new WebServer(9000);
  }

  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      System.out.println("Server started on port " + port);
      while (true) {
        sock = server.accept();
        in = sock.getInputStream();
        out = sock.getOutputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (server != null) {
        try {
          server.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public byte[] createResponse(InputStream inStream) {
    byte[] response = null;
    BufferedReader in = null;

    try {
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
      String request = null;
      boolean done = false;
      while (!done) {
        String line = in.readLine();
        System.out.println("Received: " + line);
        if (line == null || line.equals("")) {
          done = true;
        } else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);
          request = line.substring(firstSpace + 2, secondSpace);
        }
      }
      System.out.println("FINISHED PARSING HEADER\n");

      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        StringBuilder builder = new StringBuilder();

        // 1) Root: Serve the main page with endpoint documentation.
        if (request.length() == 0) {
          String page = "";
          try {
            page = new String(readFileInBytes(new File("www/root.html")), StandardCharsets.UTF_8);
          } catch (IOException e) {
            page = "<html><body><h1>Welcome to the WebServer</h1></body></html>";
          }
          // Replace the ${links} placeholder in the root.html with our endpoint documentation.
          page = page.replace("${links}", buildEndpointDocs());
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n\n");
          builder.append(page);

          // 2) /json: (original logic)
        } else if (request.equalsIgnoreCase("json")) {
          int index = random.nextInt(_images.size());
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

          // 3) /random: (original logic)
        } else if (request.equalsIgnoreCase("random")) {
          File file = new File("www/index.html");
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n\n");
          builder.append(new String(readFileInBytes(file), StandardCharsets.UTF_8));

          // 4) /file/filename: (original logic)
        } else if (request.contains("file/")) {
          File file = new File(request.replace("file/", ""));
          if (file.exists()) {
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else {
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("File not found: " + file);
          }

          // 5) /multiply?num1=3&num2=4
        } else if (request.contains("multiply?")) {
          Map<String, String> query_pairs = splitQuery(request.replace("multiply?", ""));
          Integer num1 = null, num2 = null;
          boolean error = false;
          String errorMessage = "";
          try {
            if (!query_pairs.containsKey("num1") || !query_pairs.containsKey("num2")) {
              error = true;
              errorMessage = "Missing parameters. Please provide both 'num1' and 'num2'.";
            } else {
              num1 = Integer.parseInt(query_pairs.get("num1"));
              num2 = Integer.parseInt(query_pairs.get("num2"));
            }
          } catch (NumberFormatException e) {
            error = true;
            errorMessage = "Invalid input. Please provide numeric values for 'num1' and 'num2'.";
          }
          if (error) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<html><body><h2>Error: " + errorMessage + "</h2></body></html>");
          } else {
            Integer result = num1 * num2;
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<html><body><h2>Result: " + result + "</h2></body></html>");
          }

          // 6) /github?query=...
        } else if (request.contains("github?")) {
          Map<String, String> query_pairs = splitQuery(request.replace("github?", ""));
          if (!query_pairs.containsKey("query") || query_pairs.get("query").trim().isEmpty()) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<html><body><h2>Error: Missing 'query' parameter.</h2></body></html>");
          } else {
            String githubQuery = query_pairs.get("query");
            String apiUrl = "https://api.github.com/" + githubQuery;
            try {
              String jsonResponse = fetchURL(apiUrl);
              StringBuilder jsonResult = new StringBuilder();
              jsonResult.append("<html><body><h2>GitHub Repositories</h2><ul>");
              JSONArray repos = new JSONArray(jsonResponse);
              for (int i = 0; i < repos.length(); i++) {
                JSONObject repo = repos.getJSONObject(i);
                String fullName = repo.getString("full_name");
                int id = repo.getInt("id");
                String ownerLogin = repo.getJSONObject("owner").getString("login");
                jsonResult.append("<li><b>Repo:</b> " + fullName + " | <b>ID:</b> " + id + " | <b>Owner:</b> " + ownerLogin + "</li>");
              }
              jsonResult.append("</ul></body></html>");
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append(jsonResult.toString());
            } catch (Exception e) {
              builder.append("HTTP/1.1 500 Internal Server Error\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append("<html><body><h2>Error fetching data from GitHub: " + e.getMessage() + "</h2></body></html>");
            }
          }

          // 7) /weather?city=...&units=...
        } else if (request.startsWith("weather?")) {
          int idx = request.indexOf("?");
          String queryString = request.substring(idx + 1);
          Map<String, String> query_pairs = splitQuery(queryString);
          if (!query_pairs.containsKey("city") || !query_pairs.containsKey("units")) {
            builder.append("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n");
            builder.append("<html><body><h2>Error: Missing 'city' or 'units' parameter. Example: /weather?city=Phoenix&units=metric</h2></body></html>");
          } else {
            String city = query_pairs.get("city");
            String units = query_pairs.get("units");
            if (!units.equalsIgnoreCase("metric") && !units.equalsIgnoreCase("imperial")) {
              builder.append("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n");
              builder.append("<html><body><h2>Error: 'units' must be 'metric' or 'imperial'.</h2></body></html>");
            } else {
              String apiKey = "04ad74aecbc44d3ad8bc53d3a9d7a6fd"; // Replace with your valid OpenWeather API key if needed
              String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=" + units;
              try {
                String jsonResponse = fetchURL(apiUrl);
                JSONObject weatherData = new JSONObject(jsonResponse);
                String description = weatherData.getJSONArray("weather").getJSONObject(0).getString("description");
                double temp = weatherData.getJSONObject("main").getDouble("temp");
                double windSpeed = weatherData.getJSONObject("wind").getDouble("speed");
                builder.append("HTTP/1.1 200 OK\nContent-Type: text/html; charset=utf-8\n\n");
                builder.append("<html><body>");
                builder.append("<h2>Weather in " + city + " (" + units + ")</h2>");
                builder.append("<p>Temperature: " + temp + "°</p>");
                builder.append("<p>Conditions: " + description + "</p>");
                builder.append("<p>Wind Speed: " + windSpeed + " m/s</p>");
                builder.append("</body></html>");
              } catch (Exception e) {
                builder.append("HTTP/1.1 500 Internal Server Error\nContent-Type: text/html; charset=utf-8\n\n");
                builder.append("<html><body><h2>Error fetching weather data: " + e.getMessage() + "</h2></body></html>");
              }
            }
          }

          // 8) /countdown?seconds=...&message=...
        } else if (request.startsWith("countdown?")) {
          int idx = request.indexOf("?");
          String queryString = request.substring(idx + 1);
          Map<String, String> query_pairs = splitQuery(queryString);
          if (!query_pairs.containsKey("seconds") || !query_pairs.containsKey("message")) {
            builder.append("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n");
            builder.append("<html><body><h2>Error: Missing 'seconds' or 'message' parameter. Example: /countdown?seconds=10&message=Time%27s%20up</h2></body></html>");
          } else {
            try {
              int seconds = Integer.parseInt(query_pairs.get("seconds"));
              if (seconds <= 0) throw new NumberFormatException();
              String message = query_pairs.get("message");
              builder.append("HTTP/1.1 200 OK\nContent-Type: text/html; charset=utf-8\n\n");
              builder.append("<html><body><h2>Countdown:</h2><ul>");
              for (int i = seconds; i >= 0; i--) {
                builder.append("<li>" + i + "</li>");
              }
              builder.append("</ul>");
              builder.append("<h3>" + message + "</h3>");
              builder.append("</body></html>");
            } catch (NumberFormatException e) {
              builder.append("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n");
              builder.append("<html><body><h2>Error: 'seconds' must be a positive number.</h2></body></html>");
            }
          }

        } else {
          builder.append("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n");
          builder.append("I am not sure what you want me to do...");
        }

        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  // -------------------- Helper Methods --------------------

  /**
   * Parses a query string (e.g., "num1=3&num2=4") into a Map.
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
              URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    return query_pairs;
  }

  /**
   * Builds HTML documentation for the available endpoints.
   */
  public static String buildEndpointDocs() {
    StringBuilder sb = new StringBuilder();
    sb.append("<h2>Available Endpoints</h2>");
    sb.append("<ul>");
    sb.append("<li><strong>Root:</strong> / - Displays this help page.</li>");
    sb.append("<li><strong>Random Image (HTML):</strong> /random - Displays a random image page.</li>");
    sb.append("<li><strong>Random Image (JSON):</strong> /json - Returns JSON for a random image.</li>");
    sb.append("<li><strong>File Serving:</strong> /file/filename - Returns file contents if it exists.</li>");
    sb.append("<li><strong>Multiply:</strong> /multiply?num1=3&num2=4 - Multiplies two numbers.</li>");
    sb.append("<li><strong>GitHub:</strong> /github?query=users/amehlhase316/repos - Fetches GitHub repository data.</li>");
    sb.append("<li><strong>Weather:</strong> /weather?city=Phoenix&units=metric - Fetches weather data for a city. ");
    sb.append("Parameters: <em>city</em> (e.g., Phoenix) and <em>units</em> (<code>metric</code> or <code>imperial</code>).</li>");
    sb.append("<li><strong>Countdown Timer:</strong> /countdown?seconds=10&message=Time%27s%20up - Displays a countdown timer from the specified seconds and shows the provided message when finished.</li>");
    sb.append("</ul>");
    return sb.toString();
  }

  /**
   * Reads in a file and returns its contents as a byte array.
   */
  public static byte[] readFileInBytes(File f) throws IOException {
    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());
    byte[] buffer = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();
    byte[] result = data.toByteArray();
    data.close();
    return result;
  }

  /**
   * Fetches content from a URL.
   */
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // 20 seconds timeout
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        int ch;
        while ((ch = br.read()) != -1) {
          sb.append((char) ch);
        }
        br.close();
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in URL request:" + ex.getMessage());
    }
    return sb.toString();
  }
}