package com.example.espserver;

import static spark.Spark.*;

import com.google.gson.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    static class DataEntry {
        long ts;
        Map<String, Float> sensors = new HashMap<>();
        Map<String, Object> metrics = new HashMap<>();
    }

    private static final List<DataEntry> registros = new CopyOnWriteArrayList<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        port(8080);
        staticFiles.location("/public");

        // ---------- POST /datos ----------
        post("/datos", (req, res) -> {
            try {
                JsonObject obj = JsonParser.parseString(req.body()).getAsJsonObject();
                DataEntry entry = new DataEntry();
                entry.ts = obj.get("ts").getAsLong();

                if (obj.has("sensors")) {
                    JsonObject s = obj.getAsJsonObject("sensors");
                    for (String k : s.keySet())
                        entry.sensors.put(k, s.get(k).getAsFloat());
                }

                if (obj.has("metrics")) {
                    JsonObject m = obj.getAsJsonObject("metrics");
                    for (String k : m.keySet()) {
                        JsonElement val = m.get(k);
                        entry.metrics.put(k, val.isJsonPrimitive() ?
                                val.getAsJsonPrimitive().isNumber() ? val.getAsFloat() : val.getAsString() :
                                val.toString());
                    }
                }

                registros.add(entry);
                res.status(200);
                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                res.status(400);
                return "Error parsing JSON";
            }
        });

        // ---------- GET /datos ----------
        get("/datos", (req, res) -> {
            res.type("text/html");
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Datos ESP32</title>")
                .append("<style>")
                .append("table{border-collapse:collapse;width:95%;margin:auto;}th,td{border:1px solid #ccc;padding:8px;text-align:center;}")
                .append("th{background-color:#4CAF50;color:white;}")
                .append("</style></head><body>")
                .append("<h2 style='text-align:center'>Lecturas Recibidas del ESP32</h2>")
                .append("<table><tr><th>Timestamp</th><th>Temp</th><th>Hum</th><th>MQ</th><th>UV</th><th>Vbat</th>")
                .append("<th>I2C Latency (us)</th><th>WiFi RSSI</th><th>Bytes Tx</th><th>Bytes Rx</th><th>Comm Errors</th></tr>");

            for (DataEntry e : registros) {
                html.append("<tr>")
                    .append("<td>").append(e.ts).append("</td>")
                    .append("<td>").append(e.sensors.getOrDefault("temp", Float.NaN)).append("</td>")
                    .append("<td>").append(e.sensors.getOrDefault("hum", Float.NaN)).append("</td>")
                    .append("<td>").append(e.sensors.getOrDefault("mqVolt", Float.NaN)).append("</td>")
                    .append("<td>").append(e.sensors.getOrDefault("uvVolt", Float.NaN)).append("</td>")
                    .append("<td>").append(e.sensors.getOrDefault("vbat", Float.NaN)).append("</td>")
                    .append("<td>").append(e.metrics.getOrDefault("i2cLatencyUs", "")).append("</td>")
                    .append("<td>").append(e.metrics.getOrDefault("wifiRSSI", "")).append("</td>")
                    .append("<td>").append(e.metrics.getOrDefault("wifiBytesSent", "")).append("</td>")
                    .append("<td>").append(e.metrics.getOrDefault("wifiBytesRecv", "")).append("</td>")
                    .append("<td>").append(e.metrics.getOrDefault("commErrors", "")).append("</td>")
                    .append("</tr>");
            }

            html.append("</table><p style='text-align:center'>")
                .append("<a href='/metricas'>Ver Gráficas</a> | ")
                .append("<a href='/estado'>Ver Estado del Sistema</a>")
                .append("</p></body></html>");
            return html.toString();
        });

        // ---------- GET /metricas ----------
        get("/metricas", (req, res) -> {
            res.type("text/html");
            StringBuilder html = new StringBuilder();

            html.append("<html><head><meta charset='UTF-8'><title>Métricas ESP32</title>")
                .append("<script src='https://cdn.jsdelivr.net/npm/chart.js@4.4.1'></script>")
                .append("<style>")
                .append("body{font-family:Arial;background:#f7f7f7;color:#333;text-align:center;}")
                .append("canvas{display:block;margin:30px auto;border:1px solid #ccc;background:white;padding:10px;border-radius:10px;}")
                .append("</style></head><body>")
                .append("<h2>Evolución de Variables</h2>")
                .append("<canvas id='chart' width='900' height='400'></canvas>")
                .append("<p><a href='/datos'>← Volver a datos</a></p>")
                .append("<script>")
                // Convertimos la lista de registros a JSON
                .append("const data = ").append(gson.toJson(registros)).append(";")
                // Creamos etiquetas legibles
                .append("const labels = data.map(e => new Date(e.ts).toLocaleTimeString());")
                // Función segura para evitar errores de propiedades inexistentes
                .append("const safe = (obj, key) => (obj && obj[key] !== undefined ? obj[key] : null);")
                // Configuración del gráfico
                .append("const ctx = document.getElementById('chart').getContext('2d');")
                .append("new Chart(ctx, {")
                .append("type:'line',")
                .append("data:{")
                .append("labels:labels,")
                .append("datasets:[")
                .append("{label:'Temp (°C)',data:data.map(e=>safe(e.sensors,'temp')),borderColor:'red',fill:false,tension:0.2},")
                .append("{label:'Hum (%)',data:data.map(e=>safe(e.sensors,'hum')),borderColor:'blue',fill:false,tension:0.2},")
                .append("{label:'I2C Latency (us)',data:data.map(e=>safe(e.metrics,'i2cLatencyUs')),borderColor:'orange',fill:false,tension:0.2},")
                .append("{label:'WiFi RSSI (dBm)',data:data.map(e=>safe(e.metrics,'wifiRSSI')),borderColor:'green',fill:false,tension:0.2},")
                .append("{label:'Comm Errors',data:data.map(e=>safe(e.metrics,'commErrors')),borderColor:'purple',fill:false,tension:0.2}")
                .append("]},")
                .append("options:{")
                .append("responsive:true,")
                .append("interaction:{mode:'index',intersect:false},")
                .append("plugins:{legend:{position:'bottom'}},")
                .append("scales:{")
                .append("x:{title:{display:true,text:'Hora'},grid:{display:false}},")
                .append("y:{title:{display:true,text:'Valor'},beginAtZero:true}")
                .append("}")
                .append("}")
                .append("});")
                .append("</script></body></html>");

            return html.toString();
        });

        // ---------- GET /estado ----------
        get("/estado", (req, res) -> {
            res.type("text/html");
            Runtime runtime = Runtime.getRuntime();
            long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long totalMem = runtime.totalMemory() / 1024 / 1024;
            double usedPct = (double) usedMem / totalMem * 100;

            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            double load = os.getSystemLoadAverage();
            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            double wifiUp = calcularWiFiUptime();
            double i2cAvg = promedioI2CLatencia();
            long resets = contarReinicios();

            // Colores adaptativos
            String colorRAM = usedPct < 60 ? "#4CAF50" : usedPct < 80 ? "#FFC107" : "#F44336";
            String colorCPU = load < 0 || load < 0.25 ? "#4CAF50" : load < 0.75 ? "#FFC107" : "#F44336";
            String colorI2C = i2cAvg < 10000 ? "#4CAF50" : i2cAvg < 20000 ? "#FFC107" : "#F44336";
            String colorWiFi = wifiUp > 95 ? "#4CAF50" : wifiUp > 85 ? "#FFC107" : "#F44336";

            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Estado del Sistema</title>")
                .append("<meta http-equiv='refresh' content='30'>")
                .append("<style>")
                .append("body{font-family:Arial;text-align:center;background:#f7f7f7;color:#333;}")
                .append(".bar-container{width:70%;margin:10px auto;background:#ddd;border-radius:20px;overflow:hidden;height:25px;}")
                .append(".bar{height:100%;text-align:right;padding-right:10px;color:white;font-weight:bold;transition:width 1s ease;}")
                .append("table{margin:20px auto;border-collapse:collapse;}th,td{padding:8px 12px;border:1px solid #ccc;}")
                .append("</style></head><body>")
                .append("<h2>Dashboard de Rendimiento ESP32</h2>")
                .append("<h4>Actualización cada 30 s</h4><br>");

            // RAM
            html.append("<p><b>RAM usada:</b> ").append(String.format("%.1f", usedPct)).append("%</p>")
                .append("<div class='bar-container'><div class='bar' style='width:")
                .append((int) usedPct).append("%;background:").append(colorRAM).append(";'>")
                .append(String.format("%.1f%%", usedPct)).append("</div></div>");

            // CPU
            double cpuLoad = (load < 0) ? 0 : load;
            html.append("<p><b>Carga CPU (LoadAvg):</b> ").append(String.format("%.2f", cpuLoad)).append("</p>")
                .append("<div class='bar-container'><div class='bar' style='width:")
                .append((int) (cpuLoad * 100)).append("%;background:").append(colorCPU).append(";'>")
                .append(String.format("%.0f%%", cpuLoad * 100)).append("</div></div>");

            // I2C
            html.append("<p><b>I2C Latencia Promedio:</b> ").append(String.format("%.0f µs", i2cAvg)).append("</p>")
                .append("<div class='bar-container'><div class='bar' style='width:")
                .append(Math.min(i2cAvg / 1000, 100)).append("%;background:").append(colorI2C).append(";'>")
                .append(String.format("%.0f µs", i2cAvg)).append("</div></div>");

            // WiFi
            html.append("<p><b>WiFi Uptime:</b> ").append(String.format("%.1f%%", wifiUp)).append("</p>")
                .append("<div class='bar-container'><div class='bar' style='width:")
                .append((int) wifiUp).append("%;background:").append(colorWiFi).append(";'>")
                .append(String.format("%.1f%%", wifiUp)).append("</div></div>");

            // Tabla resumen
            html.append("<h3>Resumen</h3><table><tr><th>Parámetro</th><th>Valor</th><th>Objetivo</th></tr>")
                .append("<tr><td>RAM usada</td><td>").append(String.format("%.1f%%", usedPct)).append("</td><td><60%</td></tr>")
                .append("<tr><td>CPU Idle</td><td>").append(String.format("%.0f%%", 100 - cpuLoad * 100)).append("</td><td>>75%</td></tr>")
                .append("<tr><td>I2C Transmisión</td><td>").append(String.format("%.0f µs", i2cAvg)).append("</td><td><10 000 µs</td></tr>")
                .append("<tr><td>WiFi Uptime</td><td>").append(String.format("%.1f%%", wifiUp)).append("</td><td>>95%</td></tr>")
                .append("<tr><td>Reinicios Inesperados</td><td>").append(resets).append("</td><td>0</td></tr>")
                .append("</table><br><a href='/datos'>← Volver a datos</a>")
                .append("</body></html>");

            return html.toString();
        });
    }

    // ---------- Funciones auxiliares ----------
    private static double promedioI2CLatencia() {
        return registros.stream()
                .mapToDouble(e -> e.metrics.containsKey("i2cLatencyUs") ?
                        Float.parseFloat(e.metrics.get("i2cLatencyUs").toString()) : 0)
                .filter(v -> v > 0)
                .average().orElse(0);
    }

    private static double calcularWiFiUptime() {
        if (registros.isEmpty()) return 100.0;
        long total = registros.size();
        long ok = registros.stream()
                .filter(e -> e.metrics.containsKey("wifiRSSI") &&
                        Float.parseFloat(e.metrics.get("wifiRSSI").toString()) > -90)
                .count();
        return (ok * 100.0) / total;
    }

    private static long contarReinicios() {
        return registros.stream()
                .filter(e -> "POWERON".equals(e.metrics.getOrDefault("resetReason", "")))
                .count() - 1;
    }
}
